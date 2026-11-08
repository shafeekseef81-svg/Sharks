package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration of {@link ProtectionSquarePlacer} with {@link BoardFactory} on real boards
 * (Task 07 follow-up, issue #8; SRS FR-11, FR-12, FR-33, Table 1).
 */
class BoardProtectionIntegrationTest {

    private static final int SEEDS = 100;
    private static final Set<Cell> START_CELLS =
            Set.of(new Cell(6, 1), new Cell(1, 8), new Cell(8, 13), new Cell(13, 6));
    private static final Set<Cell> ARROW_CELLS =
            Set.of(new Cell(7, 0), new Cell(0, 7), new Cell(7, 14), new Cell(14, 7));

    private static Board easy(int seed) {
        return BoardFactory.create(Difficulty.EASY, new Random(seed));
    }

    private static List<Square> ofType(Board board, SquareType type) {
        return board.squares().stream().filter(s -> s.type() == type).collect(Collectors.toList());
    }

    private static Set<Cell> cells(List<Square> squares) {
        return squares.stream().map(Square::cell).collect(Collectors.toSet());
    }

    private static Map<Cell, SquareType> layout(Board board) {
        Map<Cell, SquareType> result = new HashMap<>();
        for (Square s : board.squares()) {
            result.put(s.cell(), s.type());
        }
        return result;
    }

    private static boolean hasAdjacentPair(Set<Cell> cells) {
        for (Cell a : cells) {
            for (Cell b : cells) {
                if (!a.equals(b) && Math.abs(a.row() - b.row()) <= 1 && Math.abs(a.col() - b.col()) <= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    @DisplayName("FR-11, FR-12: 100 seeds each give 4 different protection squares on plain shared-track squares")
    void protectionCountAndPlacement() {
        Set<Cell> track = new HashSet<>(BoardFactory.innerTrack(0));
        for (int seed = 0; seed < SEEDS; seed++) {
            Board board = easy(seed);
            List<Square> protection = ofType(board, SquareType.PROTECTION);
            Set<Cell> protectionCells = cells(protection);
            assertEquals(4, protection.size(), "seed " + seed);
            assertEquals(4, protectionCells.size(), "duplicate on seed " + seed);
            assertTrue(track.containsAll(protectionCells), "off the track on seed " + seed);
            assertTrue(protection.stream().allMatch(s -> s.owner() == null), "owned square on seed " + seed);
            assertFalse(protectionCells.stream().anyMatch(START_CELLS::contains), "on a start square, seed " + seed);
            assertFalse(protectionCells.stream().anyMatch(ARROW_CELLS::contains), "on an arrow, seed " + seed);
        }
    }

    @Test
    @DisplayName("FR-33, Table 1: over 100 seeds the start, arrow and home squares stay intact")
    void fixedSquaresStayIntact() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Board board = easy(seed);
            assertEquals(START_CELLS, cells(ofType(board, SquareType.START)), "seed " + seed);
            assertEquals(ARROW_CELLS, cells(ofType(board, SquareType.ARROW)), "seed " + seed);
            assertEquals(20, ofType(board, SquareType.HOME_PATH).size(), "seed " + seed);
            assertEquals(40, ofType(board, SquareType.TRACK).size(), "seed " + seed);
            for (PlayerColor color : PlayerColor.values()) {
                for (int i = board.homeStartIndex(); i < board.finishIndex(); i++) {
                    Square home = board.squareAt(board.cellAt(color, i));
                    assertEquals(SquareType.HOME_PATH, home.type(), "seed " + seed);
                    assertEquals(color, home.owner(), "seed " + seed);
                }
            }
        }
    }

    @Test
    @DisplayName("FR-12: board construction is repeatable: the same seed gives the same layout")
    void constructionIsStable() {
        for (int seed = 0; seed < SEEDS; seed++) {
            assertEquals(layout(easy(seed)), layout(easy(seed)), "seed " + seed);
        }
    }

    @Test
    @DisplayName("FR-12: every free square can be chosen, and adjacent protection squares are allowed")
    void placementComesFromThePlacer() {
        Set<Cell> chosen = new HashSet<>();
        boolean adjacent = false;
        for (int seed = 0; seed < SEEDS; seed++) {
            Set<Cell> protection = cells(ofType(easy(seed), SquareType.PROTECTION));
            chosen.addAll(protection);
            adjacent |= hasAdjacentPair(protection);
        }
        assertEquals(44, chosen.size());
        assertTrue(adjacent, "the old 'not adjacent' placement must no longer be used");
    }

    @Test
    @DisplayName("FR-11, FR-12: Medium and Hard keep every special square count, with no overlap")
    void otherLevelsKeepTheirCounts() {
        for (Difficulty level : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            for (int seed = 0; seed < SEEDS; seed++) {
                Board board = BoardFactory.create(level, new Random(seed));
                assertEquals(level.protectionSquares(), board.count(SquareType.PROTECTION), level + " seed " + seed);
                assertEquals(level.questionSquares(), board.count(SquareType.QUESTION), level + " seed " + seed);
                assertEquals(level.surpriseSquares(), board.count(SquareType.SURPRISE), level + " seed " + seed);
            }
        }
    }
}
