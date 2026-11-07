package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Phase A smoke tests for the {@link GameModel} skeleton (Task 27). They check that the shared contracts
 * are wired together and that a game can be created. They do <b>not</b> verify game rules: the rule tests
 * are written in Phase B (Tasks 05, 14, 20, 21, 25).
 *
 * <p>Owner: Rakan Ziyan.
 */
class GameModelSkeletonTest {

    private static GameSettings easy(int players) {
        PlayerColor[] colors = PlayerColor.values();
        List<GameSettings.PlayerSetup> list = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            list.add(new GameSettings.PlayerSetup("P" + (i + 1), colors[i]));
        }
        return new GameSettings(Difficulty.EASY, list);
    }

    private static GameModel model(int players) {
        return new GameModel(easy(players), new Random(1), new FixedDice(6, 1), () -> 0L);
    }

    @Test
    @DisplayName("A game model can be created for 2, 3 and 4 players")
    void createsGameForEveryPlayerCount() {
        for (int n = 2; n <= 4; n++) {
            GameModel m = model(n);
            assertEquals(n, m.players().size());
            assertEquals(Difficulty.EASY, m.difficulty());
            assertEquals(57, m.board().path(PlayerColor.RED).size(), "Easy path: 51 track + 5 home + finish");
        }
    }

    @Test
    @DisplayName("Initial state: first player in setup order, no roll, no legal moves, game not over")
    void initialState() {
        GameModel m = model(3);
        assertSame(m.players().get(0), m.currentPlayer());
        assertEquals(TurnPhase.AWAITING_ROLL, m.phase());
        assertNull(m.lastRoll());
        assertTrue(m.legalMoves().isEmpty());
        assertTrue(!m.isGameOver());
        for (Player p : m.players()) {
            assertEquals(4, p.pieces().size());
            assertTrue(p.pieces().stream().allMatch(Piece::inCastle));
        }
    }

    @Test
    @DisplayName("Observer: registered listeners receive model events")
    void listenersAreNotified() {
        GameModel m = model(2);
        List<GameEvent> received = new ArrayList<>();
        m.addListener(received::add);
        m.refresh();
        assertEquals(1, received.size());
        assertEquals(GameEvent.Type.STATE_CHANGED, received.get(0).type());
    }

    @Test
    @DisplayName("Phase A placeholders are not implemented yet (they must be replaced in Phase B)")
    void placeholdersAreNotImplemented() {
        GameModel m = model(2);
        assertThrows(UnsupportedOperationException.class, m::roll);
        assertThrows(UnsupportedOperationException.class, () -> m.move(m.players().get(0).pieces().get(0)));
    }

    @Test
    @DisplayName("Test helper FixedDice returns its values in order")
    void fixedDiceReturnsSequence() {
        FixedDice dice = new FixedDice(6, 3);
        assertEquals(6, dice.roll(Difficulty.EASY).regular());
        assertEquals(3, dice.roll(Difficulty.EASY).movement());
        assertEquals(0, dice.remaining());
        assertThrows(IllegalArgumentException.class, () -> new FixedDice(7));
    }
}
