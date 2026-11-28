package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Acceptance tests of task <b>T2-15</b> ([#66](https://github.com/shafeekseef81-svg/Sharks/issues/66)) for the
 * Iteration 2 stories, written by the Product Owner (Mohamed Zereny). Games are played to their end through the
 * {@link GameModel} facade with the real dice, the real board and all the real rule classes - no stubs - so a
 * passing test here means a player really can play that game.
 *
 * <p><b>Scope of this class.</b> <b>US-2.1 #48</b> (Medium and Hard boards, Boost and UltraBoost dice:
 * FR-05, FR-10..FR-12, FR-16, FR-17, FR-25), <b>US-2.2 #49</b> (Double Blocks: FR-28..FR-30) and the end of
 * a game from <b>US-2.4 #51</b> (FR-43..FR-45).
 *
 * <p><b>These games run on the fully integrated game.</b> <b>T2-10 part 2 (#92) is merged</b>
 * (`iteration-2` 1d47f2d, 23/11/2026), together with T2-09 (#90) and T2-11 (#91), so a move onto a question
 * or surprise square now leaves the turn in {@link TurnPhase#RESOLVING} and the next {@code roll()} is
 * refused until that square has been resolved. {@code playToEnd} therefore resolves every waiting square,
 * and any chain it sets off, before taking the next turn. Until #92 merged, this class ran on a baseline in
 * which a question or surprise square did nothing at all; the five tests that play a complete game failed
 * the moment it did, and resolving the queue is what fixes them.
 *
 * <p><b>A rule that only shows up here.</b> The {@code EXTRA_ROLL} surprise rolls the <b>Regular and
 * UltraBoost</b> dice whatever level is being played (SRS Table 2, Q27), so on Medium an ordinary roll is a
 * Boost roll while a surprise extra roll is an UltraBoost one. The assertion that a level only ever rolls
 * its own dice is therefore false on the integrated game, and the two kinds of roll are checked and bounded
 * separately below. A consequence worth knowing: <b>a Medium game can move more than 12 squares</b> - the
 * measured longest surprise-roll movement on Medium is 24, against the 12 of its own dice.
 *
 * <p><b>What this class does and does not assert about the effects.</b> It resolves them <i>neutrally</i>: it
 * answers questions and carries out surprises the way a player would, and checks only that the turn leaves
 * {@code RESOLVING} and that the chain guard holds (FR-42). It deliberately makes no assertion about what a
 * question or a surprise <i>does</i>. That belongs to {@code EffectsAcceptanceTest} (the planned test
 * <b>P12</b>, PR #103) for the end-to-end view and to Rakan's {@code EffectChainIndependentTest} (T2-12,
 * #95) for the individual rules. Duplicating their assertions here would add no coverage and two more places
 * to maintain. What this class keeps is its own subject: the dice, the boards, the Double Blocks, the
 * captures and the rankings - now checked while the special squares really fire.
 *
 * <p><b>Still not covered by anything automated:</b> a game played by a person on the real window, and
 * NFR-05. Those are the manual scripts M1-M7 of {@code Iteration2/docs/T2-15_AcceptancePlan.md} and are
 * <b>not</b> claimed as passed by this file.
 */
class MediumHardAcceptanceTest {

    /** A complete game must end well inside this many rolls, or the test fails instead of hanging. */
    private static final int ROLL_LIMIT = 500_000;

    /**
     * The level whose dice the surprise extra roll uses, whatever level is being played: the Regular and
     * UltraBoost dice (SRS Table 2 Q27). Read straight from {@link StandardTurnManager#SURPRISE_ROLL_DICE}
     * rather than copied, so the test cannot drift from the rule it is checking.
     */
    private static final Difficulty SURPRISE_ROLL_LEVEL = StandardTurnManager.SURPRISE_ROLL_DICE;

    private static GameSettings settings(Difficulty level, int count) {
        PlayerColor[] colors = PlayerColor.values();
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            setup.add(new GameSettings.PlayerSetup("P" + (i + 1), colors[i]));
        }
        return new GameSettings(level, setup);
    }

    /** What one complete game showed. */
    private static final class Played {
        GameModel model;
        final List<Player> finishEvents = new ArrayList<>();
        int gameOverEvents;
        int rolls;
        int moves;
        int captures;
        int zeroMovementRolls;
        int longestMovement;
        /** Surprise extra rolls taken (FR-39 EXTRA_ROLL): these use the Regular + UltraBoost dice (Q27). */
        int surpriseRolls;
        /** The longest movement of an ordinary roll of the level's own dice. */
        int longestLevelMovement;
        /** The longest movement of a surprise extra roll, whatever the level. */
        int longestSurpriseMovement;
        /** Rolls at which at least one Double Block of any colour stood on the shared track (FR-28). */
        int rollsWithABlockStanding;
        /** Moves made while a Double Block of a colour other than the mover's stood on the shared track. */
        int movesWithOpponentBlockStanding;
        /** Squares a move actually entered and that were checked against the standing blocks (FR-29). */
        int squaresCheckedForBlocks;
        /** Question squares resolved while these games were played (FR-34..FR-37). */
        int questionsResolved;
        /** Surprise squares resolved while these games were played (FR-39, FR-40). */
        int surprisesResolved;
        /** Effects that ran while another effect of the same move was already resolving (FR-41, FR-42). */
        int chainSteps;
        /** The longest chain of effects one move set off. */
        int longestChain;
        final Set<Integer> specialValues = new HashSet<>();
    }

    /**
     * Plays one complete game of {@code level} and checks, after every single step, the rules that must always
     * hold: the movement formula of the level (FR-17), that no piece leaves the board, that only opponents are
     * captured and never on a protected square (FR-26, FR-27), and that no move lands on <i>or passes through</i>
     * an opponent's Double Block (FR-29).
     *
     * <p>Every special square the move sets off is resolved before the next turn is taken, because since #92
     * the turn stays in {@link TurnPhase#RESOLVING} until it is and {@code roll()} is refused.
     */
    private static Played playToEnd(Difficulty level, int count, long seed) {
        Played p = new Played();
        LevelConfig config = LevelConfig.of(level);
        Random choice = new Random(seed * 31 + 7);
        GameModel model = new GameModel(settings(level, count), new Random(seed));
        p.model = model;
        model.addListener(e -> {
            if (e.type() == GameEvent.Type.PLAYER_FINISHED) {
                p.finishEvents.add(e.player());
            } else if (e.type() == GameEvent.Type.GAME_OVER) {
                p.gameOverEvents++;
            }
        });
        int finish = model.board().finishIndex();

        while (!model.isGameOver()) {
            assertTrue(p.rolls++ < ROLL_LIMIT, level + " seed " + seed + ": the game must end");
            Player active = model.currentPlayer();
            assertFalse(active.allFinished(), "FR-43: a finished player takes no more turns");

            DiceRoll roll = model.roll();
            assertNotNull(roll, "a roll is always reported");

            // FR-16, FR-17: the dice and the movement formula. An ordinary roll uses the dice of the level,
            // but the extra roll granted by the EXTRA_ROLL surprise uses the Regular and UltraBoost dice
            // whatever the level does (SRS Table 2 Q27, and the text of the outcome itself: "Roll again right
            // away with the Regular + UltraBoost dice"). So on Medium an ordinary roll is a Boost roll while a
            // surprise roll is an UltraBoost one, and the two are checked separately rather than together.
            DieType expectedDie = roll.surpriseRoll()
                    ? LevelConfig.of(SURPRISE_ROLL_LEVEL).specialDie()
                    : config.specialDie();
            assertEquals(expectedDie, roll.specialType(),
                    level + (roll.surpriseRoll() ? ": a surprise extra roll uses the UltraBoost die"
                            : ": the special die of the level is rolled"));
            assertTrue(roll.special() >= 1 && roll.special() <= expectedDie.faces(),
                    level + ": " + expectedDie + " shows 1.." + expectedDie.faces() + ", not " + roll.special());
            assertEquals(roll.regular() * (roll.special() - 1), roll.movement(),
                    level + ": FR-17 movement = Regular x (special - 1)");
            p.longestMovement = Math.max(p.longestMovement, roll.movement());
            if (roll.surpriseRoll()) {
                p.surpriseRolls++;
                p.longestSurpriseMovement = Math.max(p.longestSurpriseMovement, roll.movement());
            } else {
                p.specialValues.add(roll.special());
                p.longestLevelMovement = Math.max(p.longestLevelMovement, roll.movement());
            }
            if (roll.movement() == 0) {
                p.zeroMovementRolls++;
            }

            // The Double Blocks standing before this move, by the colour that owns them (FR-28). They are read
            // before the move, because the move itself can make a block or break one.
            Map<Cell, PlayerColor> blocks = doubleBlocks(model);
            if (!blocks.isEmpty()) {
                p.rollsWithABlockStanding++;
            }

            List<Piece> movable = new ArrayList<>(model.legalMoves().keySet());
            if (movable.isEmpty()) {
                continue;                                  // FR-23: no legal move, the turn passes
            }
            Piece chosen = movable.get(choice.nextInt(movable.size()));
            PlayerColor mover = chosen.color();
            boolean opponentBlockStood = false;
            for (PlayerColor owner : blocks.values()) {
                if (owner != mover) {
                    opponentBlockStood = true;
                    break;
                }
            }

            MoveResult r = model.move(chosen);
            p.moves++;
            p.captures += r.captures().size();
            if (opponentBlockStood) {
                p.movesWithOpponentBlockStanding++;
            }

            // FR-29: an opponent's Double Block may be neither landed on NOR passed, in either direction.
            // r.cells() is the whole route of the move, from the square the piece stood on to the one it
            // reached, inclusive, so the squares the move really ENTERED are every cell after the first -
            // except on a castle exit, where the one cell in the route is the start square the piece entered
            // (FR-19). Home-path cells can raise no false alarm: `blocks` holds shared-track squares only.
            List<Cell> route = r.cells();
            int firstEntered = r.kind() == MoveResult.Kind.EXIT_CASTLE ? 0 : 1;
            for (int i = firstEntered; i < route.size(); i++) {
                PlayerColor blockOwner = blocks.get(route.get(i));
                p.squaresCheckedForBlocks++;
                assertTrue(blockOwner == null || blockOwner == mover,
                        level + " seed " + seed + ": FR-29 a " + mover + " piece entered a Double Block of "
                                + blockOwner + " at step " + i + " of " + (route.size() - 1)
                                + " (" + r.kind() + " from " + r.fromProgress() + " to " + r.toProgress() + ")");
            }

            // Nothing leaves the board, and captures follow FR-26 and FR-27.
            for (Player player : model.players()) {
                for (Piece piece : player.pieces()) {
                    assertTrue(piece.progress() >= Piece.IN_CASTLE && piece.progress() <= finish,
                            level + ": a piece left the board at " + piece.progress());
                }
            }
            for (MoveResult.Capture c : r.captures()) {
                assertTrue(c.piece().inCastle(), "FR-26: a captured piece goes back to its castle");
                assertFalse(c.piece().color() == chosen.color(), "FR-26: only opponents are captured");
                assertFalse(model.board().squareAt(c.from()).isProtected(),
                        "FR-27: never captured on a protected square");
            }

            // Since #92 a move onto a question or surprise square leaves the turn in RESOLVING and the next
            // roll is refused, so the square - and any chain it sets off - is resolved before the next turn.
            resolvePendingEffects(model, p, choice);

            // The effects move pieces too, so the board invariant is checked again once the chain is done.
            for (Player player : model.players()) {
                for (Piece piece : player.pieces()) {
                    assertTrue(piece.progress() >= Piece.IN_CASTLE && piece.progress() <= finish,
                            level + ": a piece left the board at " + piece.progress() + " after an effect");
                }
            }
        }
        return p;
    }

    /**
     * Resolves every special square the last move set off, and any chain it leads to (FR-41, FR-42), so the
     * game can go on to the next roll.
     *
     * <p>This is deliberately <b>neutral</b>: it decides what a player would decide - the answer to give and
     * the opponent to push - and then checks only that the turn really was in {@code RESOLVING} while a
     * square waited, that the chain guard holds, and that {@code RESOLVING} is left once the queue is empty.
     * What a question or a surprise actually does to the board is asserted by {@code EffectsAcceptanceTest}
     * (#103) and by Rakan's {@code EffectChainIndependentTest} (#95), and is not re-asserted here.
     *
     * <p>Answers alternate between right and wrong so that both directions of FR-35 occur in these games
     * instead of only one.
     */
    private static void resolvePendingEffects(GameModel model, Played p, Random choice) {
        int chain = 0;
        while (model.pendingEffect() != null) {
            EffectQueue.Activation waiting = model.pendingEffect();
            assertSame(TurnPhase.RESOLVING, model.phase(),
                    "while a special square waits, the turn is RESOLVING");
            assertTrue(waiting.type() == SquareType.QUESTION || waiting.type() == SquareType.SURPRISE,
                    "only question and surprise squares wait, not " + waiting.type());
            if (chain > 0) {
                p.chainSteps++;
            }
            assertTrue(++chain < 50, "FR-42: the chain guard must stop an endless chain");

            if (waiting.type() == SquareType.QUESTION) {
                Question asked = model.pendingQuestion();
                assertNotNull(asked, "FR-34: a question square draws a question");
                p.questionsResolved++;
                int answer = p.questionsResolved % 2 == 0
                        ? asked.correctIndex()
                        : (asked.correctIndex() + 1) % Question.ANSWER_COUNT;
                model.applyAnswer(model.judgeAnswer(answer));
            } else {
                SurprisePrompt prompt = model.pendingSurprise();
                assertNotNull(prompt, "FR-39: a surprise square draws an outcome");
                p.surprisesResolved++;
                SurpriseResult result;
                if (prompt.needsChoice()) {
                    List<Piece> targets = prompt.targets();
                    assertFalse(targets.isEmpty(), "needsChoice implies at least one target");
                    result = model.resolveSurprise(prompt, targets.get(choice.nextInt(targets.size())));
                } else {
                    result = prompt.resolve();
                }
                model.applySurprise(result);
            }
        }
        p.longestChain = Math.max(p.longestChain, chain);
        assertNotSame(TurnPhase.RESOLVING, model.phase(), "RESOLVING is left once the queue is empty");
    }

    /**
     * The cells of the shared track that carry two or more pieces of one colour, i.e. the Double Blocks
     * standing right now (FR-28), mapped to the colour that owns each one.
     */
    private static Map<Cell, PlayerColor> doubleBlocks(GameModel model) {
        Map<Cell, PlayerColor> owners = new HashMap<>();
        int homeStart = model.board().homeStartIndex();
        for (Player player : model.players()) {
            Map<Cell, Integer> mine = new HashMap<>();
            for (Piece piece : player.pieces()) {
                if (piece.onBoard() && piece.progress() < homeStart) {
                    Cell cell = model.board().cellAt(player.color(), piece.progress());
                    mine.merge(cell, 1, Integer::sum);
                }
            }
            for (Map.Entry<Cell, Integer> e : mine.entrySet()) {
                if (e.getValue() >= 2) {
                    owners.put(e.getKey(), player.color());
                }
            }
        }
        return owners;
    }

    // ------------------------------------------------------------------ US-2.1 #48: setup and boards

    @Test
    @DisplayName("T2-15 US-2.1 FR-05: a player can start a Medium or a Hard game with 2, 3 or 4 players")
    void everyLevelAndPlayerCountCanBeStarted() {
        for (Difficulty level : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            for (int count = 2; count <= 4; count++) {
                GameModel model = new GameModel(settings(level, count), new Random(7));
                assertEquals(count, model.players().size(), level + ": " + count + " players");
                assertEquals(level, model.board().difficulty(), "the board is of the chosen level");
                assertFalse(model.isGameOver(), "a new game is not over");
                assertNotNull(model.currentPlayer(), "someone starts");
                for (Player player : model.players()) {
                    assertEquals(Player.PIECES_PER_PLAYER, player.pieces().size(), "four pieces each");
                    for (Piece piece : player.pieces()) {
                        assertTrue(piece.inCastle(), "every piece starts in its castle");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("T2-15 US-2.1 FR-10..FR-12: the Medium and Hard board a player gets matches Table 1, over 20 games")
    void theBoardOfEachLevelMatchesTheSpecification() {
        for (Difficulty level : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            LevelConfig config = LevelConfig.of(level);
            for (long seed = 0; seed < 20; seed++) {
                Board board = new GameModel(settings(level, 4), new Random(seed)).board();
                assertEquals(config.gridSize(), board.size(), level + " seed " + seed + ": grid size");
                assertEquals(config.totalTrackSquares(), board.trackSquareCount(),
                        level + " seed " + seed + ": track squares");
                assertEquals(config.protectionSquares(), board.count(SquareType.PROTECTION),
                        level + " seed " + seed + ": protection squares");
                assertEquals(config.questionSquares(), board.count(SquareType.QUESTION),
                        level + " seed " + seed + ": question squares");
                assertEquals(config.surpriseSquares(), board.count(SquareType.SURPRISE),
                        level + " seed " + seed + ": surprise squares");
                assertEquals(config.startingSquares(), board.count(SquareType.START),
                        level + " seed " + seed + ": starting squares");
                assertEquals(config.arrowSquares(), board.count(SquareType.ARROW),
                        level + " seed " + seed + ": arrow squares");
                // Every question level is represented as Table 1 asks (FR-12).
                for (Difficulty questionLevel : Difficulty.values()) {
                    assertEquals(config.questionsPerLevel(), board.countQuestions(questionLevel),
                            level + " seed " + seed + ": question squares of level " + questionLevel);
                }
            }
        }
    }

    // ------------------------------------------------------------------ US-2.1 #48: the dice in a real game

    @Test
    @DisplayName("T2-15 US-2.1 FR-16, FR-17: a complete Medium game rolls Boost 1-3 and moves Regular x (Boost - 1)")
    void aCompleteMediumGameUsesTheBoostDice() {
        Played p = playToEnd(Difficulty.MEDIUM, 4, 2026);
        assertTrue(p.model.isGameOver(), "the Medium game was played to its end");
        assertEquals(DieType.BOOST, LevelConfig.of(Difficulty.MEDIUM).specialDie(), "Medium rolls the Boost die");
        // Every value of the Boost die really came up, so the formula was exercised across its whole range.
        assertEquals(Set.of(1, 2, 3), p.specialValues,
                "every Boost value 1-3 came up in " + p.rolls + " rolls, but saw " + p.specialValues);
        assertTrue(p.zeroMovementRolls > 0, "FR-17: a Boost 1 gives a movement of 0 and moves nothing");
        assertTrue(p.longestLevelMovement <= 12,
                "the longest Medium movement from the level's own dice is Regular 6 x (Boost 3 - 1) = 12, saw "
                        + p.longestLevelMovement);

        // The EXTRA_ROLL surprise rolls the UltraBoost die even on Medium (Q27), so a Medium game really can
        // produce a movement of more than 12. That is the specified behaviour, not a defect, and it is
        // recorded here rather than asserted away: the bound that holds is the UltraBoost one.
        assertTrue(p.surpriseRolls > 0,
                "the EXTRA_ROLL surprise was drawn in this Medium game, so surprise rolls were taken");
        assertTrue(p.longestSurpriseMovement <= 30,
                "a Medium surprise roll is bounded by Regular 6 x (UltraBoost 6 - 1) = 30, saw "
                        + p.longestSurpriseMovement);
    }

    @Test
    @DisplayName("T2-15 US-2.1 FR-16, FR-17: a complete Hard game rolls UltraBoost 1-6 and never moves over 30")
    void aCompleteHardGameUsesTheUltraBoostDice() {
        Played p = playToEnd(Difficulty.HARD, 4, 2026);
        assertTrue(p.model.isGameOver(), "the Hard game was played to its end");
        assertEquals(DieType.ULTRA_BOOST, LevelConfig.of(Difficulty.HARD).specialDie(),
                "Hard rolls the UltraBoost die");
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), p.specialValues,
                "every UltraBoost value 1-6 came up in " + p.rolls + " rolls, but saw " + p.specialValues);
        assertTrue(p.zeroMovementRolls > 0, "FR-17: an UltraBoost 1 gives a movement of 0 and moves nothing");
        assertTrue(p.longestMovement <= 30,
                "the longest Hard movement is Regular 6 x (UltraBoost 6 - 1) = 30, saw " + p.longestMovement
                        + ", including " + p.surpriseRolls + " surprise rolls, which use the same dice here");
    }

    // ------------------------------------------------------------------ US-2.2 #49: Double Blocks

    /**
     * FR-28 and FR-29 over complete games. The per-square check itself lives in {@code playToEnd}, which tests
     * every square every move entered against the blocks that stood before that move. The job of this test is
     * to show that the check was <b>not empty</b>: that Double Blocks belonging to an opponent really did
     * stand while these six games were played.
     *
     * <p>Counting the moves made, which is what an earlier version of this test did, would not show that. The
     * numbers below are the real condition and are asserted per level, because the levels differ sharply: a
     * Hard board is much larger, so blocks are rarer on it, and one of the three Hard seeds (12) happens to
     * produce no opponent Double Block at all. The assertion is therefore made over the three seeds of a level
     * together; per game it would be a false claim about Hard seed 12. The per-game figures are printed in the
     * failure message so the reader sees what was really exercised rather than taking it on trust.
     *
     * <p>That an opponent's block cannot be landed on or passed <i>when one does stand in the way</i> is
     * pinned down by the scripted scenarios of T2-06/T2-07 ({@code DoubleBlockEngineTest},
     * {@code DoubleBlockIndependentTest}), and is not repeated here. What this test adds is that over six
     * complete games with the real dice, no route ever crossed one.
     */
    @Test
    @DisplayName("T2-15 US-2.2 FR-28, FR-29: over complete Medium and Hard games no route lands on or passes an opponent's Double Block")
    void doubleBlocksAreNeverEnteredInRealGames() {
        Map<Difficulty, Integer> movesUnderAnOpponentBlock = new EnumMap<>(Difficulty.class);
        Map<Difficulty, Integer> squaresChecked = new EnumMap<>(Difficulty.class);
        StringBuilder report = new StringBuilder();

        for (long seed : new long[] {11, 12, 13}) {
            for (Difficulty level : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
                Played p = playToEnd(level, 4, seed);
                assertTrue(p.model.isGameOver(), level + " seed " + seed + ": the game ended");
                assertTrue(p.moves > 0, "pieces were really moved");
                movesUnderAnOpponentBlock.merge(level, p.movesWithOpponentBlockStanding, Integer::sum);
                squaresChecked.merge(level, p.squaresCheckedForBlocks, Integer::sum);
                report.append(String.format(
                        "%n  %-6s seed %2d: %4d moves, %5d squares entered and checked, "
                                + "%3d rolls with a block standing, %3d moves under an opponent's block",
                        level, seed, p.moves, p.squaresCheckedForBlocks,
                        p.rollsWithABlockStanding, p.movesWithOpponentBlockStanding));
            }
        }

        // The check must have had something to check: in each level an opponent's Double Block really stood
        // while moves were being made. Without this, the FR-29 assertion in playToEnd would be vacuous.
        for (Difficulty level : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            assertTrue(squaresChecked.get(level) > 0,
                    level + ": squares were entered and checked against the standing blocks" + report);
            assertTrue(movesUnderAnOpponentBlock.get(level) > 0,
                    level + ": at least one move was made while an opponent's Double Block stood, so the"
                            + " FR-29 check in playToEnd was not empty." + report);
        }
    }

    // ------------------------------------------------------------------ US-2.4 #51: the end of a game

    @Test
    @DisplayName("T2-15 FR-43..FR-45: complete Medium games end with a winner and a full ranking, over 5 seeds")
    void completeMediumGamesEndWithAWinnerAndRankings() {
        assertCompleteGamesEndProperly(Difficulty.MEDIUM, new long[] {1, 2, 3, 4, 5});
    }

    @Test
    @DisplayName("T2-15 FR-43..FR-45: complete Hard games end with a winner and a full ranking, over 5 seeds")
    void completeHardGamesEndWithAWinnerAndRankings() {
        assertCompleteGamesEndProperly(Difficulty.HARD, new long[] {1, 2, 3, 4, 5});
    }

    /** Plays complete games of a level and checks how each one ended (FR-43, FR-44, FR-45). */
    private static void assertCompleteGamesEndProperly(Difficulty level, long[] seeds) {
        for (long seed : seeds) {
            Played p = playToEnd(level, 4, seed);
            String where = level + " seed " + seed;

            assertTrue(p.model.isGameOver(), where + ": FR-44 the game ended");
            assertEquals(1, p.gameOverEvents, where + ": FR-44 the end is announced exactly once");

            // FR-44: the game ends when all but one player have finished.
            long finished = p.model.players().stream().filter(Player::allFinished).count();
            assertEquals(p.model.players().size() - 1L, finished,
                    where + ": FR-44 all but one player finished");

            // FR-43: a player finishes only with all four pieces at the finish.
            for (Player player : p.model.players()) {
                if (player.allFinished()) {
                    assertEquals(Player.PIECES_PER_PLAYER, player.finishedCount(),
                            where + ": FR-43 a finished player has all four pieces home");
                    for (Piece piece : player.pieces()) {
                        assertTrue(piece.finished(), where + ": FR-43 every piece of a finished player is home");
                    }
                }
            }

            // FR-45: the ranking follows the order in which the players finished.
            assertEquals(finished, p.finishEvents.size(),
                    where + ": FR-45 one finish event per finished player");
            for (int i = 0; i < p.finishEvents.size(); i++) {
                assertEquals(i + 1, p.finishEvents.get(i).rank(),
                        where + ": FR-45 the player who finished " + (i + 1) + " has rank " + (i + 1));
            }
            // There is exactly one winner, and it is the first player who finished.
            assertFalse(p.finishEvents.isEmpty(), where + ": somebody won");
            assertEquals(1, p.finishEvents.get(0).rank(), where + ": FR-45 the first to finish is rank 1");
            long winners = p.model.players().stream().filter(player -> player.rank() == 1).count();
            assertEquals(1, winners, where + ": FR-45 exactly one player has rank 1");
        }
    }
}
