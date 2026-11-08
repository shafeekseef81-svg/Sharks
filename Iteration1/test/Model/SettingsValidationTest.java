package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests for Task 04 (model validator, immutable settings, Player and Piece checks).
 * The independent unit tests of Task 05 are written by Shafeek Saif in {@code GameSettingsTest}.
 *
 * <p>Owner: Rakan Ziyan.
 */
class SettingsValidationTest {

    private static GameSettings.PlayerSetup p(String name, PlayerColor color) {
        return new GameSettings.PlayerSetup(name, color);
    }

    private static List<GameSettings.FieldError> validate(GameSettings.PlayerSetup... players) {
        return GameSettings.validate(Difficulty.EASY, Arrays.asList(players));
    }

    private static boolean hasError(List<GameSettings.FieldError> errors, int row, String field) {
        return errors.stream().anyMatch(e -> e.player() == row && e.field().equals(field));
    }

    // ---- FR-04 -------------------------------------------------------------------------------------

    @Test
    @DisplayName("FR-04: 2, 3 and 4 players are accepted; 0, 1 and 5 players are rejected")
    void fr04PlayerCount() {
        PlayerColor[] c = PlayerColor.values();
        List<GameSettings.PlayerSetup> list = new ArrayList<>();
        for (int n = 0; n <= 5; n++) {
            list.clear();
            for (int i = 0; i < n; i++) {
                list.add(p("P" + i, c[i % c.length]));
            }
            List<GameSettings.FieldError> errors = GameSettings.validate(Difficulty.EASY, list);
            boolean countError = hasError(errors, -1, GameSettings.FIELD_PLAYERS);
            assertEquals(n < 2 || n > 4, countError, n + " players");
        }
        assertTrue(hasError(GameSettings.validate(Difficulty.EASY, null), -1, GameSettings.FIELD_PLAYERS));
    }

    // ---- FR-05 / FR-08 -----------------------------------------------------------------------------

    @Test
    @DisplayName("FR-05/FR-08: a missing difficulty is reported on the difficulty field")
    void missingDifficulty() {
        List<GameSettings.FieldError> errors = GameSettings.validate(null,
                List.of(p("Ana", PlayerColor.RED), p("Ben", PlayerColor.BLUE)));
        assertEquals(1, errors.size());
        assertTrue(hasError(errors, -1, GameSettings.FIELD_DIFFICULTY));
    }

    // ---- FR-06 -------------------------------------------------------------------------------------

    @Test
    @DisplayName("FR-06: names of 1 and 12 characters are valid; empty, blank and 13 characters are not")
    void fr06NameLengthBoundaries() {
        assertTrue(validate(p("A", PlayerColor.RED), p("B23456789012", PlayerColor.BLUE)).isEmpty());
        assertTrue(hasError(validate(p("", PlayerColor.RED), p("Ben", PlayerColor.BLUE)), 0, GameSettings.FIELD_NAME));
        assertTrue(hasError(validate(p("   ", PlayerColor.RED), p("Ben", PlayerColor.BLUE)), 0, GameSettings.FIELD_NAME));
        assertTrue(hasError(validate(p(null, PlayerColor.RED), p("Ben", PlayerColor.BLUE)), 0, GameSettings.FIELD_NAME));
        assertTrue(hasError(validate(p("Ana", PlayerColor.RED), p("B234567890123", PlayerColor.BLUE)), 1,
                GameSettings.FIELD_NAME));
    }

    @Test
    @DisplayName("FR-06: the length is counted after trimming spaces, and Hebrew names are counted per letter")
    void fr06TrimAndHebrew() {
        assertTrue(validate(p("  B23456789012  ", PlayerColor.RED), p("Ana", PlayerColor.BLUE)).isEmpty());
        assertTrue(validate(p("רקאן", PlayerColor.RED), p("שפיק", PlayerColor.BLUE)).isEmpty());
        assertTrue(validate(p("אבגדהוזחטיכל", PlayerColor.RED), p("Ana", PlayerColor.BLUE)).isEmpty(),
                "12 Hebrew letters");
    }

    @Test
    @DisplayName("FR-06: names must be unique, ignoring case and surrounding spaces")
    void fr06UniqueNames() {
        List<GameSettings.FieldError> errors = validate(p("Rakan", PlayerColor.RED), p(" rakan ", PlayerColor.BLUE));
        assertTrue(hasError(errors, 1, GameSettings.FIELD_NAME));
        assertFalse(hasError(errors, 0, GameSettings.FIELD_NAME), "the first use of a name is valid");
        assertTrue(validate(p("Rakan", PlayerColor.RED), p("Rakan2", PlayerColor.BLUE)).isEmpty());
    }

    @Test
    @DisplayName("FR-06/FR-08: a name of only Unicode spaces is reported, not accepted and then refused by Player")
    void fr06UnicodeSpacesOnly() {
        assertTrue(hasError(validate(p("　 ", PlayerColor.RED), p("Ben", PlayerColor.BLUE)), 0,
                GameSettings.FIELD_NAME));
        GameSettings s = new GameSettings(Difficulty.EASY,
                List.of(p("　Ana ", PlayerColor.RED), p("Ben", PlayerColor.BLUE)));
        assertEquals("Ana", s.players().get(0).name(), "Unicode spaces around a name are removed");
        new GameModel(s, new Random(1), new FixedDice(1), () -> 0L); // Player accepts the stored name
    }

    @Test
    @DisplayName("FR-06: a name is counted in characters: 12 emoji (24 UTF-16 units) are valid, 13 are not")
    void fr06CodePoints() {
        String twelve = "😀".repeat(12);
        assertEquals(24, twelve.length());
        assertTrue(validate(p(twelve, PlayerColor.RED), p("Ben", PlayerColor.BLUE)).isEmpty());
        assertTrue(hasError(validate(p(twelve + "😀", PlayerColor.RED), p("Ben", PlayerColor.BLUE)), 0,
                GameSettings.FIELD_NAME));
    }

    // ---- FR-07-------------------------------------------------------------------------------------

    @Test
    @DisplayName("FR-07: every player needs a colour, and colours must be unique")
    void fr07Colours() {
        assertTrue(hasError(validate(p("Ana", null), p("Ben", PlayerColor.BLUE)), 0, GameSettings.FIELD_COLOR));
        List<GameSettings.FieldError> dup = validate(p("Ana", PlayerColor.RED), p("Ben", PlayerColor.GREEN),
                p("Dan", PlayerColor.RED));
        assertTrue(hasError(dup, 2, GameSettings.FIELD_COLOR));
        assertFalse(hasError(dup, 0, GameSettings.FIELD_COLOR));
        assertTrue(validate(p("A", PlayerColor.RED), p("B", PlayerColor.GREEN), p("C", PlayerColor.YELLOW),
                p("D", PlayerColor.BLUE)).isEmpty());
    }

    // ---- FR-08 -------------------------------------------------------------------------------------

    @Test
    @DisplayName("FR-08: every invalid field is reported with its row, and a missing row does not crash")
    void fr08AllErrorsReported() {
        List<GameSettings.FieldError> errors = validate(p("", null), null, p("Ana", PlayerColor.RED));
        assertTrue(hasError(errors, 0, GameSettings.FIELD_NAME));
        assertTrue(hasError(errors, 0, GameSettings.FIELD_COLOR));
        assertTrue(hasError(errors, 1, GameSettings.FIELD_NAME));
        assertTrue(hasError(errors, 1, GameSettings.FIELD_COLOR));
        assertFalse(hasError(errors, 2, GameSettings.FIELD_NAME));
        assertFalse(hasError(errors, 2, GameSettings.FIELD_COLOR));
    }

    @Test
    @DisplayName("FR-08: a game cannot be created from invalid settings")
    void fr08ConstructorRejectsInvalidInput() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new GameSettings(
                Difficulty.EASY, List.of(p("Ana", PlayerColor.RED), p("ana", PlayerColor.RED))));
        assertTrue(e.getMessage().contains("Player 2"), e.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> new GameSettings(Difficulty.EASY, List.of(p("Solo", PlayerColor.RED))));
    }

    // ---- FR-09 -------------------------------------------------------------------------------------

    @Test
    @DisplayName("FR-09: settings cannot be changed after creation, also not through the caller's list")
    void fr09SettingsImmutable() {
        List<GameSettings.PlayerSetup> input = new ArrayList<>(
                List.of(p("  Ana ", PlayerColor.RED), p("Ben", PlayerColor.BLUE)));
        GameSettings s = new GameSettings(Difficulty.EASY, input);
        input.set(0, p("Changed", PlayerColor.GREEN));
        input.add(p("Extra", PlayerColor.YELLOW));

        assertEquals(2, s.playerCount());
        assertEquals("Ana", s.players().get(0).name(), "stored trimmed and not affected by the caller's list");
        assertEquals(PlayerColor.RED, s.players().get(0).color());
        assertThrows(UnsupportedOperationException.class, () -> s.players().add(p("X", PlayerColor.GREEN)));
        assertThrows(UnsupportedOperationException.class, () -> s.players().set(0, p("X", PlayerColor.GREEN)));
    }

    @Test
    @DisplayName("FR-09/FR-13: the started game keeps the setup names, colours, order and level; 4 pieces in castle")
    void fr09GameKeepsSettings() {
        GameSettings s = new GameSettings(Difficulty.EASY,
                List.of(p("Ana", PlayerColor.YELLOW), p("Ben", PlayerColor.RED), p("Dan", PlayerColor.BLUE)));
        GameModel m = new GameModel(s, new Random(3), new FixedDice(1), () -> 0L);
        assertEquals(Difficulty.EASY, m.difficulty());
        for (int i = 0; i < 3; i++) {
            Player pl = m.players().get(i);
            assertEquals(s.players().get(i).name(), pl.name());
            assertEquals(s.players().get(i).color(), pl.color());
            assertEquals(Player.PIECES_PER_PLAYER, pl.pieces().size());
            assertTrue(pl.pieces().stream().allMatch(Piece::inCastle));
        }
        Player first = m.players().get(0);
        assertThrows(UnsupportedOperationException.class, () -> first.pieces().clear());
    }

    // ---- Player and Piece --------------------------------------------------------------------------

    @Test
    @DisplayName("Player: name and colour are required; four distinct pieces of the player's colour")
    void playerValidation() {
        assertThrows(NullPointerException.class, () -> new Player(null, PlayerColor.RED, 56));
        assertThrows(NullPointerException.class, () -> new Player("Ana", null, 56));
        assertThrows(IllegalArgumentException.class, () -> new Player("  ", PlayerColor.RED, 56));
        assertThrows(IllegalArgumentException.class, () -> new Player("Ana", PlayerColor.RED, 0));

        Player pl = new Player("Ana", PlayerColor.GREEN, 56);
        assertEquals(4, pl.pieces().size());
        for (int i = 0; i < 4; i++) {
            Piece piece = pl.pieces().get(i);
            assertEquals(i, piece.index());
            assertEquals(PlayerColor.GREEN, piece.color());
            assertEquals(56, piece.finishIndex());
        }
        assertNotSame(pl.pieces().get(0), pl.pieces().get(1));
        assertEquals(0, pl.rank());
        assertThrows(IllegalArgumentException.class, () -> pl.setRank(0));
        pl.setRank(2);
        assertEquals(2, pl.rank());
    }

    @Test
    @DisplayName("Piece: index 0-3 and a positive finish index; progress only between castle and finish")
    void pieceValidation() {
        assertThrows(NullPointerException.class, () -> new Piece(null, 0, 56));
        assertThrows(IllegalArgumentException.class, () -> new Piece(PlayerColor.RED, -1, 56));
        assertThrows(IllegalArgumentException.class, () -> new Piece(PlayerColor.RED, 4, 56));
        assertThrows(IllegalArgumentException.class, () -> new Piece(PlayerColor.RED, 0, 0));

        Piece piece = new Piece(PlayerColor.RED, 3, 56);
        assertTrue(piece.inCastle());
        assertEquals(Piece.IN_CASTLE, piece.progress());

        piece.setProgress(0);
        assertTrue(piece.onBoard());
        piece.setProgress(56);
        assertTrue(piece.finished());
        assertFalse(piece.onBoard());
        piece.setProgress(Piece.IN_CASTLE);
        assertTrue(piece.inCastle());

        assertThrows(IllegalArgumentException.class, () -> piece.setProgress(-2));
        assertThrows(IllegalArgumentException.class, () -> piece.setProgress(57));
        assertTrue(piece.inCastle(), "a rejected value does not move the piece");
    }
}
