package ray.snake;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Optional display test: real keyboard events, on-screen rendering and button actions. */
public final class SnakeUiTest {
    private static JFrame frame;

    public static void main(String[] arguments) {
        try {
            run();
            System.exit(0);
        } catch (Exception | AssertionError error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("A display is required; run under Xvfb or a desktop.");
        }
        Path scoreDirectory = Files.createTempDirectory("ray-snake-ui-");
        System.setProperty("ray.snake.dataDir", scoreDirectory.toString());
        try {
            SnakeApp.main(new String[0]);
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JFrame found && found.isVisible()) frame = found;
                }
            });
            check(frame != null, "game window opens");
            Robot robot = new Robot();
            robot.setAutoDelay(35);
            robot.waitForIdle();
            JButton play = component("play", JButton.class);
            JButton restart = component("restart", JButton.class);
            JComboBox<?> pace = component("difficulty", JComboBox.class);
            JComponent board = component("board", JComponent.class);
            Path preview = Path.of("build", "snake-preview.png");
            SwingUtilities.invokeAndWait(() -> {
                BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics();
                frame.paint(graphics);
                graphics.dispose();
                try { ImageIO.write(image, "png", preview.toFile()); }
                catch (java.io.IOException error) { throw new RuntimeException(error); }
                pace.setSelectedIndex(0); // Easy gives more time to inspect keyboard movement.
                frame.toFront();
                frame.requestFocus();
            });
            check(enabled(pace), "difficulty can be selected before a round");
            // Xvfb has no window manager: a real click reliably establishes keyboard focus.
            Point location = new Point();
            SwingUtilities.invokeAndWait(() -> location.setLocation(board.getLocationOnScreen()));
            robot.mouseMove(location.x + 20, location.y + 20);
            robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle();
            press(robot, KeyEvent.VK_ENTER);
            check(text(play).startsWith("Pause"), "Enter starts play");
            check(!enabled(pace), "difficulty is locked during a round");
            // Turn up and allow an actual timer tick; green pixels must leave the starting row.
            press(robot, KeyEvent.VK_UP);
            robot.delay(220);
            check(hasSnakeAboveCenter(board), "arrow key changes the rendered snake direction");
            press(robot, KeyEvent.VK_SPACE);
            check(text(play).startsWith("Resume"), "Space pauses play");
            BufferedImage paused = render(board);
            robot.delay(250);
            check(samePixels(paused, render(board)), "paused board stays still");
            press(robot, KeyEvent.VK_SPACE);
            check(text(play).startsWith("Pause"), "Space resumes play");
            press(robot, KeyEvent.VK_R);
            check(text(play).startsWith("Pause"), "R starts a fresh round");
            // The fresh snake heads into the right wall unless steered.
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (!text(play).startsWith("Play again") && System.nanoTime() < deadline) robot.delay(80);
            check(text(play).startsWith("Play again"), "wall collision shows game over");
            check(enabled(pace), "difficulty unlocks after game over");
            SwingUtilities.invokeAndWait(play::doClick);
            check(text(play).startsWith("Pause"), "Play again button starts a new round");
            SwingUtilities.invokeAndWait(restart::doClick);
            check(text(play).startsWith("Pause"), "Restart button starts a new round");
            System.out.println("SnakeUiTest: 13 checks passed; preview: " + preview);
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) window.dispose();
            });
            // Swing timers would keep the VM alive after dispose; main exits explicitly below.
            try (var files = Files.walk(scoreDirectory)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void press(Robot robot, int key) throws Exception {
        robot.keyPress(key);
        robot.keyRelease(key);
        // Robot.waitForIdle() performs a slow native display sync on bare Xvfb.
        // Allow delivery, then drain Swing's queue without freezing the live game for seconds.
        robot.delay(50);
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static <T extends Component> T component(String name, Class<T> type) throws Exception {
        AtomicReference<T> found = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> found.set(find(frame, name, type)));
        if (found.get() == null) throw new AssertionError("Missing component: " + name);
        return found.get();
    }

    private static <T extends Component> T find(Component root, String name, Class<T> type) {
        if (name.equals(root.getName()) && type.isInstance(root)) return type.cast(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T result = find(child, name, type);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static String text(JButton button) throws Exception {
        AtomicReference<String> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(button.getText()));
        return result.get();
    }

    private static boolean enabled(Component component) throws Exception {
        AtomicReference<Boolean> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(component.isEnabled()));
        return result.get();
    }

    private static BufferedImage render(JComponent component) throws Exception {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            var graphics = image.createGraphics();
            component.paint(graphics);
            graphics.dispose();
        });
        return image;
    }

    private static boolean hasSnakeAboveCenter(JComponent board) throws Exception {
        BufferedImage image = render(board);
        for (int y = 1; y < image.getHeight() / 2; y++) {
            for (int x = 1; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y) & 0xFFFFFF;
                if (rgb == 0x5BECAC || rgb == 0x31AA7E) return true;
            }
        }
        return false;
    }

    private static boolean samePixels(BufferedImage first, BufferedImage second) {
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) return false;
            }
        }
        return true;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PASS " + message);
    }
}
