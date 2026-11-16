package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Author's tests for T2-06, Double Blocks in {@link StandardMoveEngine} (FR-19, FR-28..FR-30). Written by Rakan
 * Ziyan, who implemented them; the independent tests are T2-07 (Shafeek Saif, SDP 5.3.4).
 *
 * <p>All tests use a real Easy board with the real capture and home-path rules. Red moves; Green (and in one test
 * Yellow) builds the blocks. Positions are Red progress indices on squares that Green's path also crosses.
 */
class DoubleBlockEngineTest {

    private Board board;
    private Player red;
    private Player green;
    private Player yellow;
    private MoveEngine engine;
    /** Red progress of the Double Block square used by most tests: a plain square, after a plain square. */
    private int block;

    @BeforeEach
    void setUp() {
        board = BoardFactory.create(Difficulty.EASY, new Random(7));
        red = new Player("Red", PlayerColor.RED, board.finishIndex());
        green = new Player("Green", PlayerColor.GREEN, board.finishIndex());
        yellow = new Player("Yellow", PlayerColor.YELLOW, board.finishIndex());
        List<Player> players = List.of(red, green, yellow);
        engine = new StandardMoveEngine(board, players, new StandardSquareRules(board, players),
                new StandardHomePathRules(board));
        block = plainPairAfter(10);
    }

    // ------------------------------------------------------------------ helpers

    private Piece piece(Player p, int i) {
        return p.pieces().get(i);
    }

    private Square redSquare(int redProgress) {
        return board.squareAt(board.cellAt(PlayerColor.RED, redProgress));
    }

    /** Progress of {@code color} on the cell Red reaches at {@code redProgress}, before its home path, or -1. */
    private int progressOn(PlayerColor color, int redProgress) {
        Cell cell = board.cellAt(PlayerColor.RED, redProgress);
        for (int p = 0; p < board.homeStartIndex(); p++) {
            if (board.cellAt(color, p).equals(cell)) {
                return p;
            }
        }
        return -1;
    }

    /** First Red progress at or after {@code from} where it and the square before it are plain, shared squares. */
    private int plainPairAfter(int from) {
        for (int p = from; p < board.homeStartIndex() - 8; p++) {
            if (redSquare(p).type() == SquareType.TRACK && redSquare(p - 1).type() == SquareType.TRACK
                    && progressOn(PlayerColor.GREEN, p) >= 0 && progressOn(PlayerColor.GREEN, p - 1) >= 0
                    && progressOn(PlayerColor.YELLOW, p) >= 0) {
                return p;
            }
        }
        throw new AssertionError("no two plain shared squares in a row");
    }

    /** Puts pieces of {@code owner} (by index) on the cell Red reaches at {@code redProgress}. */
    private void place(Player owner, int redProgress, int... indices) {
        int p = progressOn(owner.color(), redProgress);
        assertTrue(p >= 0, "the square must be on " + owner.color() + "'s shared track");
        for (int i : indices) {
            piece(owner, i).setProgress(p);
        }
    }

    private Map<Piece, Move> redMoves(int regular) {
        return engine.legalMoves(red, DiceRoll.regularOnly(regular));
    }

    private Cell redCell(int redProgress) {
        return board.cellAt(PlayerColor.RED, redProgress);
    }

    // ------------------------------------------------------------------ FR-28: forming a Double Block

    @Test
    @DisplayName("FR-28: two pieces of one opponent colour on a cell make it impassable; one piece does not")
    void twoPiecesFormABlock() {
        place(green, block, 0);
        assertTrue(engine.isPassable(redCell(block), red), "one Green piece is no Double Block");
        place(green, block, 1);
        assertFalse(engine.isPassable(redCell(block), red), "two Green pieces form a Double Block");
        assertTrue(engine.isPassable(redCell(block), green), "a Double Block never blocks its own colour");
    }

    @Test
    @DisplayName("FR-28: three pieces of one colour still block")
    void threePiecesStillBlock() {
        place(green, block, 0, 1, 2);
        assertFalse(engine.isPassable(redCell(block), red));
    }

    @Test
    @DisplayName("FR-28: pieces of two different opponents on one cell are no Double Block")
    void twoDifferentOpponentsDoNotBlock() {
        place(green, block, 0);
        place(yellow, block, 0);
        assertTrue(engine.isPassable(redCell(block), red));
    }

    @Test
    @DisplayName("FR-28: a Double Block forms on any square type, also a protected one")
    void blockOnAProtectedSquare() {
        int prot = -1;
        for (int p = 1; p < board.homeStartIndex(); p++) {
            if (redSquare(p).type() == SquareType.PROTECTION && progressOn(PlayerColor.GREEN, p) >= 0) {
                prot = p;
                break;
            }
        }
        assertTrue(prot > 0, "the board has a protection square on Green's track");
        place(green, prot, 0, 1);
        assertFalse(engine.isPassable(redCell(prot), red));
    }

    // ------------------------------------------------------------------ FR-29: blocking

    @Test
    @DisplayName("FR-29: a piece may not land on an opponent Double Block when another piece can use the roll")
    void cannotLandOnABlock() {
        place(green, block, 0, 1);
        piece(red, 0).setProgress(block - 3);
        piece(red, 1).setProgress(block + 5);
        Map<Piece, Move> moves = redMoves(3);
        assertFalse(moves.containsKey(piece(red, 0)), "landing on the block is not a move");
        assertEquals(new Move(piece(red, 1), block + 8), moves.get(piece(red, 1)));
        assertEquals(1, moves.size());
    }

    @Test
    @DisplayName("FR-29: a piece may not pass an opponent Double Block when another piece can use the roll")
    void cannotPassABlock() {
        place(green, block, 0, 1);
        piece(red, 0).setProgress(block - 2);
        piece(red, 1).setProgress(block + 5);
        Map<Piece, Move> moves = redMoves(4);
        assertFalse(moves.containsKey(piece(red, 0)), "passing the block is not a move");
        assertTrue(moves.containsKey(piece(red, 1)));
    }

    @Test
    @DisplayName("FR-20, FR-28: a player's own pieces pass and join their own Double Block")
    void ownPiecesPassTheirBlock() {
        place(red, block, 0, 1);
        piece(red, 2).setProgress(block - 2);
        Map<Piece, Move> pass = redMoves(4);
        assertEquals(new Move(piece(red, 2), block + 2), pass.get(piece(red, 2)));
        Map<Piece, Move> join = redMoves(2);
        assertEquals(new Move(piece(red, 2), block), join.get(piece(red, 2)));
    }

    @Test
    @DisplayName("FR-19, FR-29: an opponent Double Block on the start square keeps the castle pieces in")
    void blockOnTheStartSquareStopsTheCastleExit() {
        place(green, 0, 0, 1);
        assertTrue(redMoves(6).isEmpty(), "no castle exit onto a Double Block; no other red piece is out");
    }

    @Test
    @DisplayName("FR-19: a free start square still lets a piece out on a 6")
    void freeStartSquareLetsAPieceOut() {
        place(green, block, 0, 1);
        assertEquals(4, redMoves(6).size());
    }

    // ------------------------------------------------------------------ stopping before the block

    @Test
    @DisplayName("FR-29: when the blocked move is the only movement, the piece stops just before the block")
    void stopsBeforeTheBlockWhenItIsTheOnlyMovement() {
        place(green, block, 0, 1);
        Piece r = piece(red, 0);
        r.setProgress(block - 3);
        Map<Piece, Move> moves = redMoves(5);
        assertEquals(Map.of(r, new Move(r, block - 1)), moves);
        MoveResult result = engine.apply(moves.get(r));
        assertEquals(block - 1, r.progress());
        assertEquals(MoveResult.Kind.STEPS, result.kind());
        assertEquals(List.of(redCell(block - 3), redCell(block - 2), redCell(block - 1)), result.cells());
    }

    @Test
    @DisplayName("FR-29: every blocked piece is offered its stop, and the player chooses")
    void everyBlockedPieceIsOffered() {
        place(green, block, 0, 1);
        piece(red, 0).setProgress(block - 2);
        piece(red, 1).setProgress(block - 4);
        Map<Piece, Move> moves = redMoves(5);
        assertEquals(new Move(piece(red, 0), block - 1), moves.get(piece(red, 0)));
        assertEquals(new Move(piece(red, 1), block - 1), moves.get(piece(red, 1)));
        assertEquals(2, moves.size());
    }

    @Test
    @DisplayName("FR-29: a castle exit on a 6 uses the whole roll, so the blocked piece does not stop short")
    void castleExitCountsAsAFullMove() {
        place(green, block, 0, 1);
        piece(red, 0).setProgress(block - 3);
        Map<Piece, Move> moves = redMoves(6);
        assertFalse(moves.containsKey(piece(red, 0)));
        assertEquals(3, moves.size(), "the three castle pieces may leave");
    }

    @Test
    @DisplayName("FR-29: a block right in front of the piece leaves it without a move (zero steps)")
    void adjacentBlockGivesNoMove() {
        place(green, block, 0, 1);
        piece(red, 0).setProgress(block - 1);
        assertTrue(redMoves(3).isEmpty());
    }

    @Test
    @DisplayName("FR-26, FR-29: stopping before the block captures a lone opponent on that square")
    void stoppingBeforeTheBlockCaptures() {
        place(green, block, 0, 1);
        place(yellow, block - 1, 0);
        Piece r = piece(red, 0);
        r.setProgress(block - 3);
        MoveResult result = engine.apply(redMoves(4).get(r));
        assertEquals(block - 1, r.progress());
        assertEquals(1, result.captures().size());
        assertTrue(piece(yellow, 0).inCastle());
    }

    @Test
    @DisplayName("FR-31: an overshooting piece is not offered a stop before a block")
    void overshootIsNotTurnedIntoAStop() {
        piece(red, 0).setProgress(board.finishIndex() - 2);
        assertTrue(redMoves(5).isEmpty());
    }

    // ------------------------------------------------------------------ FR-30: persistence and breaking

    @Test
    @DisplayName("FR-30: the Double Block stays while its pieces stay, also over several rolls")
    void blockPersists() {
        place(green, block, 0, 1);
        piece(red, 0).setProgress(block - 2);
        piece(red, 1).setProgress(block + 5);
        for (int roll = 3; roll <= 5; roll++) {
            assertFalse(redMoves(roll).containsKey(piece(red, 0)), "roll " + roll);
        }
        engine.apply(redMoves(3).get(piece(red, 1)));
        assertFalse(engine.isPassable(redCell(block), red), "after another red move");
    }

    @Test
    @DisplayName("FR-30: moving one of the two pieces breaks the Double Block")
    void movingOnePieceBreaksTheBlock() {
        place(green, block, 0, 1);
        Piece g = piece(green, 0);
        Move away = engine.legalMoves(green, DiceRoll.regularOnly(2)).get(g);
        assertNotNull(away);
        engine.apply(away);
        assertTrue(engine.isPassable(redCell(block), red));
        piece(red, 0).setProgress(block - 2);
        assertEquals(new Move(piece(red, 0), block + 2), redMoves(4).get(piece(red, 0)));
    }

    @Test
    @DisplayName("FR-26, FR-30: a captured block piece breaks the Double Block")
    void capturedPieceBreaksTheBlock() {
        place(green, block, 0, 1);
        piece(green, 1).setProgress(Piece.IN_CASTLE);
        assertTrue(engine.isPassable(redCell(block), red));
    }

    @Test
    @DisplayName("isPassable refuses null arguments")
    void isPassableRefusesNull() {
        assertThrows(NullPointerException.class, () -> engine.isPassable(null, red));
        assertThrows(NullPointerException.class, () -> engine.isPassable(redCell(block), null));
    }
}
