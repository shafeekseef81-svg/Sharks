package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the Easy board built by {@link BoardFactory} (Task 07; SRS FR-10..FR-12, FR-25, FR-33, Table 1). */
class EasyBoardTest {

    private static final int TRACK_SQUARES = 52;
    private static final int LAST_TRACK_INDEX = 50;
    private static final int HOME_START = 51;
    private static final int FINISH = 56;

    private static Board board() {
        return BoardFactory.create(Difficulty.EASY, new Random(7));
    }

    private static List<Cell> track() {
        return BoardFactory.innerTrack(0);
    }

    private static boolean adjacent(Cell a, Cell b) {
        int dr = Math.abs(a.row() - b.row());
        int dc = Math.abs(a.col() - b.col());
        return Math.max(dr, dc) == 1;
    }

    private static Set<Cell> protectionCells(Board b) {
        return b.squares().stream().filter(s -> s.type() == SquareType.PROTECTION)
                .map(Square::cell).collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------ board construction

    @Test
    @DisplayName("FR-10: Easy is the 15x15 board without outer rings")
    void gridSize() {
        Board b = board();
        assertEquals(Difficulty.EASY, b.difficulty());
        assertEquals(15, b.size());
        assertEquals(0, b.offset());
    }

    @Test
    @DisplayName("FR-10, Table 1: 52 shared-track squares and 5 coloured home squares per colour")
    void squareCounts() {
        Board b = board();
        assertEquals(TRACK_SQUARES, b.trackSquareCount());
        assertEquals(20, b.count(SquareType.HOME_PATH));
        assertEquals(72, b.squares().size());
        for (Square s : b.squares()) {
            assertEquals(s.type() == SquareType.HOME_PATH ? -1 : 0, s.ring(), s.cell().toString());
        }
    }

    @Test
    @DisplayName("FR-11, Table 1: 4 start, 4 arrow and 4 protection squares; no question or surprise squares")
    void specialSquareCounts() {
        LevelConfig config = LevelConfig.of(Difficulty.EASY);
        Board b = board();
        assertEquals(config.startingSquares(), b.count(SquareType.START));
        assertEquals(config.arrowSquares(), b.count(SquareType.ARROW));
        assertEquals(config.protectionSquares(), b.count(SquareType.PROTECTION));
        assertEquals(config.questionSquares(), b.count(SquareType.QUESTION));
        assertEquals(config.surpriseSquares(), b.count(SquareType.SURPRISE));
    }

    @Test
    @DisplayName("FR-10: the shared track is one closed loop of 52 different squares")
    void trackIsClosedLoop() {
        Board b = board();
        List<Cell> track = track();
        assertEquals(TRACK_SQUARES, track.size());
        assertEquals(TRACK_SQUARES, new HashSet<>(track).size());
        for (int i = 0; i < TRACK_SQUARES; i++) {
            Cell c = track.get(i);
            assertNotNull(b.squareAt(c), c.toString());
            assertTrue(adjacent(c, track.get((i + 1) % TRACK_SQUARES)), "gap after " + c);
        }
    }

    @Test
    @DisplayName("FR-10: castles, the centre and the finishing squares are not board squares")
    void castlesAreNotSquares() {
        Board b = board();
        for (int r = 0; r <= 5; r++) {
            for (int c = 0; c <= 5; c++) {
                assertNull(b.squareAt(new Cell(r, c)));
                assertNull(b.squareAt(new Cell(r, c + 9)));
                assertNull(b.squareAt(new Cell(r + 9, c)));
                assertNull(b.squareAt(new Cell(r + 9, c + 9)));
            }
        }
        assertNull(b.squareAt(new Cell(7, 7)));
    }

    @Test
    @DisplayName("FR-10: the colours sit clockwise from green in the top-left corner (official Easy diagram)")
    void cornersFollowDiagram() {
        Board b = board();
        assertEquals(Board.Corner.TOP_LEFT, b.corner(PlayerColor.GREEN));
        assertEquals(Board.Corner.TOP_RIGHT, b.corner(PlayerColor.YELLOW));
        assertEquals(Board.Corner.BOTTOM_RIGHT, b.corner(PlayerColor.BLUE));
        assertEquals(Board.Corner.BOTTOM_LEFT, b.corner(PlayerColor.RED));
        for (PlayerColor color : PlayerColor.values()) {
            assertEquals(color, b.colorAt(b.corner(color)));
        }
    }

    // ------------------------------------------------------------------ paths

    @Test
    @DisplayName("FR-25: each path is 51 track squares, 5 home squares and the finish, all different and connected")
    void pathShape() {
        Board b = board();
        assertEquals(FINISH, b.finishIndex());
        assertEquals(HOME_START, b.homeStartIndex());
        for (PlayerColor color : PlayerColor.values()) {
            List<Cell> path = b.path(color);
            assertEquals(FINISH + 1, path.size(), color.label());
            assertEquals(FINISH + 1, new HashSet<>(path).size(), color.label());
            for (int i = 0; i < FINISH; i++) {
                assertTrue(adjacent(path.get(i), path.get(i + 1)), color.label() + " gap at progress " + i);
            }
        }
    }

    @Test
    @DisplayName("FR-25: every colour moves clockwise along the shared track from its start square")
    void pathsFollowTrackClockwise() {
        Board b = board();
        List<Cell> track = track();
        for (PlayerColor color : PlayerColor.values()) {
            List<Cell> path = b.path(color);
            int from = track.indexOf(path.get(0));
            assertTrue(from >= 0, color.label());
            for (int i = 0; i <= LAST_TRACK_INDEX; i++) {
                assertEquals(track.get((from + i) % TRACK_SQUARES), path.get(i), color.label() + " progress " + i);
            }
        }
    }

    @Test
    @DisplayName("FR-25: a path passes over the arrows of the other colours and follows only its own")
    void pathsCrossOtherArrows() {
        Board b = board();
        for (PlayerColor color : PlayerColor.values()) {
            List<Cell> path = b.path(color);
            long own = 0;
            long others = 0;
            for (int i = 0; i <= LAST_TRACK_INDEX; i++) {
                Square s = b.squareAt(path.get(i));
                if (s.type() == SquareType.ARROW) {
                    if (s.owner() == color) {
                        own++;
                        assertEquals(LAST_TRACK_INDEX, i, color.label() + ": own arrow must be the last track square");
                    } else {
                        others++;
                    }
                }
            }
            assertEquals(1, own, color.label());
            assertEquals(3, others, color.label());
        }
    }

    @Test
    @DisplayName("FR-25: an arrow points from the track to the first home square of its colour")
    void arrowPointsIntoHomePath() {
        Board b = board();
        for (PlayerColor color : PlayerColor.values()) {
            Cell arrow = b.cellAt(color, LAST_TRACK_INDEX);
            Cell home = b.cellAt(color, HOME_START);
            Direction expected = switch (home.row() - arrow.row()) {
                case 1 -> Direction.SOUTH;
                case -1 -> Direction.NORTH;
                default -> home.col() > arrow.col() ? Direction.EAST : Direction.WEST;
            };
            assertEquals(expected, b.squareAt(arrow).arrowDirection(), color.label());
        }
    }

    @Test
    @DisplayName("Board.cellAt: null in the castle, then the path; values past the finish stay on the finish")
    void cellAtProgress() {
        Board b = board();
        for (PlayerColor color : PlayerColor.values()) {
            assertNull(b.cellAt(color, -1), color.label());
            assertEquals(b.path(color).get(0), b.cellAt(color, 0), color.label());
            assertEquals(b.path(color).get(FINISH), b.cellAt(color, FINISH), color.label());
            assertEquals(b.path(color).get(FINISH), b.cellAt(color, FINISH + 1), color.label());
        }
    }

    // ------------------------------------------------------------------ start squares

    @Test
    @DisplayName("FR-19, Table 1: each colour has its own start square, 13 squares after the previous colour")
    void startSquares() {
        Board b = board();
        List<Cell> track = track();
        PlayerColor[] clockwise = {PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE, PlayerColor.RED};
        int first = track.indexOf(b.startCell(clockwise[0]));
        for (int i = 0; i < clockwise.length; i++) {
            PlayerColor color = clockwise[i];
            Cell start = b.startCell(color);
            assertEquals(b.path(color).get(0), start);
            assertEquals(track.get((first + 13 * i) % TRACK_SQUARES), start, color.label());
            Square square = b.squareAt(start);
            assertEquals(SquareType.START, square.type(), color.label());
            assertEquals(color, square.owner(), color.label());
        }
    }

    @Test
    @DisplayName("FR-27: start and protection squares are protected; arrows and plain track squares are not")
    void protectedSquares() {
        for (Square s : board().squares()) {
            boolean expected = s.type() == SquareType.START || s.type() == SquareType.PROTECTION;
            assertEquals(expected, s.isProtected(), s.cell() + " " + s.type());
        }
    }

    // ------------------------------------------------------------------ home paths

    @Test
    @DisplayName("Table 1: a home path has 6 squares, the last being the finish, which is not a board square")
    void homePathLength() {
        Board b = board();
        assertEquals(LevelConfig.of(Difficulty.EASY).homePathSquares(), FINISH - HOME_START + 1);
        for (PlayerColor color : PlayerColor.values()) {
            for (int i = HOME_START; i < FINISH; i++) {
                Square s = b.squareAt(b.cellAt(color, i));
                assertNotNull(s, color.label() + " progress " + i);
                assertEquals(SquareType.HOME_PATH, s.type(), color.label() + " progress " + i);
                assertEquals(color, s.owner(), color.label() + " progress " + i);
            }
            assertNull(b.squareAt(b.cellAt(color, FINISH)), color.label());
        }
    }

    @Test
    @DisplayName("FR-33: home paths never overlap and no colour's path enters another colour's home path")
    void homePathsBelongToOneColour() {
        Board b = board();
        Set<Cell> allHome = new HashSet<>();
        for (PlayerColor color : PlayerColor.values()) {
            List<Cell> home = b.path(color).subList(HOME_START, FINISH + 1);
            assertTrue(allHome.addAll(home), color.label());
        }
        for (PlayerColor color : PlayerColor.values()) {
            for (int i = 0; i <= LAST_TRACK_INDEX; i++) {
                assertFalse(allHome.contains(b.cellAt(color, i)), color.label() + " progress " + i);
            }
        }
    }

    // ------------------------------------------------------------------ random placement

    @Test
    @DisplayName("FR-11, FR-12, FR-33: 100 boards each keep 4 protection squares on plain shared-track squares")
    void hundredBoardsHaveValidProtectionSquares() {
        Set<Set<Cell>> layouts = new HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            Board b = BoardFactory.create(Difficulty.EASY, new Random(seed));
            Set<Cell> protection = protectionCells(b);
            assertEquals(4, protection.size(), "seed " + seed);
            assertEquals(4, b.count(SquareType.START), "seed " + seed);
            assertEquals(4, b.count(SquareType.ARROW), "seed " + seed);
            assertEquals(20, b.count(SquareType.HOME_PATH), "seed " + seed);
            assertTrue(track().containsAll(protection), "seed " + seed);
            layouts.add(protection);
        }
        assertTrue(layouts.size() >= 95, "only " + layouts.size() + " different layouts in 100 boards");
    }

    @Test
    @DisplayName("FR-12: the same seed gives the same board, a different seed a different board")
    void seedControlsLayout() {
        assertEquals(protectionCells(BoardFactory.create(Difficulty.EASY, new Random(11))),
                protectionCells(BoardFactory.create(Difficulty.EASY, new Random(11))));
        assertFalse(protectionCells(BoardFactory.create(Difficulty.EASY, new Random(11)))
                .equals(protectionCells(BoardFactory.create(Difficulty.EASY, new Random(12)))));
    }

    // ------------------------------------------------------------------ invalid use

    @Test
    @DisplayName("NFR-11: a null level or random source is refused")
    void nullArgumentsAreRefused() {
        assertThrows(NullPointerException.class, () -> BoardFactory.create(null, new Random(1)));
        assertThrows(NullPointerException.class, () -> BoardFactory.create(Difficulty.EASY, null));
    }

    @Test
    @DisplayName("NFR-08: callers cannot change the squares or paths of a board")
    void boardViewsAreReadOnly() {
        Board b = board();
        assertThrows(UnsupportedOperationException.class, () -> b.squares().clear());
        assertThrows(UnsupportedOperationException.class, () -> b.path(PlayerColor.RED).clear());
    }
}
