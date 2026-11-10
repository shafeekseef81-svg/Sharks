package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Independent tests of turns and the Rule of 6 with fixed die sequences (Task 14: FR-15, FR-16 partial,
 * FR-17 partial, FR-21..FR-23). Written by Shafeek Saif, who did not implement {@link StandardTurnManager}
 * (SDP 5.3.4, 7.2).
 *
 * <p>The author's tests cover chosen cases. Here every die sequence of up to three rolls is checked against a
 * small expected-result calculation written from the SRS text, and only the frozen {@link TurnManager} contract
 * is used. The move engine and the ranking are replaced, so only the turn rules are under test.
 */
class TurnSequenceIndependentTest {

    /** Move engine double: offers one move to the active player, or none. */
    private static final class Engine implements MoveEngine {
        boolean canMove;
        int applied;

        @Override
        public Map<Piece, Move> legalMoves(Player player, DiceRoll roll) {
            Map<Piece, Move> moves = new LinkedHashMap<>();
            if (canMove && !roll.cancelled()) {
                Piece piece = player.pieces().get(0);
                moves.put(piece, new Move(piece, 1));
            }
            return moves;
        }

        @Override
        public MoveResult apply(Move move) {
            applied++;
            return MoveResult.none(move.piece());
        }

        @Override
        public boolean isPassable(Cell cell, Player player) {
            return true;
        }
    }

    /** Ranking double in which nobody finishes. */
    private static final class NoFinish implements RankingTracker {
        @Override
        public void onPieceFinished(Player player) {
        }

        @Override
        public boolean isGameOver() {
            return false;
        }

        @Override
        public List<Player> ranking() {
            return List.of();
        }

        @Override
        public boolean hasFinished(Player player) {
            return false;
        }
    }

    private static List<Player> players(int count) {
        List<Player> list = new ArrayList<>();
        PlayerColor[] colors = PlayerColor.values();
        for (int i = 0; i < count; i++) {
            list.add(new Player("P" + (i + 1), colors[i], 56));
        }
        return list;
    }

    private static TurnManager manager(List<Player> players, Engine engine, int... dice) {
        return new StandardTurnManager(players, Difficulty.EASY, new FixedDice(dice), engine, new NoFinish());
    }

    /** Every complete turn: 1-5; a 6 then 1-5; two 6s then 1-5; and three 6s. */
    private static List<int[]> sequences() {
        List<int[]> result = new ArrayList<>();
        for (int last = 1; last <= 5; last++) {
            result.add(new int[] {last});
            result.add(new int[] {6, last});
            result.add(new int[] {6, 6, last});
        }
        result.add(new int[] {6, 6, 6});
        return result;
    }

    private static void playSequence(int[] seq, boolean canMove) {
        List<Player> players = players(3);
        Engine engine = new Engine();
        engine.canMove = canMove;
        TurnManager t = manager(players, engine, seq);
        String where = java.util.Arrays.toString(seq) + " canMove=" + canMove;
        int expectedMoves = 0;

        for (int i = 0; i < seq.length; i++) {
            DiceRoll roll = t.roll();
            boolean third = i == 2 && seq[0] == 6 && seq[1] == 6 && seq[2] == 6;
            boolean six = seq[i] == 6;

            assertEquals(seq[i], roll.regular(), where);
            assertEquals(DieType.NONE, roll.specialType(), "FR-16: Easy has only the Regular die, " + where);
            assertEquals(seq[i], roll.movement(), "FR-17: Easy movement is the Regular die, " + where);
            assertEquals(third, roll.cancelled(), "FR-22: " + where);
            assertSame(players.get(0), t.current(), "FR-15: the turn stays with the roller, " + where);
            assertEquals(canMove && !third, !t.legalMoves().isEmpty(), "FR-20/FR-22: " + where);
            assertEquals(six && !third, t.hasBonusRoll(), "FR-21/FR-22: " + where);

            if (canMove && !third) {
                t.move(players.get(0).pieces().get(0));
                expectedMoves++;
                assertTrue(t.legalMoves().isEmpty(), "FR-20: one move per roll, " + where);
            }
        }
        assertEquals(expectedMoves, engine.applied, where);
        assertFalse(t.hasBonusRoll(), where);
        String message = t.endTurn();
        boolean lastWasCancelled = seq.length == 3 && seq[2] == 6;
        if (canMove && !lastWasCancelled) {
            assertNull(message, "a move was made, so no pass message: " + where);
        } else {
            assertNotNull(message, "FR-22/FR-23: the player is told why the turn passed, " + where);
            assertFalse(message.isBlank(), where);
        }
        assertSame(players.get(1), t.current(), "FR-15: the next player follows, " + where);
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase(), where);
    }

    @Test
    @DisplayName("T14 FR-16, FR-17, FR-21, FR-22: every sequence of up to three rolls, with a legal move")
    void everySequenceWithMove() {
        for (int[] seq : sequences()) {
            playSequence(seq, true);
        }
    }

    @Test
    @DisplayName("T14 FR-21, FR-22, FR-23: every sequence of up to three rolls, without a legal move")
    void everySequenceWithoutMove() {
        for (int[] seq : sequences()) {
            playSequence(seq, false);
        }
    }

    @Test
    @DisplayName("T14 FR-22: a 6, then a non-6 and a new turn, then 6 6: the count of 6s does not carry over")
    void sixesDoNotCarryOverBetweenPlayers() {
        List<Player> players = players(2);
        Engine engine = new Engine();
        engine.canMove = true;
        TurnManager t = manager(players, engine, 6, 6, 2, 6, 6, 2);
        for (int who = 0; who < 2; who++) {
            for (int roll = 0; roll < 3; roll++) {
                assertFalse(t.roll().cancelled(), "player " + who + " roll " + roll);
                t.move(players.get(who).pieces().get(0));
            }
            assertNull(t.endTurn());
        }
    }

    @Test
    @DisplayName("T14 FR-15: with 4 players, mixed sequences keep the setup order and wrap around")
    void orderWithMixedSequences() {
        List<Player> players = players(4);
        Engine engine = new Engine();
        TurnManager t = manager(players, engine, 3, 6, 2, 1, 6, 6, 6, 5, 4);
        int[][] rollsPerTurn = {{3}, {6, 2}, {1}, {6, 6, 6}, {5}, {4}};
        int[] expected = {0, 1, 2, 3, 0, 1};
        for (int turn = 0; turn < rollsPerTurn.length; turn++) {
            assertSame(players.get(expected[turn]), t.current(), "turn " + turn);
            for (int ignored : rollsPerTurn[turn]) {
                t.roll();
            }
            assertNotNull(t.endTurn(), "no piece could move in turn " + turn);
        }
    }

    @Test
    @DisplayName("T14 FR-21: a pending extra roll must be used: the turn cannot end, and a second roll needs a move")
    void extraRollAndMoveOrder() {
        List<Player> players = players(2);
        Engine engine = new Engine();
        engine.canMove = true;
        TurnManager t = manager(players, engine, 6, 3);
        t.roll();
        assertThrows(IllegalStateException.class, t::roll, "the 6 must be used for the legal move first (FR-20)");
        assertThrows(IllegalStateException.class, t::endTurn, "a legal move may not be skipped (FR-20)");
        t.move(players.get(0).pieces().get(0));
        assertThrows(IllegalStateException.class, t::endTurn, "the extra roll is still pending (FR-21)");
        t.roll();
        t.move(players.get(0).pieces().get(0));
        assertNull(t.endTurn());
        assertSame(players.get(1), t.current());
    }

    @Test
    @DisplayName("T14 FR-22: after a cancelled third 6 no further roll or move is possible in that turn")
    void nothingAfterCancelledRoll() {
        List<Player> players = players(2);
        Engine engine = new Engine();
        engine.canMove = true;
        TurnManager t = manager(players, engine, 6, 6, 6, 1);
        Piece piece = players.get(0).pieces().get(0);
        t.roll();
        t.move(piece);
        t.roll();
        t.move(piece);
        assertTrue(t.roll().cancelled());
        assertTrue(t.legalMoves().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> t.move(piece));
        assertThrows(IllegalStateException.class, t::roll);
        assertEquals(2, engine.applied);
    }

    @Test
    @DisplayName("T14 FR-20: the roll cannot be used by a piece that was not offered")
    void onlyOfferedPieceMoves() {
        List<Player> players = players(2);
        Engine engine = new Engine();
        engine.canMove = true;
        TurnManager t = manager(players, engine, 3);
        t.roll();
        assertThrows(IllegalArgumentException.class, () -> t.move(players.get(0).pieces().get(1)));
        assertThrows(IllegalArgumentException.class, () -> t.move(players.get(1).pieces().get(0)));
        assertEquals(0, engine.applied);
        assertThrows(IllegalStateException.class, t::endTurn, "a legal move may not be skipped, also without a 6");
    }
}
