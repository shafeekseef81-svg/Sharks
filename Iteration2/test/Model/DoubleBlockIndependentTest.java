package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Independent tests of Double Blocks (Task T2-07, US-2.2: FR-19, FR-28, FR-29, FR-30). Written by Shafeek Saif,
 * who did not implement them (T2-06, Rakan), from the rule text and not from the author's tests (SDP 5.3.4).
 *
 * <p>The cases are those of SDP 7.2: <b>cross</b>, <b>join</b>, <b>block both ways</b>, <b>stop</b>,
 * <b>forced break</b> and <b>castle exit</b>. The rules, as worded in the SRS:
 * <ul>
 *   <li>Two or more pieces of one player on one square form a Double Block, which the owner's pieces may cross
 *       or join (FR-28).</li>
 *   <li>An opponent's Double Block prevents landing or passing in either direction. The player moves another
 *       legal piece if possible; otherwise the movement stops before the block (FR-29).</li>
 *   <li>A Double Block stays active while at least two pieces share its square. Its owner must move a blocking
 *       piece if no other legal move exists (FR-30).</li>
 *   <li>A piece leaves the castle only on a Regular 6, onto its own start square (FR-19), so an opponent block
 *       there keeps it in the castle.</li>
 * </ul>
 *
 * <p>Every game is played through the {@link GameModel} facade, with the real board and the real rule classes,
 * and with dice whose values the test chooses. Positions are set by hand; the expected cells are found from the
 * board's paths, so each scenario works on any random placement of the protection squares. Backward moves
 * (questions and surprises) are T2-10 (#61); for them, "either direction" is checked here through
 * {@link MoveEngine#isPassable}, the check every move of the engine goes through.
 */
class DoubleBlockIndependentTest {

    /** Dice whose next rolls the test chooses. */
    private static final class ScriptDice implements Dice {
        final Deque<DiceRoll> next = new ArrayDeque<>();

        @Override
        public DiceRoll roll(Difficulty level) {
            return next.remove();
        }
    }

    /** Red's progress of the block square in most scenarios: on the shared track, far from every start. */
    private static final int BLOCK = 20;

    private final ScriptDice dice = new ScriptDice();
    private GameModel model;

    // ------------------------------------------------------------------ set-up helpers

    /** A game on the level with the given colours in this turn order, the first colour to play. */
    private void start(Difficulty level, PlayerColor... colors) {
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        for (PlayerColor c : colors) {
            setup.add(new GameSettings.PlayerSetup(c.label(), c));
        }
        model = new GameModel(new GameSettings(level, setup), new Random(7), dice, () -> 0L);
        assertSame(player(colors[0]), model.currentPlayer());
    }

    private void start(PlayerColor... colors) {
        start(Difficulty.EASY, colors);
    }

    private Player player(PlayerColor color) {
        return model.playerOf(color);
    }

    private Piece piece(PlayerColor color, int index) {
        return player(color).pieces().get(index);
    }

    /**
     * The progress of {@code color} on the square that {@code ref} reaches at {@code refProgress}. Fails unless that
     * square is on the shared track of both colours, so a scenario never puts a piece on a home path by mistake.
     */
    private int on(PlayerColor color, PlayerColor ref, int refProgress) {
        Board board = model.board();
        int p = board.path(color).indexOf(board.cellAt(ref, refProgress));
        assertTrue(p >= 0 && p < board.homeStartIndex(),
                color + " does not reach " + ref + " progress " + refProgress + " on the shared track");
        return p;
    }

    /** Puts two pieces of {@code owner} on the square that Red reaches at {@code redProgress}: a Double Block. */
    private void block(PlayerColor owner, int first, int second, int redProgress) {
        int p = on(owner, PlayerColor.RED, redProgress);
        piece(owner, first).setProgress(p);
        piece(owner, second).setProgress(p);
    }

    private static DiceRoll regular(int value) {
        return DiceRoll.regularOnly(value);
    }

    /** Rolls the given roll for the active player. */
    private DiceRoll roll(DiceRoll roll) {
        dice.next.add(roll);
        return model.roll();
    }

    private DiceRoll roll(int value) {
        return roll(regular(value));
    }

    /** The target progress offered for a piece after the last roll, or -2 when the piece is not offered. */
    private int offered(Piece piece) {
        Move move = model.legalMoves().get(piece);
        return move == null ? -2 : move.targetProgress();
    }

    /** A fresh engine over this game's board and pieces, to ask {@link MoveEngine#isPassable} directly. */
    private MoveEngine engine() {
        Board board = model.board();
        List<Player> players = model.players();
        return new StandardMoveEngine(board, players, new StandardSquareRules(board, players),
                new StandardHomePathRules(board));
    }

    // ------------------------------------------------------------------ forming a block (FR-28)

    @Test
    @DisplayName("T2-07 FR-28: two pieces of one player on a square form a Double Block for every opponent")
    void twoPiecesFormABlockForEveryOpponent() {
        start(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        Cell cell = model.board().cellAt(PlayerColor.RED, BLOCK);
        MoveEngine engine = engine();

        assertFalse(engine.isPassable(cell, player(PlayerColor.RED)));
        assertFalse(engine.isPassable(cell, player(PlayerColor.YELLOW)));
        assertFalse(engine.isPassable(cell, player(PlayerColor.BLUE)));
        assertTrue(engine.isPassable(cell, player(PlayerColor.GREEN)), "the owner is never blocked");
        assertTrue(engine.isPassable(model.board().cellAt(PlayerColor.RED, BLOCK - 1), player(PlayerColor.RED)));
        assertTrue(engine.isPassable(model.board().cellAt(PlayerColor.RED, BLOCK + 1), player(PlayerColor.RED)));
    }

    @Test
    @DisplayName("T2-07 FR-28: one piece alone is no Double Block; an opponent passes it")
    void onePieceDoesNotBlock() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        piece(PlayerColor.GREEN, 0).setProgress(on(PlayerColor.GREEN, PlayerColor.RED, BLOCK));
        Piece mover = piece(PlayerColor.RED, 0);
        mover.setProgress(BLOCK - 2);

        roll(4);
        assertEquals(BLOCK + 2, offered(mover));
    }

    @Test
    @DisplayName("T2-07 FR-28: pieces of two different opponents on one square are no Double Block")
    void twoDifferentOpponentsDoNotBlock() {
        start(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW);
        piece(PlayerColor.GREEN, 0).setProgress(on(PlayerColor.GREEN, PlayerColor.RED, BLOCK));
        piece(PlayerColor.YELLOW, 0).setProgress(on(PlayerColor.YELLOW, PlayerColor.RED, BLOCK));
        Piece mover = piece(PlayerColor.RED, 0);
        mover.setProgress(BLOCK - 2);

        roll(4);
        assertEquals(BLOCK + 2, offered(mover));
        model.move(mover);
        assertEquals(BLOCK + 2, mover.progress());
    }

    // ------------------------------------------------------------------ cross and join (FR-28)

    @Test
    @DisplayName("T2-07 FR-28 cross: the owner's piece passes its own Double Block, which stays")
    void ownerCrossesOwnBlock() {
        start(PlayerColor.GREEN, PlayerColor.RED);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        int blockAt = piece(PlayerColor.GREEN, 0).progress();
        Piece crosser = piece(PlayerColor.GREEN, 2);
        crosser.setProgress(blockAt - 2);

        roll(5);
        assertEquals(blockAt + 3, offered(crosser));
        model.move(crosser);

        assertEquals(blockAt + 3, crosser.progress());
        assertEquals(blockAt, piece(PlayerColor.GREEN, 0).progress());
        assertEquals(blockAt, piece(PlayerColor.GREEN, 1).progress());
        assertFalse(engine().isPassable(model.board().cellAt(PlayerColor.RED, BLOCK), player(PlayerColor.RED)),
                "the block is still there for the opponent");
    }

    @Test
    @DisplayName("T2-07 FR-28 join: the owner's third piece lands on its own Double Block; no capture, still a block")
    void ownerJoinsOwnBlock() {
        start(PlayerColor.GREEN, PlayerColor.RED);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        int blockAt = piece(PlayerColor.GREEN, 0).progress();
        Piece joiner = piece(PlayerColor.GREEN, 2);
        joiner.setProgress(blockAt - 3);

        roll(3);
        assertEquals(blockAt, offered(joiner));
        MoveResult r = model.move(joiner);

        assertTrue(r.captures().isEmpty(), "own pieces are never captured");
        for (int i = 0; i < 3; i++) {
            assertEquals(blockAt, piece(PlayerColor.GREEN, i).progress());
        }
        assertFalse(engine().isPassable(model.board().cellAt(PlayerColor.RED, BLOCK), player(PlayerColor.RED)));
    }

    // ------------------------------------------------------------------ block: no landing, no passing (FR-29)

    @Test
    @DisplayName("T2-07 FR-29: an opponent may not land on a Double Block; the player moves another piece")
    void noLandingOnBlock() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        Piece blocked = piece(PlayerColor.RED, 0);
        Piece free = piece(PlayerColor.RED, 1);
        blocked.setProgress(BLOCK - 4);
        free.setProgress(BLOCK + 6);

        roll(4);
        assertEquals(Set.of(free), model.legalMoves().keySet(),
                "another piece can use the whole roll, so the blocked piece gets no shorter stop");
        assertEquals(BLOCK + 10, offered(free));
    }

    @Test
    @DisplayName("T2-07 FR-29: an opponent may not pass a Double Block; the player moves another piece")
    void noPassingABlock() {
        for (int value = 2; value <= 5; value++) {
            start(PlayerColor.RED, PlayerColor.GREEN);
            block(PlayerColor.GREEN, 0, 1, BLOCK);
            Piece blocked = piece(PlayerColor.RED, 0);
            Piece free = piece(PlayerColor.RED, 1);
            blocked.setProgress(BLOCK - 1);
            free.setProgress(BLOCK + 6);

            roll(value); // the blocked piece would pass the block with any of these values
            assertEquals(Set.of(free), model.legalMoves().keySet(), "die " + value);
            assertEquals(BLOCK + 6 + value, offered(free), "die " + value);
        }
    }

    @Test
    @DisplayName("T2-07 FR-29 both ways: the check used for every step does not depend on the side of the piece")
    void blockHoldsFromBothSides() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        Cell cell = model.board().cellAt(PlayerColor.RED, BLOCK);
        Player red = player(PlayerColor.RED);
        MoveEngine engine = engine();

        piece(PlayerColor.RED, 0).setProgress(BLOCK - 3); // would cross it going forward
        assertFalse(engine.isPassable(cell, red));
        piece(PlayerColor.RED, 0).setProgress(BLOCK + 3); // would cross it going back (question, surprise: T2-10)
        assertFalse(engine.isPassable(cell, red));
        piece(PlayerColor.RED, 0).setProgress(Piece.IN_CASTLE);
        assertFalse(engine.isPassable(cell, red));
    }

    @Test
    @DisplayName("T2-07 FR-29: a Double Block also stops a large move with a Boost (Medium) or UltraBoost (Hard) roll")
    void blockStopsSpecialDiceMoves() {
        for (Difficulty level : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            start(level, PlayerColor.RED, PlayerColor.GREEN);
            block(PlayerColor.GREEN, 0, 1, BLOCK);
            Piece blocked = piece(PlayerColor.RED, 0);
            Piece free = piece(PlayerColor.RED, 1);
            blocked.setProgress(BLOCK - 5);
            free.setProgress(BLOCK + 2);
            DiceRoll big = level == Difficulty.MEDIUM
                    ? new DiceRoll(4, DieType.BOOST, 3, 8, false, false)        // 4 x (3 - 1) = 8
                    : new DiceRoll(3, DieType.ULTRA_BOOST, 4, 9, false, false); // 3 x (4 - 1) = 9

            roll(big);
            assertEquals(Set.of(free), model.legalMoves().keySet(), level.toString());
            assertEquals(BLOCK + 2 + big.movement(), offered(free), level.toString());
        }
    }

    // ------------------------------------------------------------------ stop before the block (FR-29)

    @Test
    @DisplayName("T2-07 FR-29 stop: with no other legal piece, the blocked piece stops on the square before the block")
    void onlyBlockedPieceStopsBeforeBlock() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        Piece blocked = piece(PlayerColor.RED, 0);
        blocked.setProgress(BLOCK - 3);

        roll(5); // would reach BLOCK + 2
        assertEquals(Map.of(blocked, new Move(blocked, BLOCK - 1)), model.legalMoves());
        MoveResult r = model.move(blocked);

        assertEquals(BLOCK - 1, blocked.progress());
        assertEquals(BLOCK - 3, r.fromProgress());
        assertEquals(BLOCK - 1, r.toProgress());
        assertSame(player(PlayerColor.GREEN), model.currentPlayer(), "no 6, so the turn passes");
        assertEquals(on(PlayerColor.GREEN, PlayerColor.RED, BLOCK), piece(PlayerColor.GREEN, 0).progress());
        assertEquals(on(PlayerColor.GREEN, PlayerColor.RED, BLOCK), piece(PlayerColor.GREEN, 1).progress());
    }

    @Test
    @DisplayName("T2-07 FR-29 stop: the piece stops before the first of two Double Blocks on its way")
    void stopsBeforeTheFirstOfTwoBlocks() {
        start(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW);
        block(PlayerColor.YELLOW, 0, 1, BLOCK);
        block(PlayerColor.GREEN, 0, 1, BLOCK - 2);
        Piece blocked = piece(PlayerColor.RED, 0);
        blocked.setProgress(BLOCK - 5);

        roll(5); // would reach BLOCK exactly
        assertEquals(BLOCK - 3, offered(blocked));
        assertEquals(1, model.legalMoves().size());
    }

    @Test
    @DisplayName("T2-07 FR-29 stop: a castle exit on a 6 is another legal move, so a blocked piece gets no stop")
    void castleExitIsAnotherLegalMove() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        Piece blocked = piece(PlayerColor.RED, 0);
        blocked.setProgress(BLOCK - 3);

        roll(6);
        assertEquals(Set.of(piece(PlayerColor.RED, 1), piece(PlayerColor.RED, 2), piece(PlayerColor.RED, 3)),
                model.legalMoves().keySet());
        assertEquals(0, offered(piece(PlayerColor.RED, 1)));
    }

    @Test
    @DisplayName("T2-07 FR-29, FR-23 stop: a block on the very next square leaves no move, and the turn passes")
    void blockRightAheadGivesNoMove() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        Piece blocked = piece(PlayerColor.RED, 0);
        blocked.setProgress(BLOCK - 1);

        roll(3);
        assertEquals(BLOCK - 1, blocked.progress());
        assertSame(player(PlayerColor.GREEN), model.currentPlayer());
    }

    @Test
    @DisplayName("T2-07 FR-29, FR-21 stop: a blocked 6 with no move still gives the extra roll")
    void blockedSixStillRollsAgain() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        block(PlayerColor.GREEN, 2, 3, 0); // Red's start square too, so no castle exit
        for (Piece p : player(PlayerColor.RED).pieces().subList(1, 4)) {
            assertTrue(p.inCastle());
        }
        piece(PlayerColor.RED, 0).setProgress(BLOCK - 1);

        roll(6);
        assertTrue(model.legalMoves().isEmpty());
        assertSame(player(PlayerColor.RED), model.currentPlayer());
        assertTrue(model.hasBonusRoll());
    }

    // ------------------------------------------------------------------ persistence and forced break (FR-30)

    @Test
    @DisplayName("T2-07 FR-30: the block stays over several turns while its pieces stay, and breaks when one leaves")
    void blockLastsUntilAPieceLeaves() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        int blockAt = piece(PlayerColor.GREEN, 0).progress();
        Piece other = piece(PlayerColor.GREEN, 2);
        other.setProgress(blockAt + 10);
        Piece red = piece(PlayerColor.RED, 0);
        red.setProgress(BLOCK - 1);

        for (int round = 0; round < 3; round++) {
            roll(4);
            assertEquals(BLOCK - 1, red.progress(), "round " + round);
            assertSame(player(PlayerColor.GREEN), model.currentPlayer());
            roll(1);
            model.move(other); // Green chooses its free piece, so the block stays
        }
        assertEquals(blockAt, piece(PlayerColor.GREEN, 0).progress());
        assertEquals(blockAt, piece(PlayerColor.GREEN, 1).progress());

        roll(4); // Red, still blocked
        roll(2);
        model.move(piece(PlayerColor.GREEN, 1)); // one piece leaves: the block is gone

        roll(3);
        assertEquals(BLOCK + 2, offered(red), "Red now passes the single Green piece");
    }

    @Test
    @DisplayName("T2-07 FR-30 forced break: with no other legal move, the owner must move a piece of its block")
    void forcedBreak() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        block(PlayerColor.GREEN, 0, 1, BLOCK);
        int blockAt = piece(PlayerColor.GREEN, 0).progress();
        Piece red = piece(PlayerColor.RED, 0);
        red.setProgress(BLOCK - 1);

        roll(2); // Red is blocked, the turn passes
        assertSame(player(PlayerColor.GREEN), model.currentPlayer());

        roll(3);
        assertEquals(Set.of(piece(PlayerColor.GREEN, 0), piece(PlayerColor.GREEN, 1)), model.legalMoves().keySet(),
                "only the block pieces can move, and the move may not be skipped (FR-20)");
        assertSame(player(PlayerColor.GREEN), model.currentPlayer());
        model.move(piece(PlayerColor.GREEN, 0));
        assertEquals(blockAt + 3, piece(PlayerColor.GREEN, 0).progress());
        assertTrue(engine().isPassable(model.board().cellAt(PlayerColor.RED, BLOCK), player(PlayerColor.RED)),
                "one piece left: the block is broken");

        roll(2);
        assertEquals(BLOCK + 1, offered(red));
        model.move(red);
        assertEquals(BLOCK + 1, red.progress());
    }

    // ------------------------------------------------------------------ castle exit (FR-19, FR-29)

    @Test
    @DisplayName("T2-07 FR-19, FR-29 castle exit: an opponent block on the start square keeps the piece in the castle")
    void blockOnStartSquareStopsCastleExit() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        block(PlayerColor.GREEN, 0, 1, 0);

        roll(6);
        assertTrue(model.legalMoves().isEmpty());
        assertTrue(player(PlayerColor.RED).pieces().stream().allMatch(Piece::inCastle));
        assertTrue(model.hasBonusRoll(), "the 6 still gives the extra roll (FR-21)");

        roll(6);
        assertTrue(model.legalMoves().isEmpty(), "the block still stands for the second 6");
    }

    @Test
    @DisplayName("T2-07 FR-19 castle exit: a single opponent piece on the start square does not stop the exit")
    void singleOpponentOnStartSquareDoesNotStopExit() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        Piece green = piece(PlayerColor.GREEN, 0);
        green.setProgress(on(PlayerColor.GREEN, PlayerColor.RED, 0));

        roll(6);
        Piece first = piece(PlayerColor.RED, 0);
        assertEquals(0, offered(first));
        MoveResult r = model.move(first);
        assertEquals(MoveResult.Kind.EXIT_CASTLE, r.kind());
        assertEquals(0, first.progress());
        assertTrue(r.captures().isEmpty(), "the start square is protected (FR-27)");
    }

    @Test
    @DisplayName("T2-07 FR-19, FR-28 castle exit: the owner leaves the castle onto its own block on the start square")
    void exitJoinsOwnBlockOnStartSquare() {
        start(PlayerColor.RED, PlayerColor.GREEN);
        piece(PlayerColor.RED, 0).setProgress(0);
        piece(PlayerColor.RED, 1).setProgress(0);

        roll(6);
        Piece third = piece(PlayerColor.RED, 2);
        assertEquals(0, offered(third));
        model.move(third);
        assertEquals(0, third.progress());
        assertFalse(engine().isPassable(model.board().startCell(PlayerColor.RED), player(PlayerColor.GREEN)));
    }
}
