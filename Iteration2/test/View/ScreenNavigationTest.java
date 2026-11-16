package View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import View.components.Icons;

/**
 * Checks the way the player moves between screens and leaves the game (FR-01, FR-03, Task 01).
 *
 * <h2>What these tests cover, and what they do not</h2>
 *
 * <p><b>Covered here (the {@code View} package, which Task 01 owns).</b> Every screen offers a Back
 * option; the Back option runs the action that was registered on it through the public API of the screen
 * ({@code setBackAction}, {@code setActions}); Enter is bound to the right button of each screen; and the
 * confirmation card runs the matching action for each of its buttons, including the case where the player
 * decides not to leave. In short: each screen offers the right controls and calls back correctly.
 *
 * <p><b>Not covered here: the wiring between the screens.</b> Which screen actually opens, that leaving a
 * running game really returns to the main menu, and that Exit really closes the window are decided in
 * {@code Controller.NavigationController}. That class belongs to another team member (Rakan, Task 02), so
 * Task 01 must not change it, and it owns a real {@link javax.swing.JFrame}, which cannot be created in a
 * headless test. <b>That half is verified by the manual smoke test</b> required for a Light review
 * (SDP 5.3.4): main menu, New Game, Back, start a game, Main Menu, confirm, Exit, confirm.
 *
 * <p>So a green run here means every screen behaves correctly on its own. It does not by itself prove
 * FR-03 end to end; the manual smoke test completes it.
 *
 * <p>No {@link javax.swing.JFrame} is created, so the tests run headless as well. Every Swing component
 * is built and used on the event dispatch thread.
 *
 * <p>Owner: Mohamed Zereny (Task 01).
 */
class ScreenNavigationTest {

    // ------------------------------------------------------------------ FR-03: Back on every screen

    @Test
    @DisplayName("FR-03: the setup screen offers Back, and it runs the action of the controller")
    void setupScreenHasBack() {
        onEdt(() -> {
            GameSetupView setup = new GameSetupView();
            JButton back = button(setup, "setup.back");
            assertEquals("Main Menu", back.getText(), "the Back option must say where it leads");
            assertTrue(back.isEnabled(), "the Back option must be usable at any time (FR-03)");
            List<String> fired = new ArrayList<>();
            setup.setBackAction(() -> fired.add("back"));
            back.doClick();
            assertEquals(List.of("back"), fired, "Back must run the action the controller registered");
        });
    }

    @Test
    @DisplayName("FR-03: the game screen offers Back, and it runs the action of the controller")
    void gameScreenHasBack() {
        onEdt(() -> {
            GameBoardView game = new GameBoardView();
            JButton back = button(game, "game.back");
            assertEquals("Main Menu", back.getText(), "the Back option must say where it leads");
            assertTrue(back.isEnabled(), "the Back option must be usable at any time (FR-03)");
            List<String> fired = new ArrayList<>();
            game.setActions(() -> fired.add("roll"), () -> fired.add("back"));
            back.doClick();
            assertEquals(List.of("back"), fired, "Back must run the leave action, not the roll action");
        });
    }

    @Test
    @DisplayName("FR-03: the results screen offers Back to the main menu and Play Again")
    void resultsScreenHasBack() {
        onEdt(() -> {
            GameResultsView results = new GameResultsView();
            JButton menu = button(results, "results.menu");
            assertEquals("Main Menu", menu.getText(), "the Back option must say where it leads");
            assertTrue(menu.isEnabled(), "the Back option must be usable at any time (FR-03)");
            List<String> fired = new ArrayList<>();
            results.setActions(() -> fired.add("playAgain"), () -> fired.add("menu"));
            button(results, "results.playAgain").doClick();
            menu.doClick();
            assertEquals(List.of("playAgain", "menu"), fired,
                    "each button of the results screen must run its own action");
        });
    }

    // ------------------------------------------------------------------ FR-03: leaving and exiting

    @Test
    @DisplayName("FR-03: leaving a running game is confirmed first, and says the game is not saved")
    void leavingAGameIsConfirmed() {
        onEdt(() -> {
            // The same card the navigation controller shows for Leave Game.
            List<String> fired = new ArrayList<>();
            ConfirmDialog dialog = new ConfirmDialog("Leave this game?",
                    "The game is still running. If you leave now, it will not be saved to the game history.",
                    "Leave Game", "Keep Playing", true, Icons.Type.WARNING,
                    () -> fired.add("left"), () -> fired.add("stayed"));
            JButton leave = button(dialog, "dialog.confirm");
            JButton keepPlaying = button(dialog, "dialog.cancel");
            assertEquals("Leave Game", leave.getText());
            assertEquals("Keep Playing", keepPlaying.getText(),
                    "the player must be able to go back to the game (FR-03)");

            keepPlaying.doClick();
            assertEquals(List.of("stayed"), fired, "Keep Playing must not leave the game");
            leave.doClick();
            assertEquals(List.of("stayed", "left"), fired, "Leave Game must leave the game");
        });
    }

    @Test
    @DisplayName("FR-01: closing the application is confirmed, and Cancel does not close it")
    void exitIsConfirmed() {
        onEdt(() -> {
            // The same card the navigation controller shows for Exit.
            List<String> fired = new ArrayList<>();
            ConfirmDialog dialog = new ConfirmDialog("Exit Educational Ludo?", "The application will close.",
                    "Exit", "Cancel", false, Icons.Type.EXIT,
                    () -> fired.add("exited"), () -> fired.add("cancelled"));
            button(dialog, "dialog.cancel").doClick();
            assertEquals(List.of("cancelled"), fired, "Cancel must not close the application");
            button(dialog, "dialog.confirm").doClick();
            assertEquals(List.of("cancelled", "exited"), fired, "Exit must close the application");
        });
    }

    @Test
    @DisplayName("FR-02: an information card has one button only, so a notice cannot be confirmed by mistake")
    void informationCardHasOneButton() {
        onEdt(() -> {
            List<String> fired = new ArrayList<>();
            ConfirmDialog dialog = new ConfirmDialog("Game History",
                    "The game history is part of Iteration 3 of the project.", "OK", null, false,
                    Icons.Type.INFO, () -> fired.add("ok"), () -> fired.add("closed"));
            assertNotNull(dialog.confirmButton(), "an information card needs an OK button");
            assertEquals(null, dialog.cancelButton(), "an information card must not have a second button");
            dialog.confirmButton().doClick();
            assertEquals(List.of("ok"), fired);
        });
    }

    // ------------------------------------------------------------------ keyboard

    @Test
    @DisplayName("FR-01: Enter is bound to the main action of every screen that has one")
    void enterIsBoundToTheMainAction() {
        onEdt(() -> {
            GameSetupView setup = new GameSetupView();
            assertSame(button(setup, "setup.start"), setup.defaultButton(),
                    "on the setup screen Enter must start the game");
            GameResultsView results = new GameResultsView();
            assertSame(button(results, "results.playAgain"), results.defaultButton(),
                    "on the results screen Enter must play again");
            // The game screen has no default button on purpose: Space rolls the dice, and Enter must not
            // roll by accident while the player is choosing a piece.
            assertEquals(null, new GameBoardView().defaultButton(),
                    "the game screen must not bind Enter to a button");
        });
    }

    // ------------------------------------------------------------------ helpers

    private static JButton button(Component root, String name) {
        Component c = find(root, name);
        assertNotNull(c, "no component named " + name);
        assertTrue(c instanceof JButton, name + " is not a button");
        return (JButton) c;
    }

    private static Component find(Component c, String name) {
        if (name.equals(c.getName())) {
            return c;
        }
        if (c instanceof Container container) {
            for (Component kid : container.getComponents()) {
                Component hit = find(kid, name);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    /** Runs the body on the Swing event dispatch thread and reports its failures as its own. */
    private static void onEdt(Runnable body) {
        try {
            SwingUtilities.invokeAndWait(body);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new AssertionError(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for the event dispatch thread", e);
        }
    }
}
