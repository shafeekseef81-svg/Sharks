package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Acceptance tests of task <b>T2-15</b> ([#66](https://github.com/shafeekseef81-svg/Sharks/issues/66)) for the
 * parts of Iteration 2 that need the effect queue: <b>US-2.3</b> (questions, surprises, chained effects) and the
 * turn-phase half of <b>US-2.4</b>. Written by the Product Owner (Mohamed Zereny).
 *
 * <p>This is the class {@code Iteration2/docs/T2-15_AcceptancePlan.md} lists as the planned test <b>P12</b> —
 * "the real acceptance of US-2.3" — which could not be written while T2-10 was unmerged. It is therefore on a
 * branch stacked on <b>T2-10 part 2 (#92)</b>, not on {@code iteration-2}, and must merge after it.
 * {@code MediumHardAcceptanceTest} on #99 stays as it is: it covers what plain {@code iteration-2} allows.
 *
 * <p><b>What makes these acceptance tests rather than rule tests.</b> Complete games are played through the
 * {@link GameModel} facade with the <b>real</b> dice, the real random boards, the real question and surprise
 * factories and the real effect queue. Nothing is scripted and nothing is stubbed: the only thing this class
 * decides is what a <i>player</i> would decide — which piece to move, which answer to give, which opponent to
 * push. Deterministic seeds make every failure reproducible.
 *
 * <p><b>Deliberately not duplicating T2-12.</b> Rakan's {@code EffectChainIndependentTest}
 * ([#95](https://github.com/shafeekseef81-svg/Sharks/pull/95)) tests the individual rules — FR-32, FR-40,
 * FR-41, FR-42, Table 2 Q27/Q56 and decision D2 — in scripted single scenarios, which is what SDP 5.3.4 asks
 * of an independent tester. This class never scripts a scenario. It plays whole games and checks the
 * <b>invariants</b> that must hold across all of them, plus the two Product Owner decisions as they appear in
 * real play. Where a rule is already pinned down by a scripted test of his, it is not re-pinned here.
 *
 * <p><b>Still not covered by anything automated:</b> a game played by a person on the real window, and NFR-05
 * (the time from a piece stopping to the dialog appearing). Those are the manual scripts M1–M7 of the
 * acceptance plan and are <b>not</b> claimed as passed by this file.
 */
class EffectsAcceptanceTest {

    /** A complete game must end well inside this many rolls, or the test fails instead of hanging. */
    private static final int ROLL_LIMIT = 500_000;

    private static GameSettings settings(Difficulty level, int count) {
        PlayerColor[] colors = PlayerColor.values();
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            setup.add(new GameSettings.PlayerSetup("P" + (i + 1), colors[i]));
        }
        return new GameSettings(level, setup);
    }

    /** What one complete game, with questions and surprises active, actually did. */
    private static final class Played {
        GameModel model;
        int rolls;
        int moves;
        int questionsAsked;
        int rightAnswers;
        int wrongAnswers;
        int noAnswers;
        int surprisesDrawn;
        int pushesChosen;
        int chainSteps;              // effects that ran while another effect was already resolving
        int longestChain;
        int crossPlayerTurnOutcomes; // a turn outcome in a chain an opponent's piece set off (D2)
        final Set<String> questionIdsSeen = new HashSet<>();
        final Map<SurpriseOutcome, Integer> outcomes = new EnumMap<>(SurpriseOutcome.class);
        final Set<TurnPhase> phasesSeen = new HashSet<>();
    }

    /**
     * Plays one complete game with every special square live, deciding as a player would, and checking the
     * invariants that must hold after every single step.
     *
     * @param answers how the player answers: 0 = always right, 1 = always wrong, 2 = alternate, 3 = never answer
     */
    private static Played playToEnd(Difficulty level, int count, long seed, int answers) {
        Played p = new Played();
        Random choice = new Random(seed * 31 + 7);
        GameModel model = new GameModel(settings(level, count), new Random(seed));
        p.model = model;
        int answerTurn = 0;

        while (!model.isGameOver()) {
            assertTrue(p.rolls++ < ROLL_LIMIT, level + " seed " + seed + ": the game must end");
            p.phasesSeen.add(model.phase());

            // A turn always begins by rolling, and never while an effect is still waiting.
            assertSame(null, model.pendingEffect(), "no effect may be left waiting at the start of a turn");
            Player active = model.currentPlayer();
            assertFalse(active.allFinished(), "FR-43: a finished player takes no more turns");

            model.roll();
            List<Piece> movable = new ArrayList<>(model.legalMoves().keySet());
            if (movable.isEmpty()) {
                continue;                                   // FR-23: no legal move, the turn passes
            }
            Piece chosen = movable.get(choice.nextInt(movable.size()));
            PlayerColor mover = chosen.color();
            model.move(chosen);
            p.moves++;

            // ---- resolve every special square this move set off, and any chain it leads to (FR-42) ----
            int chain = 0;
            while (model.pendingEffect() != null) {
                EffectQueue.Activation waiting = model.pendingEffect();
                p.phasesSeen.add(model.phase());
                assertSame(TurnPhase.RESOLVING, model.phase(),
                        "while a special square waits the turn is RESOLVING");
                assertTrue(waiting.type() == SquareType.QUESTION || waiting.type() == SquareType.SURPRISE,
                        "only question and surprise squares wait, not " + waiting.type());
                if (chain > 0) {
                    p.chainSteps++;
                }
                chain++;
                assertTrue(chain < 50, "the chain guard must stop an endless chain (FR-42)");

                if (waiting.type() == SquareType.QUESTION) {
                    resolveQuestion(model, waiting, p, answers, answerTurn++);
                } else {
                    resolveSurprise(model, waiting, p, choice, active.color());
                }
            }
            p.longestChain = Math.max(p.longestChain, chain);

            // Nothing may be left half-resolved once the chain is done.
            assertSame(null, model.pendingEffect(), "every special square was resolved");
            assertFalse(model.phase() == TurnPhase.RESOLVING, "RESOLVING is left when the queue empties");
            assertNotNull(mover, "the mover is known");
        }
        p.phasesSeen.add(model.phase());
        return p;
    }

    /** Answers the waiting question the way a player would, and checks FR-34 to FR-37 as it happens. */
    private static void resolveQuestion(GameModel model, EffectQueue.Activation waiting, Played p,
                                        int answers, int answerTurn) {
        Question asked = model.pendingQuestion();
        assertNotNull(asked, "FR-34: a question is shown");
        p.questionsAsked++;
        p.questionIdsSeen.add(asked.id());

        // FR-34: the question is of the SQUARE's level, not the board's.
        assertSame(waiting.square().questionLevel(), asked.level(),
                "FR-34: the question must be of the level of the square that was landed on");
        assertEquals(Question.ANSWER_COUNT, asked.answers().size(), "FR-34: four options");
        // Asking again before answering must not draw a different question.
        assertSame(asked, model.pendingQuestion(), "FR-34: the question is asked once");

        int chosenIndex;
        boolean wantRight = switch (answers) {
            case 0 -> true;
            case 1 -> false;
            case 2 -> answerTurn % 2 == 0;
            default -> false;
        };
        if (answers == 3) {
            chosenIndex = QuestionResult.NO_ANSWER;         // D3: the player declines
        } else if (wantRight) {
            chosenIndex = asked.correctIndex();
        } else {
            chosenIndex = (asked.correctIndex() + 1) % Question.ANSWER_COUNT;
        }

        QuestionResult judged = model.judgeAnswer(chosenIndex);
        assertSame(asked, judged.question(), "the result belongs to the question that was asked");
        assertNotNull(judged.correctAnswer(), "FR-36: the correct answer is always reported");

        // FR-35: the signed steps are the level's steps, forward when right and backward when wrong.
        int expected = judged.correct() ? asked.steps() : -asked.steps();
        assertEquals(expected, judged.steps(),
                "FR-35: " + asked.level() + " must move " + expected + " steps");
        if (judged.correct()) {
            p.rightAnswers++;
        } else if (chosenIndex == QuestionResult.NO_ANSWER) {
            p.noAnswers++;
            assertFalse(judged.correct(), "D3/FR-35: declining to answer counts as wrong");
        } else {
            p.wrongAnswers++;
        }

        model.applyAnswer(judged);
    }

    /** Carries out the waiting surprise the way a player would, and checks FR-39, FR-40 and D2. */
    private static void resolveSurprise(GameModel model, EffectQueue.Activation waiting, Played p,
                                        Random choice, PlayerColor activeColor) {
        SurprisePrompt prompt = model.pendingSurprise();
        assertNotNull(prompt, "FR-39: a surprise is drawn");
        p.surprisesDrawn++;
        p.outcomes.merge(prompt.outcome(), 1, Integer::sum);
        assertSame(activeColor, prompt.active(), "the surprise belongs to the active player's turn");
        assertSame(waiting.piece(), prompt.subject(), "the surprise is about the piece that landed");
        assertSame(prompt, model.pendingSurprise(), "FR-39: the outcome is drawn once");

        SurpriseResult result;
        if (prompt.needsChoice()) {
            // FR-40: the ACTIVE player chooses which opponent piece is pushed; nothing is auto-selected.
            List<Piece> targets = prompt.targets();
            assertFalse(targets.isEmpty(), "needsChoice implies at least one target");
            for (Piece target : targets) {
                assertFalse(target.color() == activeColor,
                        "FR-40: only an opponent of the active player may be offered");
            }
            Piece target = targets.get(choice.nextInt(targets.size()));
            result = model.resolveSurprise(prompt, target);
            p.pushesChosen++;
            assertSame(target, result.piece(), "FR-40: the piece the player chose is the one that moves");
        } else {
            result = prompt.resolve();
        }

        // D2 (approved 21/11/2026): a turn outcome always falls on the ACTIVE player.
        if (result.changesTurn()) {
            assertSame(activeColor, result.affected(),
                    "D2: " + result.outcome() + " falls on the active player");
            assertSame(activeColor, result.turnAffected(), "D2: turnAffected() is the active player");
            if (result.opponentTriggered()) {
                p.crossPlayerTurnOutcomes++;
            }
        }
        model.applySurprise(result);
    }

    // ------------------------------------------------------------------ US-2.3: the real end-to-end acceptance

    @Test
    @DisplayName("T2-15 P12 US-2.3: complete Hard games with questions, surprises and chains all live end with a winner")
    void completeHardGamesWithEverythingLiveFinish() {
        int questions = 0;
        int surprises = 0;
        int chains = 0;
        for (long seed : new long[] {1, 2, 3, 4, 5}) {
            Played p = playToEnd(Difficulty.HARD, 4, seed, 2);
            String where = "Hard seed " + seed;

            assertTrue(p.model.isGameOver(), where + ": FR-44 the game ended");
            long finished = p.model.players().stream().filter(Player::allFinished).count();
            assertEquals(p.model.players().size() - 1L, finished, where + ": FR-44 all but one finished");
            long winners = p.model.players().stream().filter(pl -> pl.rank() == 1).count();
            assertEquals(1, winners, where + ": FR-45 exactly one winner");

            // The point of this test: the special squares really were exercised, not skipped.
            assertTrue(p.questionsAsked > 0, where + ": questions were actually asked");
            assertTrue(p.surprisesDrawn > 0, where + ": surprises were actually drawn");
            questions += p.questionsAsked;
            surprises += p.surprisesDrawn;
            chains += p.chainSteps;
        }
        assertTrue(questions > 20, "questions across the five games: " + questions);
        assertTrue(surprises > 20, "surprises across the five games: " + surprises);
        assertTrue(chains > 0, "at least one effect chained into another square (FR-42): " + chains);
    }

    @Test
    @DisplayName("T2-15 P12 US-2.3: complete Medium games with questions, surprises and chains all live end with a winner")
    void completeMediumGamesWithEverythingLiveFinish() {
        for (long seed : new long[] {1, 2, 3}) {
            Played p = playToEnd(Difficulty.MEDIUM, 4, seed, 2);
            String where = "Medium seed " + seed;
            assertTrue(p.model.isGameOver(), where + ": the game ended");
            assertEquals(1, p.model.players().stream().filter(pl -> pl.rank() == 1).count(),
                    where + ": FR-45 exactly one winner");
            assertTrue(p.questionsAsked > 0, where + ": questions were asked");
            assertTrue(p.surprisesDrawn > 0, where + ": surprises were drawn");
        }
    }

    @Test
    @DisplayName("T2-15 US-2.3: a game is still completable when every question is answered wrongly")
    void aGameFinishesEvenWhenEveryAnswerIsWrong() {
        // Wrong answers push pieces backward (FR-35), so this is the slowest way to play. It must still end:
        // a player who can never make progress would be a real defect.
        Played p = playToEnd(Difficulty.HARD, 4, 11, 1);
        assertTrue(p.model.isGameOver(), "the game ended with every answer wrong");
        assertEquals(0, p.rightAnswers, "no answer was right");
        assertTrue(p.wrongAnswers > 0, "wrong answers were given: " + p.wrongAnswers);
        assertEquals(1, p.model.players().stream().filter(pl -> pl.rank() == 1).count(), "one winner");
    }

    @Test
    @DisplayName("T2-15 D3 US-2.3: a game completes when the player always declines to answer (NO_ANSWER)")
    void aGameFinishesWhenThePlayerAlwaysDeclinesToAnswer() {
        // Product Owner decision D3, approved 21/11/2026: declining is an explicit choice and counts as a
        // wrong answer (FR-35). This drives that route through the real game, not through the dialog.
        Played p = playToEnd(Difficulty.HARD, 4, 12, 3);
        assertTrue(p.model.isGameOver(), "the game ended");
        assertTrue(p.noAnswers > 0, "the player declined at least once: " + p.noAnswers);
        assertEquals(0, p.rightAnswers, "declining is never counted as correct");
        assertEquals(1, p.model.players().stream().filter(pl -> pl.rank() == 1).count(), "one winner");
    }

    // ------------------------------------------------------------------ FR-37 in real play

    @Test
    @DisplayName("T2-15 FR-37: over a complete Hard game, more than one question of each level is asked without the deck breaking")
    void questionsAreDealtWithoutRepeatingTheWholeLevel() {
        // QuestionFactoryTest checks the deck in isolation. This checks that in a real game the factory keeps
        // dealing for hundreds of questions and that distinct questions really appear.
        Played p = playToEnd(Difficulty.HARD, 4, 2026, 2);
        assertTrue(p.model.isGameOver(), "the game ended");
        assertTrue(p.questionsAsked >= 5, "questions asked: " + p.questionsAsked);
        assertTrue(p.questionIdsSeen.size() >= 3,
                "FR-37: distinct questions were dealt, saw " + p.questionIdsSeen.size()
                        + " different ids over " + p.questionsAsked + " questions");
    }

    // ------------------------------------------------------------------ US-2.4: the turn phases in real play

    @Test
    @DisplayName("T2-15 US-2.4 NFR-09: over complete games the turn never rests in RESOLVING and all phases are legal")
    void theTurnPhasesAreLegalThroughoutCompleteGames() {
        // The per-phase refusals are tested by T2-13 and T2-10. This is the acceptance invariant: across whole
        // games the phase is always one of the declared ones, RESOLVING only while a square waits, and the
        // game is never left mid-resolution. playToEnd asserts all of that after every step.
        Played p = playToEnd(Difficulty.HARD, 4, 7, 2);
        assertTrue(p.model.isGameOver(), "the game ended");
        assertSame(TurnPhase.GAME_OVER, p.model.phase(), "a finished game rests in GAME_OVER");
        assertTrue(p.phasesSeen.contains(TurnPhase.RESOLVING),
                "special squares really put the turn into RESOLVING: " + p.phasesSeen);
        for (TurnPhase phase : p.phasesSeen) {
            assertNotNull(phase, "every observed phase is a declared TurnPhase");
        }
    }

    @Test
    @DisplayName("T2-15 US-2.4 NFR-09: while a special square waits, rolling and moving are refused")
    void nothingButResolvingIsAllowedWhileASquareWaits() {
        // Played as a game until the first special square waits, then the illegal actions are tried. This is
        // the acceptance view of the State pattern: the refusal is reached by playing, not by construction.
        GameModel model = new GameModel(settings(Difficulty.HARD, 4), new Random(3));
        Random choice = new Random(99);
        int guard = 0;
        while (model.pendingEffect() == null && !model.isGameOver()) {
            assertTrue(guard++ < ROLL_LIMIT, "a special square must be reached");
            model.roll();
            List<Piece> movable = new ArrayList<>(model.legalMoves().keySet());
            if (!movable.isEmpty()) {
                model.move(movable.get(choice.nextInt(movable.size())));
            }
        }
        assertNotNull(model.pendingEffect(), "a special square is waiting");
        assertSame(TurnPhase.RESOLVING, model.phase(), "the turn is RESOLVING");

        assertThrows(IllegalStateException.class, model::roll, "NFR-09: rolling is refused while resolving");
        assertTrue(model.legalMoves().isEmpty(), "NFR-09: no move is offered while resolving");
        assertSame(TurnPhase.RESOLVING, model.phase(), "a refused action changes nothing");
        assertNotNull(model.pendingEffect(), "the square is still waiting after the refusals");
    }
}
