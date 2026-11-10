package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.GameSettings.FieldError;
import Model.GameSettings.PlayerSetup;

/**
 * Independent input tests of the setup rules (Task 05: FR-06..FR-09, empty, long and duplicate input).
 * Written by Shafeek Saif, who did not implement {@link GameSettings} (SDP 5.3.4, 7.2).
 *
 * <p>The author's tests check one example of each rule. These tests go through every player row and every
 * colour pair, and check that an error is reported on exactly the field that is wrong (FR-08).
 */
class SetupInputIndependentTest {

    private static final PlayerColor[] COLORS = PlayerColor.values();

    private static List<PlayerSetup> valid(int count) {
        List<PlayerSetup> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new PlayerSetup("Name" + i, COLORS[i]));
        }
        return list;
    }

    private static List<FieldError> validate(List<PlayerSetup> players) {
        return GameSettings.validate(Difficulty.EASY, players);
    }

    private static void assertOnlyError(List<FieldError> errors, int row, String field, String where) {
        assertEquals(1, errors.size(), where + ": " + errors);
        assertEquals(row, errors.get(0).player(), where);
        assertEquals(field, errors.get(0).field(), where);
        assertFalse(errors.get(0).message().isBlank(), where);
    }

    @Test
    @DisplayName("T05 FR-06: empty and blank names are reported on the right row, for every row and player count")
    void emptyNamesOnEveryRow() {
        for (int count = 2; count <= 4; count++) {
            for (int row = 0; row < count; row++) {
                for (String empty : new String[] {"", " ", "   ", "\t", "\n", " ", null}) {
                    List<PlayerSetup> players = valid(count);
                    players.set(row, new PlayerSetup(empty, COLORS[row]));
                    assertOnlyError(validate(players), row, GameSettings.FIELD_NAME,
                            count + " players, row " + row + ", name [" + empty + "]");
                }
            }
        }
    }

    @Test
    @DisplayName("T05 FR-06: the name length limit is exact on every row, spaces inside a name count")
    void lengthLimitOnEveryRow() {
        for (int row = 0; row < 4; row++) {
            List<PlayerSetup> players = valid(4);
            players.set(row, new PlayerSetup("a".repeat(12), COLORS[row]));
            assertTrue(validate(players).isEmpty(), "12 letters, row " + row);
            players.set(row, new PlayerSetup("a".repeat(13), COLORS[row]));
            assertOnlyError(validate(players), row, GameSettings.FIELD_NAME, "13 letters, row " + row);
            players.set(row, new PlayerSetup("a b c d e f g", COLORS[row]));
            assertOnlyError(validate(players), row, GameSettings.FIELD_NAME, "13 with spaces, row " + row);
            players.set(row, new PlayerSetup("   " + "a".repeat(12) + "   ", COLORS[row]));
            assertTrue(validate(players).isEmpty(), "surrounding spaces do not count, row " + row);
            players.set(row, new PlayerSetup("x".repeat(100_000), COLORS[row]));
            assertOnlyError(validate(players), row, GameSettings.FIELD_NAME, "very long, row " + row);
        }
    }

    @Test
    @DisplayName("T05 FR-06: a duplicate is reported on the later row, for every pair of rows")
    void duplicateNamesEveryPair() {
        for (int first = 0; first < 4; first++) {
            for (int second = first + 1; second < 4; second++) {
                List<PlayerSetup> players = valid(4);
                players.set(first, new PlayerSetup("Dana", COLORS[first]));
                players.set(second, new PlayerSetup("  dANA ", COLORS[second]));
                assertOnlyError(validate(players), second, GameSettings.FIELD_NAME, first + " and " + second);
            }
        }
    }

    @Test
    @DisplayName("T05 FR-06: three equal names report the second and third row; other scripts are compared too")
    void repeatedNames() {
        List<PlayerSetup> players = valid(4);
        players.set(0, new PlayerSetup("רן", COLORS[0]));
        players.set(1, new PlayerSetup("רן", COLORS[1]));
        players.set(2, new PlayerSetup(" רן ", COLORS[2]));
        List<FieldError> errors = validate(players);
        assertEquals(2, errors.size());
        assertEquals(1, errors.get(0).player());
        assertEquals(2, errors.get(1).player());
        assertTrue(errors.stream().allMatch(e -> e.field().equals(GameSettings.FIELD_NAME)));
    }

    @Test
    @DisplayName("T05 FR-06: names that differ only by an inner space or a digit are different names")
    void nearDuplicatesAreAllowed() {
        List<PlayerSetup> players = valid(3);
        players.set(0, new PlayerSetup("Ann Lee", COLORS[0]));
        players.set(1, new PlayerSetup("AnnLee", COLORS[1]));
        players.set(2, new PlayerSetup("Ann Lee2", COLORS[2]));
        assertTrue(validate(players).isEmpty());
    }

    @Test
    @DisplayName("T05 FR-07: every order of the four colours is valid and every equal pair is reported")
    void everyColourPair() {
        for (PlayerColor a : COLORS) {
            for (PlayerColor b : COLORS) {
                List<PlayerSetup> players = List.of(new PlayerSetup("A", a), new PlayerSetup("B", b));
                if (a == b) {
                    assertOnlyError(validate(players), 1, GameSettings.FIELD_COLOR, a + " twice");
                } else {
                    assertTrue(validate(players).isEmpty(), a + " and " + b);
                }
            }
        }
    }

    @Test
    @DisplayName("T05 FR-08: a missing colour is reported on its own row and does not create a false duplicate")
    void missingColourOnEveryRow() {
        for (int row = 0; row < 4; row++) {
            List<PlayerSetup> players = valid(4);
            players.set(row, new PlayerSetup("Name" + row, null));
            assertOnlyError(validate(players), row, GameSettings.FIELD_COLOR, "row " + row);
        }
        List<PlayerSetup> twoMissing = List.of(new PlayerSetup("A", null), new PlayerSetup("B", null));
        assertEquals(2, validate(twoMissing).size(), "two missing colours are not 'the same colour'");
        assertTrue(validate(twoMissing).stream().noneMatch(e -> e.message().contains("already")));
    }

    @Test
    @DisplayName("T05 FR-08: several wrong fields are all reported, in screen order")
    void allErrorsInScreenOrder() {
        List<PlayerSetup> players = Arrays.asList(
                new PlayerSetup("", null), new PlayerSetup("ok", COLORS[0]), null, new PlayerSetup("OK", COLORS[0]));
        List<FieldError> errors = GameSettings.validate(null, players);
        List<String> found = new ArrayList<>();
        for (FieldError e : errors) {
            found.add(e.player() + ":" + e.field());
        }
        assertEquals(List.of("-1:difficulty", "0:name", "0:color", "2:name", "2:color", "3:name", "3:color"), found);
    }

    @Test
    @DisplayName("T05 FR-04, FR-08: a wrong player count is one general error, for 0, 1, 5 and many players")
    void wrongPlayerCount() {
        for (int count : new int[] {0, 1, 5, 50}) {
            List<PlayerSetup> players = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                players.add(new PlayerSetup("P" + i, COLORS[i % 4]));
            }
            assertOnlyError(validate(players), -1, GameSettings.FIELD_PLAYERS, count + " players");
        }
        assertOnlyError(validate(null), -1, GameSettings.FIELD_PLAYERS, "no list");
    }

    @Test
    @DisplayName("T05 FR-08, FR-09: invalid input creates no game; valid input is stored trimmed and cannot change")
    void constructorBehaviour() {
        List<PlayerSetup> bad = valid(2);
        bad.set(1, new PlayerSetup("Name0", COLORS[1]));
        assertThrows(IllegalArgumentException.class, () -> new GameSettings(Difficulty.EASY, bad));

        List<PlayerSetup> input = valid(2);
        input.set(0, new PlayerSetup("  Spaced  ", COLORS[0]));
        GameSettings settings = new GameSettings(Difficulty.EASY, input);
        assertEquals("Spaced", settings.players().get(0).name());
        input.set(1, new PlayerSetup("Changed", COLORS[3]));
        assertEquals("Name1", settings.players().get(1).name(), "later changes to the input list are ignored");
        assertThrows(UnsupportedOperationException.class, () -> settings.players().remove(0));
    }
}
