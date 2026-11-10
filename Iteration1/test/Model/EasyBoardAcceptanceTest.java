package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.GameSettings.PlayerSetup;

/**
 * Acceptance tests of US-1.2 "a correct Easy board with my pieces in my castle" (Task 10: FR-10..FR-13 on Easy),
 * written by the Product Owner. The expected squares are read from the official Easy diagram and written here by
 * hand, so the board is compared with the diagram and not with its own generator.
 *
 * <p><b>Still manual:</b> FR-14 and NFR-23 (the board as drawn on the screen).
 */
class EasyBoardAcceptanceTest {

    private static final int[][] RING = {
        {6, 1}, {6, 2}, {6, 3}, {6, 4}, {6, 5}, {5, 6}, {4, 6}, {3, 6}, {2, 6}, {1, 6}, {0, 6}, {0, 7}, {0, 8},
        {1, 8}, {2, 8}, {3, 8}, {4, 8}, {5, 8}, {6, 9}, {6, 10}, {6, 11}, {6, 12}, {6, 13}, {6, 14}, {7, 14}, {8, 14},
        {8, 13}, {8, 12}, {8, 11}, {8, 10}, {8, 9}, {9, 8}, {10, 8}, {11, 8}, {12, 8}, {13, 8}, {14, 8}, {14, 7},
        {14, 6}, {13, 6}, {12, 6}, {11, 6}, {10, 6}, {9, 6}, {8, 5}, {8, 4}, {8, 3}, {8, 2}, {8, 1}, {8, 0}, {7, 0},
        {6, 0}};
    private static final Set<Cell> START_CELLS =
            Set.of(new Cell(6, 1), new Cell(1, 8), new Cell(8, 13), new Cell(13, 6));
    private static final Set<Cell> ARROW_CELLS =
            Set.of(new Cell(7, 0), new Cell(0, 7), new Cell(7, 14), new Cell(14, 7));
    private static final Map<PlayerColor, Cell> START_OF_COLOR = new EnumMap<>(Map.of(
            PlayerColor.GREEN, new Cell(6, 1), PlayerColor.YELLOW, new Cell(1, 8),
            PlayerColor.BLUE, new Cell(8, 13), PlayerColor.RED, new Cell(13, 6)));

    private static GameModel game(long seed) {
        return new GameModel(new GameSettings(Difficulty.EASY, List.of(new PlayerSetup("A", PlayerColor.RED),
                new PlayerSetup("B", PlayerColor.GREEN), new PlayerSetup("C", PlayerColor.YELLOW),
                new PlayerSetup("D", PlayerColor.BLUE))), new Random(seed));
    }

    private static Set<Cell> ring() {
        Set<Cell> cells = new HashSet<>();
        for (int[] rc : RING) {
            cells.add(new Cell(rc[0], rc[1]));
        }
        return cells;
    }

    private static Set<Cell> cellsOf(Board board, SquareType... types) {
        Set<SquareType> wanted = Set.of(types);
        return board.squares().stream().filter(s -> wanted.contains(s.type())).map(Square::cell)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("T10 FR-10: the shared track is exactly the 52 squares of the official Easy diagram")
    void trackMatchesDiagram() {
        Board board = game(1).board();
        assertEquals(15, board.size());
        assertEquals(52, RING.length);
        assertEquals(ring(), cellsOf(board, SquareType.TRACK, SquareType.START, SquareType.ARROW,
                SquareType.PROTECTION));
    }

    @Test
    @DisplayName("T10 FR-10, FR-11: start squares and arrows are on the diagram's cells, with the right colours")
    void fixedSquaresMatchDiagram() {
        Board board = game(1).board();
        assertEquals(START_CELLS, cellsOf(board, SquareType.START));
        assertEquals(ARROW_CELLS, cellsOf(board, SquareType.ARROW));
        for (Map.Entry<PlayerColor, Cell> e : START_OF_COLOR.entrySet()) {
            assertEquals(e.getValue(), board.startCell(e.getKey()), e.getKey().label());
            assertEquals(e.getKey(), board.squareAt(e.getValue()).owner(), e.getKey().label());
        }
    }

    @Test
    @DisplayName("T10 FR-10, FR-33: each colour has 5 home squares of its own and a finishing square in the centre")
    void homePathsMatchDiagram() {
        Board board = game(1).board();
        Map<PlayerColor, int[]> firstHome = Map.of(PlayerColor.GREEN, new int[] {7, 1}, PlayerColor.YELLOW,
                new int[] {1, 7}, PlayerColor.BLUE, new int[] {7, 13}, PlayerColor.RED, new int[] {13, 7});
        assertEquals(20, cellsOf(board, SquareType.HOME_PATH).size());
        for (PlayerColor color : PlayerColor.values()) {
            int[] rc = firstHome.get(color);
            Cell first = board.cellAt(color, board.homeStartIndex());
            assertEquals(new Cell(rc[0], rc[1]), first, color.label());
            for (int p = board.homeStartIndex(); p < board.finishIndex(); p++) {
                Square s = board.squareAt(board.cellAt(color, p));
                assertEquals(SquareType.HOME_PATH, s.type());
                assertEquals(color, s.owner());
            }
            assertNull(board.squareAt(board.cellAt(color, board.finishIndex())), "the finish is not a board square");
        }
    }

    @Test
    @DisplayName("T10 FR-11, FR-12: 300 new games have 4 different protection squares on plain track squares only")
    void protectionSquaresInManyGames() {
        Set<Cell> forbidden = new HashSet<>(START_CELLS);
        forbidden.addAll(ARROW_CELLS);
        Set<Set<Cell>> layouts = new HashSet<>();
        for (long seed = 0; seed < 300; seed++) {
            Board board = game(seed).board();
            Set<Cell> protection = cellsOf(board, SquareType.PROTECTION);
            assertEquals(4, protection.size(), "seed " + seed);
            assertTrue(ring().containsAll(protection), "shared track only, seed " + seed);
            for (Cell c : protection) {
                assertFalse(forbidden.contains(c), "not on a start square or arrow, seed " + seed);
            }
            assertEquals(0, cellsOf(board, SquareType.QUESTION, SquareType.SURPRISE).size(), "Easy, seed " + seed);
            layouts.add(protection);
        }
        assertTrue(layouts.size() > 250, "the squares are placed at random in every game: " + layouts.size());
    }

    @Test
    @DisplayName("T10 FR-13: every player of a new game has four pieces, all in the castle")
    void allPiecesStartInCastle() {
        for (Player player : game(2).players()) {
            assertEquals(4, player.pieces().size());
            assertTrue(player.pieces().stream().allMatch(Piece::inCastle), player.name());
        }
    }
}
