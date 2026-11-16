package View;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Difficulty;
import Model.GameModel;
import Model.GameSettings;
import Model.PlayerColor;
import View.components.Theme;

/**
 * Tests for the main menu screen (Task 01): the four menu options (FR-01) and the layout of the screen at
 * every supported window size (NFR-23, NFR-24). They are regression tests for the defects found in the
 * Phase A screen:
 *
 * <ol>
 *   <li>the decorative board and the whole title collapsed to a single pixel once the window was narrower
 *       than the preferred width of the menu;</li>
 *   <li>the title used a fixed 430x140 box while the height it needs depends on the display font, so the
 *       tail of "Ludo" was cut off with Arial Black (the font used when Segoe UI Black is missing);</li>
 *   <li>the menu column was laid out at its minimum width, which was exactly the width of the subtitle.
 *       With no room to spare the subtitle was shortened to "...", the option buttons were narrowed and
 *       the column ran past the edges of the screen.</li>
 * </ol>
 *
 * <p>The layout tests therefore check real bounds: that nothing sticks out of its parent, that no text is
 * shortened, and that the title, the board and the four options are all really on the screen. They are run
 * at several window sizes, from a comfortable one down to below the smallest supported one, so that a
 * computer with slightly wider fonts or a larger window frame is covered as well.
 *
 * <p>No {@link javax.swing.JFrame} is created, so the tests also run without a screen (headless). Every
 * Swing component is built, laid out and read on the event dispatch thread.
 *
 * <p>Owner: Mohamed Zereny (Task 01).
 */
class MainMenuViewTest {

    /** A comfortable window, used to check that the designed appearance is unchanged. */
    private static final int[] NORMAL = {1264, 721};
    /**
     * Content area of the smallest supported window. The window itself is 1024x640
     * ({@code NavigationController} sets that minimum), and the window frame takes part of it away. The
     * numbers below are measured from a real {@link javax.swing.JFrame} on Windows 11: the frame reports
     * insets of 9 left, 9 right, 38 top and 9 bottom, which leaves 1006x593 for the screen. An earlier
     * version of this test assumed 1008x601, which is 2px wider and 8px higher than the application really
     * has, so the tests were measuring a slightly easier case than the manual test.
     */
    private static final int[] MINIMUM = {1006, 593};
    /**
     * Content areas every part of the screen must survive. Besides the smallest supported window they go
     * clearly below it, because the window frame is not the same everywhere: a different Windows theme, a
     * scaled display or larger system fonts leave the screen less room than the 1006x593 measured here.
     */
    private static final int[][] SUPPORTED_SIZES = {
        NORMAL, {1120, 660}, MINIMUM, {980, 570}, {940, 545}, {900, 540}, {860, 520},
    };

    private static final String[] OPTION_NAMES = {"menu.newGame", "menu.history", "menu.questions", "menu.exit"};
    private static final String[] OPTION_LABELS = {"New Game", "Game History", "Question Management", "Exit"};
    private static final String[] TEXT_NAMES = {"menu.eyebrow", "menu.subtitle"};
    /** Smallest board preview that still reads as a Ludo board rather than a coloured smudge. */
    private static final int READABLE_BOARD = 150;

    // ------------------------------------------------------------------ FR-01: the four menu options

    @Test
    @DisplayName("FR-01: the menu shows New Game, Game History, Question Management and Exit, all enabled")
    void menuShowsTheFourOptions() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            for (int i = 0; i < OPTION_NAMES.length; i++) {
                JButton b = button(menu, OPTION_NAMES[i]);
                assertEquals(OPTION_LABELS[i], b.getText(), OPTION_NAMES[i] + " has the wrong label");
                assertTrue(b.isEnabled(), OPTION_NAMES[i] + " must be enabled (FR-01)");
                assertTrue(b.isVisible(), OPTION_NAMES[i] + " must be visible (FR-01)");
            }
        });
    }

    @Test
    @DisplayName("FR-01: every menu option runs the action the controller registered for it")
    void everyOptionRunsItsOwnAction() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            List<String> fired = new ArrayList<>();
            menu.setActions(() -> fired.add("newGame"), () -> fired.add("history"),
                    () -> fired.add("questions"), () -> fired.add("exit"));
            for (String name : OPTION_NAMES) {
                button(menu, name).doClick();
            }
            assertEquals(List.of("newGame", "history", "questions", "exit"), fired,
                    "each option must run exactly its own action, in order");
        });
    }

    @Test
    @DisplayName("FR-01: Enter starts a new game (New Game is the default button of the screen)")
    void enterStartsANewGame() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            assertSame(button(menu, "menu.newGame"), menu.defaultButton(),
                    "Enter must activate New Game");
        });
    }

    // ------------------------------------------------------------------ Iteration 3 options

    @Test
    @DisplayName("FR-02: no menu tip promises a login that Iteration 1 does not have")
    void tipsDoNotPromiseWhatIsNotThere() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            String questionsTip = button(menu, "menu.questions").getToolTipText();
            String historyTip = button(menu, "menu.history").getToolTipText();
            assertNotNull(questionsTip, "Question Management must explain why it cannot be used yet");
            assertNotNull(historyTip, "Game History must explain why it cannot be used yet");
            assertTrue(questionsTip.contains("Iteration 3"),
                    "the Question Management tip must say the feature comes in Iteration 3, but it is: "
                            + questionsTip);
            assertTrue(historyTip.contains("Iteration 3"),
                    "the Game History tip must say the feature comes in Iteration 3, but it is: "
                            + historyTip);
            assertTrue(!questionsTip.contains("requires a login"),
                    "the tip must not promise a login, there is none in Iteration 1: " + questionsTip);
        });
    }

    @Test
    @DisplayName("FR-01: the two Iteration 3 options are marked, but keep their label and still work")
    void iterationThreeOptionsAreMarkedButStillWork() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            List<String> fired = new ArrayList<>();
            menu.setActions(() -> fired.add("newGame"), () -> fired.add("history"),
                    () -> fired.add("questions"), () -> fired.add("exit"));
            for (String optionName : new String[] {"menu.history", "menu.questions"}) {
                JButton b = button(menu, optionName);
                Component badge = child(menu, optionName + ".badge");
                assertTrue(b.isEnabled(), optionName + " must stay enabled (FR-01)");
                assertTrue(badge.getFont().getSize2D() >= 12f,
                        "NFR-24: the tag of " + optionName + " is set in "
                                + badge.getFont().getSize2D() + "pt, below the 12pt minimum");
            }
            assertEquals("Game History", button(menu, "menu.history").getText(), "label must not change");
            assertEquals("Question Management", button(menu, "menu.questions").getText(),
                    "label must not change");
            button(menu, "menu.history").doClick();
            button(menu, "menu.questions").doClick();
            assertEquals(List.of("history", "questions"), fired,
                    "a marked option must still run its action when clicked");
        });
    }

    @Test
    @DisplayName("FR-01: the two options without a Soon tag are New Game and Exit")
    void onlyTheUnavailableOptionsAreMarked() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            for (String optionName : new String[] {"menu.newGame", "menu.exit"}) {
                assertNull(find(menu, optionName + ".badge"),
                        optionName + " works in Iteration 1 and must not be marked");
            }
        });
    }

    @Test
    @DisplayName("FR-01: the Soon tag lets every click through to its option")
    void theTagDoesNotSwallowClicks() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            layoutAt(menu, NORMAL[0], NORMAL[1]);
            for (String optionName : new String[] {"menu.history", "menu.questions"}) {
                Component badge = child(menu, optionName + ".badge");
                JButton b = button(menu, optionName);
                assertTrue(badge.getWidth() > 0 && badge.getHeight() > 0,
                        optionName + " tag was not laid out");
                // The search has to start at the holder that contains both the tag and the button, the
                // way Swing itself looks for the component under the mouse. Starting at the button would
                // prove nothing: the button has no children, so it could only ever return itself.
                Container option = badge.getParent();
                assertSame(option, b.getParent(),
                        "the tag and " + optionName + " must sit in the same holder");
                Point middle = new Point(badge.getWidth() / 2, badge.getHeight() / 2);
                Point inOption = SwingUtilities.convertPoint(badge, middle, option);
                assertSame(b, SwingUtilities.getDeepestComponentAt(option, inOption.x, inOption.y),
                        "the tag of " + optionName + " swallows mouse events meant for the option");
            }
        });
    }

    @Test
    @DisplayName("FR-01: the Soon tag never covers the label of its option")
    void theTagDoesNotCoverTheLabel() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            for (int[] size : SUPPORTED_SIZES) {
                layoutAt(menu, size[0], size[1]);
                for (String optionName : new String[] {"menu.history", "menu.questions"}) {
                    JButton b = button(menu, optionName);
                    Component badge = child(menu, optionName + ".badge");
                    if (badge.getWidth() == 0) {
                        continue;        // no room for the tag; the option is shown without it
                    }
                    // CustomButton centres icon, gap and text over the whole width of the button.
                    int labelRight = (b.getWidth() + labelWidth(b)) / 2;
                    int badgeLeft = SwingUtilities.convertPoint(badge.getParent(),
                            badge.getX(), badge.getY(), b).x;
                    assertTrue(labelRight <= badgeLeft, "at " + name(size) + " the tag of " + optionName
                            + " starts at " + badgeLeft + " but the label runs to " + labelRight);
                }
            }
        });
    }

    // ------------------------------------------------------------------ layout: nothing cut off

    @Test
    @DisplayName("NFR-23: nothing sticks out of the screen at any supported window size")
    void nothingOverflowsTheScreen() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            for (int[] size : SUPPORTED_SIZES) {
                layoutAt(menu, size[0], size[1]);
                List<String> outside = new ArrayList<>();
                collectOverflow(menu, menu, outside);
                assertTrue(outside.isEmpty(),
                        "at " + name(size) + " these parts are outside their container: " + outside);
            }
        });
    }

    @Test
    @DisplayName("NFR-24: no text is shortened to \"...\" at any supported window size")
    void noTextIsShortened() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            for (int[] size : SUPPORTED_SIZES) {
                layoutAt(menu, size[0], size[1]);
                for (String labelName : TEXT_NAMES) {
                    JLabel label = (JLabel) child(menu, labelName);
                    int needs = textWidth(label);
                    int has = label.getWidth() - horizontalInsets(label);
                    assertTrue(needs <= has, labelName + " (\"" + label.getText() + "\") needs " + needs
                            + "px but has " + has + "px at " + name(size));
                }
                for (String optionName : OPTION_NAMES) {
                    JButton b = button(menu, optionName);
                    assertTrue(labelWidth(b) <= b.getWidth(), optionName + " (\"" + b.getText() + "\") needs "
                            + labelWidth(b) + "px but the button is " + b.getWidth() + "px at " + name(size));
                }
            }
        });
    }

    @Test
    @DisplayName("FR-01: the title and the four options are really on the screen at any supported size")
    void titleAndOptionsStayOnScreen() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            Dimension titleNeeds = measureTitle();
            for (int[] size : SUPPORTED_SIZES) {
                layoutAt(menu, size[0], size[1]);
                Component title = child(menu, "menu.title");
                assertTrue(title.getWidth() >= titleNeeds.width && title.getHeight() >= titleNeeds.height,
                        "at " + name(size) + " the title is " + box(title) + " but the font needs "
                                + titleNeeds.width + "x" + titleNeeds.height);
                assertTrue(insideScreen(menu, title),
                        "at " + name(size) + " the title is not fully inside the screen: " + box(title));
                for (String optionName : OPTION_NAMES) {
                    JButton b = button(menu, optionName);
                    assertTrue(b.getWidth() > 0 && b.getHeight() > 0,
                            "at " + name(size) + " " + optionName + " has no size");
                    assertTrue(insideScreen(menu, b),
                            "at " + name(size) + " " + optionName + " is not fully inside the screen");
                }
            }
        });
    }

    @Test
    @DisplayName("NFR-23: the board preview is shown and readable at every supported window size")
    void boardPreviewIsShownAtEverySupportedSize() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            for (int[] size : SUPPORTED_SIZES) {
                layoutAt(menu, size[0], size[1]);
                Component hero = child(menu, "menu.hero");
                Component board = child(menu, "menu.board");
                assertTrue(hero.isVisible() && board.isVisible(),
                        "the board preview is not shown at " + name(size));
                assertTrue(insideScreen(menu, hero),
                        "the board preview is not fully inside the screen at " + name(size)
                                + ": " + box(hero));
                assertTrue(board.getWidth() >= READABLE_BOARD && board.getHeight() >= READABLE_BOARD,
                        "the board shrank to " + box(board) + " at " + name(size)
                                + ", which is below the readable size of " + READABLE_BOARD + "px");
            }
        });
    }

    @Test
    @DisplayName("NFR-23: at the smallest supported window the board is still a good size")
    void boardKeepsAGoodSizeAtTheSmallestWindow() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            layoutAt(menu, MINIMUM[0], MINIMUM[1]);
            Component board = child(menu, "menu.board");
            assertTrue(board.getWidth() >= 300,
                    "at " + name(MINIMUM) + " the board is only " + box(board)
                            + "; the menu column leaves enough room for at least 300px");
        });
    }

    @Test
    @DisplayName("FR-01: the title is never cut off, whichever display font the computer has")
    void titleIsNeverCutOff() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            Component title = child(menu, "menu.title");
            Dimension needs = measureTitle();
            Dimension minimum = title.getMinimumSize();
            assertTrue(minimum.width >= needs.width && minimum.height >= needs.height,
                    "the title asks for a minimum of " + minimum.width + "x" + minimum.height
                            + " but the font " + Theme.display(68f).getFamily() + " needs "
                            + needs.width + "x" + needs.height);
        });
    }

    @Test
    @DisplayName("FR-01: the menu column keeps its full width, it is never squeezed")
    void menuColumnIsNeverSqueezed() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            JComponent col = (JComponent) child(menu, "menu.column");
            int wanted = col.getPreferredSize().width;
            for (int[] size : SUPPORTED_SIZES) {
                layoutAt(menu, size[0], size[1]);
                assertEquals(wanted, col.getWidth(),
                        "the menu column was squeezed at " + name(size));
            }
        });
    }

    @Test
    @DisplayName("NFR-23: at a normal window size the menu keeps its designed layout")
    void normalSizeKeepsTheDesignedLayout() {
        onEdt(() -> {
            MainMenuView menu = newMenu();
            layoutAt(menu, NORMAL[0], NORMAL[1]);
            Component hero = child(menu, "menu.hero");
            Component title = child(menu, "menu.title");
            assertAll("designed layout",
                    () -> assertEquals(540, hero.getWidth(), "board preview width"),
                    () -> assertEquals(470, hero.getHeight(), "board preview height"),
                    () -> assertEquals(430, title.getWidth(), "title width"),
                    () -> assertTrue(title.getHeight() >= 140, "title height must not shrink below the design"),
                    () -> assertEquals(390, button(menu, "menu.newGame").getWidth(), "menu button width"),
                    () -> assertEquals(60, button(menu, "menu.newGame").getHeight(), "New Game height"),
                    () -> assertEquals(54, button(menu, "menu.exit").getHeight(), "Exit height"));
        });
    }

    // ------------------------------------------------------------------ helpers

    /** A main menu with the same decorative preview game the navigation controller uses. */
    private static MainMenuView newMenu() {
        List<GameSettings.PlayerSetup> players = List.of(
                new GameSettings.PlayerSetup("Red", PlayerColor.RED),
                new GameSettings.PlayerSetup("Green", PlayerColor.GREEN),
                new GameSettings.PlayerSetup("Yellow", PlayerColor.YELLOW),
                new GameSettings.PlayerSetup("Blue", PlayerColor.BLUE));
        GameModel preview = new GameModel(new GameSettings(Difficulty.EASY, players), new Random(2026));
        return new MainMenuView(preview);
    }

    /**
     * The smallest size that shows both title lines in full, measured independently of the screen code.
     *
     * <p>Width: "Educational" is drawn with a drop shadow 2px to the right, and each letter of "Ludo" with
     * one 3px to the right, so the widest of the two lines plus its shadow decides.
     *
     * <p>Height: the last thing painted is the shadow of "Ludo", 4px below its baseline. The letters of
     * that word happen to sit on the baseline, but a font is free to draw below it, and the amount it may
     * use is the descent. The room needed is therefore the baseline of the second line, plus the shadow,
     * plus the descent of its font. Leaving the descent out would accept a title box that happens to fit
     * the four letters "Ludo" in today's font but clips a font that draws lower.
     */
    private static Dimension measureTitle() {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = probe.createGraphics();
        Theme.antialias(g);
        FontMetrics fmTop = g.getFontMetrics(Theme.display(50f));
        FontMetrics fmBottom = g.getFontMetrics(Theme.display(68f));
        int topShadowX = 2;
        int bottomShadowX = 3;
        int bottomShadowY = 4;
        int letterGap = 2;
        int baselineTop = fmTop.getAscent() - 6;
        int baselineBottom = baselineTop + fmBottom.getAscent() - 4;
        int right = fmTop.stringWidth("Educational") + topShadowX;
        int x = 0;
        for (int i = 0; i < 4; i++) {
            int letter = fmBottom.stringWidth("Ludo".substring(i, i + 1));
            right = Math.max(right, x + letter + bottomShadowX);
            x += letter + letterGap;
        }
        g.dispose();
        return new Dimension(right, baselineBottom + bottomShadowY + fmBottom.getDescent());
    }

    /** Collects every visible component that is painted outside the inner area of its parent. */
    private static void collectOverflow(Component c, Component root, List<String> outside) {
        if (c != root && c.isVisible()) {
            Container parent = c.getParent();
            Insets in = parent.getInsets();
            int overRight = c.getX() + c.getWidth() - (parent.getWidth() - in.right);
            int overBottom = c.getY() + c.getHeight() - (parent.getHeight() - in.bottom);
            if (c.getX() < in.left) {
                outside.add(id(c) + " " + (in.left - c.getX()) + "px past the left edge");
            }
            if (c.getY() < in.top) {
                outside.add(id(c) + " " + (in.top - c.getY()) + "px past the top edge");
            }
            if (overRight > 0) {
                outside.add(id(c) + " " + overRight + "px past the right edge");
            }
            if (overBottom > 0) {
                outside.add(id(c) + " " + overBottom + "px past the bottom edge");
            }
        }
        if (c instanceof Container container && c.isVisible()) {
            for (Component kid : container.getComponents()) {
                collectOverflow(kid, root, outside);
            }
        }
    }

    /** True when the component lies completely inside the inner area of the screen. */
    private static boolean insideScreen(JComponent screen, Component c) {
        Insets in = screen.getInsets();
        Rectangle inner = new Rectangle(in.left, in.top,
                screen.getWidth() - in.left - in.right, screen.getHeight() - in.top - in.bottom);
        Rectangle bounds = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), screen);
        return inner.contains(bounds);
    }

    /** Width the button needs for its icon, gap and label, the way CustomButton paints them. */
    private static int labelWidth(JButton b) {
        Icon icon = b.getIcon();
        return (icon == null ? 0 : icon.getIconWidth() + b.getIconTextGap())
                + b.getFontMetrics(b.getFont()).stringWidth(b.getText());
    }

    private static int textWidth(JLabel label) {
        return label.getFontMetrics(label.getFont()).stringWidth(label.getText());
    }

    private static int horizontalInsets(JComponent c) {
        return c.getInsets().left + c.getInsets().right;
    }

    /** Lays the whole screen out at the given size, without a window (so it works headless). */
    private static void layoutAt(Component root, int width, int height) {
        root.setSize(width, height);
        layoutTree(root);
    }

    private static void layoutTree(Component c) {
        if (c instanceof Container container) {
            container.doLayout();
            for (Component kid : container.getComponents()) {
                layoutTree(kid);
            }
        }
    }

    private static JButton button(Component root, String name) {
        Component c = child(root, name);
        assertTrue(c instanceof JButton, name + " is not a button");
        return (JButton) c;
    }

    /** Finds the component with this name anywhere under {@code root}. */
    private static Component child(Component root, String name) {
        Component found = find(root, name);
        assertNotNull(found, "no component named " + name + " in the main menu");
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

    private static String id(Component c) {
        return c.getName() != null ? c.getName() : c.getClass().getSimpleName();
    }

    private static String box(Component c) {
        return c.getWidth() + "x" + c.getHeight();
    }

    private static String name(int[] size) {
        return size[0] + "x" + size[1];
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
