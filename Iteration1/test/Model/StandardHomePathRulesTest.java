package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link StandardHomePathRules} on the real Easy board (Task 18; SRS FR-17, FR-18, FR-31, FR-33). */
class StandardHomePathRulesTest {

    private final Board board = BoardFactory.create(Difficulty.EASY, new Random(5));
    private final HomePathRules rules = new StandardHomePathRules(board);
    private final int finish = board.finishIndex();
    private final int homeStart = board.homeStartIndex();

    private Piece pieceAt(PlayerColor color, int progress) {
        Piece piece = new Piece(color, 0, finish);
        piece.setProgress(progress);
        return piece;
    }

    private Piece pieceAt(int progress) {
        return pieceAt(PlayerColor.RED, progress);
    }

    private static DiceRoll regular(int value) {
        return new DiceRoll(value, DieType.NONE, 0, value, false, false);
    }

    private static DiceRoll boost(int value, int special) {
        return new DiceRoll(value, DieType.BOOST, special, value * (special - 1), false, false);
    }

    @Test
    @DisplayName("FR-17: on the shared track a piece moves by the Regular die on Easy")
    void trackPieceMovesByRoll() {
        for (int value = 1; value <= 6; value++) {
            assertEquals(value, rules.stepsFor(pieceAt(10), regular(value)));
        }
    }

    @Test
    @DisplayName("FR-17: on the shared track the special die counts, and a movement of 0 moves no piece")
    void trackPieceUsesMovement() {
        assertEquals(4, rules.stepsFor(pieceAt(10), boost(2, 3)));
        assertEquals(0, rules.stepsFor(pieceAt(10), boost(5, 1)));
    }

    @Test
    @DisplayName("FR-18: on the home path a piece moves by the Regular die only")
    void homePathUsesRegularDieOnly() {
        assertEquals(2, rules.stepsFor(pieceAt(homeStart), boost(2, 3)));
        assertEquals(3, rules.stepsFor(pieceAt(homeStart + 1), boost(3, 1)));
    }

    @Test
    @DisplayName("FR-18 boundary: the last shared-track square still uses the movement of the roll")
    void lastTrackSquareIsNotHomePath() {
        assertEquals(4, rules.stepsFor(pieceAt(homeStart - 1), boost(2, 3)));
    }

    @Test
    @DisplayName("FR-19: a piece in its castle has no steps, even on a 6; leaving the castle is the engine's move")
    void castlePieceHasNoSteps() {
        for (int value = 1; value <= 6; value++) {
            assertEquals(0, rules.stepsFor(pieceAt(-1), regular(value)));
            assertFalse(rules.canReach(pieceAt(-1), value));
        }
    }

    @Test
    @DisplayName("FR-31: a finished piece cannot move")
    void finishedPieceCannotMove() {
        assertEquals(0, rules.stepsFor(pieceAt(finish), regular(1)));
        assertFalse(rules.canReach(pieceAt(finish), 1));
    }

    @Test
    @DisplayName("FR-22: a cancelled roll gives no steps")
    void cancelledRollGivesNoSteps() {
        DiceRoll cancelled = new DiceRoll(6, DieType.NONE, 0, 6, false, true);
        assertEquals(0, rules.stepsFor(pieceAt(10), cancelled));
        assertEquals(0, rules.stepsFor(pieceAt(homeStart), cancelled));
    }

    @Test
    @DisplayName("FR-31, Q77: the exact number of steps reaches the finish from every square within a roll")
    void exactStepsReachFinish() {
        for (int steps = 1; steps <= 6; steps++) {
            Piece piece = pieceAt(finish - steps);
            assertTrue(rules.canReach(piece, steps), "steps " + steps);
            assertEquals(steps, rules.stepsFor(piece, regular(steps)), "steps " + steps);
        }
    }

    @Test
    @DisplayName("FR-31, Q77: one step too many is rejected, from every square within a roll")
    void overshootIsRejected() {
        for (int remaining = 1; remaining <= 5; remaining++) {
            Piece piece = pieceAt(finish - remaining);
            assertFalse(rules.canReach(piece, remaining + 1), "remaining " + remaining);
            assertEquals(0, rules.stepsFor(piece, regular(remaining + 1)), "remaining " + remaining);
        }
    }

    @Test
    @DisplayName("Q11: a roll smaller than the distance moves the piece along the home path without finishing")
    void shortRollAdvancesOnHomePath() {
        Piece piece = pieceAt(homeStart + 1);
        int steps = rules.stepsFor(piece, regular(2));
        assertEquals(2, steps);
        piece.setProgress(piece.progress() + steps);
        assertFalse(piece.finished());
        assertTrue(piece.onBoard());
    }

    @Test
    @DisplayName("FR-33: every colour enters its own home path from the shared track")
    void entersOwnHomePath() {
        for (PlayerColor color : PlayerColor.values()) {
            Piece piece = pieceAt(color, homeStart - 2);
            int steps = rules.stepsFor(piece, regular(4));
            assertEquals(4, steps, color.label());
            Square square = board.squareAt(board.cellAt(color, piece.progress() + steps));
            assertNotNull(square, color.label());
            assertEquals(SquareType.HOME_PATH, square.type(), color.label());
            assertEquals(color, square.owner(), color.label());
        }
    }

    @Test
    @DisplayName("FR-31, FR-43: an exact move reports the piece as finished for every colour")
    void exactMoveFinishesPiece() {
        for (PlayerColor color : PlayerColor.values()) {
            Piece piece = pieceAt(color, finish - 3);
            piece.setProgress(piece.progress() + rules.stepsFor(piece, regular(3)));
            assertTrue(piece.finished(), color.label());
        }
    }

    @Test
    @DisplayName("FR-31: zero or backward steps are not a move by these rules")
    void nonPositiveStepsAreRejected() {
        assertFalse(rules.canReach(pieceAt(10), 0));
        assertFalse(rules.canReach(pieceAt(10), -1));
    }

    @Test
    @DisplayName("FR-31: over every position and roll, a move is offered only when it stays on the path")
    void everyPositionAndRoll() {
        for (int progress = -1; progress <= finish; progress++) {
            for (int value = 1; value <= 6; value++) {
                Piece piece = pieceAt(progress);
                boolean expected = progress >= 0 && progress < finish && progress + value <= finish;
                assertEquals(expected ? value : 0, rules.stepsFor(piece, regular(value)),
                        "progress " + progress + ", roll " + value);
                assertEquals(expected, rules.canReach(piece, value), "progress " + progress + ", roll " + value);
            }
        }
    }

    @Test
    @DisplayName("NFR-11: null arguments are refused")
    void nullArgumentsAreRefused() {
        assertThrows(NullPointerException.class, () -> new StandardHomePathRules(null));
        assertThrows(NullPointerException.class, () -> rules.stepsFor(null, regular(1)));
        assertThrows(NullPointerException.class, () -> rules.stepsFor(pieceAt(1), null));
        assertThrows(NullPointerException.class, () -> rules.canReach(null, 1));
    }
}
