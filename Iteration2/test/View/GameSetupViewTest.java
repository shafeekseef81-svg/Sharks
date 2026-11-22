package View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.DiceRoll;
import Model.Difficulty;
import Model.GameModel;
import Model.GameSettings;
import Model.PlayerColor;

/**
 * Tests for the game setup screen (Task 03; SRS FR-03, FR-04, FR-05 partial, FR-08, FR-09).
 *
 * <p>The screen collects the settings and shows the errors; deciding whether the settings are valid is
 * {@code GameSettings.validate} (Task 04, Rakan). These tests therefore wire the screen to the <b>real</b>
 * validator, exactly as {@code NavigationController} does, and check what the player ends up seeing.
 *
 * <p>No {@link javax.swing.JFrame} is created, so the tests run headless as well. Every Swing component is
 * built and used on the event dispatch thread.
 *
 * <p>Owner: Mohamed Zereny (Task 03). The independent input tests are Task 05 (Shafeek) and the acceptance
 * test is Task 06 (Shafeek); both work on the Model, so they do not overlap with these.
 */
class GameSetupViewTest {

    /** Records what the screen submits and answers with the real Model validation, like the controller. */
    private static final class RecordingStart {
        private final List<Difficulty> levels = new ArrayList<>();
        private final List<List<GameSettings.PlayerSetup>> submitted = new ArrayList<>();

        List<GameSettings.FieldError> apply(Difficulty level, List<GameSettings.PlayerSetup> players) {
            List<GameSettings.FieldError> errors = GameSettings.validate(level, players);
            if (errors.isEmpty()) {
                levels.add(level);
                submitted.add(players);
            }
            return errors;
        }

        int started() {
            return submitted.size();
        }
    }

    private static GameSetupView setupWith(RecordingStart start) {
        GameSetupView view = new GameSetupView();
        view.setStartHandler(start::apply);
        return view;
    }

    private static void fillNames(GameSetupView view, String... names) {
        for (int i = 0; i < names.length; i++) {
            view.setPlayerName(i, names[i]);
        }
    }

    // ------------------------------------------------------------------ FR-04: 2 to 4 players

    @Test
    @DisplayName("FR-04: the screen offers 2, 3 and 4 players and starts on 2")
    void offersTwoToFourPlayers() {
        onEdt(() -> {
            GameSetupView view = new GameSetupView();
            for (int n = 2; n <= 4; n++) {
                AbstractButton chip = toggle(view, "setup.players." + n);
                assertTrue(chip.isEnabled(), n + " players must be selectable");
            }
            assertEquals(2, view.playerCount(), "a new setup starts with two players");
            assertTrue(toggle(view, "setup.players.2").isSelected());
        });
    }

    @Test
    @DisplayName("FR-04: choosing a count shows exactly that many player rows")
    void theCountDecidesHowManyRowsAreShown() {
        onEdt(() -> {
            GameSetupView view = new GameSetupView();
            for (int n = 2; n <= 4; n++) {
                view.setPlayerCount(n);
                assertEquals(n, view.playerCount());
                assertTrue(toggle(view, "setup.players." + n).isSelected(), n + " must be the chosen chip");
                for (int row = 0; row < 4; row++) {
                    Component name = child(view, "setup.name." + row);
                    assertEquals(row < n, name.getParent().getParent().isVisible(),
                            "row " + row + " with " + n + " players");
                }
            }
        });
    }

    @Test
    @DisplayName("FR-07: every player row starts with a different colour")
    void everyRowStartsWithItsOwnColour() {
        onEdt(() -> {
            RecordingStart start = new RecordingStart();
            GameSetupView view = setupWith(start);
            view.setPlayerCount(4);
            fillNames(view, "Ann", "Bob", "Cid", "Dee");
            click(view, "setup.start");

            assertEquals(1, start.started(), "a valid setup starts a game");
            List<GameSettings.PlayerSetup> players = start.submitted.get(0);
            assertEquals(4, players.size());
            assertEquals(4, players.stream().map(GameSettings.PlayerSetup::color).distinct().count(),
                    "FR-07: four different colours");
        });
    }

    // ------------------------------------------------------------------ FR-05 partial: Easy only

    @Test
    @DisplayName("FR-05: all three levels are shown, so the player sees what the game offers")
    void allThreeLevelsAreShown() {
        onEdt(() -> {
            GameSetupView view = new GameSetupView();
            for (Difficulty d : Difficulty.values()) {
                Component card = child(view, "setup.level." + d.name().toLowerCase());
                assertTrue(card.isVisible(), d + " must be shown");
            }
        });
    }

    @Test
    @DisplayName("T2-01 FR-05: every level can be chosen; no card is disabled or marked as coming later")
    void everyLevelCanBeChosen() {
        onEdt(() -> {
            GameSetupView view = new GameSetupView();
            assertTrue(toggle(view, "setup.level.easy").isSelected(), "Easy is chosen from the start");
            assertEquals(Difficulty.EASY, view.difficulty());

            for (Difficulty d : Difficulty.values()) {
                AbstractButton card = toggle(view, "setup.level." + d.name().toLowerCase());
                assertTrue(card.isEnabled(), d + " must be choosable from Iteration 2 (T2-04 gave it dice)");
                String tip = card.getToolTipText();
                assertNotNull(tip, d + " should describe itself");
                assertFalse(tip.contains("Iteration 2"), d + " must not be marked as coming later: " + tip);
            }
        });
    }

    @Test
    @DisplayName("T2-01 FR-05: choosing Medium or Hard really changes the chosen level and the selected card")
    void choosingMediumOrHardIsKept() {
        onEdt(() -> {
            GameSetupView view = new GameSetupView();
            for (Difficulty d : Difficulty.values()) {
                view.setDifficulty(d);
                assertEquals(d, view.difficulty(), d + " must become the chosen level");
                assertTrue(toggle(view, "setup.level." + d.name().toLowerCase()).isSelected(),
                        d + " must be the selected card");
                for (Difficulty other : Difficulty.values()) {
                    if (other != d) {
                        assertFalse(toggle(view, "setup.level." + other.name().toLowerCase()).isSelected(),
                                other + " must not stay selected next to " + d);
                    }
                }
            }
        });
    }

    @Test
    @DisplayName("T2-01 FR-05: the level chosen on the screen is the level the controller is asked to start")
    void everyLevelIsHandedToTheController() {
        onEdt(() -> {
            for (Difficulty d : Difficulty.values()) {
                RecordingStart start = new RecordingStart();
                GameSetupView view = setupWith(start);
                view.setDifficulty(d);
                fillNames(view, "Ann", "Bob");
                click(view, "setup.start");

                assertEquals(1, start.started(), d + ": the game must start");
                assertEquals(d, start.levels.get(0), d + ": the controller got another level");
            }
        });
    }

    @Test
    @DisplayName("T2-01 FR-05, FR-16: a game built from what the screen submits rolls on every level")
    void aGameFromTheScreenRollsOnEveryLevel() {
        onEdt(() -> {
            for (Difficulty d : Difficulty.values()) {
                RecordingStart start = new RecordingStart();
                GameSetupView view = setupWith(start);
                view.setDifficulty(d);
                fillNames(view, "Ann", "Bob");
                click(view, "setup.start");

                // Exactly the settings the controller would build from this screen (FR-09).
                GameModel game = new GameModel(new GameSettings(start.levels.get(0), start.submitted.get(0)),
                        new Random(2026));
                assertEquals(d, game.difficulty(), d.label());
                DiceRoll roll = game.roll();
                assertTrue(roll.regular() >= 1 && roll.regular() <= 6, d + ": the first roll must work");
                assertEquals(d.specialDie(), roll.specialType(), d + ": the special die of the level (FR-16)");
            }
        });
    }

    @Test
    @DisplayName("T2-01 FR-05: the playable rule accepts every level and refuses no level at all")
    void thePlayableRuleAcceptsEveryLevel() {
        for (Difficulty d : Difficulty.values()) {
            assertTrue(GameSetupView.playable(d), "every level has its dice from T2-04: " + d);
        }
        assertFalse(GameSetupView.playable(null), "no level at all is not playable");
    }

    // ------------------------------------------------------------------ FR-08: errors and starting

    @Test
    @DisplayName("FR-08: an empty name blocks the start and is reported on its own row")
    void anEmptyNameBlocksTheStart() {
        onEdt(() -> {
            RecordingStart start = new RecordingStart();
            GameSetupView view = setupWith(start);
            fillNames(view, "Ann", "");
            click(view, "setup.start");

            assertEquals(0, start.started(), "FR-08: an invalid setup may not start a game");
            List<String> errors = view.visibleErrors();
            assertEquals(1, errors.size(), "exactly the wrong row is reported: " + errors);
            assertTrue(errors.get(0).toLowerCase().contains("name"), errors.get(0));
            assertFalse(summaryText(view).isBlank(), "the player is told that something must be fixed");
        });
    }

    @Test
    @DisplayName("FR-06, FR-08: a duplicate name blocks the start and is reported on the later row")
    void aDuplicateNameBlocksTheStart() {
        onEdt(() -> {
            RecordingStart start = new RecordingStart();
            GameSetupView view = setupWith(start);
            fillNames(view, "Ann", "ann");               // the same name, different case
            click(view, "setup.start");

            assertEquals(0, start.started());
            List<String> errors = view.visibleErrors();
            assertEquals(1, errors.size(), "only the later row is reported: " + errors);
            assertTrue(errors.get(0).contains("Player 1"), "the message names the other row: " + errors.get(0));
        });
    }

    @Test
    @DisplayName("FR-06: a name longer than 12 characters cannot even be entered, so it never blocks")
    void theNameFieldStopsAtTwelveCharacters() {
        onEdt(() -> {
            RecordingStart start = new RecordingStart();
            GameSetupView view = setupWith(start);
            int max = GameSettings.MAX_NAME_LENGTH;
            fillNames(view, "Ann", "A".repeat(max + 5));
            click(view, "setup.start");

            // The field cuts the name while it is typed, so the player is never stopped at the end by a
            // length error: what is in the field is already short enough (FR-06).
            assertEquals(1, start.started(), "the shortened name is valid, so the game starts");
            String submitted = start.submitted.get(0).get(1).name();
            assertEquals(max, submitted.length(), "the field keeps at most " + max + " characters");
            assertEquals("A".repeat(max), submitted);
            assertTrue(view.visibleErrors().isEmpty(), "nothing to report: the input was already limited");
        });
    }

    @Test
    @DisplayName("FR-08: errors of a hidden row never block the start")
    void hiddenRowsAreNotValidated() {
        onEdt(() -> {
            RecordingStart start = new RecordingStart();
            GameSetupView view = setupWith(start);
            view.setPlayerCount(4);
            fillNames(view, "Ann", "Bob", "Cid", "Dee");
            view.setPlayerCount(2);                      // rows 3 and 4 disappear, names and all
            click(view, "setup.start");

            assertEquals(1, start.started(), "only the two visible players count");
            assertEquals(2, start.submitted.get(0).size());
        });
    }

    @Test
    @DisplayName("FR-08, FR-09: a valid setup starts one game with exactly the entered names and colours")
    void aValidSetupStartsTheGame() {
        onEdt(() -> {
            RecordingStart start = new RecordingStart();
            GameSetupView view = setupWith(start);
            view.setPlayerCount(3);
            fillNames(view, "Ann", "Bob", "Cid");
            view.setPlayerColor(0, PlayerColor.BLUE);
            click(view, "setup.start");

            assertEquals(1, start.started());
            List<GameSettings.PlayerSetup> players = start.submitted.get(0);
            assertEquals(List.of("Ann", "Bob", "Cid"),
                    players.stream().map(GameSettings.PlayerSetup::name).toList());
            assertEquals(PlayerColor.BLUE, players.get(0).color(), "the chosen colour is submitted");
            assertTrue(view.visibleErrors().isEmpty(), "a valid setup shows no error");
            assertEquals(Difficulty.EASY, start.levels.get(0));
        });
    }

    @Test
    @DisplayName("FR-08: fixing the input and pressing Start again clears the old errors")
    void correctingTheInputClearsTheErrors() {
        onEdt(() -> {
            RecordingStart start = new RecordingStart();
            GameSetupView view = setupWith(start);
            fillNames(view, "Ann", "");
            click(view, "setup.start");
            assertFalse(view.visibleErrors().isEmpty());

            view.setPlayerName(1, "Bob");
            click(view, "setup.start");
            assertEquals(1, start.started(), "the corrected setup starts");
            assertTrue(view.visibleErrors().isEmpty(), "the old error is gone");
        });
    }

    @Test
    @DisplayName("FR-08: showing the screen again clears the errors of the previous attempt")
    void onShowClearsOldErrors() {
        onEdt(() -> {
            GameSetupView view = setupWith(new RecordingStart());
            fillNames(view, "Ann", "");
            click(view, "setup.start");
            assertFalse(view.visibleErrors().isEmpty());

            view.onShow();
            assertTrue(view.visibleErrors().isEmpty(), "a fresh visit starts without old errors");
            assertTrue(summaryText(view).isBlank());
        });
    }

    // ------------------------------------------------------------------ FR-03 and the keyboard

    @Test
    @DisplayName("FR-03: the screen offers Back, and it runs the action of the controller")
    void backRunsItsAction() {
        onEdt(() -> {
            GameSetupView view = new GameSetupView();
            List<String> fired = new ArrayList<>();
            view.setBackAction(() -> fired.add("back"));
            JButton back = (JButton) child(view, "setup.back");
            assertTrue(back.isEnabled(), "FR-03: Back is always available");
            back.doClick();
            assertEquals(List.of("back"), fired);
        });
    }

    @Test
    @DisplayName("FR-04: Enter starts the game (Start Game is the default button)")
    void enterStartsTheGame() {
        onEdt(() -> {
            GameSetupView view = new GameSetupView();
            assertSame(child(view, "setup.start"), view.defaultButton());
        });
    }

    @Test
    @DisplayName("FR-08: without a controller the screen cannot start anything, and does not fail")
    void withoutAStartHandlerNothingHappens() {
        onEdt(() -> {
            GameSetupView view = new GameSetupView();        // no start handler registered
            fillNames(view, "Ann", "Bob");
            click(view, "setup.start");
            assertTrue(view.visibleErrors().isEmpty());
        });
    }

    // ------------------------------------------------------------------ helpers

    private static String summaryText(GameSetupView view) {
        return ((javax.swing.JLabel) child(view, "setup.summary")).getText();
    }

    private static AbstractButton toggle(Component root, String name) {
        Component c = child(root, name);
        assertTrue(c instanceof AbstractButton, name + " is not a button");
        return (AbstractButton) c;
    }

    private static void click(Component root, String name) {
        ((AbstractButton) child(root, name)).doClick();
    }

    private static Component child(Component root, String name) {
        Component found = find(root, name);
        assertNotNull(found, "no component named " + name + " in the setup screen");
        return found;
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
