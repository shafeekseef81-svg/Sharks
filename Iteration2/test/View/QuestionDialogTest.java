package View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Difficulty;
import Model.Question;
import Model.QuestionBank;
import Model.QuestionFactory;
import Model.QuestionResult;

/**
 * Tests for the question card of T2-11 (SRS FR-34, FR-35, FR-36; NFR-05). Owner: Mohamed Zereny.
 *
 * <p>The card shows the question and the result; deciding which answer is right and how far the piece moves is
 * {@link QuestionFactory#judge} in the Model (T2-08). These tests therefore wire the card to the <b>real</b>
 * Model rule, as a controller would, and check what the player ends up seeing.
 *
 * <p>No {@link javax.swing.JFrame} is created, so the tests run headless as well. Every Swing component is
 * built and used on the event dispatch thread.
 *
 * <p>Review level for T2-11 is Light (issue #62, reviewer Rakan Ziyan). These are developer tests.
 */
class QuestionDialogTest {

    private final QuestionBank bank = bankOf(4);
    private final Map<Difficulty, QuestionFactory> factories =
            QuestionFactory.forBank(bank, new Random(7));

    /** A bank with a few known questions per level, so a test can predict what is asked. */
    private static QuestionBank bankOf(int perLevel) {
        List<Question> list = new ArrayList<>();
        int n = 0;
        for (Difficulty level : Difficulty.values()) {
            for (int i = 0; i < perLevel; i++) {
                n++;
                list.add(new Question("Q" + n, level, "Question number " + n + "?", 2,
                        "answer " + n + "a", "answer " + n + "b", "answer " + n + "c", "answer " + n + "d"));
            }
        }
        return new QuestionBank(list);
    }

    /** Records how often the Model was asked to judge, and judges with the real rule. */
    private static final class CountingJudge implements QuestionDialog.Judge {
        private final QuestionFactory factory;
        private final Question question;
        private final AtomicInteger calls = new AtomicInteger();

        CountingJudge(QuestionFactory factory, Question question) {
            this.factory = factory;
            this.question = question;
        }

        @Override
        public QuestionResult judge(int chosenIndex) {
            calls.incrementAndGet();
            return factory.judge(question, chosenIndex);
        }
    }

    /** Builds a card on the event dispatch thread and lays it out, without a window. */
    private static QuestionDialog build(Question question, QuestionDialog.Judge judge,
                                        List<QuestionResult> continued) throws Exception {
        QuestionDialog[] holder = new QuestionDialog[1];
        onEdt(() -> {
            holder[0] = new QuestionDialog(question, "Mohamed", judge, continued::add);
            layOut(holder[0]);
        });
        return holder[0];
    }

    private static void onEdt(Runnable action) throws Exception {
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    private static void layOut(Component component) {
        component.setSize(component.getPreferredSize());
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }

    /** The first component with this name, or null. */
    private static Component find(Component root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component hit = find(child, name);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    /** The text of a named label, with any HTML wrapper stripped. */
    private static String text(Component root, String name) {
        Component found = find(root, name);
        assertNotNull(found, "no component named " + name);
        assertTrue(found instanceof JLabel, name + " is not a label");
        return plain(((JLabel) found).getText());
    }

    private static String plain(String html) {
        return html == null ? "" : html.replaceAll("<[^>]*>", "").replace("‎", "").strip();
    }

    // ------------------------------------------------------------------ what the player sees first

    @Test
    @DisplayName("FR-34: the card shows the question, its level and four answers")
    void theCardShowsTheQuestionAndFourAnswers() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.MEDIUM);
        Question question = factory.ask();
        QuestionDialog dialog = build(question, new CountingJudge(factory, question), new ArrayList<>());

        assertEquals(QuestionDialog.NAME, dialog.getName());
        assertEquals(question.text(), text(dialog, "question.text"));
        assertEquals(Difficulty.MEDIUM.label(), text(dialog, "question.level"));
        assertTrue(text(dialog, "question.player").contains("Mohamed"));
        assertEquals(question.steps() + " steps at stake", text(dialog, "question.steps"));

        for (int i = 0; i < Question.ANSWER_COUNT; i++) {
            Component button = find(dialog, QuestionDialog.ANSWER_NAME + i);
            assertNotNull(button, "answer button " + i + " is missing");
            assertTrue(dialog.answerButton(i).getText().endsWith(question.answer(i)),
                    "answer " + i + " shows the wrong text: " + dialog.answerButton(i).getText());
            assertTrue(dialog.answerButton(i).isEnabled());
        }
        assertNull(find(dialog, QuestionDialog.ANSWER_NAME + Question.ANSWER_COUNT),
                "there must be exactly four answers");
    }

    @Test
    @DisplayName("Nothing is given away before the player answers")
    void noFeedbackBeforeAnswering() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.EASY);
        Question question = factory.ask();
        QuestionDialog dialog = build(question, new CountingJudge(factory, question), new ArrayList<>());

        assertFalse(dialog.answered());
        assertNull(dialog.result());
        assertFalse(find(dialog, "question.feedback").isVisible());
        assertFalse(dialog.continueButton().isVisible());
        assertNull(find(dialog, "question.verdict"));
        assertNull(find(dialog, "question.correctAnswer"));
    }

    // ------------------------------------------------------------------ answering (FR-35, FR-36)

    @Test
    @DisplayName("FR-36: a correct answer is reported as correct, with the correct answer and the steps")
    void aCorrectAnswerIsReported() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.HARD);
        Question question = factory.ask();
        List<QuestionResult> continued = new ArrayList<>();
        CountingJudge judge = new CountingJudge(factory, question);
        QuestionDialog dialog = build(question, judge, continued);

        onEdt(() -> dialog.answerButton(question.correctIndex()).doClick());

        assertTrue(dialog.answered());
        assertTrue(dialog.result().correct());
        assertEquals(1, judge.calls.get(), "the Model judged the answer exactly once");
        assertEquals("Correct!", text(dialog, "question.verdict"));
        assertTrue(text(dialog, "question.correctAnswer").contains(question.correctAnswer()));
        assertTrue(text(dialog, "question.movement").contains("forward"),
                text(dialog, "question.movement"));
        assertTrue(text(dialog, "question.movement").contains(String.valueOf(question.steps())));
        assertTrue(dialog.continueButton().isVisible());
        assertTrue(continued.isEmpty(), "the result is handed over only when the player continues");
    }

    @Test
    @DisplayName("FR-36: a wrong answer is reported as wrong and still shows the correct answer")
    void aWrongAnswerIsReported() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.EASY);
        Question question = factory.ask();
        int wrong = (question.correctIndex() + 1) % Question.ANSWER_COUNT;
        QuestionDialog dialog = build(question, new CountingJudge(factory, question), new ArrayList<>());

        onEdt(() -> dialog.answerButton(wrong).doClick());

        assertFalse(dialog.result().correct());
        assertEquals(wrong, dialog.result().chosenIndex());
        assertEquals("Not correct", text(dialog, "question.verdict"));
        assertTrue(text(dialog, "question.correctAnswer").contains(question.correctAnswer()));
        assertTrue(text(dialog, "question.movement").contains("backward"),
                text(dialog, "question.movement"));
    }

    @Test
    @DisplayName("FR-35: the steps shown are the ones the Model produced, for every level and both outcomes")
    void theStepsShownComeFromTheModel() throws Exception {
        for (Difficulty level : Difficulty.values()) {
            QuestionFactory factory = factories.get(level);
            for (boolean correct : new boolean[] {true, false}) {
                Question question = factory.ask();
                int choice = correct ? question.correctIndex()
                        : (question.correctIndex() + 2) % Question.ANSWER_COUNT;
                QuestionDialog dialog =
                        build(question, new CountingJudge(factory, question), new ArrayList<>());
                onEdt(() -> dialog.answerButton(choice).doClick());

                QuestionResult result = dialog.result();
                assertEquals(correct, result.correct(), level + "/" + correct);
                assertTrue(text(dialog, "question.movement")
                                .contains(String.valueOf(Math.abs(result.steps()))),
                        level + "/" + correct + ": " + text(dialog, "question.movement"));
                assertTrue(text(dialog, "question.movement").contains(correct ? "forward" : "backward"),
                        level + "/" + correct);
            }
        }
    }

    // ------------------------------------------------------------------ one answer only

    @Test
    @DisplayName("A second click cannot answer the question again")
    void onlyTheFirstAnswerCounts() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.MEDIUM);
        Question question = factory.ask();
        int first = question.correctIndex();
        int second = (first + 1) % Question.ANSWER_COUNT;
        CountingJudge judge = new CountingJudge(factory, question);
        QuestionDialog dialog = build(question, judge, new ArrayList<>());

        onEdt(() -> dialog.answerButton(first).doClick());
        QuestionResult after = dialog.result();
        onEdt(() -> dialog.answerButton(second).doClick());
        onEdt(() -> dialog.submit(second));

        assertSame(after, dialog.result(), "the result must not change");
        assertEquals(first, dialog.result().chosenIndex());
        assertEquals(1, judge.calls.get(), "the Model must be asked only once");
        for (int i = 0; i < Question.ANSWER_COUNT; i++) {
            assertFalse(dialog.answerButton(i).isEnabled(), "answer " + i + " is still clickable");
        }
    }

    @Test
    @DisplayName("FR-35: not answering is submitted as a wrong answer")
    void notAnsweringCountsAsWrong() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.MEDIUM);
        Question question = factory.ask();
        QuestionDialog dialog = build(question, new CountingJudge(factory, question), new ArrayList<>());

        onEdt(() -> dialog.submit(QuestionResult.NO_ANSWER));

        assertTrue(dialog.answered());
        assertFalse(dialog.result().correct());
        assertEquals(QuestionResult.NO_ANSWER, dialog.result().chosenIndex());
        assertNull(dialog.result().chosenAnswer());
        assertTrue(text(dialog, "question.correctAnswer").contains(question.correctAnswer()));
    }

    @Test
    @DisplayName("The result is handed over once, however often Continue is clicked")
    void theResultIsHandedOverOnce() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.EASY);
        Question question = factory.ask();
        List<QuestionResult> continued = new ArrayList<>();
        QuestionDialog dialog = build(question, new CountingJudge(factory, question), continued);

        onEdt(() -> dialog.continueButton().doClick());
        assertTrue(continued.isEmpty(), "nothing is handed over before the question is answered");

        onEdt(() -> dialog.answerButton(question.correctIndex()).doClick());
        onEdt(() -> dialog.continueButton().doClick());
        onEdt(() -> dialog.continueButton().doClick());

        assertEquals(1, continued.size());
        assertSame(dialog.result(), continued.get(0));
    }

    // ------------------------------------------------------------------ input validation

    @Test
    @DisplayName("The card needs a question, a name, a Model rule and somewhere to send the result")
    void theCardNeedsItsParts() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.EASY);
        Question question = factory.ask();
        QuestionDialog.Judge judge = index -> factory.judge(question, index);

        onEdt(() -> {
            assertThrows(NullPointerException.class,
                    () -> new QuestionDialog(null, "Mohamed", judge, r -> { }));
            assertThrows(NullPointerException.class,
                    () -> new QuestionDialog(question, null, judge, r -> { }));
            assertThrows(NullPointerException.class,
                    () -> new QuestionDialog(question, "Mohamed", null, r -> { }));
            assertThrows(NullPointerException.class,
                    () -> new QuestionDialog(question, "Mohamed", judge, null));
        });
    }

    @Test
    @DisplayName("A Model rule that judges nothing is a fault, not a silent empty card")
    void aJudgeThatReturnsNothingFails() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.EASY);
        Question question = factory.ask();
        QuestionDialog dialog = build(question, index -> null, new ArrayList<>());

        onEdt(() -> assertThrows(IllegalStateException.class, () -> dialog.submit(0)));
        assertFalse(dialog.answered());
    }

    @Test
    @DisplayName("An invalid answer index is refused by the Model, and the card stays unanswered")
    void anInvalidAnswerIndexIsRefused() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.EASY);
        Question question = factory.ask();
        QuestionDialog dialog = build(question, new CountingJudge(factory, question), new ArrayList<>());

        onEdt(() -> assertThrows(IllegalArgumentException.class,
                () -> dialog.submit(Question.ANSWER_COUNT)));
        assertFalse(dialog.answered());
        assertTrue(dialog.answerButton(0).isEnabled(), "the question can still be answered");
    }

    // ------------------------------------------------------------------ NFR-05, partly

    @Test
    @DisplayName("NFR-05 (part): building and laying out the card is far under one second")
    void buildingTheCardIsFast() throws Exception {
        QuestionFactory factory = factories.get(Difficulty.HARD);
        Question question = factory.ask();
        // warm up the Swing and HTML machinery, so the measurement is of the card and not of class loading
        build(question, new CountingJudge(factory, question), new ArrayList<>());

        Question measured = factory.ask();
        long start = System.nanoTime();
        QuestionDialog dialog = build(measured, index -> factory.judge(measured, index), new ArrayList<>());
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertNotNull(dialog);
        assertTrue(millis < 500, "the card took " + millis + " ms to build and lay out");
    }
}
