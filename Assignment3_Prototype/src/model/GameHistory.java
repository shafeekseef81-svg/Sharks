package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Completed games (FR-48, FR-49). Singleton, as the SRS plans for the history file (NFR-09);
 * the prototype keeps the records in memory only.
 */
public final class GameHistory {

    private static final GameHistory INSTANCE = new GameHistory();

    private final List<GameRecord> records = new ArrayList<>();

    private GameHistory() {
    }

    public static GameHistory getInstance() {
        return INSTANCE;
    }

    public synchronized void load(Collection<GameRecord> sample) {
        records.clear();
        records.addAll(sample);
    }

    /** Records sorted newest first; {@code filter} null means all levels (FR-50). */
    public synchronized List<GameRecord> newestFirst(Difficulty filter) {
        return records.stream()
                .filter(r -> filter == null || r.difficulty() == filter)
                .sorted(Comparator.comparing(GameRecord::timestamp).reversed())
                .toList();
    }

    /** Appends a completed game with the next free ID and the current time (FR-63). */
    public synchronized GameRecord add(Difficulty difficulty, List<String> placeNames, Duration duration) {
        int id = records.stream().mapToInt(GameRecord::gameId).max().orElse(1000) + 1;
        GameRecord record = new GameRecord(id, difficulty, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                List.copyOf(placeNames), duration);
        records.add(record);
        return record;
    }

    public synchronized int size() {
        return records.size();
    }
}
