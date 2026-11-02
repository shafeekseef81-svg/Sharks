package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * One completed game, with the fields of the history CSV template (SRS 3.6):
 * gameID, difficulty, timestamp(DD/MM/YYYY; HH:MM:SS), 1st..4th place, game_duration(HH:MM:SS).
 */
public record GameRecord(int gameId, Difficulty difficulty, LocalDateTime timestamp, List<String> places,
                         Duration duration) {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy; HH:mm:ss");

    public String timestampText() {
        return TIMESTAMP.format(timestamp);
    }

    public String durationText() {
        return formatDuration(duration);
    }

    /** Name at a 0-based place, or "" when the game had fewer players (FR-63: empty places stay blank). */
    public String place(int index) {
        return index < places.size() ? places.get(index) : "";
    }

    /** HH:MM:SS (FR-46). */
    public static String formatDuration(Duration d) {
        long s = Math.max(0, d.getSeconds());
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
