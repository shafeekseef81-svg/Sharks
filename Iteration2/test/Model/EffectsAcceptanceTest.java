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
 * "the real acceptance of US-2.3" — which could not be written while T2-10 was unmerged. <b>T2-10 part 2
 * (#92) merged on 23/11/2026</b> (`iteration-2` 1d47f2d), together with T2-09 (#90) and T2-11 (#91), so this
 * class now sits directly on {@code iteration-2} and no longer needs a stacked branch.
 *
 * <p><b>How it divides the work with {@code MediumHardAcceptanceTest} (#99).</b> Both play complete games
 * through the same facade, and they are not duplicates:
 * <ul>
 *   <li><b>#99</b> owns the <i>board</i> subject — the dice and the FR-17 formula, the Table 1 board counts,
 *       Double Blocks over whole routes (FR-29), captures and the rankings. Since #92 it must resolve the
 *       special squares to keep playing, so it does, <i>neutrally</i>, and asserts nothing about them.</li>
 *   <li><b>this class</b> owns the <i>effect</i> subject — that a question is of the square's level and is
 *       asked once (FR-34), that the signed steps follow the level (FR-35), that the correct answer is always
 *       reported (FR-36), that an outcome is drawn once and only an opponent of the active player may be
 *       pushed (FR-39, FR-40), that a chain terminates (FR-42), and the two Product Owner decisions D2 and
 *       D3 as they appear in real play.</li>
 * </ul>
 * Run together on {@code iteration-2} the two add 15 tests and the suite is 671/671, with no interference.
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
 * <p><b>What these games measurably do and do not reach.</b> Natural play is good at the common cases and
 * blind to the rare ones, so the limits are stated rather than left to be assumed. Measured over the games
 * below:
 * <ul>
 *   <li>all six surprise outcomes are drawn, and 26 turn outcomes reach the D2 check;</li>
 *   <li><b>no</b> turn outcome is ever opponent-triggered, so D2's cross-player half is never exercised here
 *       - it is Rakan's scripted T2-12 (#95);</li>
 *   <li>chains are rare: 2 chain steps over five Hard games, longest chain 2 effects, so the FR-42 chain
 *       <i>guard</i> is never approached here;</li>
 *   <li>no level's deck is ever used up - the most questions of one level in one game is exactly 12, the deck
 *       size - so FR-37's reshuffle is never reached here either.</li>
 * </ul>
 *
 * <p><b>On counting questions.</b> The course bank holds <b>36</b> questions, 12 of each level
 * ({@code CourseQuestions}), and each game builds its own decks. So a figure like "94 questions over five
 * games" is a count of questions <i>asked</i>, and can never be a count of distinct ids: at most 36 distinct
 * ids exist, and across those five games 34 of them appear. What is true per game is that every question a
 * single game asked was different, which is FR-37 and is what
 * {@code questionsOfALevelDoNotRepeatBeforeTheDeckIsUsedUp} checks.
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
        int turnOutcomes;            // turn outcomes seen at all, i.e. times the D2 check was reached
        final Set<String> questionIdsSeen = new HashSet<>();
        /** How many questions of each level this game asked (FR-37). */
        final Map<Difficulty, Integer> askedPerLevel = new EnumMap<>(Difficulty.class);
        /** The distinct question ids of each level this game asked (FR-37). */
        final Map<Difficulty, Set<String>> idsPerLevel = new EnumMap<>(Difficulty.class);
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
        p.askedPerLevel.merge(asked.level(), 1, Integer::sum);
        p.idsPerLevel.computeIfAbsent(asked.level(), level -> new HashSet<>()).add(asked.id());

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
            p.turnOutcomes++;
            assertSame(activeColor, result.affected(),
                    "D2: " + result.outcome() + " falls on the active player");
            assertSame(activeColor, result.turnAffected(), "D2: turnAffected() is the active player");
            if (result.opponentTriggered()) {
                // Measured over every game in this class, this has never happened: see
                // theD2CheckIsReachedButItsCrossPlayerCaseIsNot below. Counted, not asserted.
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

        // Chains are RARE in natural play: measured, these five games produce 2 chain steps in all, and the
        // longest chain is 2 effects. So this shows a chain does happen on the real game (FR-41, FR-42), and
        // nothing more. The chain GUARD, and chains of any depth, are covered by the scripted tests of T2-10
        // (#92) and T2-12 (#95); they cannot be reached by playing.
        assertTrue(chains > 0, "at least one effect chained into another square (FR-42): " + chains
                + " chain steps over the five games - rare by nature, which is why the guard itself is"
                + " covered by scripted tests and not here");
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

    /**
     * FR-37 as it is actually observable in natural play: <b>within one game, no question of a level is asked
     * twice before that level's deck is used up.</b> That is checked per level, by comparing how many
     * questions of a level were asked with how many <i>distinct</i> ids of that level were seen - they must be
     * equal for as long as the number asked has not passed the size of the level's deck.
     *
     * <p><b>What this test does not show, and must not be read as showing.</b> The course bank holds 36
     * questions, 12 of each level ({@code CourseQuestions}). No game in this class asks more than 12 of any
     * one level - the measured maximum is exactly 12 - so <b>the reshuffle that begins a new round is never
     * reached here.</b> That half of FR-37, and the whole-round behaviour, is covered in isolation by
     * {@code QuestionFactoryTest} and {@code QuestionDeck}'s own tests, which can exhaust a deck on purpose.
     * This test covers the half that natural play can reach.
     *
     * <p>It replaces an earlier version that asserted only {@code questionIdsSeen.size() >= 3} over a game
     * that in fact asks 21 distinct questions. That threshold was so far below the real figure that it could
     * not have failed for any defect worth finding.
     */
    @Test
    @DisplayName("T2-15 FR-37: within a complete game no question of a level repeats before that level's deck is used up")
    void questionsOfALevelDoNotRepeatBeforeTheDeckIsUsedUp() {
        for (Difficulty level : List.of(Difficulty.HARD, Difficulty.MEDIUM)) {
            for (long seed : new long[] {1, 2026}) {
                Played p = playToEnd(level, 4, seed, 2);
                String where = level + " seed " + seed;
                assertTrue(p.model.isGameOver(), where + ": the game ended");
                assertTrue(p.questionsAsked > 0, where + ": questions were asked");

                for (Map.Entry<Difficulty, Integer> e : p.askedPerLevel.entrySet()) {
                    Difficulty questionLevel = e.getKey();
                    int asked = e.getValue();
                    int distinct = p.idsPerLevel.get(questionLevel).size();
                    int deckSize = CourseQuestions.all().stream()
                            .filter(q -> q.level() == questionLevel).toList().size();

                    if (asked <= deckSize) {
                        assertEquals(asked, distinct, where + ": FR-37 " + questionLevel + " asked " + asked
                                + " question(s) of a deck of " + deckSize
                                + ", so all of them must be different, but only " + distinct + " were");
                    } else {
                        // Not reached by any seed today; kept so the test stays correct if a game ever runs
                        // long enough to exhaust a level.
                        assertEquals(deckSize, distinct, where + ": FR-37 " + questionLevel + " asked " + asked
                                + " of a deck of " + deckSize + ", so every question of the level must have"
                                + " been seen, but only " + distinct + " were");
                    }
                }
            }
        }
    }

    /**
     * Shows that the D2 assertion in {@code resolveSurprise} is actually <b>reached</b> in these games, and
     * that its cross-player case is <b>not</b> - so this class does not appear to cover something it never
     * exercises.
     *
     * <p>D2 (approved 21/11/2026) says a turn outcome always falls on the active player. Over the games this
     * class plays, {@code SKIP_TURN} and {@code EXTRA_ROLL} are drawn often, so the check runs many times and
     * is no empty assertion. But the interesting half of D2 - a turn outcome in a chain that an
     * <b>opponent's</b> piece set off - has <b>never occurred</b> in any game here: measured over nine games
     * across both levels, 26 turn outcomes and 0 of them opponent-triggered.
     *
     * <p>That case is covered by Rakan's scripted {@code EffectChainIndependentTest} (T2-12,
     * [#95](https://github.com/shafeekseef81-svg/Sharks/pull/95)), which constructs it deliberately, and that
     * is the right place for it: SDP 5.3.4 asks an independent tester for the rules, and a scripted scenario
     * is the only way to reach something this rare. This test records the gap rather than hiding it.
     */
    @Test
    @DisplayName("T2-15 D2: the turn-outcome check is reached in real play, and its cross-player case is not")
    void theD2CheckIsReachedButItsCrossPlayerCaseIsNot() {
        int turnOutcomes = 0;
        int crossPlayer = 0;
        for (long seed : new long[] {1, 2, 3, 4, 5}) {
            Played p = playToEnd(Difficulty.HARD, 4, seed, 2);
            turnOutcomes += p.turnOutcomes;
            crossPlayer += p.crossPlayerTurnOutcomes;
        }

        // The D2 assertion really ran: without this, "D2 holds in real play" would be an empty claim.
        assertTrue(turnOutcomes > 0,
                "D2: a turn outcome (SKIP_TURN or EXTRA_ROLL) was drawn, so the D2 check was reached; saw "
                        + turnOutcomes);

        // And it ran only on the easy half. This is a statement of the limit, not a requirement: if a future
        // change ever makes the cross-player case occur naturally, this reminds us to say so here.
        assertEquals(0, crossPlayer,
                "the cross-player D2 case is not reached by natural play in this class (" + turnOutcomes
                        + " turn outcomes, " + crossPlayer + " opponent-triggered). It is covered by the"
                        + " scripted EffectChainIndependentTest in T2-12 (#95). If this now fails, natural"
                        + " play has started reaching it and this class may assert D2's cross-player half"
                        + " directly.");
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
