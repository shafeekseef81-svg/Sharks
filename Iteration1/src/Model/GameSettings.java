package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Validated settings of a new game (SRS FR-04, FR-06..FR-09). Task 04, owner: Rakan Ziyan.
 *
 * <p>The rules checked by {@link #validate}:
 * <ul>
 *   <li>a difficulty level is chosen (FR-05);</li>
 *   <li>2, 3 or 4 players (FR-04);</li>
 *   <li>every player has a name of 1-12 characters after trimming spaces, unique ignoring case (FR-06);</li>
 *   <li>every player has a colour, and no two players share a colour (FR-07).</li>
 * </ul>
 * Every problem is reported with the row and field it belongs to, so the setup screen can mark the invalid
 * field (FR-08). An object of this class can only be created from valid input, and it cannot be changed
 * afterwards (FR-09).
 */
public final class GameSettings {

    /** Smallest number of players (FR-04). */
    public static final int MIN_PLAYERS = 2;
    /** Largest number of players (FR-04). */
    public static final int MAX_PLAYERS = 4;
    /** Largest name length in characters (FR-06). */
    public static final int MAX_NAME_LENGTH = 12;

    /** Field names used in {@link FieldError#field()}. */
    public static final String FIELD_DIFFICULTY = "difficulty";
    public static final String FIELD_PLAYERS = "players";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_COLOR = "color";

    /** Name and colour entered for one player. */
    public record PlayerSetup(String name, PlayerColor color) {
    }

    /** A validation problem; {@code player} is the 0-based row, or -1 for a general problem. */
    public record FieldError(int player, String field, String message) {
    }

    private final Difficulty difficulty;
    private final List<PlayerSetup> players;

    /**
     * Creates validated settings. Names are stored without leading and trailing spaces.
     *
     * @throws IllegalArgumentException if {@link #validate} reports any problem; the message lists all of them
     */
    public GameSettings(Difficulty difficulty, List<PlayerSetup> players) {
        List<FieldError> errors = validate(difficulty, players);
        if (!errors.isEmpty()) {
            List<String> messages = new ArrayList<>();
            for (FieldError e : errors) {
                messages.add(e.player() >= 0 ? "Player " + (e.player() + 1) + ": " + e.message() : e.message());
            }
            throw new IllegalArgumentException(String.join(" ", messages));
        }
        this.difficulty = difficulty;
        List<PlayerSetup> clean = new ArrayList<>();
        for (PlayerSetup p : players) {
            clean.add(new PlayerSetup(cleanName(p.name()), p.color()));
        }
        this.players = Collections.unmodifiableList(clean);
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    /** Players in turn order (the order entered during setup). The list cannot be modified. */
    public List<PlayerSetup> players() {
        return players;
    }

    /** Number of players (2-4). */
    public int playerCount() {
        return players.size();
    }

    /**
     * Checks every rule of the setup screen without creating an object.
     *
     * @return all problems found, in screen order; an empty list means the settings are valid
     */
    public static List<FieldError> validate(Difficulty difficulty, List<PlayerSetup> players) {
        List<FieldError> errors = new ArrayList<>();
        if (difficulty == null) {
            errors.add(new FieldError(-1, FIELD_DIFFICULTY, "Choose a difficulty level."));
        }
        if (players == null || players.size() < MIN_PLAYERS || players.size() > MAX_PLAYERS) {
            errors.add(new FieldError(-1, FIELD_PLAYERS, "Choose 2, 3 or 4 players."));
            return errors;
        }
        for (int i = 0; i < players.size(); i++) {
            PlayerSetup p = players.get(i);
            if (p == null) {
                errors.add(new FieldError(i, FIELD_NAME, "Enter a name (1-12 characters)."));
                errors.add(new FieldError(i, FIELD_COLOR, "Choose a colour."));
                continue;
            }
            String name = cleanName(p.name());
            int length = name.codePointCount(0, name.length());
            if (length == 0) {
                errors.add(new FieldError(i, FIELD_NAME, "Enter a name (1-12 characters)."));
            } else if (length > MAX_NAME_LENGTH) {
                errors.add(new FieldError(i, FIELD_NAME, "Names can have at most 12 characters."));
            } else {
                for (int j = 0; j < i; j++) {
                    PlayerSetup other = players.get(j);
                    if (other != null && sameName(cleanName(other.name()), name)) {
                        errors.add(new FieldError(i, FIELD_NAME, "Player " + (j + 1) + " already uses this name."));
                        break;
                    }
                }
            }
            if (p.color() == null) {
                errors.add(new FieldError(i, FIELD_COLOR, "Choose a colour."));
            } else {
                for (int j = 0; j < i; j++) {
                    PlayerSetup other = players.get(j);
                    if (other != null && other.color() == p.color()) {
                        errors.add(new FieldError(i, FIELD_COLOR, "Player " + (j + 1) + " already has this colour."));
                        break;
                    }
                }
            }
        }
        return errors;
    }

    private static String cleanName(String name) {
        return name == null ? "" : name.strip(); // strip(): Unicode spaces too, same rule as Player (isBlank)
    }

    private static boolean sameName(String a, String b) {
        return a.toLowerCase(Locale.ROOT).equals(b.toLowerCase(Locale.ROOT));
    }
}
