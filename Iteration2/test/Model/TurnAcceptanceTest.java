package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Acceptance tests of US-1.3 "roll in turn and see my movable pieces" (Task 15: FR-15, FR-16 partial,
 * FR-17 partial, FR-21..FR-24), written by the Product Owner. Each test plays through the {@link GameModel}
 * facade with the real rule classes, as the controller will.
 *
 * <p>Automated here: the model part. <b>Still manual:</b> seeing the highlight and the messages on the screen
 * (FR-23, FR-24 in the view), which needs N02 and T13.
 */
class TurnAcceptanceTest {

    /** Dice whose next values the test chooses. */
    private static final class ScriptDice implements Dice {
        final Deque<Integer> next = new ArrayDeque<>();

        @Override
        public DiceRoll roll(Difficulty level) {
            return DiceRoll.regularOnly(next.remove());
        }
    }

    private final ScriptDice dice = new ScriptDice();
    private final List<GameEvent> events = new ArrayList<>();

    private GameModel game(int playerCount) {
        PlayerColor[] colors = PlayerColor.values();
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            setup.add(new GameSettings.PlayerSetup("P" + (i + 1), colors[i]));
        }
        GameModel model = new GameModel(new GameSettings(Difficulty.EASY, setup), new Random(5), dice, () -> 0L);
        model.addListener(events::add);
        return model;
    }

    private DiceRoll roll(GameModel model, int value) {
        dice.next.add(value);
        return model.roll();
    }

    private long count(GameEvent.Type type) {
        return events.stream().filter(e -> e.type() == type).count();
    }

    @Test
    @DisplayName("T15 FR-15: players take turns in setup order and the order wraps around, for 2-4 players")
    void setupOrder() {
        for (int count = 2; count <= 4; count++) {
            GameModel model = game(count);
            for (int turn = 0; turn < count * 2 + 1; turn++) {
                assertSame(model.players().get(turn % count), model.currentPlayer(), count + " players, turn " + turn);
                roll(model, 1);
            }
        }
    }

    @Test
    @DisplayName("T15 FR-16, FR-17: on Easy the real dice show 1-6, no special die, and movement equals the die")
    void realDiceOnEasy() {
        List<GameSettings.PlayerSetup> setup = List.of(new GameSettings.PlayerSetup("A", PlayerColor.RED),
                new GameSettings.PlayerSetup("B", PlayerColor.BLUE));
        GameModel model = new GameModel(new GameSettings(Difficulty.EASY, setup), new Random(21));
        Set<Integer> seen = new java.util.HashSet<>();
        for (int i = 0; i < 300 && !model.isGameOver(); i++) {
            DiceRoll r = model.roll();
            assertTrue(r.regular() >= 1 && r.regular() <= 6);
            assertFalse(r.hasSpecial());
            assertEquals(r.regular(), r.movement());
            seen.add(r.regular());
            if (!model.legalMoves().isEmpty()) {
                model.move(model.legalMoves().keySet().iterator().next());
            }
        }
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), seen);
    }

    @Test
    @DisplayName("T15 FR-23: with every piece in the castle, a roll of 1-5 passes the turn with a message")
    void noMovePassesWithMessage() {
        GameModel model = game(2);
        for (int value = 1; value <= 5; value++) {
            Player roller = model.currentPlayer();
            events.clear();
            DiceRoll r = roll(model, value);
            assertEquals(value, r.regular());
            assertTrue(model.legalMoves().isEmpty());
            GameEvent passed = events.stream().filter(e -> e.type() == GameEvent.Type.TURN_PASSED).findFirst().orElseThrow();
            assertSame(roller, passed.player());
            assertNotNull(passed.message());
            assertFalse(passed.message().isBlank());
            assertFalse(model.currentPlayer() == roller, "the turn passed");
        }
    }

    @Test
    @DisplayName("T15 FR-21, FR-24: a 6 highlights the pieces; after the move the same player rolls again")
    void sixHighlightsThenExtraRoll() {
        GameModel model = game(2);
        Player first = model.currentPlayer();
        roll(model, 6);
        assertEquals(Set.copyOf(first.pieces()), model.legalMoves().keySet(), "FR-24: all four castle pieces");
        assertTrue(model.hasBonusRoll());
        model.move(first.pieces().get(0));
        assertSame(first, model.currentPlayer(), "FR-21");
        assertEquals(TurnPhase.AWAITING_ROLL, model.phase());
        assertTrue(model.legalMoves().isEmpty(), "nothing is highlighted before the next roll");
    }

    @Test
    @DisplayName("T15 FR-24: only the pieces with a legal move are highlighted")
    void onlyMovablePiecesAreHighlighted() {
        GameModel model = game(2);
        Player first = model.currentPlayer();
        first.pieces().get(0).setProgress(4);
        first.pieces().get(1).setProgress(model.board().finishIndex() - 1);
        first.pieces().get(2).setProgress(model.board().finishIndex());
        roll(model, 3);
        assertEquals(Set.of(first.pieces().get(0)), model.legalMoves().keySet());
    }

    @Test
    @DisplayName("T15 FR-21: a 6 with which no piece can move still gives the extra roll (staff Q38)")
    void sixWithoutMoveKeepsTurn() {
        GameModel model = game(2);
        Player first = model.currentPlayer();
        int finish = model.board().finishIndex();
        for (Piece piece : first.pieces()) {
            piece.setProgress(finish - 1);
        }
        roll(model, 6);
        assertTrue(model.legalMoves().isEmpty());
        assertTrue(model.hasBonusRoll());
        assertSame(first, model.currentPlayer());
        assertEquals(0, count(GameEvent.Type.TURN_PASSED));
        roll(model, 4);
        assertEquals(1, count(GameEvent.Type.TURN_PASSED), "no move and no extra roll now (FR-23)");
        assertFalse(model.currentPlayer() == first);
    }

    @Test
    @DisplayName("T15 FR-22: the third 6 in a row moves nothing, tells the player and passes the turn")
    void thirdSixIsCancelled() {
        GameModel model = game(2);
        Player first = model.currentPlayer();
        Piece a = first.pieces().get(0);
        Piece b = first.pieces().get(1);
        roll(model, 6);
        model.move(a);
        roll(model, 6);
        model.move(b);
        events.clear();

        DiceRoll third = roll(model, 6);

        assertTrue(third.cancelled());
        assertEquals(0, a.progress());
        assertEquals(0, b.progress());
        assertTrue(first.pieces().get(2).inCastle());
        GameEvent passed = events.stream().filter(e -> e.type() == GameEvent.Type.TURN_PASSED).findFirst().orElseThrow();
        assertFalse(passed.message().isBlank());
        assertFalse(model.currentPlayer() == first);
    }

    @Test
    @DisplayName("T15 FR-22: two 6s, a non-6 with a move, then a new turn: the next player's 6s start from zero")
    void sixCountRestartsForNextPlayer() {
        GameModel model = game(2);
        Player first = model.currentPlayer();
        Player second = model.players().get(1);
        roll(model, 6);
        model.move(first.pieces().get(0));
        roll(model, 6);
        model.move(first.pieces().get(0));
        roll(model, 2);
        model.move(first.pieces().get(0));
        assertSame(second, model.currentPlayer());
        assertFalse(roll(model, 6).cancelled());
        model.move(second.pieces().get(0));
        assertFalse(roll(model, 6).cancelled());
    }
}
