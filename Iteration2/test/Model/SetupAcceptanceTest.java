package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.GameSettings.PlayerSetup;

/**
 * Acceptance tests of US-1.1 "set up a game for 2-4 players" (Task 06: FR-04..FR-09, FR-05 partial), written by
 * the Product Owner. They go from the settings a player enters to a started game.
 *
 * <p>Automated here: the model part. <b>Still manual:</b> FR-01..FR-03 and the marking of invalid fields on the
 * screen (FR-08), which are screen behaviour.
 */
class SetupAcceptanceTest {

    private static List<PlayerSetup> setup(int count) {
        List<PlayerSetup> list = new ArrayList<>();
        String[] names = {"Shafeek", "Rakan", "Mohamed", "Noa"};
        PlayerColor[] order = {PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED, PlayerColor.GREEN};
        for (int i = 0; i < count; i++) {
            list.add(new PlayerSetup(names[i], order[i]));
        }
        return list;
    }

    @Test
    @DisplayName("T06 FR-04, FR-06, FR-07, FR-13: a valid 2, 3 or 4 player setup starts with those players")
    void validSetupStartsGame() {
        for (int count = 2; count <= 4; count++) {
            List<PlayerSetup> entered = setup(count);
            GameModel game = new GameModel(new GameSettings(Difficulty.EASY, entered), new Random(1));
            assertEquals(count, game.players().size());
            for (int i = 0; i < count; i++) {
                assertEquals(entered.get(i).name(), game.players().get(i).name());
                assertEquals(entered.get(i).color(), game.players().get(i).color());
                assertEquals(4, game.players().get(i).pieces().size());
                assertTrue(game.players().get(i).pieces().stream().allMatch(Piece::inCastle));
            }
            assertSame(game.players().get(0), game.currentPlayer(), "FR-15: the first entered player starts");
        }
    }

    @Test
    @DisplayName("T06 FR-08: an invalid setup cannot start a game")
    void invalidSetupCannotStart() {
        List<List<PlayerSetup>> invalid = new ArrayList<>();
        invalid.add(setup(1));
        invalid.add(List.of(new PlayerSetup("", PlayerColor.RED), new PlayerSetup("B", PlayerColor.BLUE)));
        invalid.add(List.of(new PlayerSetup("A", PlayerColor.RED), new PlayerSetup("a", PlayerColor.BLUE)));
        invalid.add(List.of(new PlayerSetup("A", PlayerColor.RED), new PlayerSetup("B", PlayerColor.RED)));
        invalid.add(List.of(new PlayerSetup("A".repeat(13), PlayerColor.RED), new PlayerSetup("B", PlayerColor.BLUE)));
        for (List<PlayerSetup> players : invalid) {
            assertFalse(GameSettings.validate(Difficulty.EASY, players).isEmpty(), players.toString());
            assertThrows(IllegalArgumentException.class, () -> new GameSettings(Difficulty.EASY, players));
        }
        assertThrows(IllegalArgumentException.class, () -> new GameSettings(null, setup(2)));
    }

    @Test
    @DisplayName("T06 FR-09: names, colours and level of a started game cannot change")
    void startedGameKeepsSettings() {
        List<PlayerSetup> entered = new ArrayList<>(setup(2));
        GameModel game = new GameModel(new GameSettings(Difficulty.EASY, entered), new Random(1));
        entered.set(0, new PlayerSetup("Other", PlayerColor.GREEN));
        assertEquals("Shafeek", game.settings().players().get(0).name());
        assertEquals("Shafeek", game.players().get(0).name());
        assertEquals(Difficulty.EASY, game.difficulty());
        assertThrows(UnsupportedOperationException.class, () -> game.settings().players().clear());
        assertThrows(UnsupportedOperationException.class, () -> game.players().clear());
    }

    @Test
    @DisplayName("T06 FR-05 (partial, SDP Iteration 1): an Easy game can be played; Medium and Hard refuse to roll")
    void onlyEasyIsPlayableInIterationOne() {
        GameModel easy = new GameModel(new GameSettings(Difficulty.EASY, setup(2)), new Random(1));
        assertTrue(easy.roll().regular() >= 1);
        for (Difficulty later : new Difficulty[] {Difficulty.MEDIUM, Difficulty.HARD}) {
            GameModel game = new GameModel(new GameSettings(later, setup(2)), new Random(1));
            assertThrows(UnsupportedOperationException.class, game::roll, later.label());
        }
    }
}
