package ray.snake;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class HighScoreStoreTest {
    public static void main(String[] arguments) throws Exception {
        Path directory = Files.createTempDirectory("ray-snake-scores-");
        try {
            HighScoreStore store = new HighScoreStore(directory);
            check(store.best("NORMAL") == 0, "new player has no best score");
            store.record("NORMAL", 50);
            store.record("NORMAL", 10);
            store.record("FAST", 20);
            HighScoreStore reloaded = new HighScoreStore(directory);
            check(reloaded.best("NORMAL") == 50, "best persists without being lowered");
            check(reloaded.best("FAST") == 20, "each difficulty has its own best");
            Files.writeString(directory.resolve("high-scores.properties"), "NORMAL=broken\nFAST=-50\n");
            HighScoreStore corrupt = new HighScoreStore(directory);
            check(corrupt.best("NORMAL") == 0 && corrupt.best("FAST") == 0, "invalid scores are ignored");
            Path blocked = directory.resolve("not-a-directory");
            Files.writeString(blocked, "file");
            HighScoreStore fallback = new HighScoreStore(blocked);
            fallback.record("NORMAL", 70);
            check(fallback.best("NORMAL") == 70 && !fallback.persistent(), "unwritable storage keeps a session best");
            System.out.println("HighScoreStoreTest: 5 checks passed");
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
