package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Task 21: independent unit tests for capture, protection and finishing (SRS FR-18, FR-26 P, FR-27 P, FR-31,
 * FR-33), written from the SRS text by Rakan Ziyan, who did not implement these rules (SDP 5.3.4, Definition of
 * Done). They test the real {@link StandardSquareRules} and {@link StandardHomePathRules} (Shafeek Saif, Tasks 17
 * and 18), alone and through the real {@link StandardMoveEngine} on a real Easy board.
 *
 * <p>Iteration 1 scope: no Double Blocks (FR-28..FR-30), no questions or surprise effects.
 */
class CaptureProtectionFinishTest {

    private Board board;
    private Player red;
    private Player green;
    private SquareRules squareRules;
    private HomePathRules homeRules;
    private MoveEngine engine;

    @BeforeEach
    void setUp() {
        board = BoardFactory.create(Difficulty.EASY, new Random(7));
        red = new Player("Red", PlayerColor.RED, board.finishIndex());
        green = new Player("Green", PlayerColor.GREEN, board.finishIndex());
        List<Player> players = List.of(red, green);
        squareRules = new StandardSquareRules(board, players);
        homeRules = new StandardHomePathRules(board);
        engine = new StandardMoveEngine(board, players, squareRules, homeRules);
    }

    // ------------------------------------------------------------------ helpers

    private Piece piece(Player p, int i) {
        return p.pieces().get(i);
    }

    /** Red progress of the first shared-track square at or after {@code from} whose type is {@code type}. */
    private int redProgressOf(SquareType type, int from) {
        for (int p = from; p < board.homeStartIndex(); p++) {
            Square s = board.squareAt(board.cellAt(PlayerColor.RED, p));
            if (s.type() == type && greenProgressAt(s.cell()) >= 0) {
                return p;
            }
        }
        throw new AssertionError("no " + type + " square on the shared red/green track");
    }

    /** Green progress for a cell on Green's path before its home path, or -1. */
    private int greenProgressAt(Cell cell) {
        for (int p = 0; p < board.homeStartIndex(); p++) {
            if (board.cellAt(PlayerColor.GREEN, p).equals(cell)) {
                return p;
            }
        }
        return -1;
    }

    /** Puts a Green piece on the square that Red reaches at {@code redProgress}. */
    private Piece greenOnRedSquare(int index, int redProgress) {
        Piece g = piece(green, index);
        int gp = greenProgressAt(board.cellAt(PlayerColor.RED, redProgress));
        assertTrue(gp >= 0, "the square must be on Green's shared track");
        g.setProgress(gp);
        return g;
    }

    /** Moves a Red piece from {@code from} by {@code roll} through the real engine. */
    private MoveResult redMoves(int from, int roll) {
        Piece r = piece(red, 0);
        r.setProgress(from);
        Map<Piece, Move> legal = engine.legalMoves(red, DiceRoll.regularOnly(roll));
        Move move = legal.get(r);
        assertNotNull(move, "the move must be legal");
        return engine.apply(move);
    }

    // ------------------------------------------------------------------ FR-26 capture

    @Test
    @DisplayName("FR-26: landing on a plain square with one opponent piece captures it")
    void loneOpponentIsCaptured() {
        int target = redProgressOf(SquareType.TRACK, 3);
        Piece g = greenOnRedSquare(0, target);
        assertEquals(List.of(g), squareRules.capturesOnLanding(piece(red, 0), board.cellAt(PlayerColor.RED, target)));
    }

    @Test
    @DisplayName("FR-26: after the real move the captured piece is back in its castle and the mover holds the square")
    void captureThroughTheEngineSendsThePieceHome() {
        int target = redProgressOf(SquareType.TRACK, 3);
        Piece g = greenOnRedSquare(0, target);
        MoveResult result = redMoves(target - 3, 3);
        assertTrue(g.inCastle(), "FR-26: back to the castle");
        assertEquals(target, piece(red, 0).progress());
        assertEquals(1, result.captures().size());
        assertEquals(g, result.captures().get(0).piece());
    }

    @Test
    @DisplayName("FR-26: a piece never captures a piece of its own colour")
    void ownPieceIsNotCaptured() {
        int target = redProgressOf(SquareType.TRACK, 3);
        piece(red, 1).setProgress(target);
        assertTrue(squareRules.capturesOnLanding(piece(red, 0), board.cellAt(PlayerColor.RED, target)).isEmpty());
    }

    @Test
    @DisplayName("FR-26: only landing captures; passing over an opponent piece leaves it in place")
    void passingDoesNotCapture() {
        int over = redProgressOf(SquareType.TRACK, 3);
        int land = over + 1;
        while (board.squareAt(board.cellAt(PlayerColor.RED, land)).isProtected()) {
            land++;
        }
        Piece g = greenOnRedSquare(0, over);
        int gp = g.progress();
        redMoves(over - 1, land - over + 1);
        assertEquals(gp, g.progress(), "passed, not captured");
    }

    @Test
    @DisplayName("FR-26 (\"one opponent piece\", reading recorded in issue #13): two opponent pieces are not captured")
    void twoOpponentPiecesAreNotCaptured() {
        int target = redProgressOf(SquareType.TRACK, 3);
        greenOnRedSquare(0, target);
        greenOnRedSquare(1, target);
        assertTrue(squareRules.capturesOnLanding(piece(red, 0), board.cellAt(PlayerColor.RED, target)).isEmpty());
    }

    // ------------------------------------------------------------------ FR-27 protection

    @Test
    @DisplayName("FR-27: protection, starting and surprise squares and the finishing square are protected; plain ones are not")
    void protectedSquareTypes() {
        for (Square s : board.squares()) {
            boolean expected = s.type() == SquareType.PROTECTION || s.type() == SquareType.START
                    || s.type() == SquareType.SURPRISE;
            assertEquals(expected, squareRules.isProtected(s), s.type() + " at " + s.cell());
        }
        assertTrue(squareRules.isProtected(null), "the finishing square");
    }

    @Test
    @DisplayName("FR-27: on a protection square the opponent piece is not captured and both pieces share the square")
    void protectionSquareLetsPiecesCoexist() {
        int target = redProgressOf(SquareType.PROTECTION, 3);
        Piece g = greenOnRedSquare(0, target);
        int gp = g.progress();
        MoveResult result = redMoves(target - 2, 2);
        assertTrue(result.captures().isEmpty());
        assertEquals(gp, g.progress());
        assertEquals(board.cellAt(PlayerColor.RED, target), board.cellAt(PlayerColor.GREEN, g.progress()));
    }

    @Test
    @DisplayName("FR-27: on an opponent's starting square nothing is captured")
    void startingSquareIsSafe() {
        int target = greenProgressAt(board.startCell(PlayerColor.GREEN)) == 0
                ? redProgressAtCell(board.startCell(PlayerColor.GREEN)) : -1;
        assertTrue(target > 1, "Red passes Green's starting square");
        piece(green, 0).setProgress(0);
        MoveResult result = redMoves(target - 1, 1);
        assertTrue(result.captures().isEmpty());
        assertEquals(0, piece(green, 0).progress());
    }

    @Test
    @DisplayName("FR-27: a surprise square is protected too (Iteration 2 boards have them; checked by marking one)")
    void surpriseSquareIsSafe() {
        int target = redProgressOf(SquareType.TRACK, 3);
        board.squareAt(board.cellAt(PlayerColor.RED, target)).setType(SquareType.SURPRISE);
        greenOnRedSquare(0, target);
        assertTrue(squareRules.capturesOnLanding(piece(red, 0), board.cellAt(PlayerColor.RED, target)).isEmpty());
    }

    private int redProgressAtCell(Cell cell) {
        for (int p = 0; p < board.homeStartIndex(); p++) {
            if (board.cellAt(PlayerColor.RED, p).equals(cell)) {
                return p;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ FR-33 home path

    @Test
    @DisplayName("FR-33: over 100 boards a home path has no special squares and lies on no other colour's path")
    void homePathsAreClean() {
        for (int seed = 0; seed < 100; seed++) {
            Board b = BoardFactory.create(Difficulty.EASY, new Random(seed));
            for (PlayerColor owner : PlayerColor.values()) {
                Set<Cell> home = new HashSet<>();
                for (int p = b.homeStartIndex(); p < b.finishIndex(); p++) {
                    Square s = b.squareAt(b.cellAt(owner, p));
                    assertEquals(SquareType.HOME_PATH, s.type(), "seed " + seed);
                    assertEquals(owner, s.owner(), "seed " + seed);
                    home.add(s.cell());
                }
                for (PlayerColor other : PlayerColor.values()) {
                    if (other != owner) {
                        for (int p = 0; p <= b.finishIndex(); p++) {
                            assertFalse(home.contains(b.cellAt(other, p)),
                                    other + " path enters the home path of " + owner + ", seed " + seed);
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("FR-33: nothing is ever captured on a home path")
    void noCaptureOnAHomePath() {
        int home = board.homeStartIndex() + 1;
        piece(red, 1).setProgress(home);
        assertTrue(squareRules.capturesOnLanding(piece(red, 0), board.cellAt(PlayerColor.RED, home)).isEmpty());
    }

    // ------------------------------------------------------------------ FR-18, FR-31 finishing

    @Test
    @DisplayName("FR-31: finishing needs the exact number of steps; overshooting is not a move")
    void exactFinish() {
        Piece r = piece(red, 0);
        r.setProgress(board.finishIndex() - 3);
        assertTrue(homeRules.canReach(r, 3));
        assertFalse(homeRules.canReach(r, 4));
        assertEquals(3, homeRules.stepsFor(r, DiceRoll.regularOnly(3)));
        assertEquals(0, homeRules.stepsFor(r, DiceRoll.regularOnly(4)));
    }

    @Test
    @DisplayName("FR-31: with an overshooting piece, another piece that can move must move")
    void overshootingPieceIsNotLegalButAnotherIs() {
        piece(red, 0).setProgress(board.finishIndex() - 2);
        piece(red, 1).setProgress(redProgressOf(SquareType.TRACK, 3));
        Map<Piece, Move> legal = engine.legalMoves(red, DiceRoll.regularOnly(5));
        assertFalse(legal.containsKey(piece(red, 0)));
        assertTrue(legal.containsKey(piece(red, 1)));
    }

    @Test
    @DisplayName("FR-31, FR-23: when the only piece on the board would overshoot, no move is legal")
    void noLegalMoveWhenOnlyAnOvershootIsPossible() {
        piece(red, 0).setProgress(board.finishIndex() - 2);
        assertTrue(engine.legalMoves(red, DiceRoll.regularOnly(5)).isEmpty());
    }

    @Test
    @DisplayName("FR-31: the exact roll takes the piece to the finishing square and it counts as finished")
    void exactRollFinishes() {
        MoveResult result = redMoves(board.finishIndex() - 4, 4);
        assertTrue(piece(red, 0).finished());
        // Reaching the finish by exact steps is animated step by step (TO_FINISH is a jump, used by later effects).
        assertEquals(MoveResult.Kind.STEPS, result.kind());
        assertTrue(result.pieceFinished());
        assertEquals(board.cellAt(PlayerColor.RED, board.finishIndex()), result.cells().get(result.cells().size() - 1));
        assertEquals(1, red.finishedCount());
    }

    @Test
    @DisplayName("FR-18: on the home path a piece moves by the Regular die only; before it, by the full movement")
    void homePathUsesTheRegularDie() {
        DiceRoll boosted = new DiceRoll(2, DieType.BOOST, 3, 4, false, false);
        Piece r = piece(red, 0);
        r.setProgress(board.homeStartIndex());
        assertEquals(2, homeRules.stepsFor(r, boosted), "home path: Regular die only");
        r.setProgress(redProgressOf(SquareType.TRACK, 3));
        assertEquals(4, homeRules.stepsFor(r, boosted), "shared track: full movement (FR-17)");
    }

    @Test
    @DisplayName("FR-22: a cancelled roll gives no steps to any piece")
    void cancelledRollGivesNoSteps() {
        Piece r = piece(red, 0);
        r.setProgress(redProgressOf(SquareType.TRACK, 3));
        assertEquals(0, homeRules.stepsFor(r, new DiceRoll(6, DieType.NONE, 0, 6, false, true)));
    }
}
