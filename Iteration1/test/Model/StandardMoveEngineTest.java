package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Author tests for {@link StandardMoveEngine} (Task 16, FR-19 P, FR-20, FR-25 P). The independent tests are
 * Task 20 (Shafeek).
 *
 * <p>The real {@link HomePathRules} and {@link SquareRules} are Tasks 18 and 17 (Shafeek) and are not
 * implemented yet, so these tests use small test doubles that follow the frozen contracts. The board is the
 * real Easy board from {@link BoardFactory}.
 *
 * <p>Owner: Rakan Ziyan.
 */
class StandardMoveEngineTest {

    /** Test double for Task 18: Easy steps = Regular die, exact finish. */
    static final class EasyHomePath implements HomePathRules {
        int stepsOverride = -1;
        Boolean reachOverride;
        int stepsCalls;

        @Override
        public int stepsFor(Piece piece, DiceRoll roll) {
            stepsCalls++;
            return stepsOverride >= 0 ? stepsOverride : roll.regular();
        }

        @Override
        public boolean canReach(Piece piece, int steps) {
            return reachOverride != null ? reachOverride : piece.progress() + steps <= piece.finishIndex();
        }
    }

    /** Test double for Task 17: returns a scripted list and records each call. */
    static final class ScriptedSquareRules implements SquareRules {
        List<Piece> toCapture = List.of();
        final List<Cell> landings = new ArrayList<>();
        final List<Piece> movers = new ArrayList<>();

        @Override
        public boolean isProtected(Square square) {
            return false;
        }

        @Override
        public List<Piece> capturesOnLanding(Piece mover, Cell target) {
            movers.add(mover);
            landings.add(target);
            return toCapture;
        }
    }

    private Board board;
    private Player red;
    private Player green;
    private List<Player> players;
    private EasyHomePath home;
    private ScriptedSquareRules rules;
    private StandardMoveEngine engine;

    @BeforeEach
    void setUp() {
        board = BoardFactory.create(Difficulty.EASY, new Random(3));
        red = new Player("Rakan", PlayerColor.RED, board.finishIndex());
        green = new Player("Shafeek", PlayerColor.GREEN, board.finishIndex());
        players = List.of(red, green);
        home = new EasyHomePath();
        rules = new ScriptedSquareRules();
        engine = new StandardMoveEngine(board, players, rules, home);
    }

    private static DiceRoll roll(int value) {
        return DiceRoll.regularOnly(value);
    }

    private static DiceRoll cancelled() {
        return new DiceRoll(6, DieType.NONE, 0, 6, false, true);
    }

    private Piece redPiece(int i) {
        return red.pieces().get(i);
    }

    // ------------------------------------------------------------------ castle (FR-19)

    @Test
    @DisplayName("FR-19: a Regular 6 offers every castle piece a move to its start square")
    void sixLeavesCastle() {
        Map<Piece, Move> moves = engine.legalMoves(red, roll(6));
        assertEquals(4, moves.size());
        for (Piece p : red.pieces()) {
            assertEquals(new Move(p, 0), moves.get(p));
        }
    }

    @Test
    @DisplayName("FR-19, FR-23: with 1-5 a player whose pieces are all in the castle has no legal move")
    void noExitWithoutSix() {
        for (int v = 1; v <= 5; v++) {
            assertTrue(engine.legalMoves(red, roll(v)).isEmpty(), "roll " + v);
        }
        assertEquals(0, home.stepsCalls, "castle pieces do not ask HomePathRules");
    }

    @Test
    @DisplayName("FR-19: leaving the castle puts the piece on its own start square")
    void exitCastleResult() {
        Piece p = redPiece(0);
        engine.legalMoves(red, roll(6));
        MoveResult r = engine.apply(new Move(p, 0));
        assertEquals(0, p.progress());
        assertEquals(MoveResult.Kind.EXIT_CASTLE, r.kind());
        assertEquals(Piece.IN_CASTLE, r.fromProgress());
        assertEquals(0, r.toProgress());
        assertNull(r.fromCell());
        assertEquals(List.of(board.startCell(PlayerColor.RED)), r.cells());
        assertSame(board.squareAt(board.startCell(PlayerColor.RED)), r.landedOn());
        assertEquals(SquareType.START, r.landedOn().type());
        assertFalse(r.pieceFinished());
    }

    // ------------------------------------------------------------------ steps and paths (FR-20, FR-25)

    @Test
    @DisplayName("FR-20: an active piece moves exactly the number of steps, through every cell on the way")
    void activePieceMovesBySteps() {
        Piece p = redPiece(1);
        p.setProgress(10);
        Map<Piece, Move> moves = engine.legalMoves(red, roll(4));
        assertEquals(Map.of(p, new Move(p, 14)), moves);
        MoveResult r = engine.apply(moves.get(p));
        assertEquals(14, p.progress());
        assertEquals(MoveResult.Kind.STEPS, r.kind());
        assertEquals(board.cellAt(PlayerColor.RED, 10), r.fromCell());
        assertEquals(board.path(PlayerColor.RED).subList(10, 15), r.cells());
        assertSame(board.squareAt(board.cellAt(PlayerColor.RED, 14)), r.landedOn());
    }

    @Test
    @DisplayName("FR-20: one move per piece; castle and active pieces are offered together, in castle order")
    void onePerPieceInOrder() {
        redPiece(1).setProgress(3);
        redPiece(3).setProgress(20);
        Map<Piece, Move> moves = engine.legalMoves(red, roll(6));
        assertEquals(List.of(redPiece(0), redPiece(1), redPiece(2), redPiece(3)), new ArrayList<>(moves.keySet()));
        assertEquals(9, moves.get(redPiece(1)).targetProgress());
        assertEquals(26, moves.get(redPiece(3)).targetProgress());
        assertEquals(0, moves.get(redPiece(0)).targetProgress());
    }

    @Test
    @DisplayName("FR-25: every colour moves clockwise along its own path from its own start square")
    void eachColourUsesItsOwnPath() {
        List<Player> all = new ArrayList<>();
        for (PlayerColor c : PlayerColor.values()) {
            all.add(new Player(c.label(), c, board.finishIndex()));
        }
        StandardMoveEngine e = new StandardMoveEngine(board, all, rules, home);
        for (Player p : all) {
            Piece piece = p.pieces().get(0);
            piece.setProgress(5);
            e.legalMoves(p, roll(3));
            MoveResult r = e.apply(new Move(piece, 8));
            assertEquals(board.path(p.color()).get(8), r.cells().get(r.cells().size() - 1), p.color().label());
            assertEquals(board.startCell(p.color()), board.path(p.color()).get(0));
        }
    }

    @Test
    @DisplayName("FR-25: a piece passes the arrow of another colour and stays on the shared track")
    void otherArrowIsPlainSquare() {
        List<Cell> path = board.path(PlayerColor.RED);
        int arrowAt = -1;
        for (int i = 1; i < board.homeStartIndex() - 6; i++) {
            Square s = board.squareAt(path.get(i));
            if (s.type() == SquareType.ARROW && s.owner() != PlayerColor.RED) {
                arrowAt = i;
                break;
            }
        }
        assertTrue(arrowAt > 0, "the red path crosses another colour's arrow");
        Piece p = redPiece(0);
        p.setProgress(arrowAt - 1);
        engine.legalMoves(red, roll(3));
        MoveResult r = engine.apply(new Move(p, arrowAt + 2));
        assertTrue(r.cells().contains(path.get(arrowAt)));
        assertTrue(r.landedOn() != null && r.landedOn().type() != SquareType.HOME_PATH);
    }

    @Test
    @DisplayName("FR-25, FR-33: from its own arrow a piece enters its own home path; no capture is asked there")
    void entersOwnHomePath() {
        Piece p = redPiece(0);
        p.setProgress(board.homeStartIndex() - 2);
        engine.legalMoves(red, roll(4));
        MoveResult r = engine.apply(new Move(p, board.homeStartIndex() + 2));
        Square reached = board.squareAt(board.cellAt(PlayerColor.RED, p.progress()));
        assertEquals(SquareType.HOME_PATH, reached.type());
        assertEquals(PlayerColor.RED, reached.owner());
        assertNull(r.landedOn(), "landedOn is only for the shared track");
        assertTrue(rules.landings.isEmpty(), "SquareRules is not asked on the home path");
    }

    // ------------------------------------------------------------------ finish (FR-31)

    @Test
    @DisplayName("FR-31: the exact roll reaches the finish; the piece is finished")
    void exactFinish() {
        Piece p = redPiece(0);
        p.setProgress(board.finishIndex() - 3);
        engine.legalMoves(red, roll(3));
        MoveResult r = engine.apply(new Move(p, board.finishIndex()));
        assertTrue(p.finished());
        assertTrue(r.pieceFinished());
        assertEquals(MoveResult.Kind.STEPS, r.kind());
        assertNull(r.landedOn());
        assertFalse(r.playerFinished());
    }

    @Test
    @DisplayName("FR-31: a roll that overshoots the finish gives that piece no move; another piece may move")
    void overshootIsNotLegal() {
        redPiece(0).setProgress(board.finishIndex() - 2);
        redPiece(1).setProgress(4);
        Map<Piece, Move> moves = engine.legalMoves(red, roll(5));
        assertFalse(moves.containsKey(redPiece(0)));
        assertEquals(9, moves.get(redPiece(1)).targetProgress());
    }

    @Test
    @DisplayName("Finished pieces get no move, and a player with all pieces finished has none at all")
    void finishedPiecesDoNotMove() {
        redPiece(0).setProgress(board.finishIndex());
        assertFalse(engine.legalMoves(red, roll(6)).containsKey(redPiece(0)));
        for (Piece p : red.pieces()) {
            p.setProgress(board.finishIndex());
        }
        assertTrue(engine.legalMoves(red, roll(6)).isEmpty());
    }

    // ------------------------------------------------------------------ delegation to Task 17 / 18

    @Test
    @DisplayName("HomePathRules decides the steps: its value is used instead of the die")
    void stepsComeFromHomePathRules() {
        redPiece(0).setProgress(10);
        home.stepsOverride = 2;
        assertEquals(12, engine.legalMoves(red, roll(5)).get(redPiece(0)).targetProgress());
        home.stepsOverride = 0;
        assertTrue(engine.legalMoves(red, roll(5)).isEmpty(), "0 steps means no move");
        home.stepsOverride = -1;
        home.reachOverride = false;
        assertTrue(engine.legalMoves(red, roll(5)).isEmpty(), "canReach false means no move");
    }

    @Test
    @DisplayName("A HomePathRules error that would pass the finish is reported, not applied")
    void homePathRulesPastFinishIsRefused() {
        Piece p = redPiece(0);
        p.setProgress(board.finishIndex() - 1);
        home.reachOverride = true;
        assertThrows(IllegalStateException.class, () -> engine.legalMoves(red, roll(4)));
        assertEquals(board.finishIndex() - 1, p.progress());
    }

    @Test
    @DisplayName("FR-26: pieces returned by SquareRules go back to their castle and are reported")
    void captureSendsOpponentHome() {
        Piece mover = redPiece(0);
        mover.setProgress(10);
        Cell target = board.cellAt(PlayerColor.RED, 13);
        Piece victim = green.pieces().get(2);
        victim.setProgress(board.path(PlayerColor.GREEN).indexOf(target));
        assertTrue(victim.onBoard());
        rules.toCapture = List.of(victim);

        engine.legalMoves(red, roll(3));
        MoveResult r = engine.apply(new Move(mover, 13));
        assertEquals(List.of(target), rules.landings);
        assertSame(mover, rules.movers.get(0));
        assertTrue(victim.inCastle());
        assertEquals(List.of(new MoveResult.Capture(victim, target)), r.captures());
    }

    @Test
    @DisplayName("SquareRules is asked when leaving the castle too (the start square is its decision, FR-27)")
    void exitAsksSquareRules() {
        engine.legalMoves(red, roll(6));
        engine.apply(new Move(redPiece(0), 0));
        assertEquals(List.of(board.startCell(PlayerColor.RED)), rules.landings);
    }

    @Test
    @DisplayName("A SquareRules error (own piece or piece elsewhere) is refused and nothing changes")
    void badCaptureListIsRefused() {
        Piece mover = redPiece(0);
        mover.setProgress(10);
        Piece own = redPiece(1);
        own.setProgress(13);
        rules.toCapture = List.of(own);
        engine.legalMoves(red, roll(3));
        assertThrows(IllegalStateException.class, () -> engine.apply(new Move(mover, 13)));
        assertEquals(10, mover.progress());
        assertEquals(13, own.progress());

        Piece far = green.pieces().get(0);
        far.setProgress(1);
        rules.toCapture = List.of(far);
        assertThrows(IllegalStateException.class, () -> engine.apply(new Move(mover, 13)));
        assertEquals(1, far.progress());
    }

    // ------------------------------------------------------------------ illegal moves (FR-20)

    @Test
    @DisplayName("FR-20: a move that was not offered is refused and nothing changes")
    void moveNotOfferedIsRefused() {
        Piece p = redPiece(0);
        p.setProgress(10);
        engine.legalMoves(red, roll(4));
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(p, 13)), "wrong target");
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(redPiece(1), 0)),
                "castle piece without a 6");
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(green.pieces().get(0), 0)),
                "another player's piece");
        assertEquals(10, p.progress());
        assertTrue(redPiece(1).inCastle());
    }

    @Test
    @DisplayName("FR-20: steps cannot be used twice: after one move the same roll offers nothing more")
    void oneMovePerRoll() {
        engine.legalMoves(red, roll(6));
        engine.apply(new Move(redPiece(0), 0));
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(redPiece(1), 0)));
        assertTrue(redPiece(1).inCastle());
    }

    @Test
    @DisplayName("Only the latest offer counts, and nothing can be applied before legalMoves is called")
    void staleOfferIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(redPiece(0), 0)));
        engine.legalMoves(red, roll(6));
        engine.legalMoves(red, roll(2));
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(redPiece(0), 0)));
        assertTrue(redPiece(0).inCastle());
    }

    @Test
    @DisplayName("FR-22: a cancelled roll has no legal moves")
    void cancelledRollHasNoMoves() {
        redPiece(0).setProgress(5);
        assertTrue(engine.legalMoves(red, cancelled()).isEmpty());
    }

    @Test
    @DisplayName("Invalid arguments are refused")
    void invalidArguments() {
        Player stranger = new Player("X", PlayerColor.BLUE, board.finishIndex());
        assertThrows(IllegalArgumentException.class, () -> engine.legalMoves(stranger, roll(6)));
        assertThrows(NullPointerException.class, () -> engine.legalMoves(null, roll(6)));
        assertThrows(NullPointerException.class, () -> engine.legalMoves(red, null));
        assertThrows(NullPointerException.class, () -> engine.apply(null));
        assertThrows(NullPointerException.class, () -> new StandardMoveEngine(null, players, rules, home));
    }

    // ------------------------------------------------------------------ result flags

    @Test
    @DisplayName("FR-43, FR-44: the last piece of a player finishes the player and, with 2 players, the game")
    void playerAndGameFinished() {
        for (int i = 0; i < 3; i++) {
            redPiece(i).setProgress(board.finishIndex());
        }
        redPiece(3).setProgress(board.finishIndex() - 1);
        engine.legalMoves(red, roll(1));
        MoveResult r = engine.apply(new Move(redPiece(3), board.finishIndex()));
        assertTrue(r.pieceFinished());
        assertTrue(r.playerFinished());
        assertTrue(r.gameOver());
    }

    @Test
    @DisplayName("The result lists cannot be changed, and isPassable is always true in Iteration 1")
    void resultIsReadOnly() {
        engine.legalMoves(red, roll(6));
        MoveResult r = engine.apply(new Move(redPiece(0), 0));
        assertThrows(UnsupportedOperationException.class, () -> r.cells().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.captures().clear());
        assertThrows(UnsupportedOperationException.class, () -> engine.legalMoves(red, roll(6)).clear());
        assertTrue(engine.isPassable(board.startCell(PlayerColor.GREEN), red));
    }

    @Test
    @DisplayName("Integration status: with the real (placeholder) Task 18 rules only castle exits work so far")
    void realRulesAreStillPlaceholders() {
        StandardMoveEngine real = new StandardMoveEngine(board, players,
                new StandardSquareRules(board, players), new StandardHomePathRules(board));
        assertEquals(4, real.legalMoves(red, roll(6)).size());
        redPiece(0).setProgress(3);
        assertThrows(UnsupportedOperationException.class, () -> real.legalMoves(red, roll(2)),
                "replace this test when Task 18 is merged");
    }
}
