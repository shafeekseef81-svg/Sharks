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
    @DisplayName("FR-19 P, FR-21 with the real engine: a 6 lets any castle piece out, and the player rolls again")
    void realEngineSixLeavesTheCastleAndKeepsTheTurn() {
        GameModel m = model(2);
        Player first = m.currentPlayer();
        List<GameEvent> events = new ArrayList<>();
        m.addListener(events::add);

        assertEquals(6, m.roll().regular());
        assertEquals(TurnPhase.AWAITING_MOVE, m.phase());
        assertEquals(4, m.legalMoves().size(), "every piece in the castle may leave on a 6");
        assertTrue(m.legalMoves().keySet().containsAll(first.pieces()));

        Piece piece = first.pieces().get(0);
        MoveResult result = m.move(piece);
        assertEquals(MoveResult.Kind.EXIT_CASTLE, result.kind());
        assertTrue(piece.onBoard());
        assertSame(first, m.currentPlayer(), "FR-21: the 6 keeps the turn");
        assertTrue(m.hasBonusRoll());
        assertEquals(TurnPhase.AWAITING_ROLL, m.phase());
        assertEquals(List.of(GameEvent.Type.ROLLED, GameEvent.Type.MOVED),
                events.stream().map(GameEvent::type).toList());
    }

    @Test
    @DisplayName("FR-23 with the real engine: no piece can leave the castle without a 6, so the turn passes")
    void realEngineNoMovePassesTheTurn() {
        GameModel m = new GameModel(easy(2), new Random(1), new FixedDice(1), () -> 0L);
        Player first = m.players().get(0);
        List<GameEvent> events = new ArrayList<>();
        m.addListener(events::add);

        m.roll();
        assertTrue(m.legalMoves().isEmpty());
        assertSame(m.players().get(1), m.currentPlayer());
        assertEquals(TurnPhase.AWAITING_ROLL, m.phase());
        assertEquals(List.of(GameEvent.Type.ROLLED, GameEvent.Type.TURN_PASSED, GameEvent.Type.TURN_CHANGED),
                events.stream().map(GameEvent::type).toList());
        assertSame(first, events.get(1).player());
        assertTrue(first.pieces().stream().allMatch(Piece::inCastle));
    }

    @Test
    @DisplayName("FR-22 with the real engine: after two 6s and two moves, the third 6 is cancelled")
    void realEngineThirdSixIsCancelled() {
        GameModel m = new GameModel(easy(2), new Random(1), new FixedDice(6, 6, 6), () -> 0L);
        Player first = m.currentPlayer();
        m.roll();
        m.move(first.pieces().get(0));
        m.roll();
        m.move(first.pieces().get(1));
        assertSame(first, m.currentPlayer());

        m.roll();
        assertTrue(m.legalMoves().isEmpty(), "the third 6 gives no move");
        assertSame(m.players().get(1), m.currentPlayer(), "the turn ends");
        assertEquals(2, first.pieces().stream().filter(Piece::onBoard).count());
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
