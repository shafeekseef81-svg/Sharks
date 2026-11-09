package Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.GameEvent;
import Model.GameModel;
import Model.Difficulty;
import Model.GameSettings;
import Model.Piece;
import Model.Player;
import Model.PlayerColor;
import View.components.Animations;
import View.components.ModalOverlay;

/**
 * Integration tests of {@link GameController} (task N02, NFR-08): the real screens, the real model and the real
 * rules, driven the way the player drives them (Roll, then a click on a highlighted piece).
 *
 * <p>Animations run in instant mode, so every roll and move finishes inside the call and the tests are
 * deterministic. A game is started with a fixed seed; the first roll of that seed is read from an identical
 * probe game, because the board and the dice come from the same {@link Random}. The window is never shown.
 *
 * <p>Owner: Rakan Ziyan.
 */
class GameControllerTest {

    private NavigationController nav;

    @BeforeEach
    void setUp() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display: NavigationController owns a JFrame");
        Animations.instant = true;
        onEdt(() -> {
            nav = new NavigationController();
            nav.setExitHandler(() -> { });
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (nav != null) {
            onEdt(() -> {
                nav.game().stop();
                ModalOverlay.closeAll();
                nav.frame().dispose();
            });
        }
        Animations.instant = false;
    }

    private static GameSettings twoPlayers() {
        return new GameSettings(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Rakan", PlayerColor.RED),
                new GameSettings.PlayerSetup("Mohamed", PlayerColor.YELLOW)));
    }

    /** The first Regular die value of a game started with this seed. */
    private static int firstRoll(long seed) {
        return new GameModel(twoPlayers(), new Random(seed)).roll().regular();
    }

    /** A seed whose first roll is (or is not) a 6. */
    private static long seedWhereFirstRollIsSix(boolean six) {
        for (long seed = 0; seed < 1000; seed++) {
            if ((firstRoll(seed) == 6) == six) {
                return seed;
            }
        }
        throw new AssertionError("no such seed");
    }

    private GameController game() {
        return nav.game();
    }

    private GameModel model() {
        return nav.game().model();
    }

    // ------------------------------------------------------------------ start of a game

    @Test
    @DisplayName("N02, FR-15, FR-46: a new game starts with the first player's turn, Roll enabled and the clock running")
    void newGameStartsTheFirstTurn() throws Exception {
        onEdt(() -> {
            nav.startGame(twoPlayers(), new Random(1));
            assertEquals(NavigationController.GAME, nav.currentScreen());
            assertSame(model().players().get(0), model().currentPlayer());
            assertTrue(model().clock().running(), "the game time is measured from the start");
            assertTrue(model().legalMoves().isEmpty(), "nothing is highlighted before the roll");
            assertTrue(button("game.roll").isEnabled());
            assertTrue(texts(nav.gameView()).contains("Rakan, roll the dice."));
            assertFalse(game().isBusy());
        });
    }

    // ------------------------------------------------------------------ roll

    @Test
    @DisplayName("N02, FR-15, FR-20: after a roll with a legal move, a second roll is ignored until a piece moves")
    void secondRollIsIgnored() throws Exception {
        long seed = seedWhereFirstRollIsSix(true);
        onEdt(() -> {
            nav.startGame(twoPlayers(), new Random(seed));
            List<GameEvent.Type> events = listen();
            game().roll();
            assertEquals(6, model().lastRoll().regular(), "the probe predicted the roll");
            assertEquals(4, model().legalMoves().size(), "FR-19, FR-24: all four castle pieces are highlighted");
            game().roll();
            assertEquals(1, events.stream().filter(t -> t == GameEvent.Type.ROLLED).count());
        });
    }

    @Test
    @DisplayName("N02, FR-23: when no piece can move, the model's message is used and the next player gets the turn")
    void noMovePassesTheTurn() throws Exception {
        long seed = seedWhereFirstRollIsSix(false);
        onEdt(() -> {
            nav.startGame(twoPlayers(), new Random(seed));
            List<String> messages = new ArrayList<>();
            model().addListener(e -> {
                if (e.type() == GameEvent.Type.TURN_PASSED) {
                    messages.add(e.message());
                }
            });
            game().roll();
            assertEquals(1, messages.size());
            assertFalse(messages.get(0).isBlank(), "FR-23: a message is shown");
            assertSame(model().players().get(1), model().currentPlayer());
            assertTrue(texts(nav.gameView()).contains("Mohamed, roll the dice."));
            assertTrue(button("game.roll").isEnabled());
        });
    }

    // ------------------------------------------------------------------ move

    @Test
    @DisplayName("N02, FR-20, FR-24: a piece that is not highlighted is ignored; a highlighted one moves")
    void onlyHighlightedPiecesMove() throws Exception {
        long seed = seedWhereFirstRollIsSix(true);
        onEdt(() -> {
            nav.startGame(twoPlayers(), new Random(seed));
            game().roll();
            Piece opponent = model().players().get(1).pieces().get(0);
            game().pieceSelected(opponent);
            assertTrue(opponent.inCastle(), "another player's piece cannot be moved");
            assertEquals(4, model().legalMoves().size(), "the offer is unchanged");

            Piece mine = model().players().get(0).pieces().get(0);
            game().pieceSelected(mine);
            assertEquals(0, mine.progress(), "FR-19: the piece is on its start square");
            assertFalse(game().isBusy());
        });
    }

    @Test
    @DisplayName("N02, FR-21: after a move with a 6 the same player rolls again, and the screen says so")
    void sixGivesAnotherRoll() throws Exception {
        long seed = seedWhereFirstRollIsSix(true);
        onEdt(() -> {
            nav.startGame(twoPlayers(), new Random(seed));
            game().roll();
            game().pieceSelected(model().players().get(0).pieces().get(0));
            assertSame(model().players().get(0), model().currentPlayer());
            assertTrue(texts(nav.gameView()).contains("roll again"));
            assertTrue(button("game.roll").isEnabled());
        });
    }

    // ------------------------------------------------------------------ whole game

    @Test
    @DisplayName("N02, FR-43..FR-47: a full Easy game played through the controller ends on the results screen")
    void fullGameEndsOnTheResultsScreen() throws Exception {
        onEdt(() -> {
            nav.startGame(twoPlayers(), new Random(11));
            GameModel m = model();
            int steps = 0;
            while (!NavigationController.RESULTS.equals(nav.currentScreen())) {
                assertTrue(++steps < 20_000, "the game must end");
                assertFalse(game().isBusy(), "instant mode: nothing is left running between clicks");
                if (m.legalMoves().isEmpty()) {
                    game().roll();
                } else {
                    game().pieceSelected(m.legalMoves().keySet().iterator().next());
                }
            }
            assertTrue(m.isGameOver());
            List<Player> ranking = m.ranking();
            assertEquals(2, ranking.size());
            assertEquals(1, ranking.get(0).rank());
            assertEquals(2, ranking.get(1).rank(), "FR-44: the remaining player is ranked last");
            assertTrue(ranking.get(0).allFinished());
            assertFalse(m.clock().running(), "FR-44, FR-46: the clock stops when the game ends");
            assertTrue(texts(nav.resultsView()).contains(Model.GameClock.format(m.clock().elapsedMillis())),
                    "FR-47: the results show the measured duration");
            // The podium paints the names itself, so the labels checked here are the level, the time and the count.
            assertTrue(texts(nav.resultsView()).contains("2 players"));
            assertTrue(texts(nav.resultsView()).contains("Easy"), "FR-47: the level is shown");
        });
    }

    // ------------------------------------------------------------------ leaving

    @Test
    @DisplayName("N02, FR-03: leaving a game stops its clock; a new game replaces it and its delayed actions are dropped")
    void leavingStopsTheGame() throws Exception {
        onEdt(() -> {
            nav.startGame(twoPlayers(), new Random(1));
            GameModel first = model();
            nav.confirmLeaveGame(nav::showMainMenu);
            ((View.ConfirmDialog) ModalOverlay.top().card()).confirmButton().doClick();
            assertEquals(NavigationController.MENU, nav.currentScreen());
            assertFalse(first.clock().running(), "FR-03: the left game is not measured any more");

            nav.startGame(twoPlayers(), new Random(2));
            assertNotSame(first, model());
            assertTrue(model().clock().running());
            assertFalse(first.clock().running());
        });
    }

    // ------------------------------------------------------------------ helpers

    private List<GameEvent.Type> listen() {
        List<GameEvent.Type> events = new ArrayList<>();
        model().addListener(e -> events.add(e.type()));
        return events;
    }

    private AbstractButton button(String name) {
        Component c = find(nav.gameView(), name);
        assertTrue(c instanceof AbstractButton, "no button named " + name);
        return (AbstractButton) c;
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

    /** All label and text-component text inside a component, joined. */
    private static String texts(Container root) {
        StringBuilder sb = new StringBuilder();
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l && l.getText() != null) {
                sb.append(l.getText()).append('\n');
            } else if (c instanceof javax.swing.text.JTextComponent t) {
                sb.append(t.getText()).append('\n');
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
