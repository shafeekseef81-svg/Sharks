package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Independent tests of the move engine (Task 20: FR-19 partial, FR-20, FR-25 partial, no Double Block).
 * Written by Shafeek Saif, who did not implement {@link StandardMoveEngine} (SDP 5.3.4, 7.2).
 *
 * <p>The author's tests check chosen examples against {@link Board#path}. These tests do not trust that path:
 * the expected squares come from a table written by hand from the official Easy diagram, and every combination
 * of colour, position and die value is checked. The rule doubles below follow the SRS, not the implementation.
 */
class MoveEngineIndependentTest {

    /** Last progress index (the finishing square) and the first home-path index, from the Easy diagram. */
    private static final int FINISH = 56;
    private static final int HOME_START = 51;

    /** The 52 shared-track squares, clockwise from the start square of the top-left colour. */
    private static final int[][] RING = {
        {6, 1}, {6, 2}, {6, 3}, {6, 4}, {6, 5}, {5, 6}, {4, 6}, {3, 6}, {2, 6}, {1, 6}, {0, 6}, {0, 7}, {0, 8},
        {1, 8}, {2, 8}, {3, 8}, {4, 8}, {5, 8}, {6, 9}, {6, 10}, {6, 11}, {6, 12}, {6, 13}, {6, 14}, {7, 14}, {8, 14},
        {8, 13}, {8, 12}, {8, 11}, {8, 10}, {8, 9}, {9, 8}, {10, 8}, {11, 8}, {12, 8}, {13, 8}, {14, 8}, {14, 7},
        {14, 6}, {13, 6}, {12, 6}, {11, 6}, {10, 6}, {9, 6}, {8, 5}, {8, 4}, {8, 3}, {8, 2}, {8, 1}, {8, 0}, {7, 0},
        {6, 0}};

    /** The 5 home squares and the finish of each corner, in the order TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT. */
    private static final int[][][] HOME = {
        {{7, 1}, {7, 2}, {7, 3}, {7, 4}, {7, 5}, {7, 6}},
        {{1, 7}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {6, 7}},
        {{7, 13}, {7, 12}, {7, 11}, {7, 10}, {7, 9}, {7, 8}},
        {{13, 7}, {12, 7}, {11, 7}, {10, 7}, {9, 7}, {8, 7}}};

    /** Home-path rules written from FR-17, FR-18 and FR-31. */
    private static final class SrsHomeRules implements HomePathRules {
        final List<Piece> asked = new ArrayList<>();

        @Override
        public int stepsFor(Piece piece, DiceRoll roll) {
            asked.add(piece);
            return piece.progress() >= HOME_START ? roll.regular() : roll.movement();
        }

        @Override
        public boolean canReach(Piece piece, int steps) {
            return steps > 0 && piece.progress() + steps <= FINISH;
        }
    }

    /** Capture rules that return a chosen list and record every question. */
    private static final class RecordingRules implements SquareRules {
        List<Piece> answer = List.of();
        final List<Cell> targets = new ArrayList<>();
        final List<Piece> movers = new ArrayList<>();

        @Override
        public boolean isProtected(Square square) {
            return false;
        }

        @Override
        public List<Piece> capturesOnLanding(Piece mover, Cell target) {
            movers.add(mover);
            targets.add(target);
            return answer;
        }
    }

    private Board board;
    private Map<PlayerColor, Player> byColor;
    private List<Player> players;
    private SrsHomeRules home;
    private RecordingRules rules;
    private StandardMoveEngine engine;

    @BeforeEach
    void setUp() {
        board = BoardFactory.create(Difficulty.EASY, new Random(11));
        byColor = new EnumMap<>(PlayerColor.class);
        players = new ArrayList<>();
        for (PlayerColor c : PlayerColor.values()) {
            Player p = new Player(c.label(), c, board.finishIndex());
            byColor.put(c, p);
            players.add(p);
        }
        home = new SrsHomeRules();
        rules = new RecordingRules();
        engine = new StandardMoveEngine(board, players, rules, home);
    }

    private static DiceRoll die(int value) {
        return DiceRoll.regularOnly(value);
    }

    /** The square a piece of this colour must be on, computed from the tables only. */
    private Cell expectedCell(PlayerColor color, int progress) {
        int corner = board.corner(color).ordinal();
        int[] rc = progress < HOME_START
                ? RING[(corner * 13 + progress) % RING.length]
                : HOME[corner][progress - HOME_START];
        return new Cell(rc[0], rc[1]);
    }

    private List<Integer> positions() {
        List<Integer> all = new ArrayList<>();
        for (Player p : players) {
            for (Piece piece : p.pieces()) {
                all.add(piece.progress());
            }
        }
        return all;
    }

    private void resetAll() {
        for (Player p : players) {
            for (Piece piece : p.pieces()) {
                piece.setProgress(Piece.IN_CASTLE);
            }
        }
    }

    @Test
    @DisplayName("T20 FR-19, FR-20, FR-31: legal moves are right for every colour, position and die value")
    void everyPositionAndDieValue() {
        assertEquals(FINISH, board.finishIndex(), "the Easy path of the diagram");
        assertEquals(HOME_START, board.homeStartIndex(), "the Easy path of the diagram");
        for (PlayerColor color : PlayerColor.values()) {
            Player player = byColor.get(color);
            Piece piece = player.pieces().get(2);
            for (int from = Piece.IN_CASTLE; from <= FINISH; from++) {
                for (int value = 1; value <= 6; value++) {
                    resetAll();
                    piece.setProgress(from);
                    Integer expected;
                    if (from == Piece.IN_CASTLE) {
                        expected = value == 6 ? 0 : null;
                    } else if (from == FINISH || from + value > FINISH) {
                        expected = null;
                    } else {
                        expected = from + value;
                    }
                    Move move = engine.legalMoves(player, die(value)).get(piece);
                    String where = color + " from " + from + " die " + value;
                    if (expected == null) {
                        assertNull(move, where);
                    } else {
                        assertNotNull(move, where);
                        assertEquals(expected, move.targetProgress(), where);
                        assertSame(piece, move.piece(), where);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("T20 FR-20, FR-25, FR-33: every applied move reports the squares of the official diagram")
    void everyAppliedMoveFollowsTheDiagram() {
        for (PlayerColor color : PlayerColor.values()) {
            Player player = byColor.get(color);
            Piece piece = player.pieces().get(1);
            for (int from = 0; from < FINISH; from++) {
                for (int value = 1; value <= 6 && from + value <= FINISH; value++) {
                    resetAll();
                    rules.targets.clear();
                    piece.setProgress(from);
                    List<Integer> before = positions();
                    engine.legalMoves(player, die(value));
                    MoveResult r = engine.apply(new Move(piece, from + value));

                    int to = from + value;
                    String where = color + " from " + from + " die " + value;
                    assertEquals(to, piece.progress(), where);
                    assertEquals(MoveResult.Kind.STEPS, r.kind(), where);
                    assertEquals(from, r.fromProgress(), where);
                    assertEquals(to, r.toProgress(), where);
                    assertEquals(expectedCell(color, from), r.fromCell(), where);
                    List<Cell> cells = new ArrayList<>();
                    for (int p = from; p <= to; p++) {
                        cells.add(expectedCell(color, p));
                    }
                    assertEquals(cells, r.cells(), where);
                    assertEquals(to == FINISH, r.pieceFinished(), where);
                    if (to < HOME_START) {
                        assertEquals(List.of(expectedCell(color, to)), rules.targets, where);
                        assertSame(board.squareAt(expectedCell(color, to)), r.landedOn(), where);
                    } else {
                        assertTrue(rules.targets.isEmpty(), "no capture on the home path: " + where);
                        assertNull(r.landedOn(), where);
                    }
                    before.set(players.indexOf(player) * 4 + 1, to);
                    assertEquals(before, positions(), "only the moved piece changes: " + where);
                }
            }
        }
    }

    @Test
    @DisplayName("T20 FR-19: leaving the castle lands on the colour's own start square from the diagram")
    void castleExitUsesOwnStartSquare() {
        for (PlayerColor color : PlayerColor.values()) {
            Player player = byColor.get(color);
            for (Piece piece : player.pieces()) {
                engine.legalMoves(player, die(6));
                MoveResult r = engine.apply(new Move(piece, 0));
                assertEquals(MoveResult.Kind.EXIT_CASTLE, r.kind());
                assertEquals(Piece.IN_CASTLE, r.fromProgress());
                assertNull(r.fromCell());
                assertEquals(List.of(expectedCell(color, 0)), r.cells());
                assertSame(piece, rules.movers.get(rules.movers.size() - 1));
            }
            assertTrue(player.pieces().stream().allMatch(p -> p.progress() == 0),
                    "several pieces of one colour may stand on the start square (staff Q7, Q34)");
        }
    }

    @Test
    @DisplayName("T20 FR-19: the castle exit depends on the Regular 6 only and ignores the special die")
    void castleExitIgnoresSpecialDie() {
        Player red = byColor.get(PlayerColor.RED);
        Piece castle = red.pieces().get(0);
        assertEquals(0, engine.legalMoves(red, new DiceRoll(6, DieType.BOOST, 1, 0, false, false))
                .get(castle).targetProgress(), "a movement of 0 still leaves the castle");
        assertEquals(0, engine.legalMoves(red, new DiceRoll(6, DieType.ULTRA_BOOST, 3, 12, false, false))
                .get(castle).targetProgress());
        assertTrue(engine.legalMoves(red, new DiceRoll(3, DieType.ULTRA_BOOST, 3, 6, false, false)).isEmpty(),
                "a movement of 6 from a Regular 3 is not a Regular 6");
        assertTrue(home.asked.isEmpty(), "castle pieces do not use the step rules");
    }

    @Test
    @DisplayName("T20 FR-22: a cancelled 6 offers nothing, also to castle pieces, and asks no rule")
    void cancelledSixOffersNothing() {
        Player red = byColor.get(PlayerColor.RED);
        red.pieces().get(3).setProgress(7);
        DiceRoll cancelled = new DiceRoll(6, DieType.NONE, 0, 6, false, true);
        assertTrue(engine.legalMoves(red, cancelled).isEmpty());
        assertTrue(home.asked.isEmpty());
    }

    @Test
    @DisplayName("T20 FR-20: the offer keeps castle order, skips pieces without a move and cannot be changed")
    void offerOrderAndImmutability() {
        Player red = byColor.get(PlayerColor.RED);
        red.pieces().get(0).setProgress(FINISH);
        red.pieces().get(2).setProgress(FINISH - 1);
        Map<Piece, Move> offer = engine.legalMoves(red, die(6));
        assertEquals(List.of(red.pieces().get(1), red.pieces().get(3)), new ArrayList<>(offer.keySet()));
        assertThrows(UnsupportedOperationException.class, offer::clear);
        assertThrows(UnsupportedOperationException.class,
                () -> offer.put(red.pieces().get(0), new Move(red.pieces().get(0), FINISH)));
    }

    @Test
    @DisplayName("T20 FR-20: asking for legal moves changes no piece and no piece of another player is offered")
    void legalMovesChangesNothing() {
        Player green = byColor.get(PlayerColor.GREEN);
        byColor.get(PlayerColor.RED).pieces().get(0).setProgress(9);
        green.pieces().get(0).setProgress(20);
        List<Integer> before = positions();
        Map<Piece, Move> offer = engine.legalMoves(green, die(6));
        assertEquals(before, positions());
        assertTrue(green.pieces().containsAll(offer.keySet()));
        assertTrue(rules.targets.isEmpty(), "capture rules are only asked when a move is applied");
    }

    @Test
    @DisplayName("T20 FR-20: forged, replayed and outdated moves are refused, and the valid offer stays usable")
    void illegalMovesChangeNothing() {
        Player red = byColor.get(PlayerColor.RED);
        Player green = byColor.get(PlayerColor.GREEN);
        Piece mover = red.pieces().get(0);
        mover.setProgress(10);
        engine.legalMoves(red, die(3));
        List<Integer> before = positions();

        for (int forged : new int[] {Piece.IN_CASTLE, 0, 10, 12, 14, FINISH}) {
            assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(mover, forged)), "" + forged);
        }
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(red.pieces().get(1), 0)));
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(green.pieces().get(0), 0)));
        Piece stranger = new Piece(PlayerColor.RED, 0, FINISH);
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(stranger, 0)));
        assertEquals(before, positions());
        assertTrue(rules.targets.isEmpty(), "a refused move asks no rule");

        Move valid = new Move(mover, 13);
        engine.apply(valid);
        assertEquals(13, mover.progress());
        assertThrows(IllegalArgumentException.class, () -> engine.apply(valid), "a move is applied only once");
        assertEquals(13, mover.progress());

        engine.legalMoves(red, die(5));
        engine.legalMoves(green, die(6));
        assertThrows(IllegalArgumentException.class, () -> engine.apply(new Move(mover, 18)),
                "the offer to another player replaces the old offer");
        assertEquals(13, mover.progress());
    }

    @Test
    @DisplayName("T20 FR-20: pieces of one colour may share a square and pass each other (no Double Block yet)")
    void ownPiecesDoNotBlock() {
        Player red = byColor.get(PlayerColor.RED);
        Piece a = red.pieces().get(0);
        Piece b = red.pieces().get(1);
        Piece c = red.pieces().get(2);
        a.setProgress(10);
        b.setProgress(13);
        c.setProgress(12);
        Map<Piece, Move> offer = engine.legalMoves(red, die(3));
        assertEquals(13, offer.get(a).targetProgress(), "may land on an own piece");
        assertEquals(15, offer.get(c).targetProgress(), "may pass an own piece");
        engine.apply(offer.get(a));
        assertEquals(a.progress(), b.progress());
    }

    @Test
    @DisplayName("T20 FR-26: the engine asks with the real mover and cell and sends back exactly the listed pieces")
    void captureUsesExactArguments() {
        Player red = byColor.get(PlayerColor.RED);
        Player green = byColor.get(PlayerColor.GREEN);
        Player blue = byColor.get(PlayerColor.BLUE);
        Piece mover = red.pieces().get(3);
        mover.setProgress(20);
        Cell target = expectedCell(PlayerColor.RED, 25);
        Piece g = green.pieces().get(1);
        Piece b = blue.pieces().get(2);
        Piece bystander = blue.pieces().get(0);
        g.setProgress(progressOn(PlayerColor.GREEN, target));
        b.setProgress(progressOn(PlayerColor.BLUE, target));
        bystander.setProgress(progressOn(PlayerColor.BLUE, target));
        rules.answer = List.of(g, b);

        engine.legalMoves(red, die(5));
        MoveResult r = engine.apply(new Move(mover, 25));

        assertEquals(List.of(mover), rules.movers);
        assertEquals(List.of(target), rules.targets);
        assertTrue(g.inCastle());
        assertTrue(b.inCastle());
        assertEquals(progressOn(PlayerColor.BLUE, target), bystander.progress(), "only listed pieces go back");
        assertEquals(List.of(new MoveResult.Capture(g, target), new MoveResult.Capture(b, target)), r.captures());
    }

    @Test
    @DisplayName("T20 FR-26: leaving the castle can report a capture when the rules return one")
    void captureOnCastleExitIsApplied() {
        Player red = byColor.get(PlayerColor.RED);
        Player green = byColor.get(PlayerColor.GREEN);
        Cell start = expectedCell(PlayerColor.RED, 0);
        Piece victim = green.pieces().get(0);
        victim.setProgress(progressOn(PlayerColor.GREEN, start));
        rules.answer = List.of(victim);
        engine.legalMoves(red, die(6));
        MoveResult r = engine.apply(new Move(red.pieces().get(0), 0));
        assertTrue(victim.inCastle());
        assertEquals(List.of(new MoveResult.Capture(victim, start)), r.captures());
        assertEquals(MoveResult.Kind.EXIT_CASTLE, r.kind());
    }

    @Test
    @DisplayName("T20: one bad entry in the capture list refuses the whole move, and the offer stays usable")
    void captureListIsAllOrNothing() {
        Player red = byColor.get(PlayerColor.RED);
        Player green = byColor.get(PlayerColor.GREEN);
        Piece mover = red.pieces().get(0);
        mover.setProgress(30);
        Cell target = expectedCell(PlayerColor.RED, 34);
        Piece good = green.pieces().get(0);
        good.setProgress(progressOn(PlayerColor.GREEN, target));
        Piece castle = green.pieces().get(1);
        Piece finished = green.pieces().get(2);
        finished.setProgress(FINISH);
        Piece own = red.pieces().get(1);
        own.setProgress(34);

        engine.legalMoves(red, die(4));
        List<Integer> before = positions();
        for (List<Piece> bad : List.of(List.of(good, castle), List.of(good, finished), List.of(good, own))) {
            rules.answer = bad;
            assertThrows(IllegalStateException.class, () -> engine.apply(new Move(mover, 34)));
            assertEquals(before, positions());
        }
        rules.answer = Arrays.asList(good, null);
        assertThrows(IllegalStateException.class, () -> engine.apply(new Move(mover, 34)));
        assertEquals(before, positions());

        rules.answer = List.of(good);
        assertEquals(34, engine.apply(new Move(mover, 34)).toProgress());
        assertTrue(good.inCastle());
    }

    @Test
    @DisplayName("T20 FR-43, FR-44: playerFinished and gameOver are right for 2, 3 and 4 players")
    void finishFlagsForEveryPlayerCount() {
        for (int count = 2; count <= 4; count++) {
            List<Player> game = new ArrayList<>(players.subList(0, count));
            resetAll();
            StandardMoveEngine e = new StandardMoveEngine(board, game, rules, home);
            for (int who = 0; who < count - 1; who++) {
                Player p = game.get(who);
                for (int i = 0; i < 4; i++) {
                    Piece piece = p.pieces().get(i);
                    piece.setProgress(FINISH - 2);
                    e.legalMoves(p, die(2));
                    MoveResult r = e.apply(new Move(piece, FINISH));
                    String where = count + " players, player " + who + ", piece " + i;
                    assertTrue(r.pieceFinished(), where);
                    assertEquals(i == 3, r.playerFinished(), where);
                    assertEquals(i == 3 && who == count - 2, r.gameOver(), where);
                }
            }
        }
    }

    @Test
    @DisplayName("T20 FR-20: a piece that cannot move does not hide the legal move of another piece")
    void blockedPieceDoesNotHideOthers() {
        Player red = byColor.get(PlayerColor.RED);
        Piece stuck = red.pieces().get(0);
        Piece free = red.pieces().get(1);
        stuck.setProgress(FINISH - 1);
        free.setProgress(HOME_START);
        Map<Piece, Move> offer = engine.legalMoves(red, die(5));
        assertFalse(offer.containsKey(stuck));
        assertEquals(FINISH, offer.get(free).targetProgress());
    }

    private int progressOn(PlayerColor color, Cell cell) {
        for (int p = 0; p < HOME_START; p++) {
            if (expectedCell(color, p).equals(cell)) {
                return p;
            }
        }
        throw new AssertionError(color + " never passes " + cell);
    }
}
