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

/** Tests of {@link StandardSquareRules} on the real Easy board (Task 17; SRS FR-26, FR-27, FR-33; Q41, Q54, Q80). */
class StandardSquareRulesTest {

    private final Board board = BoardFactory.create(Difficulty.EASY, new Random(3));
    private final List<Player> players = new ArrayList<>();
    private final SquareRules rules;
    private final Player red;
    private final Player green;
    private final Player blue;

    StandardSquareRulesTest() {
        red = addPlayer("Red", PlayerColor.RED);
        green = addPlayer("Green", PlayerColor.GREEN);
        blue = addPlayer("Blue", PlayerColor.BLUE);
        rules = new StandardSquareRules(board, players);
    }

    private Player addPlayer(String name, PlayerColor color) {
        Player player = new Player(name, color, board.finishIndex());
        players.add(player);
        return player;
    }

    /** First cell on the red and green shared paths with this square type. */
    private Cell sharedCell(SquareType type) {
        List<Cell> redPath = board.path(PlayerColor.RED);
        for (int i = 0; i < board.homeStartIndex(); i++) {
            Cell cell = redPath.get(i);
            if (greenProgress(cell) >= 0 && board.squareAt(cell).type() == type) {
                return cell;
            }
        }
        throw new AssertionError("No shared " + type + " cell");
    }

    private int greenProgress(Cell cell) {
        int index = board.path(PlayerColor.GREEN).indexOf(cell);
        return index < board.homeStartIndex() ? index : -1;
    }

    /** Places a piece of the player on a cell of the shared track. */
    private void place(Piece piece, Cell cell) {
        int progress = board.path(piece.color()).indexOf(cell);
        assertTrue(progress >= 0 && progress < board.homeStartIndex(), "cell not on the shared path");
        piece.setProgress(progress);
    }

    private Piece mover() {
        return red.pieces().get(0);
    }

    private Piece greenPiece(int index) {
        return green.pieces().get(index);
    }

    private List<Integer> allProgress() {
        List<Integer> values = new ArrayList<>();
        for (Player player : players) {
            for (Piece piece : player.pieces()) {
                values.add(piece.progress());
            }
        }
        return values;
    }

    private static Square square(SquareType type) {
        Square square = new Square(new Cell(0, 0), 0);
        square.setType(type);
        return square;
    }

    // ------------------------------------------------------------------ isProtected

    @Test
    @DisplayName("FR-27: protection, starting and surprise squares and the finishing square are protected")
    void protectedSquares() {
        assertTrue(rules.isProtected(square(SquareType.PROTECTION)));
        assertTrue(rules.isProtected(square(SquareType.START)));
        assertTrue(rules.isProtected(square(SquareType.SURPRISE)));
        assertTrue(rules.isProtected(null), "null stands for the finishing square");
    }

    @Test
    @DisplayName("FR-27: plain track, arrow and question squares are not protected")
    void unprotectedSquares() {
        assertFalse(rules.isProtected(square(SquareType.TRACK)));
        assertFalse(rules.isProtected(square(SquareType.ARROW)));
        assertFalse(rules.isProtected(square(SquareType.QUESTION)));
    }

    // ------------------------------------------------------------------ landing and capturing

    @Test
    @DisplayName("FR-26: landing on an empty square captures nothing")
    void emptySquare() {
        assertTrue(rules.capturesOnLanding(mover(), sharedCell(SquareType.TRACK)).isEmpty());
    }

    @Test
    @DisplayName("FR-26: landing on a lone opponent on a plain square captures that piece")
    void loneOpponentIsCaptured() {
        Cell cell = sharedCell(SquareType.TRACK);
        Piece opponent = greenPiece(2);
        place(opponent, cell);
        List<Piece> captured = rules.capturesOnLanding(mover(), cell);
        assertEquals(1, captured.size());
        assertSame(opponent, captured.get(0));
    }

    @Test
    @DisplayName("FR-25, FR-26: an arrow is an ordinary square for capturing")
    void opponentOnArrowIsCaptured() {
        Cell cell = sharedCell(SquareType.ARROW);
        Piece opponent = greenPiece(0);
        place(opponent, cell);
        assertEquals(List.of(opponent), rules.capturesOnLanding(mover(), cell));
    }

    @Test
    @DisplayName("FR-26: the mover's own pieces are never captured")
    void ownPieceIsNotCaptured() {
        Cell cell = sharedCell(SquareType.TRACK);
        place(red.pieces().get(1), cell);
        assertTrue(rules.capturesOnLanding(mover(), cell).isEmpty());
    }

    @Test
    @DisplayName("FR-26: the mover standing on the target does not count as an opponent")
    void moverOnTargetIsIgnored() {
        Cell cell = sharedCell(SquareType.TRACK);
        place(mover(), cell);
        assertTrue(rules.capturesOnLanding(mover(), cell).isEmpty());
    }

    @Test
    @DisplayName("FR-26: two pieces of one opponent are not a lone piece, so neither is captured")
    void twoPiecesOfOneOpponentAreNotCaptured() {
        Cell cell = sharedCell(SquareType.TRACK);
        place(greenPiece(0), cell);
        place(greenPiece(1), cell);
        assertTrue(rules.capturesOnLanding(mover(), cell).isEmpty());
    }

    @Test
    @DisplayName("FR-26: pieces of two different opponents are not a lone piece")
    void piecesOfTwoOpponentsAreNotCaptured() {
        Cell cell = null;
        for (Cell candidate : board.path(PlayerColor.BLUE).subList(0, board.homeStartIndex())) {
            if (board.squareAt(candidate).type() == SquareType.TRACK && greenProgress(candidate) >= 0) {
                cell = candidate;
                break;
            }
        }
        place(greenPiece(0), cell);
        place(blue.pieces().get(0), cell);
        assertTrue(rules.capturesOnLanding(mover(), cell).isEmpty());
    }

    // ------------------------------------------------------------------ captures that are not allowed

    @Test
    @DisplayName("FR-27, Q54: a lone opponent on a protection square is not captured")
    void protectionSquarePreventsCapture() {
        Cell cell = sharedCell(SquareType.PROTECTION);
        place(greenPiece(0), cell);
        assertTrue(rules.capturesOnLanding(mover(), cell).isEmpty());
    }

    @Test
    @DisplayName("FR-27, Q80: a lone opponent on a start square is not captured, also when leaving the castle")
    void startSquarePreventsCapture() {
        Cell redStart = board.startCell(PlayerColor.RED);
        place(greenPiece(0), redStart);
        assertTrue(rules.capturesOnLanding(mover(), redStart).isEmpty());
    }

    @Test
    @DisplayName("FR-27: nothing is captured on the finishing square")
    void finishSquarePreventsCapture() {
        Cell finish = board.cellAt(PlayerColor.GREEN, board.finishIndex());
        greenPiece(0).setProgress(board.finishIndex());
        assertTrue(rules.capturesOnLanding(mover(), finish).isEmpty());
    }

    @Test
    @DisplayName("FR-33: nothing is captured on a home path")
    void homePathPreventsCapture() {
        int progress = board.homeStartIndex() + 1;
        Cell home = board.cellAt(PlayerColor.GREEN, progress);
        greenPiece(0).setProgress(progress);
        assertTrue(rules.capturesOnLanding(mover(), home).isEmpty());
    }

    @Test
    @DisplayName("FR-13: a piece in its castle is not on the board and is not captured")
    void castlePieceIsNotCaptured() {
        assertTrue(greenPiece(0).inCastle());
        assertTrue(rules.capturesOnLanding(mover(), board.startCell(PlayerColor.GREEN)).isEmpty());
        assertTrue(rules.capturesOnLanding(mover(), sharedCell(SquareType.TRACK)).isEmpty());
    }

    @Test
    @DisplayName("FR-26, FR-27: over every shared square, a lone opponent is captured unless the square is protected")
    void everySharedSquare() {
        for (Cell cell : BoardFactory.innerTrack(0)) {
            int progress = greenProgress(cell);
            if (progress < 0) {
                continue;
            }
            Piece opponent = greenPiece(0);
            opponent.setProgress(progress);
            List<Piece> expected = board.squareAt(cell).isProtected() ? List.of() : List.of(opponent);
            assertEquals(expected, rules.capturesOnLanding(mover(), cell), cell.toString());
        }
    }

    // ------------------------------------------------------------------ no side effects

    @Test
    @DisplayName("FR-26: finding a capture changes no piece")
    void capturesOnLandingChangesNothing() {
        Cell cell = sharedCell(SquareType.TRACK);
        place(greenPiece(1), cell);
        mover().setProgress(3);
        blue.pieces().get(2).setProgress(7);
        List<Integer> before = allProgress();
        rules.capturesOnLanding(mover(), cell);
        assertEquals(before, allProgress());
    }

    @Test
    @DisplayName("FR-26, Q41: returning the captured piece to the castle leaves every other piece unchanged")
    void returningCapturedPieceOnlyChangesThatPiece() {
        Cell cell = sharedCell(SquareType.TRACK);
        Piece opponent = greenPiece(1);
        place(opponent, cell);
        place(greenPiece(2), sharedCell(SquareType.ARROW));
        mover().setProgress(3);
        blue.pieces().get(2).setProgress(7);
        List<Integer> before = allProgress();

        for (Piece captured : rules.capturesOnLanding(mover(), cell)) {
            captured.setProgress(-1);
        }

        assertTrue(opponent.inCastle());
        before.set(players.indexOf(green) * 4 + opponent.index(), -1);
        assertEquals(before, allProgress());
    }

    @Test
    @DisplayName("NFR-08: the returned list cannot be modified")
    void resultIsReadOnly() {
        Cell cell = sharedCell(SquareType.TRACK);
        place(greenPiece(0), cell);
        assertThrows(UnsupportedOperationException.class, () -> rules.capturesOnLanding(mover(), cell).clear());
    }

    @Test
    @DisplayName("NFR-11: null arguments are refused")
    void nullArgumentsAreRefused() {
        assertThrows(NullPointerException.class, () -> new StandardSquareRules(null, players));
        assertThrows(NullPointerException.class, () -> new StandardSquareRules(board, null));
        assertThrows(NullPointerException.class, () -> rules.capturesOnLanding(null, new Cell(6, 1)));
        assertThrows(NullPointerException.class, () -> rules.capturesOnLanding(mover(), null));
    }
}
