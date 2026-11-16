package Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Difficulty;
import Model.GameSettings;
import Model.PlayerColor;
import View.ConfirmDialog;
import View.GameBoardView;
import View.components.ModalOverlay;

/**
 * Screen routing of {@link NavigationController} (Task 02; SRS FR-01, FR-02 partial, FR-03, FR-08, FR-09).
 *
 * <p>Every test drives the real screens through their named buttons, on the Event Dispatch Thread, and never
 * shows the window. The exit handler is replaced, so no test can close the JVM. Owner: Rakan Ziyan.
 */
class NavigationControllerTest {

    private NavigationController nav;
    private final AtomicInteger exits = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display: NavigationController owns a JFrame");
        onEdt(() -> {
            nav = new NavigationController();
            nav.setExitHandler(exits::incrementAndGet);
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (nav != null) {
            onEdt(() -> {
                ModalOverlay.closeAll();
                nav.frame().dispose();
            });
        }
    }

    private static GameSettings easyGame() {
        return new GameSettings(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Ann", PlayerColor.RED),
                new GameSettings.PlayerSetup("Ben", PlayerColor.GREEN)));
    }

    @Test
    @DisplayName("FR-01: the application opens on the main menu with no dialog and no game")
    void startsOnTheMainMenu() throws Exception {
        onEdt(() -> {
            assertEquals(NavigationController.MENU, nav.currentScreen());
            assertEquals(0, ModalOverlay.openCount());
            assertNull(nav.gameView());
            assertSame(nav.menuView().defaultButton(), nav.frame().getRootPane().getDefaultButton());
        });
    }

    @Test
    @DisplayName("FR-02, FR-03: New Game opens the setup screen and its Back returns to the menu")
    void newGameAndBack() throws Exception {
        onEdt(() -> {
            click(nav.menuView(), "menu.newGame");
            assertEquals(NavigationController.SETUP, nav.currentScreen());
            assertSame(nav.setupView().defaultButton(), nav.frame().getRootPane().getDefaultButton());
            click(nav.setupView(), "setup.back");
            assertEquals(NavigationController.MENU, nav.currentScreen());
        });
    }

    @Test
    @DisplayName("FR-02 partial: Game History and Question Management show an Iteration 3 notice and stay on the menu")
    void laterIterationOptionsShowANotice() throws Exception {
        for (String option : new String[] {"menu.history", "menu.questions"}) {
            onEdt(() -> {
                click(nav.menuView(), option);
                assertEquals(NavigationController.MENU, nav.currentScreen(), option);
                assertEquals(1, ModalOverlay.openCount(), option);
                ConfirmDialog notice = (ConfirmDialog) ModalOverlay.top().card();
                assertNull(notice.cancelButton(), option + ": a notice has one button only");
                assertTrue(texts(notice).contains("Iteration 3"), option);
                notice.confirmButton().doClick();
                assertEquals(0, ModalOverlay.openCount(), option);
                assertSame(nav.menuView().defaultButton(), nav.frame().getRootPane().getDefaultButton(),
                        option + ": Enter goes back to New Game once the notice is closed");
            });
        }
    }

    @Test
    @DisplayName("FR-08: invalid settings keep the player on the setup screen and start no game")
    void invalidSettingsStartNoGame() throws Exception {
        onEdt(() -> {
            click(nav.menuView(), "menu.newGame");
            nav.setupView().setPlayerCount(2);
            nav.setupView().setPlayerName(0, "Same");
            nav.setupView().setPlayerName(1, "Same");
            click(nav.setupView(), "setup.start");
            assertEquals(NavigationController.SETUP, nav.currentScreen());
            assertNull(nav.gameView());
            assertFalse(nav.setupView().visibleErrors().isEmpty());
        });
    }

    @Test
    @DisplayName("FR-09: a valid game opens the board screen; a new game replaces the old screen instead of adding one")
    void startGameReplacesTheBoardScreen() throws Exception {
        onEdt(() -> {
            int screensBefore = nav.frame().getContentPane().getComponentCount();
            nav.startGame(easyGame(), new Random(1));
            GameBoardView first = nav.gameView();
            assertNotNull(first);
            assertEquals(NavigationController.GAME, nav.currentScreen());
            assertEquals(easyGame().players(), nav.game().model().settings().players());
            assertEquals(screensBefore + 1, nav.frame().getContentPane().getComponentCount());

            nav.startGame(easyGame(), new Random(2));
            assertNotSame(first, nav.gameView());
            assertEquals(screensBefore + 1, nav.frame().getContentPane().getComponentCount(),
                    "the old board screen is removed, so repeated games do not pile up");
            assertNull(first.getParent());
        });
    }

    @Test
    @DisplayName("FR-03: leaving a running game asks first; Keep Playing stays, Leave Game returns to the menu")
    void leavingAGameIsConfirmed() throws Exception {
        onEdt(() -> {
            nav.startGame(easyGame(), new Random(1));
            click(nav.gameView(), "game.back");
            assertEquals(1, ModalOverlay.openCount());
            ConfirmDialog dialog = (ConfirmDialog) ModalOverlay.top().card();
            assertTrue(texts(dialog).contains("not be saved"));
            dialog.cancelButton().doClick();
            assertEquals(NavigationController.GAME, nav.currentScreen());
            assertEquals(0, ModalOverlay.openCount());

            click(nav.gameView(), "game.back");
            ((ConfirmDialog) ModalOverlay.top().card()).confirmButton().doClick();
            assertEquals(NavigationController.MENU, nav.currentScreen());
            assertEquals(0, ModalOverlay.openCount());
        });
    }

    @Test
    @DisplayName("FR-03: closing the window during a game asks first; on the menu it closes at once")
    void closingTheWindow() throws Exception {
        onEdt(() -> {
            nav.startGame(easyGame(), new Random(1));
            closeWindow();
            assertEquals(0, exits.get(), "a running game is not closed without a confirmation");
            assertEquals(1, ModalOverlay.openCount());
            ((ConfirmDialog) ModalOverlay.top().card()).confirmButton().doClick();
            assertEquals(1, exits.get());
        });
        onEdt(() -> {
            nav.showMainMenu();
            closeWindow();
            assertEquals(2, exits.get());
        });
    }

    @Test
    @DisplayName("FR-01: Exit asks first; Cancel keeps the application open")
    void exitIsConfirmed() throws Exception {
        onEdt(() -> {
            click(nav.menuView(), "menu.exit");
            ((ConfirmDialog) ModalOverlay.top().card()).cancelButton().doClick();
            assertEquals(0, exits.get());
            assertEquals(NavigationController.MENU, nav.currentScreen());

            click(nav.menuView(), "menu.exit");
            ((ConfirmDialog) ModalOverlay.top().card()).confirmButton().doClick();
            assertEquals(1, exits.get());
        });
    }

    @Test
    @DisplayName("FR-47 routing: results lead back to the menu, and Play Again starts the same settings again")
    void resultsRouting() throws Exception {
        onEdt(() -> {
            nav.startGame(easyGame(), new Random(1));
            GameBoardView played = nav.gameView();
            nav.showResults(nav.game().model().players(), Difficulty.EASY, "00:01:02");
            assertEquals(NavigationController.RESULTS, nav.currentScreen());

            click(nav.resultsView(), "results.playAgain");
            assertEquals(NavigationController.GAME, nav.currentScreen());
            assertNotSame(played, nav.gameView());
            assertEquals(easyGame().players(), nav.game().model().settings().players(),
                    "FR-09: Play Again keeps the names and colours");
            assertEquals(Difficulty.EASY, nav.game().model().difficulty());

            nav.showResults(nav.game().model().players(), Difficulty.EASY, "00:01:02");
            click(nav.resultsView(), "results.menu");
            assertEquals(NavigationController.MENU, nav.currentScreen());
        });
    }

    // ------------------------------------------------------------------ helpers

    private void closeWindow() {
        WindowEvent closing = new WindowEvent(nav.frame(), WindowEvent.WINDOW_CLOSING);
        for (WindowListener l : nav.frame().getWindowListeners()) {
            l.windowClosing(closing);
        }
    }

    private static void click(Container root, String name) {
        Component c = find(root, name);
        assertNotNull(c, "no component named " + name);
        ((AbstractButton) c).doClick();
    }

    private static Component find(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container inner) {
                Component hit = find(inner, name);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    /** All text shown by labels, text components and buttons inside a component, joined. */
    private static String texts(Container root) {
        StringBuilder sb = new StringBuilder();
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l && l.getText() != null) {
                sb.append(l.getText()).append('\n');
            } else if (c instanceof JTextComponent t) {
                sb.append(t.getText()).append('\n');
            } else if (c instanceof JButton b && b.getText() != null) {
                sb.append(b.getText()).append('\n');
            }
            if (c instanceof Container inner) {
                sb.append(texts(inner));
            }
        }
        return sb.toString();
    }

    private static void onEdt(ThrowingRunnable body) throws Exception {
        Throwable[] failure = new Throwable[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    body.run();
                } catch (Throwable t) {
                    failure[0] = t;
                }
            });
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        }
        if (failure[0] instanceof Error e) {
            throw e;
        }
        if (failure[0] instanceof Exception e) {
            throw e;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
