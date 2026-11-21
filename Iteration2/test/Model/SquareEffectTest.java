package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the Command pattern for square effects (Task T2-10, owner: Shafeek Saif): the receiver
 * {@link EffectMover}, the commands and the invoker {@link EffectQueue} with its chain guard (FR-26, FR-27, FR-29,
 * FR-31, FR-32, FR-38, FR-41, FR-42; SDP risk R2). The independent rule tests are T2-12 (Rakan Ziyan).
 *
 * <p>The tests play on a Medium board with Red and Blue. Special and protection squares are placed at random, so
 * each test first turns them all into plain track squares and then puts the squares it needs on Red's path.
 * Positions are given as Red progress values from 1 to 15, where Red's path is a plain track (cell 9 is Green's
 * arrow, which is a plain square for Red and Blue) and Blue's path passes the same cells (Blue progress = Red
 * progress + 16).
 */
class SquareEffectTest {

    private GameModel model;
    private Board board;
    private EffectMover mover;
    private EffectQueue queue;
    /**
     * Dice moves go through a turn without effects, so the queue under test is the only one. Since T2-10 part 2
     * the turn inside {@link GameModel} has a queue of its own, which these unit tests of the queue leave out.
     */
    private StandardTurnManager turns;

    @BeforeEach
    void setUp() {
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        setup.add(new GameSettings.PlayerSetup("Red", PlayerColor.RED));
        setup.add(new GameSettings.PlayerSetup("Blue", PlayerColor.BLUE));
        model = new GameModel(new GameSettings(Difficulty.MEDIUM, setup), new Random(7), level -> roll(2), () -> 0L);
        board = model.board();
        for (Square square : board.squares()) {
            SquareType type = square.type();
            if (type == SquareType.QUESTION || type == SquareType.SURPRISE || type == SquareType.PROTECTION) {
                square.setType(SquareType.TRACK);
            }
        }
        List<Player> players = model.players();
        SquareRules squareRules = new StandardSquareRules(board, players);
        HomePathRules homePathRules = new StandardHomePathRules(board);
        MoveEngine engine = new StandardMoveEngine(board, players, squareRules, homePathRules);
        mover = new EffectMover(board, players, squareRules, homePathRules, engine);
        queue = new EffectQueue(mover);
        turns = new StandardTurnManager(players, Difficulty.MEDIUM, level -> roll(2), engine,
                new StandardRankingTracker(players));
    }

    // ---------------------------------------------------------------- helpers

    /** A Medium roll that moves {@code steps} (1-6) squares: Regular x (Boost 2 - 1). */
    private static DiceRoll roll(int steps) {
        return new DiceRoll(steps, DieType.BOOST, 2, steps, false, false);
    }

    private Piece red(int index) {
        return model.playerOf(PlayerColor.RED).pieces().get(index);
    }

    private Piece blue(int index) {
        return model.playerOf(PlayerColor.BLUE).pieces().get(index);
    }

    /** The progress of a colour on the cell that Red reaches at {@code redProgress}. */
    private int on(PlayerColor color, int redProgress) {
        int p = board.path(color).indexOf(board.cellAt(PlayerColor.RED, redProgress));
        assertTrue(p >= 0 && p < board.homeStartIndex(), color + " does not pass Red " + redProgress);
        return p;
    }

    private Piece put(Piece piece, int redProgress) {
        piece.setProgress(on(piece.color(), redProgress));
        return piece;
    }

    private Square squareAt(int redProgress) {
        return board.squareAt(board.cellAt(PlayerColor.RED, redProgress));
    }

    private Square mark(int redProgress, SquareType type) {
        Square square = squareAt(redProgress);
        assertEquals(SquareType.TRACK, square.type(), "Red " + redProgress + " is not a plain square");
        square.setType(type);
        if (type == SquareType.QUESTION) {
            square.setQuestionLevel(Difficulty.MEDIUM);
        }
        return square;
    }

    private List<Integer> redCells(MoveResult result) {
        List<Integer> progress = new ArrayList<>();
        for (Cell cell : result.cells()) {
            progress.add(board.path(PlayerColor.RED).indexOf(cell));
        }
        return progress;
    }

    /** A dice move of the first Red piece from {@code from} by the scripted 2, through the real turn rules. */
    private MoveResult diceMove(int from) {
        Piece piece = put(red(0), from);
        assertEquals(2, turns.roll().movement(), "the scripted dice roll 2");
        return turns.move(piece);
    }

    /** Ends Red's turn, lets Blue pass (no piece out, no move), rolls the scripted 2 for Red and returns {@code piece}. */
    private Piece rollForRed(Piece piece) {
        turns.endTurn();
        while (turns.current() != model.playerOf(PlayerColor.RED)) {
            turns.roll();
            turns.endTurn(); // Blue has no piece out: no legal move (FR-23)
        }
        turns.roll();
        return piece;
    }

    // ---------------------------------------------------------------- receiver: movement rules (FR-41)

    @Test
    @DisplayName("FR-35: an effect moves the piece forward or back along its path; cells are listed in the order passed")
    void forwardAndBackwardSteps() {
        Piece piece = put(red(0), 5);
        MoveResult forward = new MoveStepsEffect(piece, 2).execute(mover);
        assertEquals(MoveResult.Kind.STEPS, forward.kind());
        assertEquals(List.of(5, 6, 7), redCells(forward));
        MoveResult back = new MoveStepsEffect(piece, -3).execute(mover);
        assertEquals(List.of(7, 6, 5, 4), redCells(back));
        assertEquals(4, piece.progress());
        assertSame(squareAt(4), back.landedOn());
    }

    @Test
    @DisplayName("FR-32: backward movement stops at the starting square and never crosses it")
    void backwardStopsAtTheStartSquare() {
        Piece piece = put(red(0), 2);
        MoveResult back = mover.moveSteps(piece, -3);
        assertEquals(0, back.toProgress());
        assertEquals(List.of(2, 1, 0), redCells(back));
        MoveResult again = mover.moveSteps(piece, -1);
        assertEquals(MoveResult.Kind.NONE, again.kind(), "already on the starting square: no move");
        assertEquals(0, piece.progress());
    }

    @Test
    @DisplayName("FR-29, FR-41: an opponent block stops a forward effect on the square before it")
    void blockStopsForwardEffect() {
        put(blue(0), 8);
        put(blue(1), 8);
        Piece piece = put(red(0), 6);
        assertEquals(7, mover.moveSteps(piece, 3).toProgress());
        MoveResult none = mover.moveSteps(piece, 2);
        assertEquals(MoveResult.Kind.NONE, none.kind(), "block right ahead: no move");
        assertEquals(7, piece.progress());
    }

    @Test
    @DisplayName("FR-29, FR-41: an opponent block also stops a backward effect, on the square before it")
    void blockStopsBackwardEffect() {
        put(blue(0), 4);
        put(blue(1), 4);
        Piece piece = put(red(0), 7);
        assertEquals(5, mover.moveSteps(piece, -3).toProgress());
        assertEquals(MoveResult.Kind.NONE, mover.moveSteps(piece, -1).kind(), "block right behind: no move");
        assertEquals(5, piece.progress());
    }

    @Test
    @DisplayName("FR-28: the piece crosses or joins a block of its own colour")
    void ownBlockDoesNotStop() {
        put(red(1), 8);
        put(red(2), 8);
        Piece piece = put(red(0), 6);
        assertEquals(9, mover.moveSteps(piece, 3).toProgress(), "cross");
        assertEquals(8, mover.moveSteps(piece, -1).toProgress(), "join");
    }

    @Test
    @DisplayName("FR-26: landing after an effect, forward or backward, captures a lone opponent on a track square")
    void effectCaptures() {
        Piece victim = put(blue(0), 7);
        Piece piece = put(red(0), 5);
        MoveResult forward = mover.moveSteps(piece, 2);
        assertEquals(List.of(new MoveResult.Capture(victim, board.cellAt(PlayerColor.RED, 7))), forward.captures());
        assertTrue(victim.inCastle());

        Piece second = put(blue(1), 4);
        mover.moveSteps(piece, -3);
        assertTrue(second.inCastle(), "a backward effect captures as well");
    }

    @Test
    @DisplayName("FR-27: protection and surprise squares let the pieces stay together after an effect")
    void protectedSquaresPreventCapture() {
        mark(7, SquareType.PROTECTION);
        mark(3, SquareType.SURPRISE);
        Piece onProtection = put(blue(0), 7);
        Piece onSurprise = put(blue(1), 3);
        Piece piece = put(red(0), 5);
        assertTrue(mover.moveSteps(piece, 2).captures().isEmpty());
        assertTrue(mover.moveSteps(piece, -4).captures().isEmpty());
        assertEquals(on(PlayerColor.BLUE, 7), onProtection.progress());
        assertEquals(on(PlayerColor.BLUE, 3), onSurprise.progress());
    }

    @Test
    @DisplayName("FR-31: a forward effect enters the home path, may finish exactly, and does not overshoot")
    void homePathAndExactFinish() {
        Piece piece = red(0);
        piece.setProgress(board.homeStartIndex() - 1);
        MoveResult home = mover.moveSteps(piece, 3);
        assertEquals(board.homeStartIndex() + 2, home.toProgress());
        assertNull(home.landedOn(), "the home path is not a shared-track square");

        piece.setProgress(board.finishIndex() - 2);
        assertEquals(MoveResult.Kind.NONE, mover.moveSteps(piece, 3).kind(), "overshoot: no move");
        MoveResult finish = mover.moveSteps(piece, 2);
        assertTrue(finish.pieceFinished());
    }

    @Test
    @DisplayName("FR-41 exception: the jump to the finish ignores blocks and exact steps")
    void jumpToFinishIgnoresBlocks() {
        put(blue(0), 8);
        put(blue(1), 8);
        Piece piece = put(red(0), 6);
        for (int i = 1; i < 4; i++) {
            red(i).setProgress(board.finishIndex());
        }
        MoveResult jump = new JumpToFinishEffect(piece).execute(mover);
        assertEquals(MoveResult.Kind.TO_FINISH, jump.kind());
        assertTrue(piece.finished());
        assertTrue(jump.pieceFinished() && jump.playerFinished() && jump.gameOver(), "two players: Red done ends it");
    }

    @Test
    @DisplayName("Table 2: return to the castle")
    void returnToCastle() {
        Piece piece = put(red(0), 6);
        MoveResult back = new ReturnToCastleEffect(piece).execute(mover);
        assertEquals(MoveResult.Kind.TO_CASTLE, back.kind());
        assertEquals(board.cellAt(PlayerColor.RED, 6), back.fromCell());
        assertTrue(piece.inCastle());
    }

    @Test
    @DisplayName("Invalid input: zero steps, a piece off the board or from another game; nothing changes")
    void invalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new MoveStepsEffect(red(0), 0));
        assertThrows(NullPointerException.class, () -> new JumpToFinishEffect(null));
        Piece inCastle = red(0);
        assertThrows(IllegalArgumentException.class, () -> mover.moveSteps(inCastle, 2));
        assertThrows(IllegalArgumentException.class, () -> mover.returnToCastle(inCastle));
        Piece finished = red(1);
        finished.setProgress(board.finishIndex());
        assertThrows(IllegalArgumentException.class, () -> mover.jumpToFinish(finished));
        Piece stranger = new Piece(PlayerColor.RED, 0, board.finishIndex());
        stranger.setProgress(5);
        assertThrows(IllegalArgumentException.class, () -> mover.moveSteps(stranger, 1));
        assertTrue(inCastle.inCastle());
        assertTrue(finished.finished());
    }

    // ---------------------------------------------------------------- invoker: order and chain guard (FR-42, R2)

    @Test
    @DisplayName("FR-34, FR-38: a dice move onto a question square queues it; its capture comes first and stays")
    void diceMoveQueuesTheSquareAndCapturesFirst() {
        Square question = mark(7, SquareType.QUESTION);
        Piece victim = put(blue(0), 7);
        MoveResult move = diceMove(5);
        assertEquals(1, move.captures().size(), "captured by the move itself, before the question");

        queue.start(move);
        EffectQueue.Activation next = queue.next();
        assertSame(red(0), next.piece());
        assertSame(question, next.square());
        assertEquals(SquareType.QUESTION, next.type());

        queue.run(new MoveStepsEffect(red(0), -2)); // a wrong answer
        assertTrue(victim.inCastle(), "the answer does not undo the capture (FR-38)");
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("A dice move onto a plain square starts no effect")
    void plainSquareStartsNothing() {
        queue.start(diceMove(5));
        assertTrue(queue.isEmpty());
        assertNull(queue.next());
    }

    @Test
    @DisplayName("FR-42: chained squares are resolved one at a time, in the order they are reached")
    void chainInOrder() {
        Square q7 = mark(7, SquareType.QUESTION);
        Square s10 = mark(10, SquareType.SURPRISE);
        Square q6 = mark(6, SquareType.QUESTION);
        Piece piece = red(0);
        queue.start(diceMove(5));
        assertSame(q7, queue.next().square());

        queue.run(new MoveStepsEffect(piece, 3)); // right answer: 7 -> 10, a surprise square
        assertSame(s10, queue.next().square());
        queue.run(new MoveStepsEffect(piece, -4)); // 10 -> 6, a question square
        assertSame(q6, queue.next().square());
        queue.run(new MoveStepsEffect(piece, 2)); // 6 -> 8, a plain square: the chain ends
        assertTrue(queue.isEmpty());

        List<Integer> stops = new ArrayList<>();
        for (MoveResult r : queue.results()) {
            stops.add(r.toProgress());
        }
        assertEquals(List.of(10, 6, 8), stops);
    }

    @Test
    @DisplayName("Q81: an effect that moves zero steps does not activate the square again")
    void zeroStepsDoNotReactivate() {
        mark(7, SquareType.QUESTION);
        put(blue(0), 8);
        put(blue(1), 8);
        queue.start(diceMove(5));
        MoveResult none = queue.run(new MoveStepsEffect(red(0), 2)); // a block right ahead
        assertEquals(MoveResult.Kind.NONE, none.kind());
        assertTrue(queue.isEmpty(), "the piece stays on the question square, which is not asked again");
    }

    @Test
    @DisplayName("A piece leaving its castle activates no square: only a STEPS move does (review of #87)")
    void castleExitActivatesNothing() {
        Square surprise = mark(5, SquareType.SURPRISE);
        Piece piece = red(0);
        MoveResult exit = new MoveResult(piece, MoveResult.Kind.EXIT_CASTLE, Piece.IN_CASTLE, 5,
                List.of(surprise.cell()), null, List.of(), surprise, false, false, false);
        queue.start(exit);
        assertTrue(queue.isEmpty(), "even on a special square, an exit from the castle starts no chain");
    }

    @Test
    @DisplayName("R2 chain guard: a square is activated once per chain, so the chain ends; the next dice move resets it")
    void eachSquareOncePerChain() {
        Square q7 = mark(7, SquareType.QUESTION);
        Square q5 = mark(5, SquareType.QUESTION);
        Piece piece = red(0);
        queue.start(diceMove(3)); // 3 -> 5
        assertSame(q5, queue.next().square());
        queue.run(new MoveStepsEffect(piece, 2)); // 5 -> 7
        assertSame(q7, queue.next().square());
        queue.run(new MoveStepsEffect(piece, -2)); // back to 5: already activated in this chain
        assertTrue(queue.isEmpty());

        // A new dice move starts a new chain: 5 -> 7 activates q7 again.
        queue.start(turns.move(rollForRed(piece)));
        assertSame(q7, queue.next().square());
    }

    @Test
    @DisplayName("FR-42: an opponent pushed back onto a special square activates it, with the opponent's piece")
    void opponentPushedOntoSpecialSquare() {
        Square q4 = mark(4, SquareType.QUESTION);
        mark(7, SquareType.SURPRISE);
        Piece opponent = put(blue(0), 6);
        queue.start(diceMove(5)); // Red reaches the surprise square
        queue.run(new MoveStepsEffect(opponent, -2)); // "move an opponent back" (FR-40)
        assertSame(opponent, queue.next().piece());
        assertSame(q4, queue.next().square());
    }

    @Test
    @DisplayName("Jumps to the castle or the finish reach no special square; skip resolves a square without a move")
    void jumpsAndSkipEndTheChain() {
        mark(7, SquareType.SURPRISE);
        queue.start(diceMove(5));
        queue.run(new ReturnToCastleEffect(red(0)));
        assertTrue(queue.isEmpty());

        mark(3, SquareType.SURPRISE);
        queue.start(turns.move(rollForRed(put(red(1), 1))));
        EffectQueue.Activation skipped = queue.skip(); // e.g. "skip the next turn" moves no piece
        assertSame(red(1), skipped.piece());
        assertTrue(queue.isEmpty());
        assertTrue(queue.results().isEmpty());
    }

    @Test
    @DisplayName("Invoker misuse: no square waiting, a new chain too early, a failing command keeps the square")
    void invokerRefusesMisuse() {
        assertThrows(IllegalStateException.class, () -> queue.run(new MoveStepsEffect(red(0), 1)));
        assertThrows(IllegalStateException.class, queue::skip);

        mark(7, SquareType.QUESTION);
        MoveResult move = diceMove(5);
        queue.start(move);
        assertThrows(IllegalStateException.class, () -> queue.start(move));
        assertThrows(IllegalArgumentException.class, () -> queue.run(new MoveStepsEffect(red(1), 1)),
                "red 2 is still in its castle");
        assertSame(red(0), queue.next().piece(), "the square still waits");
        assertThrows(NullPointerException.class, () -> queue.run(null));
    }
}
