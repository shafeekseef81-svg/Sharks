package Model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Independent verification of the Medium and Hard boards built by {@link BoardFactory}
 * (task T2-02, issue #53; SRS FR-10, FR-11, FR-12, FR-25, FR-33, Table 1).
 *
 * <p>Owner of these tests: Mohamed Zereny. The code under test ({@code BoardFactory}, {@code Board},
 * {@code LevelConfig}, {@code Square}) is owned by Shafeek Saif (task T07), so the rules are verified by a
 * member who did not implement them (SDP 5.3.4, Definition of Done). {@link EasyBoardTest} covers the Easy
 * board; nothing here repeats it.
 *
 * <p><b>Where the expected values come from.</b> Every number below is taken from the project documents, not
 * read back out of the implementation:
 * <ul>
 *   <li>the official board diagrams (<i>Ludo Project-V2 - 2026.pdf</i>, page 2), as recorded cell by cell in
 *       <i>Assignment3_Prototype/README.md</i>, section "Board fidelity notes": Medium is the classic board
 *       plus one ring of 64 squares (17x17), Hard plus two rings of 72 and 64 squares (19x19); the start
 *       square of a colour is the outer corner next to its castle; the arrows of one colour form a staircase,
 *       red on Hard at (7,0), (8,1), (9,2); Medium has 8 arrows and Hard 12; red sits in the top-left corner
 *       on both diagrams and the clockwise colour order is the same on every board; the classic start square
 *       stays coloured but has no special function there; the three corners the Hard diagram does not draw
 *       mirror the drawn one (SRS assumption 4);</li>
 *   <li>SRS Table 1: 8 and 12 protection squares, 8 and 12 arrows, 6 and 9 question squares (2 and 3 per
 *       question level), 2 and 3 surprise squares, 4 start squares and 20 home squares. These are written out
 *       as literals in this file and {@link LevelConfig} is then checked against them, so a wrong value in
 *       {@code LevelConfig} fails the test instead of being echoed back by it. The same numbers are recorded a
 *       second time, independently of the code under test, in the Assignment 3 prototype that was submitted
 *       before this iteration: the expectation table of {@code Assignment3_Prototype/src/devtools/SelfTest.java}
 *       ("Board configurations (SRS Table 1)") and the enum
 *       {@code Assignment3_Prototype/src/model/Difficulty.java}. That table also gives the route lengths 116
 *       and 188 used below;</li>
 *   <li>the legs of a route, counted off the diagrams by hand: on the outermost ring a piece walks from its
 *       corner to its own arrow, which is 17 + 16 + 16 + 9 = 58 of the 64 cells on Medium and
 *       19 + 18 + 18 + 11 = 66 of the 72 cells on Hard; it then laps each inner ring once and the classic
 *       52-square track once, and ends on 5 coloured home squares and the finish.</li>
 * </ul>
 *
 * <p>Like the Easy board, the classic 52-square track is a cross whose arms are three cells wide, so a route
 * turns the corner around the central block with a diagonal step. That happens exactly four times per lap,
 * always between two squares of the classic track, and is checked as such below.</p>
 *
 * <p><b>What these tests cannot do.</b> The copy of the SRS in this repository stops at the end of section 2,
 * so neither the FR table nor Table 1 itself is available here, and the diagram file
 * <i>Ludo Project-V2 - 2026.pdf</i> is not in the repository either. The expected values are therefore taken
 * from the two in-repo records named above, which both state that they follow the official documents; they are
 * not read from the official documents by these tests. A reviewer still has to compare the geometry with the
 * diagram file itself (issue #53, "against the official diagrams"). Whether a surprise square protects a piece
 * from capture (FR-27) is not asserted here: it has no verifiable source in this repository and belongs to the
 * surprise tasks T2-09 and T2-10. The capture rules themselves are covered by {@code StandardSquareRulesTest},
 * {@code CaptureProtectionFinishTest} and {@code MoveEngineIndependentTest}.</p>
 *
 * <p>Special-square counts on random boards are also checked, against {@code LevelConfig}, by
 * {@code BoardProtectionIntegrationTest} (issue #8). The overlap is deliberate and the assertions here are
 * stronger: they use the Table 1 literals instead of {@code LevelConfig}, and they also cover the start,
 * arrow and home squares and the question squares per question level.</p>
 */
class MediumHardBoardIndependentTest {

    /** The two levels this task verifies; Easy is covered by {@link EasyBoardTest}. */
    private static final Difficulty[] LEVELS = {Difficulty.MEDIUM, Difficulty.HARD};

    /** Boards per level in the random-placement tests (issue #53: 100 random boards per level). */
    private static final int BOARDS_PER_LEVEL = 100;

    /**
     * Layouts that must differ among the {@link #BOARDS_PER_LEVEL} boards. A new random set per game (FR-12)
     * is what matters, not that no two of 100 boards ever coincide, so this is a floor and not an equality:
     * 95 of 100, the same threshold {@link EasyBoardTest} uses. It still catches a frozen layout, a layout
     * that depends on the level only, or a seed that is ignored.
     */
    private static final int MIN_DIFFERENT_LAYOUTS = 95;

    /**
     * Share of the free track squares that the 100 boards of a level must use between them, as a percentage.
     * Every free square has to be reachable (FR-12), but demanding all of them makes the test depend on the
     * exact order in which the placement draws from the random source. 80% is far above what a placement
     * restricted to part of the track could reach and far below the coverage a uniform placement gives: with
     * 16 of 100 free squares taken per board on Medium and 24 of 168 on Hard, the chance that a uniform
     * placement leaves more than a fifth of the squares untouched over 100 boards is negligible.
     */
    private static final int MIN_COVERAGE_PERCENT = 80;

    /** Start squares on a board, SRS Table 1: one per colour on every level. */
    private static final int START_SQUARES = 4;

    /** Home squares drawn on a board: 5 coloured squares per colour, the finish excluded. */
    private static final int HOME_SQUARES = 20;

    /**
     * Classic start cells that stay in a player's colour on Medium and Hard, one per colour. They are plain
     * track squares with no function there, and no special square may be placed on them.
     */
    private static final int TINTED_SQUARES = 4;

    /** Protection squares, SRS Table 1: Medium 8, Hard 12. */
    private static int expectedProtectionSquares(Difficulty level) {
        return level == Difficulty.MEDIUM ? 8 : 12;
    }

    /** Arrow squares, SRS Table 1: Medium 8, Hard 12 (one per ring and colour). */
    private static int expectedArrowSquares(Difficulty level) {
        return level == Difficulty.MEDIUM ? 8 : 12;
    }

    /** Question squares, SRS Table 1: Medium 6, Hard 9. */
    private static int expectedQuestionSquares(Difficulty level) {
        return level == Difficulty.MEDIUM ? 6 : 9;
    }

    /** Question squares per question level, SRS Table 1: Medium 2 of each, Hard 3 of each. */
    private static int expectedQuestionsPerLevel(Difficulty level) {
        return level == Difficulty.MEDIUM ? 2 : 3;
    }

    /** Surprise squares, SRS Table 1: Medium 2, Hard 3. */
    private static int expectedSurpriseSquares(Difficulty level) {
        return level == Difficulty.MEDIUM ? 2 : 3;
    }

    /** Cells per side, from the diagrams: Medium 17x17, Hard 19x19. */
    private static int expectedGrid(Difficulty level) {
        return level == Difficulty.MEDIUM ? 17 : 19;
    }

    /** Shared-track squares per ring, innermost first: the classic 52, then 64, then 72. */
    private static int[] expectedRings(Difficulty level) {
        return level == Difficulty.MEDIUM ? new int[] {52, 64} : new int[] {52, 64, 72};
    }

    /** Cells of a route, leg by leg: the outermost ring, each inner ring, the classic track, home, finish. */
    private static int[] expectedRouteLegs(Difficulty level) {
        return level == Difficulty.MEDIUM ? new int[] {58, 52, 5, 1} : new int[] {66, 64, 52, 5, 1};
    }

    /** The staircase arrows of the top-left colour, outermost ring first (the diagram's red on Hard). */
    private static List<Cell> expectedStaircase(Difficulty level) {
        return level == Difficulty.MEDIUM
                ? List.of(new Cell(7, 0), new Cell(8, 1))
                : List.of(new Cell(7, 0), new Cell(8, 1), new Cell(9, 2));
    }

    private static Board board(Difficulty level) {
        return BoardFactory.create(level, new Random(2));
    }

    /** The cells of one square type on a board. */
    private static Set<Cell> cellsOf(Board b, SquareType type) {
        return b.squares().stream().filter(s -> s.type() == type).map(Square::cell).collect(Collectors.toSet());
    }

    /** Every randomly placed special square of a board (FR-12). */
    private static Set<Cell> randomSpecials(Board b) {
        Set<Cell> cells = new HashSet<>();
        cells.addAll(cellsOf(b, SquareType.PROTECTION));
        cells.addAll(cellsOf(b, SquareType.QUESTION));
        cells.addAll(cellsOf(b, SquareType.SURPRISE));
        return cells;
    }

    /** Cells a special square must never take: start squares, arrows, home paths and the tinted cells. */
    private static Set<Cell> reservedCells(Board b) {
        Set<Cell> reserved = new HashSet<>();
        reserved.addAll(cellsOf(b, SquareType.START));
        reserved.addAll(cellsOf(b, SquareType.ARROW));
        reserved.addAll(cellsOf(b, SquareType.HOME_PATH));
        b.squares().stream().filter(Square::isTinted).map(Square::cell).forEach(reserved::add);
        return reserved;
    }

    private static boolean orthogonallyAdjacent(Cell a, Cell b) {
        return Math.abs(a.row() - b.row()) + Math.abs(a.col() - b.col()) == 1;
    }

    private static boolean diagonallyAdjacent(Cell a, Cell b) {
        return Math.abs(a.row() - b.row()) == 1 && Math.abs(a.col() - b.col()) == 1;
    }

    /**
     * The part a square plays in the fixed geometry of the board. The randomly placed special squares
     * (protection, question, surprise) all sit on plain shared-track squares, so they count as
     * {@link SquareType#TRACK} here: their positions change with every game (FR-12) and are therefore not
     * part of the mirror symmetry of the four corners.
     */
    private static SquareType fixedRole(Square square) {
        return switch (square.type()) {
            case START, ARROW, HOME_PATH -> square.type();
            default -> SquareType.TRACK;
        };
    }

    private static String where(Difficulty level, PlayerColor color) {
        return level.label() + " " + color.label();
    }

    // ------------------------------------------------------------------ geometry (FR-10)

    @Test
    @DisplayName("T2-02 FR-10: Medium is 17x17 with one outer ring, Hard 19x19 with two (official diagrams)")
    void gridSizeAndRings() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            assertEquals(level, b.difficulty(), level.label());
            assertEquals(expectedGrid(level), b.size(), level.label());
            assertEquals(expectedRings(level).length - 1, b.offset(), level.label() + " outer rings");
            assertEquals(expectedGrid(level), LevelConfig.of(level).gridSize(), level.label());
            assertEquals(expectedRings(level).length - 1, LevelConfig.of(level).outerRings(),
                    level.label() + " outer rings in Table 1");
        }
    }

    @Test
    @DisplayName("T2-02 FR-10, Table 1: 52 + 64 = 116 and 52 + 64 + 72 = 188 track squares, 20 home squares")
    void trackAndHomeSquareCounts() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            int[] rings = expectedRings(level);
            int expectedTrack = Arrays.stream(rings).sum();
            assertEquals(expectedTrack, b.trackSquareCount(), level.label() + " shared-track squares");
            assertEquals(expectedTrack, LevelConfig.of(level).totalTrackSquares(), level.label() + " Table 1");
            assertArrayEquals(rings, LevelConfig.of(level).trackSizes(), level.label() + " Table 1 rings");
            assertEquals(20, b.count(SquareType.HOME_PATH), level.label() + " home squares");
            assertEquals(expectedTrack + 20, b.squares().size(), level.label() + " squares in total");
            for (int ring = 0; ring < rings.length; ring++) {
                int onThisRing = ring;
                assertEquals(rings[ring], b.squares().stream().filter(s -> s.ring() == onThisRing).count(),
                        level.label() + " squares on ring " + ring);
            }
            assertEquals(20, b.squares().stream().filter(s -> s.ring() == -1).count(),
                    level.label() + " home squares carry ring -1");
        }
    }

    @Test
    @DisplayName("T2-02 FR-10: every outer ring is a closed clockwise loop of 4(n-1-2d) different cells")
    void outerRingsAreClosedLoops() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            int n = b.size();
            for (int depth = 0; depth < b.offset(); depth++) {
                List<Cell> ring = BoardFactory.ring(n, depth);
                String what = level.label() + " ring at depth " + depth;
                assertEquals(4 * (n - 1 - 2 * depth), ring.size(), what);
                assertEquals(ring.size(), new HashSet<>(ring).size(), what + " repeats a cell");
                for (int i = 0; i < ring.size(); i++) {
                    Cell c = ring.get(i);
                    assertNotNull(b.squareAt(c), what + " has no square at " + c);
                    assertTrue(orthogonallyAdjacent(c, ring.get((i + 1) % ring.size())), what + " gap after " + c);
                }
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-10: all squares lie inside the grid; the castles and the centre are not squares")
    void squaresStayInsideTheGrid() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            int n = b.size();
            int o = b.offset();
            for (Square s : b.squares()) {
                Cell c = s.cell();
                assertTrue(c.row() >= 0 && c.row() < n && c.col() >= 0 && c.col() < n,
                        level.label() + " cell outside the grid: " + c);
            }
            assertNull(b.squareAt(new Cell(n / 2, n / 2)), level.label() + " centre");
            for (int r = 0; r <= 5; r++) {
                for (int c = 0; c <= 5; c++) {
                    Cell castle = new Cell(o + r, o + c);
                    assertNull(b.squareAt(castle), level.label() + " castle cell " + castle);
                }
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-10: red is in the top-left corner on Medium and Hard, the colours clockwise after it")
    void cornersFollowTheMediumAndHardDiagrams() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            assertEquals(Board.Corner.TOP_LEFT, b.corner(PlayerColor.RED), level.label());
            assertEquals(Board.Corner.TOP_RIGHT, b.corner(PlayerColor.GREEN), level.label());
            assertEquals(Board.Corner.BOTTOM_RIGHT, b.corner(PlayerColor.YELLOW), level.label());
            assertEquals(Board.Corner.BOTTOM_LEFT, b.corner(PlayerColor.BLUE), level.label());
            for (PlayerColor color : PlayerColor.values()) {
                assertEquals(color, b.colorAt(b.corner(color)), where(level, color));
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-10: the corners the Hard diagram leaves out mirror the drawn one (SRS assumption 4)")
    void cornersMirrorTheDrawnCorner() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            int n = b.size();
            PlayerColor drawnColour = b.colorAt(Board.Corner.TOP_LEFT);
            List<Cell> drawn = b.path(drawnColour);
            for (PlayerColor color : PlayerColor.values()) {
                int quarterTurns = b.corner(color).ordinal();
                List<Cell> mirrored = drawn.stream()
                        .map(c -> c.rotate(quarterTurns, n)).collect(Collectors.toList());
                assertEquals(mirrored, b.path(color), where(level, color) + " is not the drawn corner rotated");
                for (int i = 0; i < mirrored.size() - 1; i++) {
                    Square drawnSquare = b.squareAt(drawn.get(i));
                    Square mirroredSquare = b.squareAt(mirrored.get(i));
                    assertNotNull(mirroredSquare, where(level, color) + " progress " + i);
                    assertEquals(fixedRole(drawnSquare), fixedRole(mirroredSquare),
                            where(level, color) + " progress " + i + " plays a different part on the board");
                    assertEquals(drawnSquare.ring(), mirroredSquare.ring(),
                            where(level, color) + " progress " + i + " is on another ring");
                    assertEquals(drawnSquare.isTinted(), mirroredSquare.isTinted(),
                            where(level, color) + " progress " + i + " is tinted differently");
                    boolean drawnOwned = drawnSquare.owner() == drawnColour;
                    assertEquals(drawnOwned, mirroredSquare.owner() == color,
                            where(level, color) + " progress " + i + " belongs to the wrong colour");
                }
            }
        }
    }

    // ------------------------------------------------------------------ colour routes (FR-25)

    @Test
    @DisplayName("T2-02 FR-25: a route is 116 cells on Medium and 188 on Hard, all different and connected")
    void routeShape() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            int expectedLength = Arrays.stream(expectedRouteLegs(level)).sum();
            assertEquals(expectedLength - 1, b.finishIndex(), level.label() + " finish index");
            assertEquals(expectedLength - 6, b.homeStartIndex(), level.label() + " home start index");
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> route = b.path(color);
                assertEquals(expectedLength, route.size(), where(level, color));
                assertEquals(expectedLength, new HashSet<>(route).size(), where(level, color) + " repeats a cell");
                for (int i = 0; i < route.size() - 1; i++) {
                    Cell from = route.get(i);
                    Cell to = route.get(i + 1);
                    assertTrue(orthogonallyAdjacent(from, to) || diagonallyAdjacent(from, to),
                            where(level, color) + " jumps at progress " + i + ": " + from + " -> " + to);
                }
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-25: a route steps square by square, turning the corner only at the 4 arms of the cross")
    void routeTurnsDiagonallyOnlyAtTheFourCornersOfTheClassicTrack() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> route = b.path(color);
                int diagonalTurns = 0;
                for (int i = 0; i < route.size() - 1; i++) {
                    Cell from = route.get(i);
                    Cell to = route.get(i + 1);
                    if (orthogonallyAdjacent(from, to)) {
                        continue;
                    }
                    diagonalTurns++;
                    Square fromSquare = b.squareAt(from);
                    Square toSquare = b.squareAt(to);
                    assertNotNull(toSquare, where(level, color) + " progress " + i);
                    assertEquals(0, fromSquare.ring(),
                            where(level, color) + ": a corner turn at " + from + " outside the classic track");
                    assertEquals(0, toSquare.ring(),
                            where(level, color) + ": a corner turn into " + to + " outside the classic track");
                }
                assertEquals(4, diagonalTurns,
                        where(level, color) + ": the classic track turns the corner around the centre 4 times");
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-10, FR-25: a route laps the outer ring, turns inward ring by ring, then the 52 track")
    void routeVisitsTheRingsFromOutsideInwards() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            List<Integer> expectedLegs = Arrays.stream(expectedRouteLegs(level)).boxed()
                    .collect(Collectors.toList());
            List<Integer> expectedRingOrder = new ArrayList<>();
            for (int ring = b.offset(); ring >= 0; ring--) {
                expectedRingOrder.add(ring);
            }
            expectedRingOrder.add(-1);
            expectedRingOrder.add(Integer.MIN_VALUE);
            for (PlayerColor color : PlayerColor.values()) {
                List<Integer> rings = new ArrayList<>();
                List<Integer> legs = new ArrayList<>();
                for (Cell cell : b.path(color)) {
                    Square s = b.squareAt(cell);
                    int ring = s == null ? Integer.MIN_VALUE : s.ring();
                    if (rings.isEmpty() || rings.get(rings.size() - 1) != ring) {
                        rings.add(ring);
                        legs.add(1);
                    } else {
                        legs.set(legs.size() - 1, legs.get(legs.size() - 1) + 1);
                    }
                }
                assertEquals(expectedRingOrder, rings, where(level, color) + " ring order along the route");
                assertEquals(expectedLegs, legs, where(level, color) + " cells per leg");
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-10: a start square is the outer corner next to its castle and carries its colour")
    void startSquaresAreTheOuterCorners() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            int n = b.size();
            Map<Board.Corner, Cell> cornerCells = new LinkedHashMap<>();
            cornerCells.put(Board.Corner.TOP_LEFT, new Cell(0, 0));
            cornerCells.put(Board.Corner.TOP_RIGHT, new Cell(0, n - 1));
            cornerCells.put(Board.Corner.BOTTOM_RIGHT, new Cell(n - 1, n - 1));
            cornerCells.put(Board.Corner.BOTTOM_LEFT, new Cell(n - 1, 0));
            assertEquals(START_SQUARES, b.count(SquareType.START), level.label() + " start squares");
            for (PlayerColor color : PlayerColor.values()) {
                Cell start = b.startCell(color);
                assertEquals(cornerCells.get(b.corner(color)), start, where(level, color) + " start square");
                assertEquals(start, b.path(color).get(0), where(level, color) + " route does not begin there");
                Square square = b.squareAt(start);
                assertEquals(SquareType.START, square.type(), where(level, color));
                assertEquals(color, square.owner(), where(level, color));
                assertEquals(b.offset(), square.ring(), where(level, color) + " start is on the outermost ring");
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-11, Table 1: 8 arrows on Medium and 12 on Hard, one per ring and colour, as a staircase")
    void staircaseArrows() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            int n = b.size();
            int arrowsPerColour = b.offset() + 1;
            assertEquals(expectedArrowSquares(level), b.count(SquareType.ARROW), level.label() + " arrows");
            assertEquals(4L * arrowsPerColour, b.count(SquareType.ARROW), level.label());
            List<Cell> staircase = expectedStaircase(level);
            assertEquals(arrowsPerColour, staircase.size(), level.label() + " expected staircase");
            for (PlayerColor color : PlayerColor.values()) {
                int quarterTurns = b.corner(color).ordinal();
                for (int step = 0; step < staircase.size(); step++) {
                    Cell cell = staircase.get(step).rotate(quarterTurns, n);
                    Square arrow = b.squareAt(cell);
                    assertNotNull(arrow, where(level, color) + " has no arrow at " + cell);
                    assertEquals(SquareType.ARROW, arrow.type(), where(level, color) + " arrow at " + cell);
                    assertEquals(color, arrow.owner(), where(level, color) + " arrow at " + cell);
                    assertEquals(b.offset() - step, arrow.ring(),
                            where(level, color) + " arrow " + step + " sits on the wrong ring");
                    assertNotNull(arrow.arrowDirection(), where(level, color) + " arrow at " + cell);
                }
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-25: each arrow of a colour ends a ring leg of its route and points to the next cell")
    void arrowsTurnTheRouteInwards() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> route = b.path(color);
                List<Integer> ownArrows = new ArrayList<>();
                for (int i = 0; i < route.size(); i++) {
                    Square s = b.squareAt(route.get(i));
                    if (s != null && s.type() == SquareType.ARROW && s.owner() == color) {
                        ownArrows.add(i);
                    }
                }
                assertEquals(b.offset() + 1, ownArrows.size(), where(level, color) + " own arrows on the route");
                for (int index : ownArrows) {
                    Cell arrow = route.get(index);
                    Cell next = route.get(index + 1);
                    Direction expected = next.row() == arrow.row()
                            ? (next.col() > arrow.col() ? Direction.EAST : Direction.WEST)
                            : (next.row() > arrow.row() ? Direction.SOUTH : Direction.NORTH);
                    assertEquals(expected, b.squareAt(arrow).arrowDirection(),
                            where(level, color) + " arrow at " + arrow + " points the wrong way");
                    Square nextSquare = b.squareAt(next);
                    int nextRing = nextSquare == null ? -1 : nextSquare.ring();
                    assertTrue(nextRing < b.squareAt(arrow).ring(),
                            where(level, color) + " arrow at " + arrow + " does not lead inwards");
                }
                assertEquals(b.homeStartIndex() - 1, ownArrows.get(ownArrows.size() - 1),
                        where(level, color) + " the last arrow must be the square before the home path");
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-25: a route crosses the other colours' arrows and start squares without turning")
    void routeCrossesTheOtherColoursSquares() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            for (PlayerColor color : PlayerColor.values()) {
                long foreignArrows = 0;
                long foreignStarts = 0;
                for (Cell cell : b.path(color)) {
                    Square s = b.squareAt(cell);
                    if (s == null || s.owner() == color) {
                        continue;
                    }
                    if (s.type() == SquareType.ARROW) {
                        foreignArrows++;
                    } else if (s.type() == SquareType.START) {
                        foreignStarts++;
                    }
                }
                assertEquals(3L * (b.offset() + 1), foreignArrows, where(level, color) + " foreign arrows crossed");
                assertEquals(3L, foreignStarts, where(level, color) + " foreign start squares crossed");
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-10: the classic start square keeps its colour on Medium and Hard but has no function")
    void classicStartSquareIsOnlyTinted() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            int n = b.size();
            int o = b.offset();
            assertEquals(4, b.squares().stream().filter(Square::isTinted).count(),
                    level.label() + " tinted classic start squares");
            for (PlayerColor color : PlayerColor.values()) {
                Cell cell = new Cell(o + 6, o + 1).rotate(b.corner(color).ordinal(), n);
                Square square = b.squareAt(cell);
                assertNotNull(square, where(level, color) + " classic start cell " + cell);
                assertTrue(square.isTinted(), where(level, color) + " classic start cell is not tinted");
                assertEquals(color, square.owner(), where(level, color) + " classic start cell");
                assertEquals(SquareType.TRACK, square.type(),
                        where(level, color) + " the classic start cell must stay a plain track square");
                assertFalse(square.isProtected(), where(level, color) + " the classic start cell is not protected");
                assertEquals(0, square.ring(), where(level, color) + " the classic start cell is on the 52 track");
                assertTrue(b.path(color).contains(cell), where(level, color) + " classic start cell off the route");
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-33, Table 1: 5 coloured home squares and a finish per colour, never shared")
    void homePaths() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            assertEquals(6, LevelConfig.of(level).homePathSquares(), level.label() + " Table 1");
            Set<Cell> allHome = new HashSet<>();
            for (PlayerColor color : PlayerColor.values()) {
                for (int progress = b.homeStartIndex(); progress < b.finishIndex(); progress++) {
                    Cell cell = b.cellAt(color, progress);
                    Square square = b.squareAt(cell);
                    assertNotNull(square, where(level, color) + " progress " + progress);
                    assertEquals(SquareType.HOME_PATH, square.type(), where(level, color) + " progress " + progress);
                    assertEquals(color, square.owner(), where(level, color) + " progress " + progress);
                    assertEquals(-1, square.ring(), where(level, color) + " progress " + progress);
                    assertTrue(allHome.add(cell), where(level, color) + " shares the home square " + cell);
                }
                assertNull(b.squareAt(b.cellAt(color, b.finishIndex())),
                        where(level, color) + " the finish is not a board square");
            }
            for (PlayerColor color : PlayerColor.values()) {
                for (int progress = 0; progress < b.homeStartIndex(); progress++) {
                    assertFalse(allHome.contains(b.cellAt(color, progress)),
                            where(level, color) + " enters a home path at progress " + progress);
                }
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-10, FR-25: every square of the board lies on at least one colour's route")
    void everySquareIsReachable() {
        for (Difficulty level : LEVELS) {
            Board b = board(level);
            Set<Cell> onSomeRoute = new HashSet<>();
            for (PlayerColor color : PlayerColor.values()) {
                onSomeRoute.addAll(b.path(color));
            }
            for (Square s : b.squares()) {
                assertTrue(onSomeRoute.contains(s.cell()),
                        level.label() + " square " + s.cell() + " is on no route");
            }
        }
    }

    // ------------------------------------------------- special square counts (FR-11, Table 1)

    @Test
    @DisplayName("T2-02 Table 1: LevelConfig holds the documented counts, 8/8/6/2 on Medium and 12/12/9/3 on Hard")
    void levelConfigMatchesTheDocumentedTable1Counts() {
        for (Difficulty level : LEVELS) {
            LevelConfig config = LevelConfig.of(level);
            String what = level.label() + " Table 1";
            assertEquals(expectedProtectionSquares(level), config.protectionSquares(), what + ": protection");
            assertEquals(expectedArrowSquares(level), config.arrowSquares(), what + ": arrows");
            assertEquals(expectedQuestionSquares(level), config.questionSquares(), what + ": question squares");
            assertEquals(expectedQuestionsPerLevel(level), config.questionsPerLevel(),
                    what + ": question squares per question level");
            assertEquals(expectedSurpriseSquares(level), config.surpriseSquares(), what + ": surprise squares");
            assertEquals(START_SQUARES, config.startingSquares(), what + ": start squares");
            assertEquals(6, config.homePathSquares(), what + ": home squares per colour, finish included");
            assertEquals(3 * expectedQuestionsPerLevel(level), expectedQuestionSquares(level),
                    what + ": the question squares must divide evenly over the three question levels");
        }
    }

    @Test
    @DisplayName("T2-02 FR-11, Table 1: 100 random boards per level hold the documented special-square counts")
    void specialSquareCountsOnHundredBoards() {
        for (Difficulty level : LEVELS) {
            for (int seed = 0; seed < BOARDS_PER_LEVEL; seed++) {
                Board b = BoardFactory.create(level, new Random(seed));
                String what = level.label() + " seed " + seed;
                assertEquals(START_SQUARES, b.count(SquareType.START), what + " start squares");
                assertEquals(expectedArrowSquares(level), b.count(SquareType.ARROW), what + " arrows");
                assertEquals(expectedProtectionSquares(level), b.count(SquareType.PROTECTION),
                        what + " protection squares");
                assertEquals(expectedQuestionSquares(level), b.count(SquareType.QUESTION),
                        what + " question squares");
                assertEquals(expectedSurpriseSquares(level), b.count(SquareType.SURPRISE),
                        what + " surprise squares");
                assertEquals(HOME_SQUARES, b.count(SquareType.HOME_PATH), what + " home squares");
                assertEquals(TINTED_SQUARES, b.squares().stream().filter(Square::isTinted).count(),
                        what + " tinted classic start squares");
                for (Difficulty questionLevel : Difficulty.values()) {
                    assertEquals(expectedQuestionsPerLevel(level), b.countQuestions(questionLevel),
                            what + ": " + questionLevel.label() + " question squares");
                }
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-10, FR-12: 100 random boards per level keep the fixed geometry of the diagrams")
    void fixedGeometryIsNotTouchedByRandomPlacement() {
        for (Difficulty level : LEVELS) {
            Board first = board(level);
            Map<PlayerColor, List<Cell>> routes = new LinkedHashMap<>();
            for (PlayerColor color : PlayerColor.values()) {
                routes.put(color, first.path(color));
            }
            Set<Cell> arrows = cellsOf(first, SquareType.ARROW);
            Set<Cell> starts = cellsOf(first, SquareType.START);
            Set<Cell> homes = cellsOf(first, SquareType.HOME_PATH);
            for (int seed = 0; seed < BOARDS_PER_LEVEL; seed++) {
                Board b = BoardFactory.create(level, new Random(seed));
                String what = level.label() + " seed " + seed;
                assertEquals(expectedGrid(level), b.size(), what);
                assertEquals(Arrays.stream(expectedRings(level)).sum(), b.trackSquareCount(), what);
                assertEquals(arrows, cellsOf(b, SquareType.ARROW), what + ": the arrows moved");
                assertEquals(starts, cellsOf(b, SquareType.START), what + ": the start squares moved");
                assertEquals(homes, cellsOf(b, SquareType.HOME_PATH), what + ": the home paths moved");
                for (PlayerColor color : PlayerColor.values()) {
                    assertEquals(routes.get(color), b.path(color), what + ": " + color.label() + " route changed");
                }
            }
        }
    }

    // ------------------------------------------------- random placement (FR-12)

    @Test
    @DisplayName("T2-02 FR-12, FR-33: on 100 boards per level specials never overlap and stay on the shared track")
    void specialsNeverOverlapAndStayOnTheSharedTrack() {
        for (Difficulty level : LEVELS) {
            int expectedSpecials = expectedProtectionSquares(level) + expectedQuestionSquares(level)
                    + expectedSurpriseSquares(level);
            for (int seed = 0; seed < BOARDS_PER_LEVEL; seed++) {
                Board b = BoardFactory.create(level, new Random(seed));
                String what = level.label() + " seed " + seed;
                Set<Cell> specials = randomSpecials(b);
                assertEquals(expectedSpecials, specials.size(), what + ": special squares overlap");
                Set<Cell> reserved = reservedCells(b);
                for (Cell cell : specials) {
                    assertFalse(reserved.contains(cell), what + ": special square on a reserved cell " + cell);
                    Square square = b.squareAt(cell);
                    assertTrue(square.ring() >= 0, what + ": special square off the shared track at " + cell);
                    assertNull(square.owner(), what + ": special square owned by a colour at " + cell);
                    assertFalse(square.isTinted(), what + ": special square on a tinted cell at " + cell);
                }
            }
        }
    }

    @Test
    @DisplayName("T2-02 FR-12: 100 boards per level give different layouts spread over the free track squares")
    void placementIsRandomOverTheWholeFreeTrack() {
        for (Difficulty level : LEVELS) {
            int freeTrackSquares = Arrays.stream(expectedRings(level)).sum()
                    - START_SQUARES - expectedArrowSquares(level) - TINTED_SQUARES;
            Set<Set<Cell>> layouts = new HashSet<>();
            Set<Cell> everUsed = new HashSet<>();
            for (int seed = 0; seed < BOARDS_PER_LEVEL; seed++) {
                Set<Cell> specials = randomSpecials(BoardFactory.create(level, new Random(seed)));
                layouts.add(specials);
                everUsed.addAll(specials);
            }
            assertTrue(layouts.size() >= MIN_DIFFERENT_LAYOUTS,
                    level.label() + ": only " + layouts.size() + " different layouts in " + BOARDS_PER_LEVEL
                            + " boards, at least " + MIN_DIFFERENT_LAYOUTS + " expected");
            int required = freeTrackSquares * MIN_COVERAGE_PERCENT / 100;
            assertTrue(everUsed.size() >= required,
                    level.label() + ": the random placement used only " + everUsed.size() + " of the "
                            + freeTrackSquares + " free track squares over " + BOARDS_PER_LEVEL
                            + " boards, at least " + required + " expected");
            assertTrue(everUsed.size() <= freeTrackSquares,
                    level.label() + ": the placement used " + everUsed.size() + " squares, more than the "
                            + freeTrackSquares + " that are free");
        }
    }

    @Test
    @DisplayName("T2-02 FR-12: the same seed rebuilds the same board, a different seed a different one")
    void seedDecidesTheLayout() {
        for (Difficulty level : LEVELS) {
            Set<Cell> first = randomSpecials(BoardFactory.create(level, new Random(21)));
            Set<Cell> again = randomSpecials(BoardFactory.create(level, new Random(21)));
            Set<Cell> other = randomSpecials(BoardFactory.create(level, new Random(22)));
            assertEquals(first, again, level.label() + ": the same seed must rebuild the same board");
            assertFalse(first.equals(other), level.label() + ": a different seed must change the board");
        }
    }

}
