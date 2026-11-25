package View;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Difficulty;
import Model.Question;
import Model.QuestionBank;
import Model.QuestionFactory;
import Model.QuestionResult;

import View.components.CustomButton;
import View.components.Theme;

/**
 * Regression tests for the <b>No Answer</b> button of the question card (decision D3, FR-35, FR-36).
 * Owner: Mohamed Zereny.
 *
 * <p><b>Why this class exists.</b> Real-window GUI acceptance on 23/11/2026 found that the No Answer button
 * was <i>completely invisible</i>: it was laid out, it held keyboard focus and it submitted
 * {@link QuestionResult#NO_ANSWER} correctly, but nothing was painted where it sat. It used
 * {@code CustomButton.Variant.GHOST}, which is meant for dark backgrounds - no fill, white label
 * ({@code Theme.TEXT_ON_DARK}), a white border and a white focus ring - and the question card is white
 * ({@code Theme.SURFACE}). A mouse user could never find the button, and a keyboard user landed on it with
 * no visual indication at all.
 *
 * <p><b>Why the existing tests missed it.</b> {@code QuestionDialogTest} asserts behaviour and state:
 * {@code getText()}, {@code isVisible()}, {@code doClick()}, the key binding, the submitted result. Every one
 * of those passed while the button was invisible, because none of them looks at a painted pixel.
 *
 * <p><b>So these tests render.</b> The real card is laid out and painted into an image, the button's own
 * bounds are located inside it, and the pixels there are measured. The checks are contrast-based rather than
 * colour-exact, so re-theming the application does not break them: what is asserted is that the label and the
 * focus ring are actually <i>distinguishable from the card behind them</i>, which is the property that failed.
 *
 * <p>Contrast is the WCAG 2.1 relative-luminance ratio. The thresholds are deliberately far below the 4.5
 * that body text would need: this class is not a style guide, it is a trap for "painted nothing at all".
 */
class NoAnswerVisibilityTest {

    /** A ratio of 1.0 means identical to the background. White on white scores exactly 1.0. */
    private static final double MIN_LABEL_CONTRAST = 2.0;
    private static final double MIN_FOCUS_CONTRAST = 1.6;

    private final QuestionBank bank = bankOf();
    private final Map<Difficulty, QuestionFactory> factories =
            QuestionFactory.forBank(bank, new Random(7));

    private static QuestionBank bankOf() {
        List<Question> list = new ArrayList<>();
        int n = 0;
        for (Difficulty level : Difficulty.values()) {
            for (int i = 0; i < 4; i++) {
                n++;
                list.add(new Question("Q" + n, level, "Question number " + n + "?", 2,
                        "answer " + n + "a", "answer " + n + "b", "answer " + n + "c", "answer " + n + "d"));
            }
        }
        return new QuestionBank(list);
    }

    private static void onEdt(Runnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeAndWait(action);
        }
    }

    private QuestionDialog build(List<QuestionResult> continued) throws Exception {
        QuestionFactory factory = factories.get(Difficulty.EASY);
        Question question = factory.ask();
        QuestionDialog[] holder = new QuestionDialog[1];
        onEdt(() -> holder[0] = new QuestionDialog(question, "Mohamed",
                chosen -> factory.judge(question, chosen), continued::add));
        return holder[0];
    }

    /** Lays the card out at a realistic size and paints it, exactly as the screen would. */
    private static BufferedImage render(QuestionDialog card) throws Exception {
        BufferedImage[] img = new BufferedImage[1];
        onEdt(() -> {
            card.setSize(card.getPreferredSize());
            if (card.getWidth() < 600) {
                card.setSize(700, Math.max(520, card.getPreferredSize().height));
            }
            layoutAll(card);
            BufferedImage image = new BufferedImage(Math.max(1, card.getWidth()),
                    Math.max(1, card.getHeight()), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.SURFACE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            card.paint(g);
            g.dispose();
            img[0] = image;
        });
        return img[0];
    }

    private static void layoutAll(java.awt.Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof java.awt.Container inner) {
                layoutAll(inner);
            } else {
                child.doLayout();
            }
        }
    }

    /** The button's rectangle in the card's own coordinates. */
    private static Rectangle boundsIn(QuestionDialog card, JComponent child) {
        return SwingUtilities.convertRectangle(child.getParent(), child.getBounds(), card);
    }

    // ---------------------------------------------------------------- contrast maths (WCAG 2.1)

    private static double channel(int v) {
        double c = v / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double luminance(Color c) {
        return 0.2126 * channel(c.getRed()) + 0.7152 * channel(c.getGreen()) + 0.0722 * channel(c.getBlue());
    }

    static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        double hi = Math.max(la, lb);
        double lo = Math.min(la, lb);
        return (hi + 0.05) / (lo + 0.05);
    }

    /** The highest contrast any pixel inside {@code area} reaches against {@code background}. */
    private static double peakContrast(BufferedImage image, Rectangle area, Color background) {
        Rectangle clipped = area.intersection(
                new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        double peak = 1.0;
        for (int y = clipped.y; y < clipped.y + clipped.height; y++) {
            for (int x = clipped.x; x < clipped.x + clipped.width; x++) {
                peak = Math.max(peak, contrast(new Color(image.getRGB(x, y)), background));
            }
        }
        return peak;
    }

    // ---------------------------------------------------------------- the regression tests

    @Test
    @DisplayName("T2-15 D3/FR-35: the No Answer button is actually painted - its label contrasts with the card")
    void theNoAnswerButtonIsVisibleOnTheCard() throws Exception {
        QuestionDialog card = build(new ArrayList<>());
        BufferedImage image = render(card);
        Rectangle area = boundsIn(card, card.noAnswerButton());

        assertAll(
                () -> assertTrue(area.width > 0 && area.height > 0,
                        "the button must occupy space on the card, but its bounds are " + area),
                () -> assertTrue(area.x >= 0 && area.y >= 0,
                        "the button must sit inside the card, but its bounds are " + area));

        double peak = peakContrast(image, area, Theme.SURFACE);
        assertTrue(peak >= MIN_LABEL_CONTRAST,
                "D3: the No Answer button must be visible on the white question card, but the strongest"
                        + " pixel inside its bounds " + area + " reaches a contrast ratio of only "
                        + String.format("%.2f", peak) + " against Theme.SURFACE (1.00 means nothing was"
                        + " painted at all). GHOST paints a white label and a white border on a white card.");
    }

    @Test
    @DisplayName("T2-15 NFR-13: the No Answer button shows a visible keyboard focus indicator")
    void theNoAnswerButtonShowsVisibleFocus() throws Exception {
        QuestionDialog card = build(new ArrayList<>());
        CustomButton button = card.noAnswerButton();

        BufferedImage unfocused = render(card);
        Rectangle area = boundsIn(card, button);
        double before = peakContrast(unfocused, area, Theme.SURFACE);

        // CustomButton paints its focus ring when isFocusOwner() is true. Without a window nothing can own
        // focus, so the painted state is driven the same way the look and feel does: by the focus event.
        onEdt(() -> button.dispatchEvent(
                new java.awt.event.FocusEvent(button, java.awt.event.FocusEvent.FOCUS_GAINED)));
        BufferedImage focused = render(card);
        double after = peakContrast(focused, area, Theme.SURFACE);

        assertTrue(after >= MIN_FOCUS_CONTRAST,
                "NFR-13: the focused No Answer button must show a focus indicator that is visible against the"
                        + " white card, but the strongest pixel reaches only " + String.format("%.2f", after)
                        + ". GHOST draws its focus ring in white.");
        assertTrue(after >= before,
                "focusing the button must not make it less visible (before " + String.format("%.2f", before)
                        + ", after " + String.format("%.2f", after) + ")");
    }

    @Test
    @DisplayName("T2-15 D3: the No Answer button still submits NO_ANSWER by mouse and by SPACE")
    void theNoAnswerButtonStillWorks() throws Exception {
        List<QuestionResult> byMouse = new ArrayList<>();
        QuestionDialog mouseCard = build(byMouse);
        onEdt(() -> mouseCard.noAnswerButton().doClick());
        onEdt(() -> mouseCard.continueButton().doClick());
        assertEquals(1, byMouse.size(), "a mouse click must submit exactly one result");
        assertFalseCorrect(byMouse.get(0));

        List<QuestionResult> byKeyboard = new ArrayList<>();
        QuestionDialog keyCard = build(byKeyboard);
        CustomButton button = keyCard.noAnswerButton();
        AtomicInteger fired = new AtomicInteger();
        onEdt(() -> {
            Object bound = button.getInputMap(JComponent.WHEN_FOCUSED)
                    .get(KeyStroke.getKeyStroke("pressed SPACE"));
            assertNotNull(bound, "SPACE must be bound on the button");
            // Enter is deliberately NOT bound: no requirement asks for it, and the source comment used to
            // claim it was. This pins the real binding so the claim cannot drift again.
            assertNull(button.getInputMap(JComponent.WHEN_FOCUSED)
                    .get(KeyStroke.getKeyStroke("pressed ENTER")), "ENTER must not be bound");
            // A JButton binds SPACE as two actions: "pressed" arms it and "released" fires it.
            // Both are invoked, which is what the look and feel does for a real key press.
            Object releasedKey = button.getInputMap(JComponent.WHEN_FOCUSED)
                    .get(KeyStroke.getKeyStroke("released SPACE"));
            assertNotNull(releasedKey, "released SPACE must be bound on the button");
            button.getActionMap().get(bound).actionPerformed(
                    new java.awt.event.ActionEvent(button, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
            button.getActionMap().get(releasedKey).actionPerformed(
                    new java.awt.event.ActionEvent(button, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
            fired.incrementAndGet();
        });
        onEdt(() -> keyCard.continueButton().doClick());
        assertEquals(1, fired.get(), "the SPACE action must run once");
        assertEquals(1, byKeyboard.size(), "SPACE must submit exactly one result");
        assertFalseCorrect(byKeyboard.get(0));
    }

    private static void assertFalseCorrect(QuestionResult result) {
        assertTrue(!result.correct(), "D3/FR-35: declining to answer counts as a wrong answer");
        assertEquals(QuestionResult.NO_ANSWER, result.chosenIndex(),
                "the submitted index must be QuestionResult.NO_ANSWER");
    }

    @Test
    @DisplayName("T2-15: the dark-background GHOST button keeps its styling - the fix is local to the card")
    void ghostStillSuitsDarkBackgrounds() throws Exception {
        CustomButton[] holder = new CustomButton[1];
        onEdt(() -> holder[0] = new CustomButton("Exit", CustomButton.Variant.GHOST));
        CustomButton ghost = holder[0];
        BufferedImage onDark = new BufferedImage(200, 60, BufferedImage.TYPE_INT_RGB);
        onEdt(() -> {
            ghost.setSize(200, 60);
            ghost.doLayout();
            Graphics2D g = onDark.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.BG_TOP);
            g.fillRect(0, 0, 200, 60);
            ghost.paint(g);
            g.dispose();
        });
        Color dark = new Color(onDark.getRGB(1, 1));
        double peak = peakContrast(onDark, new Rectangle(0, 0, 200, 60), dark);
        assertTrue(peak >= MIN_LABEL_CONTRAST,
                "GHOST must stay usable on a dark background (MainMenuView's Exit button), but its peak"
                        + " contrast there is only " + String.format("%.2f", peak));
    }
}
