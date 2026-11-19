package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Model.SurpriseOutcomeTest.ScriptRandom;
import Model.SurpriseResult.SurpriseAction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests of T2-09, part 2: the surprise factory, the player's choice of an opponent piece (SRS
 * FR-40) and the instruction each outcome hands to the effect queue of T2-10. Owner: Mohamed Zereny.
 *
 * <p>The independent tests of this rule are T2-12 (issue #63, Rakan Ziyan), who did not write this code
 * (SDP 5.3.4). These tests do not replace them.
 */
class SurpriseFactoryTest {

    private final Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(11));
    private final List<Player> players = new ArrayList<>();
    private final Player red;
    private final Player green;
    private final Player blue;

    SurpriseFactoryTest() {
        red = addPlayer("Red", PlayerColor.RED);
        green = addPlayer("Green", PlayerColor.GREEN);
        blue = addPlayer("Blue", PlayerColor.BLUE);
    }

    private Player addPlayer(String name, PlayerColor color) {
        Player player = new Player(name, color, board.finishIndex());
        players.add(player);
        return player;
    }

    /** Puts a piece on the shared track at the given progress and gives it back. */
    private Piece onTrack(Player player, int pieceIndex, int progress) {
        assertTrue(progress >= 0 && progress < board.homeStartIndex(), "not on the shared track");
        Piece piece = player.pieces().get(pieceIndex);
        piece.setProgress(progress);
        return piece;
    }

    private SurpriseFactory factory(Random random) {
        return new SurpriseFactory(board, players, random);
    }

    // ------------------------------------------------------------------ building the factory

    @Test
    @DisplayName("The factory needs a board, players and a random source")
    void factoryNeedsItsParts() {
        assertThrows(NullPointerException.class, () -> new SurpriseFactory(null, players, new Random(1)));
        assertThrows(NullPointerException.class, () -> new SurpriseFactory(board, null, new Random(1)));
        assertThrows(NullPointerException.class, () -> new SurpriseFactory(board, players, null));
    }

    @Test
    @DisplayName("FR-06: a surprise needs at least two players, because it can hit an opponent")
    void factoryNeedsTwoPlayers() {
        assertThrows(IllegalArgumentException.class,
                () -> new SurpriseFactory(board, List.of(red), new Random(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new SurpriseFactory(board, List.of(), new Random(1)));
    }

    @Test
    @DisplayName("Only a piece of this game that stands on the board can land on a surprise square")
    void subjectMustBeAPieceOnTheBoard() {
        SurpriseFactory factory = factory(new Random(1));
        assertThrows(NullPointerException.class, () -> factory.draw(null));

        Piece inCastle = red.pieces().get(0);
        assertTrue(inCastle.inCastle());
        assertThrows(IllegalArgumentException.class, () -> factory.draw(inCastle));

        Piece finished = red.pieces().get(1);
        finished.setProgress(board.finishIndex());
        assertThrows(IllegalArgumentException.class, () -> factory.draw(finished));

        Player stranger = new Player("Stranger", PlayerColor.YELLOW, board.finishIndex());
        Piece foreign = stranger.pieces().get(0);
        foreign.setProgress(3);
        assertThrows(IllegalArgumentException.class, () -> factory.draw(foreign));
    }

    // ------------------------------------------------------------------ drawing a surprise (FR-39)

    @Test
    @DisplayName("FR-39: the factory draws the outcome of the drawn point and prepares it for that piece")
    void drawUsesTheProbabilityTable() {
        Piece mine = onTrack(red, 0, 4);
        SurprisePrompt prompt = factory(new ScriptRandom(60)).draw(mine);
        assertSame(SurpriseOutcome.SKIP_TURN, prompt.outcome());
        assertSame(mine, prompt.subject());
        assertSame(PlayerColor.RED, prompt.active());
    }

    @Test
    @DisplayName("An outcome that moves no opponent rolls no second die and offers no choice")
    void outcomesWithoutAChoiceRollNothingMore() {
        Piece mine = onTrack(red, 0, 4);
        onTrack(green, 0, 6);
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            if (outcome.needsOpponentPiece()) {
                continue;
            }
            ScriptRandom random = new ScriptRandom();
            SurprisePrompt prompt = factory(random).promptFor(outcome, mine);
            assertEquals(List.of(), random.bounds(), outcome + " must not roll a die");
            assertEquals(0, prompt.regularRoll(), outcome.name());
            assertEquals(List.of(), prompt.targets(), outcome.name());
            assertFalse(prompt.needsChoice(), outcome.name());
            assertEquals(0, prompt.backwardSteps(), outcome.name());
        }
    }

    @Test
    @DisplayName("FR-40: the push rolls exactly one Regular die, after the outcome was drawn")
    void thePushRollsOneRegularDie() {
        Piece mine = onTrack(red, 0, 4);
        onTrack(green, 0, 6);
        ScriptRandom random = new ScriptRandom(10, 4);
        SurprisePrompt prompt = factory(random).draw(mine);
        assertSame(SurpriseOutcome.OPPONENT_BACK, prompt.outcome());
        assertEquals(List.of(100, 6), random.bounds());
        assertEquals(5, prompt.regularRoll());
        assertEquals(0, random.remaining());
    }

    // ------------------------------------------------------------------ the instruction for T2-10

    @Test
    @DisplayName("FR-39: jump to the finish becomes a jump of the piece that landed")
    void jumpToFinishInstruction() {
        Piece mine = onTrack(red, 0, 7);
        SurpriseResult result = factory(new Random(1))
                .promptFor(SurpriseOutcome.JUMP_TO_FINISH, mine).resolve();
        assertSame(SurpriseAction.JUMP_TO_FINISH, result.action());
        assertSame(mine, result.piece());
        assertSame(PlayerColor.RED, result.affected());
        assertEquals(0, result.steps());
        assertTrue(result.movesPiece());
        assertFalse(result.changesTurn());
        assertFalse(result.cancelled());
        assertNull(result.note());
    }

    @Test
    @DisplayName("FR-39: back to the castle becomes a return of the piece that landed")
    void backToCastleInstruction() {
        Piece mine = onTrack(red, 0, 7);
        SurpriseResult result = factory(new Random(1))
                .promptFor(SurpriseOutcome.BACK_TO_CASTLE, mine).resolve();
        assertSame(SurpriseAction.RETURN_TO_CASTLE, result.action());
        assertSame(mine, result.piece());
        assertEquals(0, result.steps());
    }

    @Test
    @DisplayName("FR-39: three steps back becomes a move of exactly -3 for the piece that landed")
    void backThreeInstruction() {
        Piece mine = onTrack(red, 0, 7);
        SurpriseResult result = factory(new Random(1))
                .promptFor(SurpriseOutcome.BACK_THREE, mine).resolve();
        assertSame(SurpriseAction.MOVE_STEPS, result.action());
        assertSame(mine, result.piece());
        assertEquals(-3, result.steps());
        assertEquals(0, result.regularRoll());
    }

    @Test
    @DisplayName("FR-39: skip the next turn moves no piece and names the active player")
    void skipTurnInstruction() {
        Piece mine = onTrack(red, 0, 7);
        SurpriseResult result = factory(new Random(1))
                .promptFor(SurpriseOutcome.SKIP_TURN, mine).resolve();
        assertSame(SurpriseAction.SKIP_NEXT_TURN, result.action());
        assertNull(result.piece());
        assertSame(PlayerColor.RED, result.affected());
        assertEquals(0, result.steps());
        assertFalse(result.movesPiece());
        assertTrue(result.changesTurn());
        assertFalse(red.skipNextTurn(), "T2-09 only says it; T2-13 sets it");
    }

    @Test
    @DisplayName("FR-39: the extra roll moves no piece and is left to the turn")
    void extraRollInstruction() {
        Piece mine = onTrack(red, 0, 7);
        SurpriseResult result = factory(new Random(1))
                .promptFor(SurpriseOutcome.EXTRA_ROLL, mine).resolve();
        assertSame(SurpriseAction.EXTRA_ROLL, result.action());
        assertNull(result.piece());
        assertTrue(result.changesTurn());
        assertEquals(0, result.steps());
    }

    @Test
    @DisplayName("Every outcome gives an instruction, and only the two turn outcomes move no piece")
    void everyOutcomeGivesAnInstruction() {
        Piece mine = onTrack(red, 0, 7);
        onTrack(green, 0, 9);
        SurpriseFactory factory = factory(new Random(4));
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            SurprisePrompt prompt = factory.promptFor(outcome, mine);
            SurpriseResult result = prompt.needsChoice()
                    ? prompt.resolve(prompt.targets().get(0))
                    : prompt.resolve();
            assertNotNull(result.action(), outcome.name());
            assertEquals(!outcome.affectsTurnOnly(), result.movesPiece(), outcome.name());
            assertEquals(outcome.affectsTurnOnly(), result.changesTurn(), outcome.name());
            assertFalse(result.cancelled(), outcome.name());
            if (result.action() == SurpriseAction.MOVE_STEPS) {
                assertFalse(result.steps() == 0, outcome + " must move at least one step");
            } else {
                assertEquals(0, result.steps(), outcome.name());
            }
        }
    }

    // ------------------------------------------------------------------ FR-40: half a Regular die, rounded up

    @Test
    @DisplayName("FR-40: a Regular die of 1..6 pushes back 1,1,2,2,3,3 squares")
    void ceilingOfHalfTheRegularDie() {
        int[] expected = {0, 1, 1, 2, 2, 3, 3};
        for (int regular = 1; regular <= 6; regular++) {
            assertEquals(expected[regular], OpponentTargets.backwardSteps(regular), "die " + regular);
            assertEquals((regular + 1) / 2, OpponentTargets.backwardSteps(regular), "die " + regular);
        }
    }

    @Test
    @DisplayName("FR-40: a die value that no Regular die shows is refused")
    void invalidDiceValuesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> OpponentTargets.backwardSteps(0));
        assertThrows(IllegalArgumentException.class, () -> OpponentTargets.backwardSteps(7));
        assertThrows(IllegalArgumentException.class, () -> OpponentTargets.backwardSteps(-1));
    }

    @Test
    @DisplayName("FR-40: every die value gives an instruction to move the chosen piece back that many steps")
    void everyDieValuePushesTheChosenPiece() {
        Piece mine = onTrack(red, 0, 4);
        Piece victim = onTrack(green, 0, 12);
        for (int regular = 1; regular <= 6; regular++) {
            SurprisePrompt prompt = factory(new ScriptRandom(regular - 1))
                    .promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
            assertEquals(regular, prompt.regularRoll());
            assertEquals((regular + 1) / 2, prompt.backwardSteps());
            SurpriseResult result = prompt.resolve(victim);
            assertSame(SurpriseAction.MOVE_STEPS, result.action());
            assertSame(victim, result.piece());
            assertEquals(-((regular + 1) / 2), result.steps(), "die " + regular);
            assertEquals(regular, result.regularRoll());
            assertSame(PlayerColor.GREEN, result.affected(), "the outcome falls on the pushed player");
        }
    }

    // ------------------------------------------------------------------ FR-40: who may be chosen

    @Test
    @DisplayName("FR-40: the active player's own pieces are never offered")
    void ownPiecesAreNeverOffered() {
        Piece mine = onTrack(red, 0, 4);
        Piece alsoMine = onTrack(red, 1, 20);
        Piece victim = onTrack(green, 0, 6);
        List<Piece> targets = factory(new Random(1)).eligibleTargets(PlayerColor.RED);
        assertEquals(List.of(victim), targets);
        assertFalse(targets.contains(mine));
        assertFalse(targets.contains(alsoMine));
    }

    @Test
    @DisplayName("FR-40: a piece in its castle, a finished piece and a piece on its home path cannot be chosen")
    void onlySharedTrackPiecesAreOffered() {
        onTrack(red, 0, 4);
        Piece onTrack = onTrack(green, 0, 6);
        Piece onHomePath = green.pieces().get(1);
        onHomePath.setProgress(board.homeStartIndex());
        Piece finished = green.pieces().get(2);
        finished.setProgress(board.finishIndex());
        Piece inCastle = green.pieces().get(3);

        List<Piece> targets = factory(new Random(1)).eligibleTargets(PlayerColor.RED);
        assertEquals(List.of(onTrack), targets);
        assertFalse(targets.contains(onHomePath));
        assertFalse(targets.contains(finished));
        assertFalse(targets.contains(inCastle));
        assertTrue(inCastle.inCastle());
    }

    @Test
    @DisplayName("FR-40: a piece on the last square before the home path is still on the shared track")
    void theLastSharedSquareIsStillEligible() {
        onTrack(red, 0, 4);
        Piece last = onTrack(green, 0, board.homeStartIndex() - 1);
        assertEquals(List.of(last), factory(new Random(1)).eligibleTargets(PlayerColor.RED));
    }

    @Test
    @DisplayName("FR-40: a piece on its own start square may be chosen; stopping it there is T2-10's work")
    void aPieceOnItsStartSquareIsEligible() {
        onTrack(red, 0, 4);
        Piece atStart = onTrack(green, 0, 0);
        assertTrue(OpponentTargets.isEligible(atStart, board, PlayerColor.RED));
        assertEquals(List.of(atStart), factory(new Random(1)).eligibleTargets(PlayerColor.RED));
    }

    @Test
    @DisplayName("FR-40: every eligible opponent piece is offered, in player order and then piece order")
    void allEligibleOpponentPiecesAreOfferedInOrder() {
        onTrack(red, 0, 4);
        Piece green0 = onTrack(green, 0, 30);
        Piece green2 = onTrack(green, 2, 2);
        Piece blue1 = onTrack(blue, 1, 17);
        assertEquals(List.of(green0, green2, blue1), factory(new Random(1)).eligibleTargets(PlayerColor.RED));
    }

    @Test
    @DisplayName("FR-40: the leading opponent piece is not chosen for the player, unlike in the prototype")
    void theLeaderIsNotChosenAutomatically() {
        Piece mine = onTrack(red, 0, 4);
        Piece leader = onTrack(green, 0, 40);
        Piece trailing = onTrack(blue, 0, 2);

        SurprisePrompt prompt = factory(new ScriptRandom(3)).promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        assertTrue(prompt.needsChoice(), "the player must still choose (FR-40)");
        assertEquals(List.of(leader, trailing), prompt.targets());

        SurpriseResult result = prompt.resolve(trailing);
        assertSame(trailing, result.piece(), "the piece the player chose, not the leader");
        assertSame(PlayerColor.BLUE, result.affected());
    }

    @Test
    @DisplayName("The list of pieces to choose from cannot be changed from outside")
    void theTargetListIsReadOnly() {
        Piece mine = onTrack(red, 0, 4);
        Piece victim = onTrack(green, 0, 6);
        List<Piece> targets = factory(new Random(1)).eligibleTargets(PlayerColor.RED);
        assertThrows(UnsupportedOperationException.class, () -> targets.add(mine));
        assertThrows(UnsupportedOperationException.class, targets::clear);
        assertEquals(List.of(victim), targets);
    }

    @Test
    @DisplayName("The eligibility rules need a board and an active colour")
    void eligibilityNeedsItsArguments() {
        assertThrows(NullPointerException.class,
                () -> OpponentTargets.eligible(null, board, PlayerColor.RED));
        assertThrows(NullPointerException.class,
                () -> OpponentTargets.eligible(players, null, PlayerColor.RED));
        assertThrows(NullPointerException.class, () -> OpponentTargets.eligible(players, board, null));
        assertFalse(OpponentTargets.isEligible(null, board, PlayerColor.RED), "no piece is not eligible");
    }

    // ------------------------------------------------------------------ FR-40: a wrong or missing choice

    @Test
    @DisplayName("FR-40: a piece of the active player is refused as a target")
    void ownPieceIsRefusedAsTarget() {
        Piece mine = onTrack(red, 0, 4);
        Piece alsoMine = onTrack(red, 1, 9);
        onTrack(green, 0, 6);
        SurprisePrompt prompt = factory(new ScriptRandom(3)).promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        assertThrows(IllegalArgumentException.class, () -> prompt.resolve(alsoMine));
        assertThrows(IllegalArgumentException.class,
                () -> OpponentTargets.requireEligible(alsoMine, board, PlayerColor.RED));
    }

    @Test
    @DisplayName("FR-40: an opponent piece that is not on the shared track is refused as a target")
    void unreachableOpponentPieceIsRefusedAsTarget() {
        Piece mine = onTrack(red, 0, 4);
        onTrack(green, 0, 6);
        Piece inCastle = blue.pieces().get(0);
        Piece onHomePath = blue.pieces().get(1);
        onHomePath.setProgress(board.homeStartIndex() + 1);

        SurprisePrompt prompt = factory(new ScriptRandom(3)).promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        assertThrows(IllegalArgumentException.class, () -> prompt.resolve(inCastle));
        assertThrows(IllegalArgumentException.class, () -> prompt.resolve(onHomePath));
        assertThrows(IllegalArgumentException.class,
                () -> OpponentTargets.requireEligible(onHomePath, board, PlayerColor.RED));
        assertThrows(NullPointerException.class,
                () -> OpponentTargets.requireEligible(null, board, PlayerColor.RED));
    }

    @Test
    @DisplayName("FR-40: no target at all is refused; the prompt must be resolved the other way")
    void aMissingTargetIsRefused() {
        Piece mine = onTrack(red, 0, 4);
        onTrack(green, 0, 6);
        SurprisePrompt prompt = factory(new ScriptRandom(3)).promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        assertThrows(NullPointerException.class, () -> prompt.resolve(null));
        assertThrows(IllegalStateException.class, prompt::resolve);
    }

    @Test
    @DisplayName("An outcome that chooses nobody cannot be resolved with a piece")
    void outcomesWithoutAChoiceRefuseATarget() {
        Piece mine = onTrack(red, 0, 4);
        Piece victim = onTrack(green, 0, 6);
        SurprisePrompt prompt = factory(new Random(1)).promptFor(SurpriseOutcome.BACK_THREE, mine);
        assertThrows(IllegalStateException.class, () -> prompt.resolve(victim));
    }

    @Test
    @DisplayName("FR-40: with no opponent piece on the shared track the surprise does nothing")
    void noEligibleOpponentCancelsThePush() {
        Piece mine = onTrack(red, 0, 4);
        green.pieces().get(0).setProgress(board.homeStartIndex());
        blue.pieces().get(0).setProgress(board.finishIndex());

        SurprisePrompt prompt = factory(new ScriptRandom(5)).promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        assertEquals(List.of(), prompt.targets());
        assertFalse(prompt.needsChoice());
        assertEquals(6, prompt.regularRoll(), "the die was rolled before the pieces were counted");
        assertEquals(3, prompt.backwardSteps());

        SurpriseResult result = prompt.resolve();
        assertSame(SurpriseAction.NOTHING, result.action());
        assertTrue(result.cancelled());
        assertNull(result.piece());
        assertFalse(result.movesPiece());
        assertEquals(0, result.steps());
        assertEquals(6, result.regularRoll());
        assertSame(PlayerColor.RED, result.affected());
        assertEquals(SurprisePrompt.NO_TARGET_NOTE, result.note());
    }

    // ------------------------------------------------------------------ building a prompt or result by hand

    @Test
    @DisplayName("A prompt refuses parts that contradict each other")
    void promptRefusesContradictions() {
        Piece mine = onTrack(red, 0, 4);
        Piece victim = onTrack(green, 0, 6);
        Piece inCastle = green.pieces().get(1);

        assertThrows(NullPointerException.class,
                () -> new SurprisePrompt(null, mine, PlayerColor.RED, 0, List.of()));
        assertThrows(NullPointerException.class,
                () -> new SurprisePrompt(SurpriseOutcome.BACK_THREE, mine, PlayerColor.RED, 0, null));
        assertThrows(IllegalArgumentException.class, () ->
                new SurprisePrompt(SurpriseOutcome.BACK_THREE, mine, PlayerColor.GREEN, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new SurprisePrompt(SurpriseOutcome.BACK_THREE, inCastle, PlayerColor.GREEN, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new SurprisePrompt(SurpriseOutcome.BACK_THREE, mine, PlayerColor.RED, 4, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new SurprisePrompt(SurpriseOutcome.BACK_THREE, mine, PlayerColor.RED, 0, List.of(victim)));
        assertThrows(IllegalArgumentException.class, () ->
                new SurprisePrompt(SurpriseOutcome.OPPONENT_BACK, mine, PlayerColor.RED, 0, List.of(victim)));
        assertThrows(IllegalArgumentException.class, () ->
                new SurprisePrompt(SurpriseOutcome.OPPONENT_BACK, mine, PlayerColor.RED, 7, List.of(victim)));
        assertThrows(IllegalArgumentException.class, () ->
                new SurprisePrompt(SurpriseOutcome.OPPONENT_BACK, mine, PlayerColor.RED, 3, List.of(mine)));
        assertThrows(IllegalArgumentException.class, () ->
                new SurprisePrompt(SurpriseOutcome.OPPONENT_BACK, mine, PlayerColor.RED, 3, List.of(inCastle)));
    }

    @Test
    @DisplayName("A result refuses parts that contradict each other")
    void resultRefusesContradictions() {
        Piece mine = onTrack(red, 0, 4);
        Piece victim = onTrack(green, 0, 6);

        assertThrows(NullPointerException.class, () -> new SurpriseResult(
                null, mine, PlayerColor.RED, SurpriseAction.JUMP_TO_FINISH, 0, 0, null));
        assertThrows(NullPointerException.class, () -> new SurpriseResult(
                SurpriseOutcome.JUMP_TO_FINISH, mine, PlayerColor.RED, null, 0, 0, null));
        // a moving action without a piece, and a non-moving action with one
        assertThrows(IllegalArgumentException.class, () -> new SurpriseResult(
                SurpriseOutcome.JUMP_TO_FINISH, null, PlayerColor.RED,
                SurpriseAction.JUMP_TO_FINISH, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new SurpriseResult(
                SurpriseOutcome.SKIP_TURN, mine, PlayerColor.RED, SurpriseAction.SKIP_NEXT_TURN, 0, 0, null));
        // the piece must belong to the affected player
        assertThrows(IllegalArgumentException.class, () -> new SurpriseResult(
                SurpriseOutcome.BACK_THREE, victim, PlayerColor.RED, SurpriseAction.MOVE_STEPS, -3, 0, null));
        // MOVE_STEPS of 0 steps, and steps on an action that moves none
        assertThrows(IllegalArgumentException.class, () -> new SurpriseResult(
                SurpriseOutcome.BACK_THREE, mine, PlayerColor.RED, SurpriseAction.MOVE_STEPS, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new SurpriseResult(
                SurpriseOutcome.JUMP_TO_FINISH, mine, PlayerColor.RED,
                SurpriseAction.JUMP_TO_FINISH, -3, 0, null));
        // a die value no Regular die shows
        assertThrows(IllegalArgumentException.class, () -> new SurpriseResult(
                SurpriseOutcome.BACK_THREE, mine, PlayerColor.RED, SurpriseAction.MOVE_STEPS, -3, 9, null));
        // NOTHING without a reason, and a reason on something that does happen
        assertThrows(IllegalArgumentException.class, () -> new SurpriseResult(
                SurpriseOutcome.OPPONENT_BACK, null, PlayerColor.RED, SurpriseAction.NOTHING, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new SurpriseResult(
                SurpriseOutcome.BACK_THREE, mine, PlayerColor.RED, SurpriseAction.MOVE_STEPS, -3, 0, "why"));
    }

    @Test
    @DisplayName("A result reads clearly in a log")
    void resultReadsClearly() {
        Piece mine = onTrack(red, 0, 4);
        Piece victim = onTrack(green, 0, 6);
        SurpriseResult push = factory(new ScriptRandom(4))
                .promptFor(SurpriseOutcome.OPPONENT_BACK, mine).resolve(victim);
        String text = push.toString();
        assertTrue(text.contains("OPPONENT_BACK"), text);
        assertTrue(text.contains("MOVE_STEPS"), text);
        assertTrue(text.contains("-3"), text);
        assertTrue(text.contains("5"), text);
    }

    // ------------------------------------------------------------------ repeatable games

    @Test
    @DisplayName("NFR-13: two games with the same seed produce the same surprises and the same dice")
    void sameSeedGivesTheSameSurprises() {
        onTrack(red, 0, 4);
        onTrack(green, 0, 6);
        assertEquals(describeDraws(2026), describeDraws(2026));
        assertFalse(describeDraws(1).equals(describeDraws(2)));
    }

    private List<String> describeDraws(long seed) {
        SurpriseFactory factory = factory(new Random(seed));
        Piece mine = red.pieces().get(0);
        List<String> drawn = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            SurprisePrompt prompt = factory.draw(mine);
            drawn.add(prompt.outcome().name() + "/" + prompt.regularRoll());
        }
        return drawn;
    }

    @Test
    @DisplayName("The factory reports the players of the game without letting them be changed")
    void factoryKeepsItsOwnPlayerList() {
        SurpriseFactory factory = factory(new Random(1));
        assertEquals(3, factory.players().size());
        assertThrows(UnsupportedOperationException.class, () -> factory.players().remove(0));
        players.clear();
        assertEquals(3, factory.players().size(), "a copy was taken when the factory was built");
    }
}
