package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Validated settings of a new game (SRS FR-04..FR-09). Immutable once the game starts. */
public final class GameSettings {

    public static final int MAX_NAME_LENGTH = 12;

    /** Name and colour entered for one player. */
    public record PlayerSetup(String name, PlayerColor color) {
    }

    /** A validation problem; {@code player} is the 0-based row, or -1 for a general problem. */
    public record FieldError(int player, String field, String message) {
    }

    private final Difficulty difficulty;
    private final List<PlayerSetup> players;

    public GameSettings(Difficulty difficulty, List<PlayerSetup> players) {
        List<FieldError> errors = validate(difficulty, players);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.get(0).message());
        }
        this.difficulty = difficulty;
        List<PlayerSetup> clean = new ArrayList<>();
        for (PlayerSetup p : players) {
            clean.add(new PlayerSetup(p.name().trim(), p.color()));
        }
        this.players = Collections.unmodifiableList(clean);
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    /** Players in turn order (the order entered during setup). */
    public List<PlayerSetup> players() {
        return players;
    }

    /** Checks every rule of the setup screen; an empty list means the settings are valid. */
    public static List<FieldError> validate(Difficulty difficulty, List<PlayerSetup> players) {
        List<FieldError> errors = new ArrayList<>();
        if (difficulty == null) {
            errors.add(new FieldError(-1, "difficulty", "Choose a difficulty level."));
        }
        if (players == null || players.size() < 2 || players.size() > 4) {
            errors.add(new FieldError(-1, "players", "Choose 2, 3 or 4 players."));
            return errors;
        }
        for (int i = 0; i < players.size(); i++) {
            PlayerSetup p = players.get(i);
            String name = p.name() == null ? "" : p.name().trim();
            if (name.isEmpty()) {
                errors.add(new FieldError(i, "name", "Enter a name (1-12 characters)."));
            } else if (name.length() > MAX_NAME_LENGTH) {
                errors.add(new FieldError(i, "name", "Names can have at most 12 characters."));
            } else {
                for (int j = 0; j < i; j++) {
                    String other = players.get(j).name() == null ? "" : players.get(j).name().trim();
                    if (other.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) {
                        errors.add(new FieldError(i, "name", "Player " + (j + 1) + " already uses this name."));
                        break;
                    }
                }
            }
            if (p.color() == null) {
                errors.add(new FieldError(i, "color", "Choose a colour."));
            } else {
                for (int j = 0; j < i; j++) {
                    if (players.get(j).color() == p.color()) {
                        errors.add(new FieldError(i, "color", "Player " + (j + 1) + " already has this colour."));
                        break;
                    }
                }
            }
        }
        return errors;
    }
}
