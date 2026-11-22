package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T2-05: independent tests for the dice, the movement formula and the routes on every level (FR-16, FR-17,
 * FR-25, NFR-13). Written by Rakan Ziyan from the SRS and the project description, against the public API of
 * T2-04 ({@link DiceStrategy}, {@link LevelConfig#diceStrategy()}, {@link RandomDice}) by Shafeek Saif, who
 * implemented it (SDP 5.3.4, Definition of Done).
 *
 * <p>The expected values are worked out here and never read from the code under test: face shares from the die
 * sizes, formula results by hand, and the routes checked by geometry (clockwise turning, adjacent steps, the
 * arrows they turn at) instead of being compared with {@link Board#path} as a reference.
 */
class DiceRouteIndependentTest {

    private static final int DRAWS = 10_000;
    /** NFR-13: every face within 2 percentage points of its share over 10,000 draws. */
    private static final double TOLERANCE = 0.02;

    // ------------------------------------------------------------------ NFR-13, FR-16: fair dice

    /** Rolls {@link #DRAWS} times on a level and checks each die's faces against 1 / faces. */
    private static void checkFairness(Difficulty level, int regularFaces, int specialFaces, long seed) {
        RandomDice dice = new RandomDice(new Random(seed));
        int[] regular = new int[regularFaces + 1];
        int[] special = new int[Math.max(specialFaces, 0) + 1];
        for (int i = 0; i < DRAWS; i++) {
            DiceRoll r = dice.roll(level);
            regular[r.regular()]++;
            special[r.special()]++;
        }
        for (int face = 1; face <= regularFaces; face++) {
            double share = regular[face] / (double) DRAWS;
            assertTrue(Math.abs(share - 1.0 / regularFaces) <= TOLERANCE,
                    "NFR-13: " + level + " Regular " + face + " share " + share + ", seed " + seed);
        }
        if (specialFaces == 0) {
            assertEquals(DRAWS, special[0], "Easy rolls no special die");
            return;
        }
        assertEquals(0, special[0], level + ": the special die is rolled every time");
        for (int face = 1; face <= specialFaces; face++) {
            double share = special[face] / (double) DRAWS;
            assertTrue(Math.abs(share - 1.0 / specialFaces) <= TOLERANCE,
                    "NFR-13: " + level + " special " + face + " share " + share + ", seed " + seed);
        }
    }

    @Test
    @DisplayName("T2-05 NFR-13, FR-16: Easy - 10,000 Regular draws, each face within 2 points of 1/6")
    void easyDiceAreFair() {
        for (long seed = 1; seed <= 3; seed++) {
            checkFairness(Difficulty.EASY, 6, 0, seed);
        }
    }

    @Test
    @DisplayName("T2-05 NFR-13, FR-16: Medium - 10,000 draws, Regular within 2 points of 1/6, Boost of 1/3")
    void mediumDiceAreFair() {
        for (long seed = 1; seed <= 3; seed++) {
            checkFairness(Difficulty.MEDIUM, 6, 3, seed);
        }
    }

    @Test
    @DisplayName("T2-05 NFR-13, FR-16: Hard - 10,000 draws, Regular and UltraBoost each within 2 points of 1/6")
    void hardDiceAreFair() {
        for (long seed = 1; seed <= 3; seed++) {
            checkFairness(Difficulty.HARD, 6, 6, seed);
        }
    }

    @Test
    @DisplayName("T2-05 NFR-13, FR-17: a movement of 0 comes up 1/3 of the time on Medium and 1/6 on Hard")
    void zeroMovementShare() {
        assertZeroShare(Difficulty.MEDIUM, 1.0 / 3);
        assertZeroShare(Difficulty.HARD, 1.0 / 6);
        assertZeroShare(Difficulty.EASY, 0.0);
    }

    private static void assertZeroShare(Difficulty level, double expected) {
        RandomDice dice = new RandomDice(new Random(42));
        int zeros = 0;
        for (int i = 0; i < DRAWS; i++) {
            zeros += dice.roll(level).movement() == 0 ? 1 : 0;
        }
        double share = zeros / (double) DRAWS;
        assertTrue(Math.abs(share - expected) <= TOLERANCE, level + ": zero-movement share " + share);
    }

    @Test
    @DisplayName("T2-05 FR-16: the Regular and the special die are independent (each pair near 1/36 on Hard)")
    void diceAreIndependent() {
        RandomDice dice = new RandomDice(new Random(5));
        int n = 36 * 1_000;
        int[][] pairs = new int[7][7];
        for (int i = 0; i < n; i++) {
            DiceRoll r = dice.roll(Difficulty.HARD);
            pairs[r.regular()][r.special()]++;
        }
        for (int a = 1; a <= 6; a++) {
            for (int b = 1; b <= 6; b++) {
                double share = pairs[a][b] / (double) n;
                assertTrue(Math.abs(share - 1.0 / 36) <= 0.01, "pair " + a + "," + b + ": " + share);
            }
        }
    }

    // ------------------------------------------------------------------ FR-17: the movement formula

    @Test
    @DisplayName("T2-05 FR-16: each level has the dice of SRS Table 1 - Easy Regular, Medium Boost 1-3, Hard UltraBoost 1-6")
    void levelDice() {
        assertEquals(DieType.NONE, LevelConfig.of(Difficulty.EASY).diceStrategy().specialDie());
        assertEquals(DieType.BOOST, LevelConfig.of(Difficulty.MEDIUM).diceStrategy().specialDie());
        assertEquals(DieType.ULTRA_BOOST, LevelConfig.of(Difficulty.HARD).diceStrategy().specialDie());
        assertEquals(3, DieType.BOOST.faces());
        assertEquals(6, DieType.ULTRA_BOOST.faces());
    }

    @Test
    @DisplayName("T2-05 FR-17: hand-computed movements, including 0 for a special 1")
    void handComputedMovements() {
        DiceStrategy easy = LevelConfig.of(Difficulty.EASY).diceStrategy();
        DiceStrategy medium = LevelConfig.of(Difficulty.MEDIUM).diceStrategy();
        DiceStrategy hard = LevelConfig.of(Difficulty.HARD).diceStrategy();
        assertEquals(1, easy.movement(1, 0));
        assertEquals(6, easy.movement(6, 0));
        // Medium: Regular x (Boost - 1)
        assertEquals(0, medium.movement(6, 1));
        assertEquals(5, medium.movement(5, 2));
        assertEquals(8, medium.movement(4, 3));
        assertEquals(12, medium.movement(6, 3));
        // Hard: Regular x (UltraBoost - 1)
        assertEquals(0, hard.movement(3, 1));
        assertEquals(2, hard.movement(1, 3));
        assertEquals(12, hard.movement(3, 5));
        assertEquals(30, hard.movement(6, 6));
    }

    @Test
    @DisplayName("T2-05 FR-17: the largest movement is 6 on Easy, 12 on Medium and 30 on Hard; the smallest is 1, 0, 0")
    void movementRange() {
        int[][] expected = {{1, 6}, {0, 12}, {0, 30}};
        Difficulty[] levels = {Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD};
        for (int i = 0; i < 3; i++) {
            RandomDice dice = new RandomDice(new Random(9));
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int k = 0; k < DRAWS; k++) {
                int m = dice.roll(levels[i]).movement();
                min = Math.min(min, m);
                max = Math.max(max, m);
            }
            assertEquals(expected[i][0], min, levels[i] + " min");
            assertEquals(expected[i][1], max, levels[i] + " max");
        }
    }

    @Test
    @DisplayName("T2-05 FR-17: values that are not on their die are refused")
    void invalidValuesAreRefused() {
        DiceStrategy easy = LevelConfig.of(Difficulty.EASY).diceStrategy();
        DiceStrategy medium = LevelConfig.of(Difficulty.MEDIUM).diceStrategy();
        DiceStrategy hard = LevelConfig.of(Difficulty.HARD).diceStrategy();
        assertThrows(IllegalArgumentException.class, () -> easy.movement(0, 0));
        assertThrows(IllegalArgumentException.class, () -> easy.movement(7, 0));
        assertThrows(IllegalArgumentException.class, () -> easy.movement(3, 2));
        assertThrows(IllegalArgumentException.class, () -> medium.movement(3, 0));
        assertThrows(IllegalArgumentException.class, () -> medium.movement(3, 4));
        assertThrows(IllegalArgumentException.class, () -> hard.movement(3, 7));
        assertThrows(IllegalArgumentException.class, () -> hard.movement(0, 3));
    }

    @Test
    @DisplayName("T2-05 FR-17: through the real engine every colour moves exactly the formula on Medium and Hard")
    void engineMovesTheFormulaForEveryColour() {
        int[][] rolls = {{4, 3}, {5, 2}, {2, 6}, {3, 4}};
        for (Difficulty level : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            DieType die = level.config().diceStrategy().specialDie();
            for (int[] r : rolls) {
                if (r[1] > die.faces()) {
                    continue;
                }
                int movement = r[0] * (r[1] - 1);
                for (PlayerColor color : PlayerColor.values()) {
                    Board board = BoardFactory.create(level, new Random(3));
                    Player player = new Player(color.label(), color, board.finishIndex());
                    List<Player> players = List.of(player);
                    MoveEngine engine = new StandardMoveEngine(board, players,
                            new StandardSquareRules(board, players), new StandardHomePathRules(board));
                    Piece piece = player.pieces().get(0);
                    piece.setProgress(2);
                    DiceRoll roll = new DiceRoll(r[0], die, r[1], movement, false, false);
                    Move move = engine.legalMoves(player, roll).get(piece);
                    assertNotNull(move, level + " " + color + " roll " + r[0] + "," + r[1]);
                    assertEquals(2 + movement, move.targetProgress(), level + " " + color);
                }
            }
        }
    }

    @Test
    @DisplayName("T2-05 FR-17, FR-19: a movement of 0 moves no board piece, but a Regular 6 still opens the castle")
    void zeroMovementThroughTheEngine() {
        Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(3));
        Player red = new Player("Red", PlayerColor.RED, board.finishIndex());
        List<Player> players = List.of(red);
        MoveEngine engine = new StandardMoveEngine(board, players, new StandardSquareRules(board, players),
                new StandardHomePathRules(board));
        Piece out = red.pieces().get(0);
        out.setProgress(5);
        assertTrue(engine.legalMoves(red, new DiceRoll(4, DieType.BOOST, 1, 0, false, false)).isEmpty());
        Map<Piece, Move> six = engine.legalMoves(red, new DiceRoll(6, DieType.BOOST, 1, 0, false, false));
        assertEquals(3, six.size(), "only the three castle pieces may move");
        assertTrue(six.values().stream().allMatch(m -> m.targetProgress() == 0));
    }

    // ------------------------------------------------------------------ FR-25: routes on every level

    /** Distance of a cell from the board edge: 0 on the outermost ring. */
    private static int depth(Cell c, int n) {
        return Math.min(Math.min(c.row(), c.col()), Math.min(n - 1 - c.row(), n - 1 - c.col()));
    }

    /**
     * Cross product of the steps from the board centre to two cells, in (row, col) coordinates. A clockwise turn
     * on screen (rows grow downwards) gives a negative value; a step straight towards the centre gives 0.
     */
    private static double turn(Cell a, Cell b, int n) {
        double mid = (n - 1) / 2.0;
        double ar = a.row() - mid;
        double ac = a.col() - mid;
        double br = b.row() - mid;
        double bc = b.col() - mid;
        return ar * bc - ac * br;
    }

    private static double distanceToCentre(Cell c, int n) {
        double mid = (n - 1) / 2.0;
        return Math.hypot(c.row() - mid, c.col() - mid);
    }

    @Test
    @DisplayName("T2-05 FR-25: on every level each colour's route goes clockwise in single steps, then straight home")
    void routesAreClockwise() {
        for (Difficulty level : Difficulty.values()) {
            Board board = BoardFactory.create(level, new Random(11));
            int n = board.size();
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> path = board.path(color);
                String where = level + " " + color;
                assertEquals(board.finishIndex() + 1, path.size(), where);
                assertEquals(path.size(), new HashSet<>(path).size(), where + ": no square is visited twice");
                for (int p = 0; p < board.finishIndex(); p++) {
                    Cell a = path.get(p);
                    Cell b = path.get(p + 1);
                    int step = Math.max(Math.abs(a.row() - b.row()), Math.abs(a.col() - b.col()));
                    assertEquals(1, step, where + ": one step from " + a + " to " + b);
                    if (p + 1 < board.homeStartIndex()) {
                        assertTrue(turn(a, b, n) < 0, where + ": clockwise from " + a + " to " + b);
                    } else {
                        assertEquals(0.0, turn(a, b, n), 1e-9, where + ": home path goes straight in at " + b);
                        assertTrue(distanceToCentre(b, n) < distanceToCentre(a, n), where + ": towards the centre");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("T2-05 FR-25: each route starts on its own start square and passes every colour's start square")
    void routesMakeAFullLap() {
        for (Difficulty level : Difficulty.values()) {
            Board board = BoardFactory.create(level, new Random(11));
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> path = board.path(color);
                Square first = board.squareAt(path.get(0));
                assertEquals(SquareType.START, first.type(), level + " " + color);
                assertEquals(color, first.owner(), level + " " + color);
                for (PlayerColor other : PlayerColor.values()) {
                    assertTrue(path.contains(board.startCell(other)),
                            level + " " + color + " passes the start square of " + other);
                }
            }
        }
    }

    @Test
    @DisplayName("T2-05 FR-25: on Medium and Hard a route leaves an outer ring only at its own arrow, never at another's")
    void ringChangesOnlyAtOwnArrows() {
        for (Difficulty level : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            Board board = BoardFactory.create(level, new Random(11));
            int n = board.size();
            int outer = level.config().outerRings();
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> path = board.path(color);
                int ringChanges = 0;
                for (int p = 0; p + 1 < board.homeStartIndex(); p++) {
                    Cell a = path.get(p);
                    Cell b = path.get(p + 1);
                    if (depth(a, n) >= outer) {
                        continue; // the classic track: its corners change depth by design
                    }
                    Square sq = board.squareAt(a);
                    boolean ownArrow = sq.type() == SquareType.ARROW && sq.owner() == color;
                    if (depth(b, n) != depth(a, n)) {
                        assertTrue(ownArrow, level + " " + color + ": leaves ring " + depth(a, n) + " at " + a);
                        assertEquals(depth(a, n) + 1, depth(b, n), level + " " + color + ": one ring inwards");
                        ringChanges++;
                    } else if (sq.type() == SquareType.ARROW) {
                        assertNotEquals(color, sq.owner(), level + " " + color + ": does not stop at its arrow " + a);
                    }
                }
                assertEquals(outer, ringChanges, level + " " + color + ": one turn inwards per outer ring");
            }
        }
    }

    @Test
    @DisplayName("T2-05 FR-25, FR-33: the home path starts at the colour's own arrow and no other colour enters it")
    void homePathIsPrivate() {
        for (Difficulty level : Difficulty.values()) {
            Board board = BoardFactory.create(level, new Random(11));
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> path = board.path(color);
                Square entry = board.squareAt(path.get(board.homeStartIndex() - 1));
                assertEquals(SquareType.ARROW, entry.type(), level + " " + color);
                assertEquals(color, entry.owner(), level + " " + color);
                Set<Cell> home = new HashSet<>(path.subList(board.homeStartIndex(), path.size()));
                for (PlayerColor other : PlayerColor.values()) {
                    if (other != color) {
                        for (Cell c : board.path(other)) {
                            assertTrue(!home.contains(c), level + ": " + other + " enters " + color + "'s home at " + c);
                        }
                    }
                }
            }
        }
    }
}
