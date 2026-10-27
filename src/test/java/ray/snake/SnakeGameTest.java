package ray.snake;

import java.util.List;
import java.util.Random;

import static ray.snake.SnakeGame.Direction.*;
import static ray.snake.SnakeGame.State.*;

/** Dependency-free test runner; a failed check exits nonzero without requiring -ea. */
public final class SnakeGameTest {
    private static int passed;

    public static void main(String[] args) {
        run("initial state and start", SnakeGameTest::initialState);
        run("normal movement", SnakeGameTest::movement);
        run("food grows snake and scores", SnakeGameTest::eating);
        run("reverse and buffered turns rejected", SnakeGameTest::turns);
        run("pause freezes movement and input", SnakeGameTest::pause);
        run("wall collision ends round", SnakeGameTest::wallCollision);
        run("body collision ends round", SnakeGameTest::selfCollision);
        run("vacating tail is legal", SnakeGameTest::vacatingTail);
        run("full board is a win", SnakeGameTest::win);
        run("reset clears finished round", SnakeGameTest::reset);
        run("food always uses a free cell", SnakeGameTest::foodPlacement);
        run("snake snapshots are immutable", SnakeGameTest::snapshots);
        System.out.println("PASS: " + passed + " game-rule tests");
    }

    private static SnakeGame game() {
        return new SnakeGame(12, 10, new Random(42));
    }

    private static SnakeGame.Cell cell(int x, int y) {
        return new SnakeGame.Cell(x, y);
    }

    private static void initialState() {
        SnakeGame game = game();
        check(game.state() == READY, "starts ready");
        check(game.snake().size() == 3 && game.score() == 0, "initial length and score");
        check(game.direction() == RIGHT, "initial direction");
        List<SnakeGame.Cell> before = game.snake();
        game.step();
        check(game.snake().equals(before), "ready does not move");
        game.start();
        check(game.state() == RUNNING, "start enters running state");
    }

    private static void movement() {
        SnakeGame game = new SnakeGame(12, 10, new Random(1),
                List.of(cell(6, 5), cell(5, 5), cell(4, 5)), RIGHT, cell(0, 0));
        game.start();
        game.step();
        check(game.snake().equals(List.of(cell(7, 5), cell(6, 5), cell(5, 5))), "head and tail advance");
        check(game.score() == 0, "moving does not score");
    }

    private static void eating() {
        SnakeGame game = new SnakeGame(12, 10, new Random(1),
                List.of(cell(6, 5), cell(5, 5), cell(4, 5)), RIGHT, cell(7, 5));
        game.start();
        game.step();
        check(game.score() == 10, "food awards ten points");
        check(game.snake().size() == 4, "food adds a segment");
        check(game.snake().get(0).equals(cell(7, 5)), "head reaches food");
        check(game.food() != null && !game.snake().contains(game.food()), "new food avoids body");
    }

    private static void turns() {
        SnakeGame game = game();
        game.start();
        check(!game.turn(LEFT), "cannot reverse");
        check(!game.turn(RIGHT), "same-direction key is not a turn");
        check(game.turn(UP), "first valid turn accepted");
        check(!game.turn(LEFT), "second turn before movement rejected");
        game.step();
        check(game.snake().get(0).equals(cell(6, 4)), "only first turn applied");
        check(game.turn(LEFT), "new tick accepts a turn");
    }

    private static void pause() {
        SnakeGame game = game();
        game.start();
        game.togglePause();
        List<SnakeGame.Cell> before = game.snake();
        game.step();
        check(game.state() == PAUSED && game.snake().equals(before), "paused state freezes snake");
        check(!game.turn(UP), "paused keys do not change direction");
        game.togglePause();
        game.step();
        check(game.state() == RUNNING && !game.snake().equals(before), "resume moves again");
    }

    private static void wallCollision() {
        SnakeGame game = new SnakeGame(6, 4, new Random(1),
                List.of(cell(5, 2), cell(4, 2), cell(3, 2)), RIGHT, cell(0, 0));
        game.start();
        game.step();
        check(game.state() == GAME_OVER, "wall ends game");
        List<SnakeGame.Cell> before = game.snake();
        game.step();
        check(game.snake().equals(before), "finished game does not move");
    }

    private static void selfCollision() {
        SnakeGame game = new SnakeGame(5, 4, new Random(1),
                List.of(cell(2, 1), cell(2, 2), cell(1, 2), cell(1, 1), cell(1, 0)),
                UP, cell(4, 0));
        game.start();
        game.turn(LEFT);
        game.step();
        check(game.state() == GAME_OVER, "body collision ends game");
    }

    private static void vacatingTail() {
        SnakeGame game = new SnakeGame(5, 4, new Random(1),
                List.of(cell(2, 1), cell(2, 2), cell(1, 2), cell(1, 1)), UP, cell(4, 0));
        game.start();
        game.turn(LEFT);
        game.step();
        check(game.state() == RUNNING, "old tail square is available");
        check(game.snake().get(0).equals(cell(1, 1)), "head moved to old tail");
        check(game.snake().size() == 4, "length unchanged");
    }

    private static void win() {
        SnakeGame game = new SnakeGame(4, 2, new Random(1),
                List.of(cell(2, 0), cell(1, 0), cell(0, 0), cell(0, 1), cell(1, 1), cell(2, 1), cell(3, 1)),
                RIGHT, cell(3, 0));
        game.start();
        game.step();
        check(game.state() == WON, "filling board wins");
        check(game.food() == null && game.snake().size() == 8, "full board has no food");
        check(game.score() == 10, "winning food scores");
    }

    private static void reset() {
        SnakeGame game = new SnakeGame(4, 2, new Random(1),
                List.of(cell(2, 0), cell(1, 0), cell(0, 0)), RIGHT, cell(3, 0));
        game.start();
        game.step();
        game.step();
        check(game.state() == GAME_OVER && game.score() == 10, "round finished with score");
        game.reset();
        check(game.state() == READY && game.score() == 0 && game.snake().size() == 3, "reset restores round");
        check(game.food() != null && !game.snake().contains(game.food()), "reset places valid food");
    }

    private static void foodPlacement() {
        for (int seed = 0; seed < 200; seed++) {
            SnakeGame game = new SnakeGame(4, 2, new Random(seed));
            check(game.food().x() >= 0 && game.food().x() < game.columns(), "food x in bounds");
            check(game.food().y() >= 0 && game.food().y() < game.rows(), "food y in bounds");
            check(!game.snake().contains(game.food()), "food avoids initial body");
        }
    }

    private static void snapshots() {
        SnakeGame game = game();
        List<SnakeGame.Cell> snapshot = game.snake();
        boolean immutable = false;
        try {
            snapshot.clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        check(immutable, "snapshot is immutable");
        game.start();
        game.step();
        check(snapshot.get(0).equals(cell(6, 5)), "snapshot does not change with game");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS " + name);
    }
}
