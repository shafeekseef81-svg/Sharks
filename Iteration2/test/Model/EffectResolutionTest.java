package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests of T2-10 part 2: the turn waits in {@link TurnPhase#RESOLVING} while a question or surprise
 * square is resolved, and the game goes on correctly afterwards (FR-34..FR-43, SRS Table 2). Owner: Shafeek Saif.
 * The independent rule tests are T2-12 (Rakan Ziyan).
 *
 * <p>The board is a Medium board with Red and Blue whose special and protection squares are first made plain, as
 * in {@code SquareEffectTest}; each test then places the squares it needs. Positions are Red progress values from 1
 * to 15 (cell 9 is Green's arrow and is left alone); Blue passes the same cells. The dice are scripted.
 */
class EffectResolutionTest {

    private GameModel model;
    private Board board;
    private final Deque<DiceRoll> script = new ArrayDeque<>();
    private final List<Difficulty> diceAsked = new ArrayList<>();
    private final List<GameEvent> events = new ArrayList<>();
    /** Builds surprises with a chosen outcome, as the dialog would get them from the real draw. */
    private SurpriseFactory surprises;

    @BeforeEach
    void setUp() {
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        setup.add(new GameSettings.PlayerSetup("Red", PlayerColor.RED));
        setup.add(new GameSettings.PlayerSetup("Blue", PlayerColor.BLUE));
        Dice dice = level -> {
            diceAsked.add(level);
            return script.isEmpty() ? roll(2) : script.poll();
        };
        model = new GameModel(new GameSettings(Difficulty.MEDIUM, setup), new Random(7), dice, () -> 0L);
        board = model.board();
        for (Square square : board.squares()) {
            SquareType type = square.type();
            if (type == SquareType.QUESTION || type == SquareType.SURPRISE || type == SquareType.PROTECTION) {
                square.setType(SquareType.TRACK);
            }
        }
        surprises = new SurpriseFactory(board, model.players(), new Random(1));
        model.addListener(events::add);
    }

    // ---------------------------------------------------------------- helpers

    /** A Medium roll that moves {@code steps} squares: Regular x (Boost 2 - 1). */
    private static DiceRoll roll(int steps) {
        return new DiceRoll(steps, DieType.BOOST, 2, steps, false, false);
    }

    private Player red() {
        return model.playerOf(PlayerColor.RED);
    }

    private Player blue() {
        return model.playerOf(PlayerColor.BLUE);
    }

    private Piece put(Piece piece, int redProgress) {
        piece.setProgress(board.path(piece.color()).indexOf(board.cellAt(PlayerColor.RED, redProgress)));
        return piece;
    }

    private int redProgress(Piece piece) {
        return board.path(PlayerColor.RED).indexOf(board.cellAt(piece.color(), piece.progress()));
    }

    private Square mark(int redProgress, SquareType type, Difficulty questionLevel) {
        Square square = board.squareAt(board.cellAt(PlayerColor.RED, redProgress));
        assertEquals(SquareType.TRACK, square.type(), "Red " + redProgress + " is not a plain square");
        square.setType(type);
        if (type == SquareType.QUESTION) {
            square.setQuestionLevel(questionLevel);
        }
        return square;
    }

    private Square surpriseAt(int redProgress) {
        return mark(redProgress, SquareType.SURPRISE, null);
    }

    /** Red moves its first piece from {@code from} with the next scripted roll (2 if none is scripted). */
    private MoveResult redMoves(int from) {
        Piece piece = put(red().pieces().get(0), from);
        model.roll();
        return model.move(piece);
    }

    private SurpriseResult outcome(SurpriseOutcome outcome) {
        return surprises.promptFor(outcome, model.pendingEffect().piece(), model.currentPlayer().color()).resolve();
    }

    private boolean fired(GameEvent.Type type, Player player) {
        return events.stream().anyMatch(e -> e.type() == type && e.player() == player);
    }

    // ---------------------------------------------------------------- questions

    @Test
    @DisplayName("FR-34: a dice move onto a question square waits for the answer; roll and move are refused meanwhile")
    void questionSquareWaitsForTheAnswer() {
        Square square = mark(5, SquareType.QUESTION, Difficulty.HARD);
        redMoves(3);
        Piece piece = red().pieces().get(0);

        assertEquals(TurnPhase.RESOLVING, model.phase());
        assertSame(square, model.pendingEffect().square());
        assertSame(piece, model.pendingEffect().piece());
        assertTrue(fired(GameEvent.Type.EFFECT_PENDING, red()));
        assertSame(red(), model.currentPlayer(), "the turn does not pass while the square waits");
        assertTrue(model.legalMoves().isEmpty());
        assertThrows(IllegalStateException.class, model::roll);
        assertThrows(IllegalStateException.class, () -> model.move(piece));

        Question question = model.pendingQuestion();
        assertEquals(Difficulty.HARD, question.level(), "FR-34: the square's own level");
        assertSame(question, model.pendingQuestion(), "asked once until it is answered");
        QuestionResult right = model.judgeAnswer(question.correctIndex());
        assertEquals(5, redProgress(piece), "judging changes nothing");
        assertEquals(TurnPhase.RESOLVING, model.phase());

        MoveResult moved = model.applyAnswer(right);
        assertEquals(3, moved.toProgress() - moved.fromProgress(), "FR-35: Hard right answer, 3 forward");
        assertEquals(8, redProgress(piece));
        assertNull(model.pendingEffect());
        assertSame(blue(), model.currentPlayer(), "no 6: the turn passes after the chain");
        assertEquals(TurnPhase.AWAITING_ROLL, model.phase());
    }

    @Test
    @DisplayName("FR-35, FR-42, FR-32: a wrong answer moves back onto a surprise square, which is resolved next")
    void wrongAnswerChainsIntoASurprise() {
        mark(5, SquareType.QUESTION, Difficulty.MEDIUM);
        Square trap = surpriseAt(3);
        script.add(roll(4));
        redMoves(1); // 1 -> 5, passing the surprise square: passing activates nothing
        Piece piece = red().pieces().get(0);
        Question question = model.pendingQuestion();
        model.applyAnswer(model.judgeAnswer((question.correctIndex() + 1) % Question.ANSWER_COUNT));

        assertEquals(3, redProgress(piece), "FR-35: Medium wrong answer, 2 back");
        assertEquals(TurnPhase.RESOLVING, model.phase(), "FR-42: the new special square is activated");
        assertSame(trap, model.pendingEffect().square());

        model.applySurprise(outcome(SurpriseOutcome.BACK_THREE));
        assertEquals(0, piece.progress(), "FR-32: back three from 3 stops on the start square");
        assertSame(blue(), model.currentPlayer());
    }

    @Test
    @DisplayName("FR-21: after a chain started by a 6 the same player rolls again")
    void aSixStillRollsAgainAfterTheChain() {
        mark(10, SquareType.QUESTION, Difficulty.MEDIUM);
        script.add(roll(6));
        redMoves(4);
        assertEquals(TurnPhase.RESOLVING, model.phase());
        assertTrue(model.hasBonusRoll());
        Question question = model.pendingQuestion();
        model.applyAnswer(model.judgeAnswer(question.correctIndex()));
        assertSame(red(), model.currentPlayer());
        assertEquals(TurnPhase.AWAITING_ROLL, model.phase());
    }

    @Test
    @DisplayName("Answers are checked against the waiting question; nothing can be applied when nothing waits")
    void staleOrMisplacedAnswersAreRefused() {
        assertThrows(IllegalStateException.class, model::pendingQuestion, "nothing waits");
        mark(5, SquareType.QUESTION, Difficulty.EASY);
        redMoves(3);
        Question question = model.pendingQuestion();
        QuestionResult right = model.judgeAnswer(question.correctIndex());
        Question other = new Question("X-1", Difficulty.EASY, "Another question", 0, "a", "b", "c", "d");
        assertThrows(IllegalArgumentException.class,
                () -> model.applyAnswer(new QuestionResult(other, 0, true, 1)), "not the waiting question");
        assertThrows(IllegalStateException.class, model::pendingSurprise, "a question waits, not a surprise");
        model.applyAnswer(right);
        assertThrows(IllegalStateException.class, () -> model.applyAnswer(right), "already applied");
    }

    // ---------------------------------------------------------------- surprises: pieces

    @Test
    @DisplayName("FR-40, FR-42: an opponent pushed onto a surprise square; the active player still answers for it")
    void pushedOpponentTriggersTheNextSurprise() {
        surpriseAt(5);
        surpriseAt(11);
        surpriseAt(12);
        surpriseAt(13);
        Piece target = put(blue().pieces().get(0), 14);
        redMoves(3);
        SurprisePrompt push = surprises.promptFor(SurpriseOutcome.OPPONENT_BACK, model.pendingEffect().piece(),
                PlayerColor.RED);
        model.applySurprise(model.resolveSurprise(push, target));
        assertEquals(14 - push.backwardSteps(), redProgress(target), "FR-40: back by half a Regular die");

        assertEquals(TurnPhase.RESOLVING, model.phase());
        assertSame(target, model.pendingEffect().piece(), "Blue's piece reached the next square");
        assertSame(red(), model.currentPlayer(), "still Red's turn");
        SurprisePrompt next = model.pendingSurprise();
        assertEquals(PlayerColor.RED, next.active(), "FR-40: the active player chooses");
        assertTrue(next.opponentTriggered());
        assertTrue(next.targets().stream().noneMatch(p -> p.color() == PlayerColor.RED));
    }

    @Test
    @DisplayName("FR-43, FR-44: a jump that brings an opponent's last piece home ranks that opponent and ends the game")
    void effectFinishCountsForThePieceOwner() {
        surpriseAt(5);
        surpriseAt(11);
        surpriseAt(12);
        surpriseAt(13);
        List<Piece> bluePieces = blue().pieces();
        for (int i = 1; i < bluePieces.size(); i++) {
            bluePieces.get(i).setProgress(bluePieces.get(i).finishIndex());
        }
        Piece last = put(bluePieces.get(0), 14);
        redMoves(3);
        SurprisePrompt push = surprises.promptFor(SurpriseOutcome.OPPONENT_BACK, model.pendingEffect().piece(),
                PlayerColor.RED);
        model.applySurprise(model.resolveSurprise(push, last));
        MoveResult home = model.applySurprise(outcome(SurpriseOutcome.JUMP_TO_FINISH));

        assertTrue(home.playerFinished());
        assertTrue(last.finished());
        assertEquals(TurnPhase.GAME_OVER, model.phase(), "FR-44: one player left");
        assertTrue(fired(GameEvent.Type.PLAYER_FINISHED, blue()), "the owner finished, not the active player");
        assertTrue(fired(GameEvent.Type.GAME_OVER, red()));
        assertEquals(List.of(blue(), red()), model.ranking());
        assertNull(model.pendingEffect());
    }

    @Test
    @DisplayName("FR-40: a push with nobody to push does nothing and the chain ends")
    void pushWithoutTargetDoesNothing() {
        surpriseAt(5);
        redMoves(3);
        SurprisePrompt push = surprises.promptFor(SurpriseOutcome.OPPONENT_BACK, model.pendingEffect().piece(),
                PlayerColor.RED);
        assertFalse(push.needsChoice());
        assertNull(model.applySurprise(push.resolve()), "no piece moves");
        assertEquals(5, redProgress(red().pieces().get(0)));
        assertSame(blue(), model.currentPlayer());
    }

    @Test
    @DisplayName("FR-39: the drawn surprise is the one carried out; another outcome is refused")
    void onlyTheDrawnSurpriseIsCarriedOut() {
        surpriseAt(5);
        redMoves(3);
        SurprisePrompt drawn = model.pendingSurprise();
        assertSame(drawn, model.pendingSurprise(), "drawn once until it is carried out");
        SurpriseOutcome other = drawn.outcome() == SurpriseOutcome.BACK_THREE
                ? SurpriseOutcome.BACK_TO_CASTLE : SurpriseOutcome.BACK_THREE;
        assertThrows(IllegalArgumentException.class, () -> model.applySurprise(outcome(other)));
        assertThrows(IllegalStateException.class, model::pendingQuestion, "a surprise waits, not a question");
        assertEquals(TurnPhase.RESOLVING, model.phase(), "a refused result changes nothing");
    }

    // ---------------------------------------------------------------- surprises: the turn

    @Test
    @DisplayName("Table 2, Q56: skip the next turn cancels the pending 6 and passes over the player once")
    void skipTurnCancelsTheSixAndPassesOverThePlayerOnce() {
        surpriseAt(10);
        script.add(roll(6));
        redMoves(4);
        assertTrue(model.hasBonusRoll());
        model.applySurprise(outcome(SurpriseOutcome.SKIP_TURN));

        assertSame(blue(), model.currentPlayer(), "Q56: the pending 6 is cancelled, the turn ends");
        model.roll(); // Blue has no piece out: no move, the turn passes (FR-23) ...
        assertTrue(fired(GameEvent.Type.TURN_SKIPPED, red()), "... over Red, who misses this turn");
        assertSame(blue(), model.currentPlayer());
        model.roll();
        assertSame(red(), model.currentPlayer(), "only one turn is missed");
    }

    @Test
    @DisplayName("Table 2, Q27: the extra roll uses the Regular and UltraBoost dice and does not count for the rule of 6")
    void extraRollIsOutsideTheRuleOfSix() {
        surpriseAt(5);
        redMoves(3);
        model.applySurprise(outcome(SurpriseOutcome.EXTRA_ROLL));
        assertSame(red(), model.currentPlayer());
        assertEquals(TurnPhase.AWAITING_ROLL, model.phase());
        assertTrue(model.hasBonusRoll());

        diceAsked.clear();
        script.add(new DiceRoll(6, DieType.ULTRA_BOOST, 2, 6, false, false));
        DiceRoll extra = model.roll();
        assertEquals(List.of(Difficulty.HARD), diceAsked, "Regular + UltraBoost, whatever the board");
        assertTrue(extra.surpriseRoll());
        model.move(red().pieces().get(0));
        assertSame(blue(), model.currentPlayer(), "Q27: a 6 on the extra roll gives no further roll");
    }

    @Test
    @DisplayName("Open rule question 4: a 6 onto the extra-roll surprise gives both rolls, the surprise roll first")
    void extraRollComesBeforeTheSix() {
        surpriseAt(10);
        script.add(roll(6));
        redMoves(4);
        model.applySurprise(outcome(SurpriseOutcome.EXTRA_ROLL));

        diceAsked.clear();
        script.add(new DiceRoll(1, DieType.ULTRA_BOOST, 3, 2, false, false));
        assertTrue(model.roll().surpriseRoll(), "first the surprise roll");
        model.move(red().pieces().get(0));
        assertSame(red(), model.currentPlayer(), "then the roll of the 6");
        assertFalse(model.roll().surpriseRoll());
        assertEquals(List.of(Difficulty.HARD, Difficulty.MEDIUM), diceAsked);
    }

    @Test
    @DisplayName("Without special squares the turn is unchanged: no RESOLVING, no event, the turn passes")
    void plainSquaresChangeNothing() {
        redMoves(3);
        assertNull(model.pendingEffect());
        assertFalse(events.stream().anyMatch(e -> e.type() == GameEvent.Type.EFFECT_PENDING));
        assertSame(blue(), model.currentPlayer());
        assertNotSame(TurnPhase.RESOLVING, model.phase());
    }
}
