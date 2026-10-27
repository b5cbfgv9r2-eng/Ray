package ray.snake;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Best scores are local to this computer; a read-only home never prevents play. */
final class HighScoreStore {
    private final Path file;
    private final Properties scores = new Properties();
    private boolean persistent = true;

    HighScoreStore(Path directory) {
        file = directory.resolve("high-scores.properties");
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                scores.load(input);
            } catch (IOException | IllegalArgumentException | SecurityException error) {
                persistent = false;
            }
        }
    }

    int best(String difficulty) {
        try {
            return Math.max(0, Integer.parseInt(scores.getProperty(difficulty, "0")));
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    void record(String difficulty, int score) {
        if (score <= best(difficulty)) return;
        scores.setProperty(difficulty, Integer.toString(score));
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "scores-", ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                scores.store(output, "Ray Snake personal bests");
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            persistent = true;
        } catch (IOException | SecurityException error) {
            persistent = false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException | SecurityException ignored) { }
            }
        }
    }

    boolean persistent() { return persistent; }
}
