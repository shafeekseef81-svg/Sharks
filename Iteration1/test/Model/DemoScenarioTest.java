package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Fallback scenario of the Iteration 1 demonstration (Task 29). It plays one repeatable game through the
 * {@link GameModel} facade with chosen die values and prints what happens, step by step. The transcript can be
 * shown if random play in the real application takes too long.
 *
 * <p>Part 1 is played from the start of a game. In part 2 the pieces are placed near the end of the game by the
 * test, and the transcript says so. This exists only in the test tree: the product has no demo hooks (SDP 5.3.1).
 *
 * <p>See {@code docs/T29_Demo_Script.md}.
 */
class DemoScenarioTest {

    private static final class ScriptDice implements Dice {
        final Deque<Integer> next = new ArrayDeque<>();

        @Override
        public DiceRoll roll(Difficulty level) {
            return DiceRoll.regularOnly(next.remove());
        }
    }

    private final ScriptDice dice = new ScriptDice();
    private GameModel game;
    private Player red;
    private Player green;
    private Player yellow;

    private static void say(String text) {
        System.out.println("[DEMO] " + text);
    }

    private void pass(Player expected, int value) {
        assertSame(expected, game.currentPlayer());
        dice.next.add(value);
        game.roll();
        assertTrue(game.legalMoves().isEmpty());
        say(expected.name() + " rolls " + value + ": no legal move, the turn passes (FR-23)");
    }

    private MoveResult play(Player expected, int value, Piece piece) {
        assertSame(expected, game.currentPlayer());
        int from = piece.progress();
        dice.next.add(value);
        game.roll();
        MoveResult r = game.move(piece);
        say(expected.name() + " rolls " + value + ": " + piece + " moves from " + describe(from) + " to "
                + describe(piece.progress()) + (r.captures().isEmpty() ? "" : "  ** CAPTURE: "
                + r.captures().get(0).piece() + " goes back to its castle **"));
        return r;
    }

    private String describe(int progress) {
        if (progress == Piece.IN_CASTLE) {
            return "castle";
        }
        if (progress == game.board().finishIndex()) {
            return "FINISH";
        }
        return "step " + progress + (progress >= game.board().homeStartIndex() ? " (home path)" : "");
    }

    private void setUpGame() {
        List<GameSettings.PlayerSetup> setup = List.of(new GameSettings.PlayerSetup("Red", PlayerColor.RED),
                new GameSettings.PlayerSetup("Green", PlayerColor.GREEN),
                new GameSettings.PlayerSetup("Yellow", PlayerColor.YELLOW));
        GameSettings settings = new GameSettings(Difficulty.EASY, setup);
        for (long seed = 0; seed < 5000; seed++) {
            GameModel candidate = new GameModel(settings, new Random(seed), dice, () -> 0L);
            Board b = candidate.board();
            if (b.squareAt(b.cellAt(PlayerColor.GREEN, 3)).type() == SquareType.TRACK) {
                game = candidate;
                red = game.players().get(0);
                green = game.players().get(1);
                yellow = game.players().get(2);
                say("Game set up: 3 players, Easy, board seed " + seed + " (FR-04..FR-10)");
                return;
            }
        }
        throw new AssertionError("No suitable board");
    }

    @Test
    @DisplayName("T29: the fallback demonstration runs from setup to the final ranking")
    void demonstration() {
        say("=== PART 1: played from the start of the game ===");
        setUpGame();
        Piece hunter = red.pieces().get(0);
        Piece prey = green.pieces().get(0);

        pass(red, 3);
        pass(green, 2);
        pass(yellow, 1);

        MoveResult exit = play(red, 6, hunter);
        assertEquals(MoveResult.Kind.EXIT_CASTLE, exit.kind());
        say("  A 6 left the castle, and the same player rolls again (FR-19, FR-21)");
        play(red, 6, hunter);
        play(red, 4, hunter);
        play(green, 6, prey);
        play(green, 3, prey);
        pass(yellow, 5);

        MoveResult capture = play(red, 6, hunter);
        assertEquals(1, capture.captures().size());
        assertTrue(prey.inCastle());
        say("  The capturing 6 still gives its one extra roll (FR-21, issue 13)");
        assertSame(red, game.currentPlayer());
        play(red, 1, hunter);
        pass(green, 5);
        say("  The captured piece needs a new 6 to leave its castle (FR-19, FR-26)");
        pass(yellow, 4);

        say("=== PART 2: the test places pieces near the end, to save time ===");
        Board board = game.board();
        int star = 2;
        while (board.squareAt(board.cellAt(PlayerColor.RED, star)).type() != SquareType.PROTECTION) {
            star++;
        }
        hunter.setProgress(star - 2);
        Piece safe = green.pieces().get(1);
        safe.setProgress(board.path(PlayerColor.GREEN).indexOf(board.cellAt(PlayerColor.RED, star)));
        say("Placed: " + safe + " on a protection square; " + hunter + " two squares before it");
        MoveResult shielded = play(red, 2, hunter);
        assertTrue(shielded.captures().isEmpty());
        assertTrue(safe.onBoard());
        say("  Protection square: no capture, and both pieces stay on it (FR-27)");
        safe.setProgress(Piece.IN_CASTLE);
        say("Placed: " + safe + " back in its castle, so that Green's next turns pass quickly");
        pass(green, 1);
        pass(yellow, 2);

        int finish = board.finishIndex();
        hunter.setProgress(board.homeStartIndex() - 2);
        say("Placed: " + hunter + " two squares before its home path");
        play(red, 4, hunter);
        assertTrue(hunter.progress() >= board.homeStartIndex());
        say("  The piece turned into its own home path (FR-25, FR-33)");
        pass(green, 1);
        pass(yellow, 2);

        for (int i = 1; i < 4; i++) {
            red.pieces().get(i).setProgress(finish);
        }
        hunter.setProgress(finish - 3);
        say("Placed: three Red pieces finished; " + hunter + " three steps from the finish");
        pass(red, 5);
        say("  5 would overshoot the finish, so the piece cannot move (FR-31)");
        pass(green, 1);
        pass(yellow, 2);
        MoveResult finishing = play(red, 3, hunter);
        assertTrue(finishing.playerFinished());
        say("  Exact roll: Red has finished and takes rank 1 (FR-31, FR-43)");

        for (Piece piece : green.pieces()) {
            piece.setProgress(piece == safe ? finish - 1 : finish);
        }
        say("Placed: three Green pieces finished; " + safe + " one step from the finish");
        pass(green, 3);
        pass(yellow, 2);
        MoveResult last = play(green, 1, safe);
        assertTrue(last.gameOver());
        assertTrue(game.isGameOver());

        say("=== RESULT (FR-44, FR-45) ===");
        List<Player> ranking = game.ranking();
        assertEquals(List.of(red, green, yellow), ranking);
        for (Player p : ranking) {
            say("Rank " + p.rank() + ": " + p.name());
        }
        say("The game duration is not part of this transcript: GameClock is Task 24.");
    }
}
