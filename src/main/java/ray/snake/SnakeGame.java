package ray.snake;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Pure game rules; all UI calls are made on the Swing event thread. */
public final class SnakeGame {
    public record Cell(int x, int y) { }

    public enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        private boolean opposite(Direction other) {
            return dx == -other.dx && dy == -other.dy;
        }
    }

    public enum State { READY, RUNNING, PAUSED, GAME_OVER, WON }

    private final int columns;
    private final int rows;
    private final Random random;
    private final ArrayDeque<Cell> snake = new ArrayDeque<>();
    private Direction direction;
    private Cell food;
    private State state;
    private int score;
    private boolean turnedThisTick;

    public SnakeGame(int columns, int rows, Random random) {
        if (columns < 4 || rows < 2) {
            throw new IllegalArgumentException("Board must be at least 4 by 2 cells");
        }
        this.columns = columns;
        this.rows = rows;
        this.random = Objects.requireNonNull(random, "random");
        reset();
    }

    // Deterministic scenario constructor for rules tests, without exposing mutable state.
    SnakeGame(int columns, int rows, Random random, List<Cell> body,
              Direction heading, Cell initialFood) {
        this(columns, rows, random);
        if (body.isEmpty() || new HashSet<>(body).size() != body.size()
                || body.stream().anyMatch(cell -> !inside(cell))
                || initialFood == null || !inside(initialFood) || body.contains(initialFood)) {
            throw new IllegalArgumentException("Invalid starting position");
        }
        snake.clear();
        snake.addAll(body);
        direction = Objects.requireNonNull(heading, "heading");
        food = initialFood;
    }

    public void reset() {
        snake.clear();
        int x = columns / 2;
        int y = rows / 2;
        snake.addLast(new Cell(x, y));
        snake.addLast(new Cell(x - 1, y));
        snake.addLast(new Cell(x - 2, y));
        direction = Direction.RIGHT;
        score = 0;
        turnedThisTick = false;
        state = State.READY;
        spawnFood();
    }

    public void start() {
        if (state == State.READY) {
            state = State.RUNNING;
        }
    }

    public void togglePause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
        } else if (state == State.PAUSED) {
            state = State.RUNNING;
        }
    }

    /** Accept at most one turn between movement steps, preventing rapid-key reversals. */
    public boolean turn(Direction nextDirection) {
        Objects.requireNonNull(nextDirection, "nextDirection");
        if (state != State.RUNNING || turnedThisTick || nextDirection == direction
                || nextDirection.opposite(direction)) {
            return false;
        }
        direction = nextDirection;
        turnedThisTick = true;
        return true;
    }

    public void step() {
        if (state != State.RUNNING) {
            return;
        }
        turnedThisTick = false;
        Cell head = snake.getFirst();
        Cell next = new Cell(head.x() + direction.dx, head.y() + direction.dy);
        boolean eating = next.equals(food);
        boolean bodyCollision = snake.contains(next)
                && (eating || !next.equals(snake.getLast()));
        if (!inside(next) || bodyCollision) {
            state = State.GAME_OVER;
            return;
        }

        // Remove the old tail first so a move into its vacated cell is legal.
        if (!eating) {
            snake.removeLast();
        }
        snake.addFirst(next);
        if (eating) {
            score += 10;
            spawnFood();
        }
    }

    private boolean inside(Cell cell) {
        return cell.x() >= 0 && cell.x() < columns && cell.y() >= 0 && cell.y() < rows;
    }

    private void spawnFood() {
        int emptyCount = columns * rows - snake.size();
        if (emptyCount == 0) {
            food = null;
            state = State.WON;
            return;
        }
        int selected = random.nextInt(emptyCount);
        HashSet<Cell> occupied = new HashSet<>(snake);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                Cell candidate = new Cell(x, y);
                if (!occupied.contains(candidate) && selected-- == 0) {
                    food = candidate;
                    return;
                }
            }
        }
        throw new IllegalStateException("No free cell found");
    }

    public State state() { return state; }
    public int score() { return score; }
    public List<Cell> snake() { return List.copyOf(snake); }
    public Cell food() { return food; }
    public Direction direction() { return direction; }
    public int columns() { return columns; }
    public int rows() { return rows; }
}
