package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Acceptance tests of US-1.4 "leave the castle, move, capture and finish by the rules" on Easy (Task 22:
 * FR-18, FR-20, FR-31, FR-33; FR-19, FR-25..FR-27 partial), written by the Product Owner. Every test plays
 * through the {@link GameModel} facade with the real board and the real rule classes together.
 *
 * <p>Automated here: the model part. <b>Still manual:</b> the square-by-square animation (NFR-04).
 */
class MovementAcceptanceTest {

    /** Dice whose next values the test chooses. */
    private static final class ScriptDice implements Dice {
        final Deque<Integer> next = new ArrayDeque<>();

        @Override
        public DiceRoll roll(Difficulty level) {
            return DiceRoll.regularOnly(next.remove());
        }
    }

    private final ScriptDice dice = new ScriptDice();
    private GameModel model;
    private Player red;
    private Player green;
    private int finish;
    private int homeStart;

    /** A two-player game, Red first, whose board has plain squares where {@code condition} needs them. */
    private void start(java.util.function.Predicate<Board> condition) {
        List<GameSettings.PlayerSetup> setup = List.of(new GameSettings.PlayerSetup("Red", PlayerColor.RED),
                new GameSettings.PlayerSetup("Green", PlayerColor.GREEN));
        GameSettings settings = new GameSettings(Difficulty.EASY, setup);
        for (long seed = 0; seed < 5000; seed++) {
            GameModel candidate = new GameModel(settings, new Random(seed), dice, () -> 0L);
            if (condition.test(candidate.board())) {
                model = candidate;
                red = model.players().get(0);
                green = model.players().get(1);
                finish = model.board().finishIndex();
                homeStart = model.board().homeStartIndex();
                return;
            }
        }
        throw new AssertionError("No board satisfies the scenario");
    }

    private void start() {
        start(b -> true);
    }

    private static Square at(Board board, PlayerColor color, int progress) {
        return board.squareAt(board.cellAt(color, progress));
    }

    private static boolean plain(Board board, PlayerColor color, int progress) {
        return at(board, color, progress).type() == SquareType.TRACK;
    }

    private DiceRoll roll(int value) {
        dice.next.add(value);
        return model.roll();
    }

    private MoveResult play(int value, Piece piece) {
        roll(value);
        return model.move(piece);
    }

    /** Progress of a colour on the same square as another colour's progress. */
    private int sameSquare(PlayerColor color, PlayerColor other, int otherProgress) {
        return model.board().path(color).indexOf(model.board().cellAt(other, otherProgress));
    }

    @Test
    @DisplayName("T22 FR-19: a piece leaves the castle only on a 6, onto its own start square")
    void castleExitOnlyOnSix() {
        start();
        for (int value = 1; value <= 5; value++) {
            Player roller = model.currentPlayer();
            roll(value);
            assertTrue(model.legalMoves().isEmpty(), "die " + value);
            assertTrue(roller.pieces().stream().allMatch(Piece::inCastle));
        }
        Player roller = model.currentPlayer();
        Piece piece = roller.pieces().get(0);
        MoveResult r = play(6, piece);
        assertEquals(0, piece.progress());
        assertEquals(MoveResult.Kind.EXIT_CASTLE, r.kind());
        assertEquals(SquareType.START, r.landedOn().type());
        assertEquals(roller.color(), r.landedOn().owner());
    }

    @Test
    @DisplayName("T22 FR-20: one roll moves exactly one piece by the whole die value")
    void oneRollOnePiece() {
        start();
        Piece a = red.pieces().get(0);
        Piece b = red.pieces().get(1);
        a.setProgress(4);
        b.setProgress(9);
        roll(3);
        assertEquals(Set.of(a, b), model.legalMoves().keySet());
        MoveResult r = model.move(b);
        assertEquals(12, b.progress());
        assertEquals(4, a.progress(), "the steps are not split");
        assertEquals(4, r.cells().size(), "start square plus three steps");
        assertTrue(model.legalMoves().isEmpty());
    }

    @Test
    @DisplayName("T22 FR-25, FR-33: a piece walks clockwise from start to finish, past other arrows, into its own home")
    void fullWalkOfOnePiece() {
        start();
        Piece piece = red.pieces().get(0);
        play(6, piece);
        List<Cell> walked = new ArrayList<>(List.of(model.board().startCell(PlayerColor.RED)));
        while (!piece.finished()) {
            if (model.currentPlayer() != red) {
                roll(1); // Green has no piece on the board, so the turn comes back
                continue;
            }
            int value = Math.min(5, finish - piece.progress());
            MoveResult r = play(value, piece);
            walked.addAll(r.cells().subList(1, r.cells().size()));
        }
        assertEquals(model.board().path(PlayerColor.RED), walked);
        for (int p = 0; p < homeStart; p++) {
            assertFalse(at(model.board(), PlayerColor.RED, p).type() == SquareType.HOME_PATH, "shared track " + p);
        }
        for (int p = homeStart; p < finish; p++) {
            Square s = at(model.board(), PlayerColor.RED, p);
            assertEquals(SquareType.HOME_PATH, s.type());
            assertEquals(PlayerColor.RED, s.owner(), "FR-33: only the own home path");
        }
    }

    @Test
    @DisplayName("T22 FR-26: from the start of a game, landing on a lone Green piece sends it back to its castle")
    void captureInRealPlayFromTheStart() {
        // Green's progress 3 and Red's progress 16 are the same square; it must be a plain one.
        start(b -> plain(b, PlayerColor.GREEN, 3));
        assertEquals(16, sameSquare(PlayerColor.RED, PlayerColor.GREEN, 3));
        Piece hunter = red.pieces().get(0);
        Piece prey = green.pieces().get(0);

        play(6, hunter);
        play(6, hunter);
        play(4, hunter);
        assertEquals(10, hunter.progress());
        play(6, prey);
        play(3, prey);
        assertEquals(3, prey.progress());
        assertSame(red, model.currentPlayer());

        MoveResult r = play(6, hunter);

        assertEquals(16, hunter.progress());
        assertTrue(prey.inCastle(), "FR-26: back to the castle (staff Q41)");
        assertEquals(List.of(new MoveResult.Capture(prey, model.board().cellAt(PlayerColor.GREEN, 3))), r.captures());

        assertSame(red, model.currentPlayer(), "FR-21: the 6 that captured still gives its extra roll");
        assertTrue(model.hasBonusRoll());
        play(1, hunter);
        assertSame(green, model.currentPlayer(), "FR-21: exactly one extra roll; the capture adds none");

        roll(5);
        assertTrue(prey.inCastle(), "the captured piece needs a new 6 to leave");
        assertSame(red, model.currentPlayer());
    }

    @Test
    @DisplayName("T22 FR-21, FR-26: a capture with a roll other than 6 gives no extra roll (staff Q57)")
    void captureWithoutSixEndsTurn() {
        start(b -> plain(b, PlayerColor.RED, 20));
        Piece hunter = red.pieces().get(0);
        hunter.setProgress(17);
        Piece prey = green.pieces().get(1);
        prey.setProgress(sameSquare(PlayerColor.GREEN, PlayerColor.RED, 20));
        MoveResult r = play(3, hunter);
        assertEquals(1, r.captures().size());
        assertTrue(prey.inCastle());
        assertSame(green, model.currentPlayer());
    }

    @Test
    @DisplayName("T22 FR-27: a piece on a protection square is not captured and both pieces stay (staff Q54)")
    void protectionSquareBlocksCapture() {
        start();
        Board board = model.board();
        int star = -1;
        for (int p = 2; p < homeStart && star < 0; p++) {
            if (at(board, PlayerColor.RED, p).type() == SquareType.PROTECTION) {
                star = p;
            }
        }
        assertTrue(star > 1, "an Easy board has protection squares on the red path");
        Piece hunter = red.pieces().get(0);
        hunter.setProgress(star - 1);
        Piece safe = green.pieces().get(0);
        safe.setProgress(sameSquare(PlayerColor.GREEN, PlayerColor.RED, star));

        MoveResult r = play(1, hunter);

        assertTrue(r.captures().isEmpty());
        assertEquals(star, hunter.progress());
        assertTrue(safe.onBoard());
        assertEquals(board.cellAt(PlayerColor.RED, hunter.progress()), board.cellAt(PlayerColor.GREEN, safe.progress()));
    }

    @Test
    @DisplayName("T22 FR-19, FR-27: leaving the castle onto a start square with an opponent captures nothing (Q80)")
    void castleExitOntoOpponentDoesNotCapture() {
        start();
        Piece visitor = green.pieces().get(0);
        visitor.setProgress(sameSquare(PlayerColor.GREEN, PlayerColor.RED, 0));
        MoveResult r = play(6, red.pieces().get(0));
        assertTrue(r.captures().isEmpty());
        assertTrue(visitor.onBoard());
    }

    @Test
    @DisplayName("T22 FR-25, FR-26: another colour's arrow is a plain square, so a piece on it can be captured (Q104)")
    void arrowOfOtherColourCanCapture() {
        start();
        Board board = model.board();
        int arrow = -1;
        for (int p = 1; p < homeStart - 1 && arrow < 0; p++) {
            Square s = at(board, PlayerColor.RED, p);
            if (s.type() == SquareType.ARROW && s.owner() != PlayerColor.RED) {
                arrow = p;
            }
        }
        assertTrue(arrow > 0);
        Piece hunter = red.pieces().get(0);
        hunter.setProgress(arrow - 2);
        Piece prey = green.pieces().get(0);
        prey.setProgress(sameSquare(PlayerColor.GREEN, PlayerColor.RED, arrow));
        MoveResult r = play(2, hunter);
        assertEquals(1, r.captures().size());
        assertTrue(prey.inCastle());
    }

    @Test
    @DisplayName("T22 FR-26 (PO decision, issue 13): two opponent pieces on a plain square are not captured")
    void twoOpponentsAreNotCaptured() {
        start(b -> plain(b, PlayerColor.RED, 22));
        Piece hunter = red.pieces().get(0);
        hunter.setProgress(18);
        int shared = sameSquare(PlayerColor.GREEN, PlayerColor.RED, 22);
        green.pieces().get(0).setProgress(shared);
        green.pieces().get(1).setProgress(shared);
        MoveResult r = play(4, hunter);
        assertTrue(r.captures().isEmpty());
        assertEquals(22, hunter.progress());
        assertEquals(shared, green.pieces().get(0).progress());
        assertEquals(shared, green.pieces().get(1).progress());
    }

    @Test
    @DisplayName("T22 FR-26: a piece passed on the way is not captured, only one on the final square")
    void passingDoesNotCapture() {
        start(b -> plain(b, PlayerColor.RED, 30));
        Piece hunter = red.pieces().get(0);
        hunter.setProgress(28);
        Piece passed = green.pieces().get(0);
        passed.setProgress(sameSquare(PlayerColor.GREEN, PlayerColor.RED, 30));
        MoveResult r = play(4, hunter);
        assertTrue(r.captures().isEmpty());
        assertTrue(passed.onBoard());
    }

    @Test
    @DisplayName("T22 FR-18, FR-33: entering and moving on the home path by the die; no capture is possible there")
    void homePathEntryAndMovement() {
        start();
        Piece piece = red.pieces().get(0);
        piece.setProgress(homeStart - 1);
        MoveResult entering = play(2, piece);
        assertEquals(homeStart + 1, piece.progress());
        assertNull(entering.landedOn());
        assertTrue(entering.captures().isEmpty());
        assertEquals(SquareType.HOME_PATH, at(model.board(), PlayerColor.RED, piece.progress()).type());
        assertEquals(PlayerColor.RED, at(model.board(), PlayerColor.RED, piece.progress()).owner());
    }

    @Test
    @DisplayName("T22 FR-31: finishing needs the exact number; overshoot moves another piece or passes the turn (Q77)")
    void exactFinish() {
        start();
        Piece near = red.pieces().get(0);
        near.setProgress(finish - 3);

        roll(4);
        assertTrue(model.legalMoves().isEmpty(), "overshoot: no other piece can move");
        assertEquals(finish - 3, near.progress());
        assertSame(green, model.currentPlayer(), "the turn passes (FR-23)");

        roll(1); // Green has nothing on the board
        Piece other = red.pieces().get(1);
        other.setProgress(5);
        roll(4);
        assertEquals(Set.of(other), model.legalMoves().keySet(), "another legal piece must move");
        model.move(other);

        roll(1); // Green again
        MoveResult done = play(3, near);
        assertTrue(near.finished());
        assertTrue(done.pieceFinished());
        assertNull(done.landedOn());
        assertFalse(done.playerFinished());
    }

    @Test
    @DisplayName("T22 FR-21: finishing with a 6 keeps the extra roll of the 6; finishing with another value gives none")
    void finishingGrantsNoRollOfItsOwn() {
        start();
        Piece six = red.pieces().get(0);
        six.setProgress(finish - 6);
        play(6, six);
        assertTrue(six.finished());
        assertSame(red, model.currentPlayer());
        Piece two = red.pieces().get(1);
        two.setProgress(finish - 2);
        play(2, two);
        assertTrue(two.finished());
        assertSame(green, model.currentPlayer());
    }
}
