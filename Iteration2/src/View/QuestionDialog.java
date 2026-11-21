package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Model.Question;
import Model.QuestionResult;
import View.components.CustomButton;
import View.components.CustomPanel;
import View.components.Icons;
import View.components.Theme;
import View.components.UiKit;

/**
 * The card that asks one educational question and then shows how it went (SRS FR-34, FR-35, FR-36; NFR-05).
 * Task T2-11, owner: Mohamed Zereny.
 *
 * <p><b>What it does.</b> It shows the level of the square, whose turn it is, the question and its four
 * answers (FR-34). The player clicks one answer; the card then shows whether that was right, which answer was
 * the right one, and how far the piece moves because of it (FR-36). A <i>Continue</i> button hands the result
 * back to the caller.
 *
 * <p><b>What it does not do (MVC, NFR-08).</b> It does not know which answer is correct, it does not work out
 * how far the piece moves, and it moves nothing. Judging the answer is {@link Model.QuestionFactory#judge}
 * in the Model, which the caller passes in as a {@link Judge}; this card only displays the
 * {@link QuestionResult} it gets back. Carrying the movement out is the effect queue of T2-10, and the turn is
 * T2-13.
 *
 * <p><b>One answer only.</b> The first click wins. Every answer button is disabled as soon as one is pressed,
 * and {@link #submit(int)} refuses a second call, so a double click or a stray key cannot answer twice or
 * judge the same question again (FR-37 counts a question as asked once).
 *
 * <p>Built to be shown inside a {@link View.components.ModalOverlay}, like {@link ConfirmDialog}. No window of
 * its own is created, so it can also be laid out in a headless test.
 */
public class QuestionDialog extends CustomPanel {

    /** Width of the card in pixels. */
    private static final int CARD_WIDTH = 560;

    /** Name of this card, for tests and for {@code findByName} lookups. */
    public static final String NAME = "dialog.question";

    /** Name prefix of the four answer buttons: {@code question.answer.0} to {@code question.answer.3}. */
    public static final String ANSWER_NAME = "question.answer.";

    /** Name of the button that closes the card after the answer was shown. */
    public static final String CONTINUE_NAME = "question.continue";

    /**
     * Turns the player's choice into the result of the question. Implemented in the Model by
     * {@link Model.QuestionFactory#judge(Question, int)}; the caller passes it in so that no rule lives in
     * the view.
     */
    public interface Judge {

        /**
         * @param chosenIndex the answer the player clicked, 0 to 3, or {@link QuestionResult#NO_ANSWER}
         * @return what came of it (FR-35, FR-36)
         */
        QuestionResult judge(int chosenIndex);
    }

    private final Question question;
    private final Judge judge;
    private final Consumer<QuestionResult> onContinue;
    private final List<CustomButton> answers = new ArrayList<>();
    private final JPanel feedback = new JPanel();
    private final CustomButton continueButton = new CustomButton("Continue", CustomButton.Variant.PRIMARY);
    private QuestionResult result;
    private boolean continued;

    /**
     * @param question the question to ask, from the factory of T2-08 (FR-34)
     * @param playerName the name of the player who answers; FR-34 gives the answer to the active player
     * @param judge the Model rule that judges the answer
     * @param onContinue called once, with the result, when the player closes the card
     * @throws NullPointerException if an argument is null
     */
    public QuestionDialog(Question question, String playerName, Judge judge,
                          Consumer<QuestionResult> onContinue) {
        super(new BorderLayout(0, 16), Style.CARD);
        this.question = Objects.requireNonNull(question, "question");
        this.judge = Objects.requireNonNull(judge, "judge");
        this.onContinue = Objects.requireNonNull(onContinue, "onContinue");
        Objects.requireNonNull(playerName, "playerName");
        setPadding(22, 26, 20, 26);
        setName(NAME);

        add(header(playerName), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
    }

    private JPanel header(String playerName) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.add(new ConfirmDialog.IconBadge(Icons.Type.QUESTION, Theme.level(question.level())),
                BorderLayout.WEST);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(UiKit.label("Question square", Theme.font(Font.BOLD, 19f), Theme.TEXT));
        texts.add(UiKit.vgap(5));
        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        tags.setOpaque(false);
        JLabel level = UiKit.pill(question.level().label(), Theme.level(question.level()), Color.WHITE);
        level.setName("question.level");
        tags.add(level);
        JLabel steps = UiKit.label(question.steps() + " steps at stake",
                Theme.font(Font.PLAIN, 13.5f), Theme.TEXT_MUTED);
        steps.setName("question.steps");
        tags.add(steps);
        texts.add(tags);
        texts.add(UiKit.vgap(5));
        JLabel turn = UiKit.label(UiKit.ltr(playerName + " answers"),
                Theme.font(Font.PLAIN, 14f), Theme.TEXT_MUTED);
        turn.setName("question.player");
        texts.add(turn);
        row.add(texts, BorderLayout.CENTER);
        return row;
    }

    private JPanel body() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel text = UiKit.wrap(question.text(), CARD_WIDTH - 60, Theme.font(Font.BOLD, 16f), Theme.TEXT);
        text.setName("question.text");
        column.add(text);
        column.add(UiKit.vgap(14));

        JPanel grid = new JPanel(new GridLayout(Question.ANSWER_COUNT, 1, 0, 8));
        grid.setOpaque(false);
        for (int i = 0; i < Question.ANSWER_COUNT; i++) {
            final int index = i;
            CustomButton button = new CustomButton((char) ('A' + i) + ".  " + question.answer(i),
                    CustomButton.Variant.SECONDARY);
            button.setName(ANSWER_NAME + i);
            button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            button.addActionListener(e -> submit(index));
            answers.add(button);
            grid.add(button);
        }
        column.add(grid);

        feedback.setOpaque(false);
        feedback.setLayout(new BoxLayout(feedback, BoxLayout.Y_AXIS));
        feedback.setName("question.feedback");
        feedback.setVisible(false);
        column.add(UiKit.vgap(12));
        column.add(feedback);
        return column;
    }

    private JPanel footer() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row.setOpaque(false);
        continueButton.setName(CONTINUE_NAME);
        continueButton.setVisible(false);
        continueButton.addActionListener(e -> {
            if (result != null && !continued) {
                continued = true;
                continueButton.setEnabled(false);
                onContinue.accept(result);
            }
        });
        row.add(continueButton);
        return row;
    }

    /**
     * Answers the question, as clicking an answer button does. A controller may call it with
     * {@link QuestionResult#NO_ANSWER} when the player does not answer, which FR-35 counts as a wrong answer.
     *
     * <p>The first call wins: later calls are ignored, so the question is never judged twice.
     *
     * @param chosenIndex the answer, 0 to 3, or {@link QuestionResult#NO_ANSWER}
     * @throws IllegalStateException if the Model rule returned no result
     */
    public void submit(int chosenIndex) {
        if (result != null) {
            return;
        }
        QuestionResult judged = judge.judge(chosenIndex);
        if (judged == null) {
            throw new IllegalStateException("The Model judged " + question.id() + " as nothing");
        }
        result = judged;
        for (CustomButton button : answers) {
            button.setEnabled(false);
        }
        showFeedback(chosenIndex, judged);
        continueButton.setVisible(true);
        continueButton.setText(judged.correct() ? "Great, continue" : "Continue");
        revalidate();
        repaint();
    }

    private void showFeedback(int chosenIndex, QuestionResult judged) {
        Color accent = judged.correct() ? Theme.SUCCESS : Theme.DANGER;
        if (chosenIndex >= 0 && chosenIndex < answers.size()) {
            CustomButton chosen = answers.get(chosenIndex);
            chosen.setAccent(accent);
        }
        if (!judged.correct()) {
            answers.get(question.correctIndex()).setAccent(Theme.SUCCESS);
        }

        JLabel verdict = UiKit.label(judged.correct() ? "Correct!" : "Not correct",
                Theme.font(Font.BOLD, 16f), accent);
        verdict.setIcon(Icons.get(judged.correct() ? Icons.Type.CHECK : Icons.Type.CROSS, 17, accent));
        verdict.setIconTextGap(8);
        verdict.setName("question.verdict");
        feedback.add(verdict);

        feedback.add(UiKit.vgap(5));
        JLabel correct = UiKit.wrap("The correct answer is: " + judged.correctAnswer(),
                CARD_WIDTH - 60, Theme.font(Font.PLAIN, 14f), Theme.TEXT);
        correct.setName("question.correctAnswer");
        feedback.add(correct);

        feedback.add(UiKit.vgap(4));
        int steps = judged.steps();
        JLabel movement = UiKit.wrap(movementText(steps), CARD_WIDTH - 60,
                Theme.font(Font.PLAIN, 14f), Theme.TEXT_MUTED);
        movement.setName("question.movement");
        feedback.add(movement);
        feedback.setVisible(true);
    }

    /** Reads the signed steps the Model produced; it does not work them out. */
    private static String movementText(int steps) {
        int size = Math.abs(steps);
        String squares = size == 1 ? "square" : "squares";
        return steps > 0
                ? "Your piece moves " + size + " " + squares + " forward."
                : "Your piece moves " + size + " " + squares + " backward.";
    }

    /** @return the result of the question, or null while it has not been answered */
    public QuestionResult result() {
        return result;
    }

    /** @return true once the question has been answered, so it cannot be answered again */
    public boolean answered() {
        return result != null;
    }

    /** @return the question this card asks */
    public Question question() {
        return question;
    }

    /**
     * @param index 0 to 3
     * @return the button of that answer, for tests and for keyboard shortcuts
     */
    public CustomButton answerButton(int index) {
        return answers.get(index);
    }

    /** @return the button that closes the card; it is hidden until the question has been answered */
    public CustomButton continueButton() {
        return continueButton;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(CARD_WIDTH, super.getPreferredSize().height);
    }
}
