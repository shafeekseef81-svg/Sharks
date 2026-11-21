package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Independent tests of questions, surprises and their chains (Task T2-12, US-2.3: FR-32, FR-34..FR-42, SRS
 * Table 2). Written by Rakan Ziyan, who implemented none of these rules (T2-08/T2-09/T2-11 Mohamed, T2-10 Shafeek),
 * from the SRS text and not from the authors' tests (SDP 5.3.4). The priorities are the weak spots of the T2-14
 * RTM snapshot (#93): FR-32, FR-38, FR-41 and FR-42.
 *
 * <p>The SRS wording the tests follow:
 * <ul>
 *   <li>FR-32: backward movement caused by questions or surprises stops at the player's starting square without
 *       crossing it.</li>
 *   <li>FR-34: landing on a question square shows a random question of matching difficulty with four options;
 *       only the active player answers.</li>
 *   <li>FR-35: a correct answer moves the piece forward 1 (Easy), 2 (Medium) or 3 (Hard) steps; a wrong answer
 *       moves it back equally.</li>
 *   <li>FR-36: after an answer the game shows whether it was correct and the correct answer.</li>
 *   <li>FR-37: a question does not repeat until all questions of its level have been asked.</li>
 *   <li>FR-38: a capture on an unprotected question square happens before the question and is not reversed by
 *       the answer's effect.</li>
 *   <li>FR-39 and Table 2: a surprise square triggers exactly one outcome, with the chances 10/10/10/30/30/10.</li>
 *   <li>FR-40: "move an opponent" moves an opponent piece on a shared track back half a Regular die, rounded up,
 *       chosen by the active player, or is cancelled if there is none.</li>
 *   <li>FR-41: question and surprise movements obey the movement and blocking rules, except the direct-finish
 *       surprise.</li>
 *   <li>FR-42: such a movement activates a new special square it reaches; zero steps do not reactivate one.</li>
 * </ul>
 *
 * <p>Every scenario is played through the {@link GameModel} facade with the real board, rules, factories and
 * effect queue; only the dice are scripted. The special squares are placed at random (FR-12), so each test looks
 * for the first seeded board on which its scenario exists and computes every expected cell from that board's
 * paths. The expected numbers (1/2/3 steps, ⌈R/2⌉, the Table 2 shares) are written out here by hand, not read from
 * the code under test. A surprise outcome is chosen by building its {@link SurprisePrompt} from the model's public
 * types, because the draw itself is random (its distribution is tested on its own).
 */
class EffectChainIndependentTest {

    /** Dice whose next rolls the test chooses; it also records the level each roll was asked for. */
    private static final class ScriptDice implements Dice {
        final Deque<DiceRoll> next = new ArrayDeque<>();
        final List<Difficulty> asked = new ArrayList<>();

        @Override
        public DiceRoll roll(Difficulty level) {
            asked.add(level);
            return next.remove();
        }
    }

    /** FR-35, written out: steps of a question by its level. */
    private static final Map<Difficulty, Integer> QUESTION_STEPS =
            Map.of(Difficulty.EASY, 1, Difficulty.MEDIUM, 2, Difficulty.HARD, 3);

    /** FR-40, written out: half a Regular die, rounded up, for the values 1..6 (index 0 unused). */
    private static final int[] HALF_ROUNDED_UP = {0, 1, 1, 2, 2, 3, 3};

    /** Seeded boards tried before a scenario is declared impossible. */
    private static final int SEEDS = 3000;

    private final ScriptDice dice = new ScriptDice();
    private final List<GameEvent> events = new ArrayList<>();
    private GameModel model;

    // ------------------------------------------------------------------ set-up helpers

    private GameModel newGame(Difficulty level, long seed, PlayerColor... colors) {
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        for (PlayerColor c : colors) {
            setup.add(new GameSettings.PlayerSetup(c.label(), c));
        }
        return new GameModel(new GameSettings(level, setup), new Random(seed), dice, () -> 0L);
    }

    /**
     * Starts the first seeded game in which the scenario exists. The condition may remember what it found in
     * fields of the test; it is called with {@link #model} already set.
     */
    private void startWhere(Difficulty level, Predicate<GameModel> condition, PlayerColor... colors) {
        for (long seed = 0; seed < SEEDS; seed++) {
            model = newGame(level, seed, colors);
            if (condition.test(model)) {
                model.addListener(events::add);
                assertSame(player(colors[0]), model.currentPlayer());
                return;
            }
        }
        fail("No " + level.label() + " board among " + SEEDS + " seeds has this scenario");
    }

    private Board board() {
        return model.board();
    }

    private Player player(PlayerColor color) {
        return model.playerOf(color);
    }

    private Piece piece(PlayerColor color, int index) {
        return player(color).pieces().get(index);
    }

    private Piece red() {
        return piece(PlayerColor.RED, 0);
    }

    /** The square Red reaches at this progress, or null off the board's squares. */
    private Square redSquare(int progress) {
        return board().squareAt(board().cellAt(PlayerColor.RED, progress));
    }

    /** True for a progress of Red strictly between its start square and its home path. */
    private boolean redShared(int progress) {
        return progress > 0 && progress < board().homeStartIndex();
    }

    private boolean special(Square square) {
        return square != null && (square.type() == SquareType.QUESTION || square.type() == SquareType.SURPRISE);
    }

    /** True if Red's progress is on the shared track and not a question or surprise square. */
    private boolean plain(int progress) {
        return redShared(progress) && !special(redSquare(progress));
    }

    /** Red's progress values of the squares of a type (and a question level, or null), on the shared track. */
    private List<Integer> redProgressOf(SquareType type, Difficulty questionLevel) {
        List<Integer> found = new ArrayList<>();
        for (int p = 1; p < board().homeStartIndex(); p++) {
            Square s = redSquare(p);
            if (s != null && s.type() == type && (questionLevel == null || s.questionLevel() == questionLevel)) {
                found.add(p);
            }
        }
        return found;
    }

    /** The progress of {@code color} on the cell Red reaches at {@code redProgress}, or -1 if it does not pass it. */
    private int progressOn(PlayerColor color, int redProgress) {
        return board().path(color).indexOf(board().cellAt(PlayerColor.RED, redProgress));
    }

    /** True if {@code color} reaches Red's cell on its own shared track, after its start square. */
    private boolean sharedFor(PlayerColor color, int redProgress) {
        int p = progressOn(color, redProgress);
        return p > 0 && p < board().homeStartIndex();
    }

    /** Two pieces of {@code owner} on the cell Red reaches at {@code redProgress}: a Double Block (FR-28). */
    private void blockAt(PlayerColor owner, int redProgress) {
        int p = progressOn(owner, redProgress);
        assertTrue(p > 0 && p < board().homeStartIndex(), owner + " cannot block Red's progress " + redProgress);
        piece(owner, 0).setProgress(p);
        piece(owner, 1).setProgress(p);
    }

    /** Puts Red's first piece {@code roll} squares before {@code target}, rolls and moves it there. */
    private MoveResult landRed(int target, int roll) {
        red().setProgress(target - roll);
        dice.next.add(DiceRoll.regularOnly(roll));
        model.roll();
        MoveResult moved = model.move(red());
        assertEquals(target, red().progress(), "the dice move itself");
        return moved;
    }

    /** Answers the waiting question right or wrong through the facade. */
    private QuestionResult answer(boolean right) {
        Question q = model.pendingQuestion();
        int chosen = right ? q.correctIndex() : (q.correctIndex() + 1) % 4;
        QuestionResult result = model.judgeAnswer(chosen);
        model.applyAnswer(result);
        return result;
    }

    /** Carries out a chosen surprise outcome that needs no opponent piece, for the piece that is waiting. */
    private MoveResult surprise(SurpriseOutcome outcome) {
        EffectQueue.Activation waiting = model.pendingEffect();
        SurprisePrompt prompt = new SurprisePrompt(outcome, waiting.piece(), model.currentPlayer().color(), 0,
                List.of());
        return model.applySurprise(prompt.resolve());
    }

    /** Pushes an opponent piece back with the given Regular die value (FR-40). */
    private MoveResult push(Piece target, int regular) {
        EffectQueue.Activation waiting = model.pendingEffect();
        SurprisePrompt prompt = new SurprisePrompt(SurpriseOutcome.OPPONENT_BACK, waiting.piece(),
                model.currentPlayer().color(), regular, List.of(target));
        return model.applySurprise(model.resolveSurprise(prompt, target));
    }

    private List<GameEvent.Type> eventTypes() {
        List<GameEvent.Type> types = new ArrayList<>();
        for (GameEvent e : events) {
            if (e.type() != GameEvent.Type.STATE_CHANGED) {
                types.add(e.type());
            }
        }
        return types;
    }

    // Scenario values found on the board by the conditions.
    private int pq;
    private int ps;
    private int pg;
    private Difficulty level;

    // ------------------------------------------------------------------ FR-34, FR-36, FR-37: the question

    @Test
    @DisplayName("T2-12 FR-34: a question square asks a question of its own level with four answers, and nothing "
            + "else can happen until it is answered")
    void questionOfTheSquareLevel() {
        for (Difficulty l : Difficulty.values()) {
            startWhere(Difficulty.HARD, m -> {
                List<Integer> squares = redProgressOf(SquareType.QUESTION, l);
                for (int p : squares) {
                    if (p >= 3) {
                        pq = p;
                        return true;
                    }
                }
                return false;
            }, PlayerColor.RED, PlayerColor.GREEN);
            landRed(pq, 3);

            assertEquals(TurnPhase.RESOLVING, model.phase(), l.label());
            EffectQueue.Activation waiting = model.pendingEffect();
            assertSame(red(), waiting.piece(), l.label());
            assertEquals(board().cellAt(PlayerColor.RED, pq), waiting.square().cell(), l.label());
            Question q = model.pendingQuestion();
            assertEquals(l, q.level(), "a " + l.label() + " square must ask a " + l.label() + " question");
            assertEquals(4, q.answers().size(), l.label());
            assertSame(q, model.pendingQuestion(), "asking again shows the same question");

            assertTrue(model.legalMoves().isEmpty(), l.label());
            assertThrows(IllegalStateException.class, model::roll, l.label() + ": no roll before the answer");
            assertThrows(IllegalStateException.class, () -> model.move(red()), l.label());
            assertThrows(IllegalStateException.class, model::pendingSurprise, l.label() + ": it is no surprise");
        }
    }

    @Test
    @DisplayName("T2-12 FR-36: after an answer the result says whether it was right and gives the correct answer")
    void answerFeedback() {
        startWhere(Difficulty.MEDIUM, m -> {
            for (int p : redProgressOf(SquareType.QUESTION, null)) {
                if (p >= 3) {
                    pq = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        landRed(pq, 3);
        Question q = model.pendingQuestion();
        int wrong = (q.correctIndex() + 2) % 4;

        QuestionResult w = model.judgeAnswer(wrong);
        assertFalse(w.correct());
        assertEquals(q.answer(q.correctIndex()), w.correctAnswer(), "a wrong answer still shows the right one");
        assertEquals(q.answer(wrong), w.chosenAnswer());
        QuestionResult r = model.judgeAnswer(q.correctIndex());
        assertTrue(r.correct());
        assertEquals(q.answer(q.correctIndex()), r.correctAnswer());
        assertEquals(TurnPhase.RESOLVING, model.phase(), "judging alone moves nothing");
        assertEquals(pq, red().progress());
    }

    @Test
    @DisplayName("T2-12 FR-37: no question of a level repeats before all questions of that level were asked")
    void noRepeatBeforeTheLevelIsUsedUp() {
        QuestionBank bank = QuestionBank.ofCourseQuestions();
        Map<Difficulty, QuestionFactory> factories = QuestionFactory.forBank(bank, new Random(37));
        for (Difficulty l : Difficulty.values()) {
            int n = bank.byLevel(l).size();
            assertTrue(n > 1, l.label() + " needs more than one question for FR-37 to mean anything");
            for (int round = 1; round <= 3; round++) {
                Set<String> ids = new HashSet<>();
                for (int i = 0; i < n; i++) {
                    Question q = factories.get(l).ask();
                    assertEquals(l, q.level());
                    assertTrue(ids.add(q.id()), l.label() + " round " + round + ": " + q.id() + " repeated after "
                            + i + " of " + n + " questions");
                }
                assertEquals(n, ids.size(), l.label() + " round " + round + " asks every question once");
            }
        }
    }

    // ------------------------------------------------------------------ FR-35: steps of an answer

    @Test
    @DisplayName("T2-12 FR-35: a right answer moves forward 1/2/3 steps by the question's level, a wrong one back as "
            + "far")
    void answerStepsByLevel() {
        for (Difficulty l : Difficulty.values()) {
            for (boolean right : new boolean[] {true, false}) {
                int s = QUESTION_STEPS.get(l);
                startWhere(Difficulty.HARD, m -> {
                    for (int p : redProgressOf(SquareType.QUESTION, l)) {
                        if (p - 3 >= 0 && redShared(p - s) && redShared(p + s)) {
                            pq = p;
                            return true;
                        }
                    }
                    return false;
                }, PlayerColor.RED, PlayerColor.GREEN);
                landRed(pq, 3);
                QuestionResult result = answer(right);

                assertEquals(right, result.correct());
                assertEquals(right ? s : -s, result.steps(), l.label());
                assertEquals(right ? pq + s : pq - s, red().progress(),
                        l.label() + (right ? " right" : " wrong") + " answer from progress " + pq);
            }
        }
    }

    // ------------------------------------------------------------------ FR-38: capture before the question

    @Test
    @DisplayName("T2-12 FR-38: a capture on a question square comes before the question, and a wrong answer does "
            + "not undo it")
    void captureComesBeforeTheQuestion() {
        startWhere(Difficulty.MEDIUM, m -> {
            for (int p : redProgressOf(SquareType.QUESTION, null)) {
                int s = QUESTION_STEPS.get(redSquare(p).questionLevel());
                if (p >= 3 && !redSquare(p).isProtected() && sharedFor(PlayerColor.GREEN, p) && redShared(p - s)) {
                    pq = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        Piece victim = piece(PlayerColor.GREEN, 0);
        victim.setProgress(progressOn(PlayerColor.GREEN, pq));

        MoveResult moved = landRed(pq, 3);

        assertTrue(victim.inCastle(), "the capture has already happened when the question is asked");
        assertEquals(1, moved.captures().size());
        assertSame(victim, moved.captures().get(0).piece());
        assertEquals(List.of(GameEvent.Type.ROLLED, GameEvent.Type.MOVED, GameEvent.Type.EFFECT_PENDING),
                eventTypes(), "order: roll, move with its capture, then the question");
        assertEquals(TurnPhase.RESOLVING, model.phase());

        answer(false);
        assertTrue(red().progress() < pq, "the wrong answer moved Red back");
        assertTrue(victim.inCastle(), "the answer's effect does not bring the captured piece back");
    }

    // ------------------------------------------------------------------ FR-39, Table 2: the surprise

    @Test
    @DisplayName("T2-12 FR-39: a surprise square draws exactly one outcome, the same however often it is shown")
    void exactlyOneSurpriseOutcome() {
        startWhere(Difficulty.MEDIUM, m -> {
            for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                if (p >= 3) {
                    ps = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        landRed(ps, 3);

        assertEquals(TurnPhase.RESOLVING, model.phase());
        assertEquals(SquareType.SURPRISE, model.pendingEffect().type());
        assertSame(red(), model.pendingEffect().piece());
        SurprisePrompt first = model.pendingSurprise();
        assertSame(first, model.pendingSurprise(), "one outcome per landing, not a new draw per look");
        assertSame(red(), first.subject());
        assertEquals(PlayerColor.RED, first.active());
        assertThrows(IllegalStateException.class, model::pendingQuestion, "it is no question");
    }

    @Test
    @DisplayName("T2-12 FR-39, Table 2: over 120,000 draws the outcomes come up 10/10/10/30/30/10 % (within 1 point)")
    void surpriseChancesFollowTable2() {
        Map<SurpriseOutcome, Integer> table2 = new EnumMap<>(SurpriseOutcome.class);
        table2.put(SurpriseOutcome.JUMP_TO_FINISH, 10);
        table2.put(SurpriseOutcome.OPPONENT_BACK, 10);
        table2.put(SurpriseOutcome.BACK_TO_CASTLE, 10);
        table2.put(SurpriseOutcome.BACK_THREE, 30);
        table2.put(SurpriseOutcome.SKIP_TURN, 30);
        table2.put(SurpriseOutcome.EXTRA_ROLL, 10);
        assertEquals(table2.keySet(), Set.of(SurpriseOutcome.values()), "Table 2 has exactly these six outcomes");

        int draws = 120_000;
        Map<SurpriseOutcome, Integer> seen = new EnumMap<>(SurpriseOutcome.class);
        Random random = new Random(2026);
        for (int i = 0; i < draws; i++) {
            seen.merge(SurpriseOutcome.draw(random), 1, Integer::sum);
        }
        for (Map.Entry<SurpriseOutcome, Integer> e : table2.entrySet()) {
            double share = 100.0 * seen.getOrDefault(e.getKey(), 0) / draws;
            assertEquals(e.getValue(), share, 1.0, e.getKey() + " came up " + share + "%");
        }
    }

    @Test
    @DisplayName("T2-12 Table 2: three steps back moves the piece back three; back to the castle sends it home")
    void backThreeAndBackToCastle() {
        for (SurpriseOutcome outcome : new SurpriseOutcome[] {SurpriseOutcome.BACK_THREE,
                SurpriseOutcome.BACK_TO_CASTLE}) {
            startWhere(Difficulty.MEDIUM, m -> {
                for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                    if (p >= 4 && plain(p - 3)) {
                        ps = p;
                        return true;
                    }
                }
                return false;
            }, PlayerColor.RED, PlayerColor.GREEN);
            landRed(ps, 2);
            surprise(outcome);
            if (outcome == SurpriseOutcome.BACK_THREE) {
                assertEquals(ps - 3, red().progress());
            } else {
                assertTrue(red().inCastle());
            }
            assertNotEquals(TurnPhase.RESOLVING, model.phase(), outcome + " ends the chain");
        }
    }

    // ------------------------------------------------------------------ FR-40: push an opponent back

    @Test
    @DisplayName("T2-12 FR-40: the chosen opponent piece goes back half the Regular die, rounded up, for 1 to 6")
    void pushBackHalfTheDieRoundedUp() {
        for (int regular = 1; regular <= 6; regular++) {
            startWhere(Difficulty.MEDIUM, m -> {
                for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                    if (p < 3) {
                        continue;
                    }
                    // A Green piece far from Red's way, whose push-back cannot reach Red's piece.
                    for (int g = 10; g < board().homeStartIndex(); g++) {
                        Cell cell = board().cellAt(PlayerColor.GREEN, g);
                        boolean clear = true;
                        for (int back = 0; back <= 3 && clear; back++) {
                            Cell c = board().cellAt(PlayerColor.GREEN, g - back);
                            for (int r = p - 3; r <= p; r++) {
                                clear &= !c.equals(board().cellAt(PlayerColor.RED, r));
                            }
                        }
                        Square gs = board().squareAt(cell);
                        if (clear && !special(gs)) {
                            ps = p;
                            pg = g;
                            return true;
                        }
                    }
                }
                return false;
            }, PlayerColor.RED, PlayerColor.GREEN);
            Piece target = piece(PlayerColor.GREEN, 0);
            target.setProgress(pg);
            landRed(ps, 3);

            push(target, regular);
            assertEquals(pg - HALF_ROUNDED_UP[regular], target.progress(), "Regular " + regular);
            assertEquals(ps, red().progress(), "the active player's piece stays");
        }
    }

    @Test
    @DisplayName("T2-12 FR-40: only an opponent piece on a shared track can be pushed, never the active player's own")
    void onlyOpponentsOnTheSharedTrack() {
        startWhere(Difficulty.MEDIUM, m -> {
            for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                if (p >= 3) {
                    ps = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        Piece own = piece(PlayerColor.RED, 1);
        own.setProgress(5 == ps ? 6 : 5);
        Piece home = piece(PlayerColor.GREEN, 0);
        home.setProgress(board().homeStartIndex() + 1);
        landRed(ps, 3);
        EffectQueue.Activation waiting = model.pendingEffect();

        assertThrows(IllegalArgumentException.class, () -> new SurprisePrompt(SurpriseOutcome.OPPONENT_BACK,
                waiting.piece(), PlayerColor.RED, 4, List.of(own)), "the active player's own piece");
        SurprisePrompt toHomePath = new SurprisePrompt(SurpriseOutcome.OPPONENT_BACK, waiting.piece(),
                PlayerColor.RED, 4, List.of(home));
        assertThrows(IllegalArgumentException.class, () -> model.resolveSurprise(toHomePath, home),
                "a piece on its home path is not on a shared track");
        assertEquals(board().homeStartIndex() + 1, home.progress());
        assertEquals(TurnPhase.RESOLVING, model.phase(), "a refused pick resolves nothing");
    }

    @Test
    @DisplayName("T2-12 FR-40: with no opponent piece on a shared track the push is cancelled and the chain ends")
    void pushWithNobodyToPushIsCancelled() {
        startWhere(Difficulty.MEDIUM, m -> {
            for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                if (p >= 3) {
                    ps = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        landRed(ps, 3);
        EffectQueue.Activation waiting = model.pendingEffect();
        SurprisePrompt prompt = new SurprisePrompt(SurpriseOutcome.OPPONENT_BACK, waiting.piece(),
                PlayerColor.RED, 5, List.of());

        SurpriseResult result = prompt.resolve();
        assertTrue(result.cancelled());
        assertNull(model.applySurprise(result), "nothing moves");
        assertEquals(ps, red().progress());
        assertNotEquals(TurnPhase.RESOLVING, model.phase());
        assertNull(model.pendingEffect());
    }

    // ------------------------------------------------------------------ FR-32: never behind the start square

    @Test
    @DisplayName("T2-12 FR-32: a pushed opponent stops on its own start square and does not cross it")
    void pushStopsOnTheStartSquare() {
        for (int[] c : new int[][] {{2, 6, 0}, {1, 4, 0}, {0, 3, 0}}) { // {Green progress, Regular, expected}
            startWhere(Difficulty.MEDIUM, m -> {
                for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                    boolean clear = p >= 3;
                    for (int g = 0; g <= 2 && clear; g++) {
                        for (int r = p - 3; r <= p; r++) {
                            clear &= !board().cellAt(PlayerColor.GREEN, g).equals(board().cellAt(PlayerColor.RED, r));
                        }
                    }
                    if (clear) {
                        ps = p;
                        return true;
                    }
                }
                return false;
            }, PlayerColor.RED, PlayerColor.GREEN);
            Piece target = piece(PlayerColor.GREEN, 0);
            target.setProgress(c[0]);
            landRed(ps, 3);

            push(target, c[1]);
            assertEquals(c[2], target.progress(), "Green from progress " + c[0] + " pushed back with " + c[1]);
            assertTrue(target.onBoard(), "a push never sends a piece into its castle");
        }
    }

    @Test
    @DisplayName("T2-12 FR-32: a wrong answer next to the start square stops the piece on its start square")
    void wrongAnswerStopsOnTheStartSquare() {
        startWhere(Difficulty.HARD, m -> {
            for (int p : redProgressOf(SquareType.QUESTION, null)) {
                if (p <= 2 && QUESTION_STEPS.get(redSquare(p).questionLevel()) > p) {
                    pq = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        landRed(pq, pq);

        answer(false);
        assertEquals(0, red().progress(), "back " + QUESTION_STEPS.get(redSquare(pq).questionLevel())
                + " from progress " + pq + " stops on the start square");
        assertTrue(red().onBoard());
    }

    // ------------------------------------------------------------------ FR-41: movement and blocking rules

    @Test
    @DisplayName("T2-12 FR-41: a right answer stops before an opponent Double Block in its way")
    void rightAnswerStopsBeforeABlock() {
        startWhere(Difficulty.HARD, m -> {
            for (int p : redProgressOf(SquareType.QUESTION, Difficulty.HARD)) {
                if (p >= 3 && redShared(p + 3) && sharedFor(PlayerColor.GREEN, p + 2)) {
                    pq = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        blockAt(PlayerColor.GREEN, pq + 2);
        landRed(pq, 3);

        answer(true);
        assertEquals(pq + 1, red().progress(), "3 steps forward with a block on the second: stop on the first");
    }

    @Test
    @DisplayName("T2-12 FR-41: a wrong answer moving back stops before an opponent Double Block behind the piece")
    void wrongAnswerStopsBeforeABlockBehind() {
        startWhere(Difficulty.HARD, m -> {
            for (int p : redProgressOf(SquareType.QUESTION, Difficulty.HARD)) {
                if (p >= 3 && sharedFor(PlayerColor.GREEN, p - 2)) {
                    pq = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        blockAt(PlayerColor.GREEN, pq - 2);
        landRed(pq, 1); // arrives from the cell between the block and the square

        answer(false);
        assertEquals(pq - 1, red().progress(), "3 steps back with a block on the second: stop on the first");
    }

    @Test
    @DisplayName("T2-12 FR-41: an answer that lands on a lone opponent captures it")
    void answerMoveCaptures() {
        startWhere(Difficulty.HARD, m -> {
            for (int p : redProgressOf(SquareType.QUESTION, null)) {
                int s = QUESTION_STEPS.get(redSquare(p).questionLevel());
                if (p >= 3 && plain(p + s) && !redSquare(p + s).isProtected() && sharedFor(PlayerColor.GREEN, p + s)) {
                    pq = p;
                    level = redSquare(p).questionLevel();
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        int target = pq + QUESTION_STEPS.get(level);
        Piece victim = piece(PlayerColor.GREEN, 0);
        victim.setProgress(progressOn(PlayerColor.GREEN, target));
        landRed(pq, 3);

        answer(true);
        assertEquals(target, red().progress());
        assertTrue(victim.inCastle(), "landing after a question move captures like a dice move (FR-26)");
    }

    @Test
    @DisplayName("T2-12 FR-41: the jump to the finishing square ignores Double Blocks and exact steps")
    void jumpToFinishIgnoresBlocks() {
        startWhere(Difficulty.MEDIUM, m -> {
            for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                if (p >= 3 && sharedFor(PlayerColor.GREEN, p + 1)) {
                    ps = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        blockAt(PlayerColor.GREEN, ps + 1);
        landRed(ps, 3);

        surprise(SurpriseOutcome.JUMP_TO_FINISH);
        assertTrue(red().finished(), "a block right ahead and a long way to go do not stop the jump");
        assertEquals(board().finishIndex(), red().progress());
    }

    // ------------------------------------------------------------------ FR-42: chains

    @Test
    @DisplayName("T2-12 FR-42: an answer that moves the piece onto another special square activates it next")
    void answerChainsIntoTheNextSquare() {
        startWhere(Difficulty.HARD, m -> {
            for (int p : redProgressOf(SquareType.QUESTION, null)) {
                int s = QUESTION_STEPS.get(redSquare(p).questionLevel());
                if (p >= 3 && redShared(p + s) && special(redSquare(p + s))) {
                    pq = p;
                    level = redSquare(p).questionLevel();
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        int next = pq + QUESTION_STEPS.get(level);
        landRed(pq, 3);

        answer(true);
        assertEquals(next, red().progress());
        assertEquals(TurnPhase.RESOLVING, model.phase(), "the chain goes on");
        EffectQueue.Activation waiting = model.pendingEffect();
        assertSame(red(), waiting.piece());
        assertEquals(board().cellAt(PlayerColor.RED, next), waiting.square().cell(), "the square just reached");
        assertSame(PlayerColor.RED, model.currentPlayer().color(), "the same turn");
        long effectsPending = events.stream().filter(e -> e.type() == GameEvent.Type.EFFECT_PENDING).count();
        assertEquals(2, effectsPending, "one EFFECT_PENDING per square, in the order reached");
    }

    @Test
    @DisplayName("T2-12 FR-42: an answer that moves zero steps does not activate the same square again")
    void zeroStepsDoNotReactivate() {
        startWhere(Difficulty.MEDIUM, m -> {
            for (int p : redProgressOf(SquareType.QUESTION, null)) {
                if (p >= 3 && sharedFor(PlayerColor.GREEN, p + 1)) {
                    pq = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        blockAt(PlayerColor.GREEN, pq + 1);
        landRed(pq, 3);

        answer(true);
        assertEquals(pq, red().progress(), "a block on the very next cell: zero steps");
        assertNotEquals(TurnPhase.RESOLVING, model.phase(), "the square is not asked again");
        assertNull(model.pendingEffect());
        assertSame(PlayerColor.GREEN, model.currentPlayer().color(), "no 6, so the turn passed");
    }

    @Test
    @DisplayName("T2-12 FR-42 (Q81): a push of zero steps does not activate the question square the pushed piece "
            + "already stands on")
    void zeroStepPushDoesNotActivateTheSquareUnderThePiece() {
        // Green stands on a question square from an earlier turn; a Red Double Block is right behind it on Green's
        // path. A new chain (Red's surprise) pushes Green back: it cannot move, so its square must not wake up.
        startWhere(Difficulty.HARD, m -> {
            int home = board().homeStartIndex();
            for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                if (p < 3) {
                    continue;
                }
                for (int g = 2; g < home; g++) {
                    Cell question = board().cellAt(PlayerColor.GREEN, g);
                    Square sq = board().squareAt(question);
                    if (sq == null || sq.type() != SquareType.QUESTION) {
                        continue;
                    }
                    Cell behind = board().cellAt(PlayerColor.GREEN, g - 1);
                    int rb = board().path(PlayerColor.RED).indexOf(behind);
                    boolean clear = rb > 0 && rb < home;
                    for (int r = p - 3; r <= p && clear; r++) {
                        Cell c = board().cellAt(PlayerColor.RED, r);
                        clear = !c.equals(question) && !c.equals(behind);
                    }
                    if (clear) {
                        ps = p;
                        pg = g;
                        pq = rb;
                        return true;
                    }
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        Piece pushed = piece(PlayerColor.GREEN, 0);
        pushed.setProgress(pg);
        piece(PlayerColor.RED, 1).setProgress(pq); // Red's Double Block right behind Green (FR-29)
        piece(PlayerColor.RED, 2).setProgress(pq);
        landRed(ps, 3);

        MoveResult result = push(pushed, 2); // one step back, into the block: zero steps
        assertEquals(pg, pushed.progress(), "the block behind stops the push before its first step");
        assertTrue(result == null || result.fromProgress() == result.toProgress(), "nothing moved");
        assertNull(model.pendingEffect(), "the question square under the pushed piece is not activated");
        assertNotEquals(TurnPhase.RESOLVING, model.phase());
        assertSame(PlayerColor.GREEN, model.currentPlayer().color(), "the chain is over and the turn passed");
    }

    @Test
    @DisplayName("T2-12 FR-42, FR-21: after the chain a 6 still gives its extra roll; without a 6 the turn passes")
    void turnGoesOnAfterTheChain() {
        for (int roll : new int[] {6, 4}) {
            startWhere(Difficulty.MEDIUM, m -> {
                for (int p : redProgressOf(SquareType.QUESTION, null)) {
                    int s = QUESTION_STEPS.get(redSquare(p).questionLevel());
                    if (p >= 6 && plain(p + s)) {
                        pq = p;
                        return true;
                    }
                }
                return false;
            }, PlayerColor.RED, PlayerColor.GREEN);
            landRed(pq, roll);
            assertEquals(TurnPhase.RESOLVING, model.phase());

            answer(true);
            if (roll == 6) {
                assertEquals(TurnPhase.AWAITING_ROLL, model.phase(), "the 6 still rolls again after the question");
                assertSame(PlayerColor.RED, model.currentPlayer().color());
            } else {
                assertSame(PlayerColor.GREEN, model.currentPlayer().color(), "roll " + roll + ": next player");
            }
        }
    }

    // ------------------------------------------------------------------ Table 2: the two turn surprises

    @Test
    @DisplayName("T2-12 Table 2 (Q56): skip the next turn cancels a pending 6 and passes over the player once")
    void skipTheNextTurn() {
        startWhere(Difficulty.MEDIUM, m -> {
            for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                if (p >= 6) {
                    ps = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW);
        landRed(ps, 6); // a 6: Red would roll again

        surprise(SurpriseOutcome.SKIP_TURN);
        assertSame(PlayerColor.GREEN, model.currentPlayer().color(), "the pending 6 is cancelled");

        dice.next.add(DiceRoll.regularOnly(3)); // Green and Yellow have no piece out: no move, the turn passes
        model.roll();
        assertSame(PlayerColor.YELLOW, model.currentPlayer().color());
        events.clear();
        dice.next.add(DiceRoll.regularOnly(3));
        model.roll();
        assertSame(PlayerColor.GREEN, model.currentPlayer().color(), "Red misses this turn");
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.Type.TURN_SKIPPED
                && e.player() == player(PlayerColor.RED)), "the skipped turn is announced");

        dice.next.add(DiceRoll.regularOnly(3));
        model.roll();
        dice.next.add(DiceRoll.regularOnly(3));
        model.roll();
        assertSame(PlayerColor.RED, model.currentPlayer().color(), "only once: Red plays the round after");
    }

    @Test
    @DisplayName("T2-12 Table 2 (Q27): the extra roll uses the Regular + UltraBoost dice and a 6 on it gives no "
            + "further roll")
    void extraRollOutsideTheRuleOfSix() {
        startWhere(Difficulty.MEDIUM, m -> {
            for (int p : redProgressOf(SquareType.SURPRISE, null)) {
                if (p >= 3 && plain(p + 6)) {
                    ps = p;
                    return true;
                }
            }
            return false;
        }, PlayerColor.RED, PlayerColor.GREEN);
        landRed(ps, 3); // not a 6

        surprise(SurpriseOutcome.EXTRA_ROLL);
        assertEquals(TurnPhase.AWAITING_ROLL, model.phase());
        assertSame(PlayerColor.RED, model.currentPlayer().color(), "Red rolls again");

        dice.asked.clear();
        dice.next.add(DiceRoll.regularOnly(6));
        model.roll();
        assertEquals(List.of(Difficulty.HARD), dice.asked,
                "the extra roll asks for the Hard dice (Regular + UltraBoost) on a Medium board");
        model.move(red());
        assertEquals(ps + 6, red().progress());
        assertSame(PlayerColor.GREEN, model.currentPlayer().color(), "a 6 on the extra roll gives no further roll");
    }
}
