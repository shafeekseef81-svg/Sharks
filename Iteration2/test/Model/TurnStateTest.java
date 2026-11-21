package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Author's tests for T2-13, the State pattern of the turn phases ({@link TurnState}, NFR-09, D3). Written by Rakan
 * Ziyan, who implemented it. The Iteration 1 turn tests (Tasks 11, 12, 14, 15) still check the rules; these tests
 * check the states: which phase follows which request, and that a request a state refuses changes nothing.
 *
 * <p>They use the real Easy board, move engine and ranking with fixed Regular dice.
 */
class TurnStateTest {

    private Player red;
    private Player green;

    private StandardTurnManager turns(int... rolls) {
        Board board = BoardFactory.create(Difficulty.EASY, new Random(1));
        red = new Player("Red", PlayerColor.RED, board.finishIndex());
        green = new Player("Green", PlayerColor.GREEN, board.finishIndex());
        List<Player> players = List.of(red, green);
        MoveEngine engine = new StandardMoveEngine(board, players, new StandardSquareRules(board, players),
                new StandardHomePathRules(board));
        return new StandardTurnManager(players, Difficulty.EASY, new FixedDice(rolls), engine,
                new StandardRankingTracker(players));
    }

    @Test
    @DisplayName("T2-13: each concrete state stands for one turn phase")
    void statesAndPhases() {
        assertEquals(TurnPhase.AWAITING_ROLL, new AwaitingRollState().phase());
        assertEquals(TurnPhase.AWAITING_MOVE, new AwaitingMoveState().phase());
        assertEquals(TurnPhase.MOVING, new MovedState().phase());
        assertEquals(TurnPhase.GAME_OVER, new GameOverState().phase());
    }

    @Test
    @DisplayName("T2-13 FR-19, FR-21: roll -> move -> extra roll -> move -> end of turn -> next player")
    void fullTurnCycle() {
        StandardTurnManager t = turns(6, 4);
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase());
        t.roll();
        assertEquals(TurnPhase.AWAITING_MOVE, t.phase());
        t.move(red.pieces().get(0)); // a 6 brings the piece out
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase(), "the 6 grants an extra roll (FR-21)");
        t.roll();
        assertEquals(TurnPhase.AWAITING_MOVE, t.phase());
        t.move(red.pieces().get(0));
        assertEquals(TurnPhase.MOVING, t.phase(), "no extra roll: wait for the end of the turn");
        assertNull(t.endTurn());
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase());
        assertSame(green, t.current());
    }

    @Test
    @DisplayName("T2-13 FR-23: without a legal move the turn ends from AWAITING_MOVE with a message")
    void noMoveEndsTheTurnWithAMessage() {
        StandardTurnManager t = turns(3);
        t.roll();
        assertEquals(TurnPhase.AWAITING_MOVE, t.phase());
        assertTrue(t.legalMoves().isEmpty());
        String message = t.endTurn();
        assertNotNull(message);
        assertTrue(message.contains("no legal move"), message);
        assertSame(green, t.current());
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase());
    }

    @Test
    @DisplayName("T2-13 FR-21: AWAITING_MOVE allows a new roll only after a 6 with which no piece can move")
    void rollFromAwaitingMove() {
        StandardTurnManager t = turns(6, 2);
        t.roll();
        assertFalse(t.canRoll(), "a piece can leave the castle, so it must move first (FR-20)");
        assertThrows(IllegalStateException.class, t::roll);
        assertEquals(TurnPhase.AWAITING_MOVE, t.phase());
    }

    @Test
    @DisplayName("T2-13: AWAITING_ROLL refuses a move and the end of the turn and changes nothing")
    void awaitingRollRefuses() {
        StandardTurnManager t = turns(3);
        assertTrue(t.legalMoves().isEmpty());
        assertThrows(IllegalStateException.class, () -> t.move(red.pieces().get(0)));
        assertThrows(IllegalStateException.class, t::endTurn);
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase());
        assertSame(red, t.current());
        assertNull(t.lastRoll());
    }

    @Test
    @DisplayName("T2-13 FR-20: AWAITING_MOVE refuses to end the turn while a legal move exists")
    void awaitingMoveRefusesToSkipAMove() {
        StandardTurnManager t = turns(6);
        DiceRoll roll = t.roll();
        assertThrows(IllegalStateException.class, t::endTurn);
        assertEquals(TurnPhase.AWAITING_MOVE, t.phase());
        assertSame(roll, t.lastRoll());
        assertSame(red, t.current());
    }

    @Test
    @DisplayName("T2-13: MOVING refuses a roll and a move and changes nothing")
    void movedRefuses() {
        StandardTurnManager t = turns(6, 4);
        t.roll();
        t.move(red.pieces().get(0));
        t.roll();
        t.move(red.pieces().get(0));
        DiceRoll last = t.lastRoll();
        int progress = red.pieces().get(0).progress();
        assertFalse(t.canRoll());
        assertThrows(IllegalStateException.class, t::roll);
        assertThrows(IllegalStateException.class, () -> t.move(red.pieces().get(0)));
        assertTrue(t.legalMoves().isEmpty());
        assertEquals(TurnPhase.MOVING, t.phase());
        assertSame(last, t.lastRoll());
        assertEquals(progress, red.pieces().get(0).progress());
    }

    @Test
    @DisplayName("T2-13 FR-44: the GAME_OVER state refuses every request")
    void gameOverRefusesEverything() {
        StandardTurnManager t = turns(1);
        TurnState over = new GameOverState();
        assertFalse(over.canRoll(t));
        assertTrue(over.legalMoves(t).isEmpty());
        assertThrows(IllegalStateException.class, () -> over.roll(t));
        assertThrows(IllegalStateException.class, () -> over.move(t, red.pieces().get(0)));
        assertThrows(IllegalStateException.class, () -> over.endTurn(t));
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase(), "a refused request changes nothing");
    }

    @Test
    @DisplayName("T2-13 FR-22: a cancelled third 6 offers no move; the turn ends from AWAITING_MOVE")
    void cancelledThirdSix() {
        StandardTurnManager t = turns(6, 6, 6);
        t.roll();
        t.move(red.pieces().get(0));
        t.roll();
        t.move(red.pieces().get(1));
        DiceRoll third = t.roll();
        assertTrue(third.cancelled());
        assertEquals(TurnPhase.AWAITING_MOVE, t.phase());
        assertTrue(t.legalMoves().isEmpty());
        assertFalse(t.canRoll());
        assertTrue(t.endTurn().contains("third 6"));
        assertSame(green, t.current());
    }
}
