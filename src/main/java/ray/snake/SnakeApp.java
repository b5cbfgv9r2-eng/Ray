package ray.snake;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Desktop entry point. The game and all Swing updates run on the event thread. */
public final class SnakeApp {
    private static final Color BACKGROUND = new Color(12, 18, 29);
    private static final Color SURFACE = new Color(20, 30, 44);
    private static final Color TEXT = new Color(232, 242, 247);
    private static final Color MUTED = new Color(147, 164, 184);
    private static final Color MINT = new Color(91, 236, 172);
    private static final int CELL = 22;

    private enum Difficulty {
        EASY("Easy", 160), NORMAL("Normal", 110), FAST("Fast", 75);
        final String title;
        final int delay;
        Difficulty(String title, int delay) { this.title = title; this.delay = delay; }
        @Override public String toString() { return title; }
    }

    private final SnakeGame game = new SnakeGame(28, 22, new Random());
    private final HighScoreStore scores;
    private final JFrame frame = new JFrame("Ray Snake · 贪吃蛇");
    private final JLabel score = label("0", 28, TEXT);
    private final JLabel best = label("0", 28, MINT);
    private final JLabel status = label("Choose a pace. Make your next move.", 12, MUTED);
    private final JButton play = button("Start / Enter", true);
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final Board board = new Board();
    private final Timer timer;
    private Difficulty pace = Difficulty.NORMAL;
    private boolean changingDifficulty;

    private SnakeApp() {
        String directory = System.getProperty("ray.snake.dataDir");
        scores = new HighScoreStore(directory == null
                ? Path.of(System.getProperty("user.home"), ".ray-snake") : Path.of(directory));
        timer = new Timer(pace.delay, event -> {
            game.step();
            refresh();
        });
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 26, 20, 26));

        JPanel header = panel(new BorderLayout(18, 0));
        JPanel title = panel(new GridLayout(2, 1, 0, 5));
        title.add(label("RAY / SNAKE", 27, TEXT));
        title.add(label("A little focus. A longer snake.", 12, MUTED));
        header.add(title, BorderLayout.WEST);
        JPanel counters = panel(new GridLayout(1, 2, 12, 0));
        counters.add(counter("SCORE", score));
        counters.add(counter("BEST · THIS PACE", best));
        header.add(counters, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        root.add(board, BorderLayout.CENTER);

        JPanel footer = panel(new BorderLayout(0, 12));
        JPanel controls = panel(new BorderLayout(12, 0));
        JPanel buttons = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        play.setName("play");
        play.addActionListener(event -> togglePlay());
        JButton restart = button("Restart / R", false);
        restart.setName("restart");
        restart.addActionListener(event -> restart());
        buttons.add(play);
        buttons.add(restart);
        controls.add(buttons, BorderLayout.WEST);
        difficulty.setSelectedItem(pace);
        difficulty.setName("difficulty");
        difficulty.setFont(font(13, Font.BOLD));
        difficulty.setFocusable(false);
        difficulty.setToolTipText("Choose a pace before starting a round.");
        difficulty.addActionListener(event -> {
            if (changingDifficulty) return;
            Difficulty selected = (Difficulty) difficulty.getSelectedItem();
            boolean active = game.state() == SnakeGame.State.RUNNING || game.state() == SnakeGame.State.PAUSED;
            if (active) {
                // Keep the current round; select a new pace after it ends or before starting.
                changingDifficulty = true;
                difficulty.setSelectedItem(pace);
                changingDifficulty = false;
                return;
            }
            pace = selected;
            game.reset();
            refresh();
        });
        controls.add(difficulty, BorderLayout.EAST);
        footer.add(controls, BorderLayout.NORTH);
        JPanel hints = panel(new GridLayout(2, 1, 0, 8));
        hints.add(label("MOVE  ↑ ↓ ← →  /  W A S D       PAUSE  Space       RESTART  R", 12, MUTED));
        hints.add(status);
        footer.add(hints, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);
        frame.setContentPane(root);
        bindKeys(root);
        frame.addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent event) {
                if (game.state() == SnakeGame.State.RUNNING) {
                    game.togglePause();
                    refresh();
                }
            }
        });
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        refresh();
    }

    public static void main(String[] arguments) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Ray Snake needs a desktop display. Run the JAR on a desktop with Java 17+, or use ./test.sh for headless tests.");
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> new SnakeApp().frame.setVisible(true));
    }

    private void togglePlay() {
        switch (game.state()) {
            case READY -> game.start();
            case RUNNING, PAUSED -> game.togglePause();
            case GAME_OVER, WON -> { game.reset(); game.start(); }
        }
        refresh();
    }

    private void restart() {
        scores.record(pace.name(), game.score());
        game.reset();
        game.start();
        refresh();
    }

    private void refresh() {
        scores.record(pace.name(), game.score());
        score.setText(Integer.toString(game.score()));
        best.setText(Integer.toString(scores.best(pace.name())));
        play.setText(switch (game.state()) {
            case READY -> "Start / Enter";
            case RUNNING -> "Pause / Space";
            case PAUSED -> "Resume / Space";
            case GAME_OVER, WON -> "Play again / Enter";
        });
        boolean active = game.state() == SnakeGame.State.RUNNING || game.state() == SnakeGame.State.PAUSED;
        difficulty.setEnabled(!active);
        status.setText(!scores.persistent() ? "Best score kept for this session; local storage is unavailable."
                : switch (game.state()) {
                    case READY -> "Pick Easy, Normal or Fast, then press Enter.";
                    case RUNNING -> "Eat the coral dots. Avoid the walls and your own tail.";
                    case PAUSED -> "Take your time. Press Space when you are ready.";
                    case GAME_OVER -> "Nice run! Press Enter for another round. Best scores saved locally.";
                    case WON -> "Perfect board! You filled every cell. Press Enter to play again.";
                });
        if (game.state() == SnakeGame.State.RUNNING) {
            timer.setDelay(pace.delay);
            if (!timer.isRunning()) { timer.setInitialDelay(pace.delay); timer.start(); }
        } else {
            timer.stop();
        }
        board.repaint();
    }

    private void bindKeys(JComponent root) {
        bind(root, "ENTER", "play", this::togglePlay);
        bind(root, "SPACE", "pause", this::togglePlay);
        bind(root, "R", "restart", this::restart);
        for (String key : new String[]{"UP", "W"}) bind(root, key, key, () -> turn(SnakeGame.Direction.UP));
        for (String key : new String[]{"DOWN", "S"}) bind(root, key, key, () -> turn(SnakeGame.Direction.DOWN));
        for (String key : new String[]{"LEFT", "A"}) bind(root, key, key, () -> turn(SnakeGame.Direction.LEFT));
        for (String key : new String[]{"RIGHT", "D"}) bind(root, key, key, () -> turn(SnakeGame.Direction.RIGHT));
    }

    private void turn(SnakeGame.Direction direction) { game.turn(direction); }

    private static void bind(JComponent root, String key, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    private static JPanel panel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static Font font(int size, int style) { return new Font(Font.SANS_SERIF, style, size); }
    private static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, Font.PLAIN));
        label.setForeground(color);
        return label;
    }

    private static JPanel counter(String name, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        panel.add(label(name, 10, MUTED), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private static JButton button(String title, boolean primary) {
        JButton button = new JButton(title);
        button.setFont(font(13, Font.BOLD));
        button.setBackground(primary ? MINT : SURFACE);
        button.setForeground(primary ? BACKGROUND : TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? MINT : new Color(52, 68, 85)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setOpaque(true);
        return button;
    }

    private final class Board extends JPanel {
        private static final long serialVersionUID = 1L;

        Board() {
            setPreferredSize(new Dimension(game.columns() * CELL, game.rows() * CELL));
            setBackground(SURFACE);
            setName("board");
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, SURFACE, getWidth(), getHeight(), new Color(14, 25, 37)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 255, 255, 9));
            for (int x = 0; x <= getWidth(); x += CELL) g.drawLine(x, 0, x, getHeight());
            for (int y = 0; y <= getHeight(); y += CELL) g.drawLine(0, y, getWidth(), y);
            SnakeGame.Cell food = game.food();
            if (food != null) {
                int x = food.x() * CELL, y = food.y() * CELL;
                g.setColor(new Color(255, 116, 128, 25));
                g.fillOval(x - 4, y - 4, CELL + 8, CELL + 8);
                g.setColor(new Color(255, 116, 128));
                g.fillOval(x + 4, y + 4, CELL - 8, CELL - 8);
                g.setColor(new Color(255, 217, 211));
                g.fillOval(x + 7, y + 6, 4, 4);
            }
            var body = game.snake();
            for (int i = body.size() - 1; i >= 0; i--) {
                SnakeGame.Cell cell = body.get(i);
                int x = cell.x() * CELL, y = cell.y() * CELL;
                g.setColor(i == 0 ? MINT : new Color(49, 170, 126));
                g.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 8, 8);
                if (i == 0) drawEyes(g, x, y);
            }
            g.setColor(new Color(55, 77, 94));
            g.setStroke(new BasicStroke(2));
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            if (game.state() != SnakeGame.State.RUNNING) drawOverlay(g);
            g.dispose();
        }

        private void drawEyes(Graphics2D g, int x, int y) {
            g.setColor(BACKGROUND);
            switch (game.direction()) {
                case RIGHT -> { g.fillOval(x + 13, y + 5, 4, 4); g.fillOval(x + 13, y + 13, 4, 4); }
                case LEFT -> { g.fillOval(x + 5, y + 5, 4, 4); g.fillOval(x + 5, y + 13, 4, 4); }
                case UP -> { g.fillOval(x + 5, y + 5, 4, 4); g.fillOval(x + 13, y + 5, 4, 4); }
                case DOWN -> { g.fillOval(x + 5, y + 13, 4, 4); g.fillOval(x + 13, y + 13, 4, 4); }
            }
        }

        private void drawOverlay(Graphics2D g) {
            g.setColor(new Color(7, 13, 23, 200));
            g.fillRect(0, 0, getWidth(), getHeight());
            String heading = switch (game.state()) {
                case READY -> "ONE MORE BITE.";
                case PAUSED -> "TAKE A BREATHER.";
                case GAME_OVER -> "A GOOD RUN.";
                case WON -> "PERFECTLY PLAYED.";
                default -> "";
            };
            String subheading = switch (game.state()) {
                case READY -> "Eat. Grow. Stay curious.";
                case PAUSED -> "Your snake will wait for you.";
                case GAME_OVER -> "Score  " + game.score() + "    /    Best  " + scores.best(pace.name());
                case WON -> "Every cell is yours. Score  " + game.score();
                default -> "";
            };
            center(g, "R A Y   /   S N A K E", getHeight() / 2 - 62, 12, MINT);
            center(g, heading, getHeight() / 2 - 14, 32, TEXT);
            center(g, subheading, getHeight() / 2 + 20, 15, MUTED);
            center(g, game.state() == SnakeGame.State.PAUSED ? "SPACE TO RESUME" : "PRESS ENTER TO PLAY",
                    getHeight() / 2 + 70, 12, MINT);
        }

        private void center(Graphics2D g, String text, int y, int size, Color color) {
            g.setFont(font(size, Font.BOLD));
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(color);
            g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
        }
    }
}
