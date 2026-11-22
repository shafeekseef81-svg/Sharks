package Model;

import java.util.List;
import java.util.Objects;

/**
 * One educational multiple-choice question: four answers of which exactly one is correct (SRS FR-34).
 * Task T2-08, owner: Mohamed Zereny.
 *
 * <p>This is the <b>Product</b> of the {@link QuestionFactory} <i>Factory Method</i> pattern: the thing the
 * creators make and the only question type the rest of the game knows.
 *
 * <p>A question is immutable, so the same object can be shown to the player, judged and logged without any
 * risk of it changing in between. Editing questions is the administrator feature of Iteration 3
 * (FR-56..FR-61) and is deliberately not offered here.
 *
 * @param id          a stable identifier such as {@code "Q-007"}
 * @param level       the level of the question, which is also the colour of its square (FR-34, FR-35)
 * @param text        the question itself
 * @param answers     exactly four answers, all different and none blank
 * @param correctIndex the position of the correct answer in {@code answers}, 0 to 3
 */
public record Question(String id, Difficulty level, String text, List<String> answers, int correctIndex) {

    /** A question always offers this many answers (FR-34). */
    public static final int ANSWER_COUNT = 4;

    /**
     * @throws NullPointerException if any argument, or any single answer, is null
     * @throws IllegalArgumentException if the id or the text is blank, if there are not exactly four
     *         different non-blank answers, or if {@code correctIndex} is not one of them
     */
    public Question {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(answers, "answers");
        if (id.isBlank()) {
            throw new IllegalArgumentException("A question needs an id");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("Question " + id + " has no text");
        }
        if (answers.size() != ANSWER_COUNT) {
            throw new IllegalArgumentException(
                    "Question " + id + " must offer " + ANSWER_COUNT + " answers, not " + answers.size());
        }
        answers = List.copyOf(answers);
        for (String answer : answers) {
            if (answer.isBlank()) {
                throw new IllegalArgumentException("Question " + id + " has a blank answer");
            }
        }
        if (answers.stream().map(String::strip).distinct().count() != ANSWER_COUNT) {
            throw new IllegalArgumentException("Question " + id + " repeats an answer");
        }
        if (correctIndex < 0 || correctIndex >= ANSWER_COUNT) {
            throw new IllegalArgumentException(
                    "Question " + id + ": the correct answer must be 0-" + (ANSWER_COUNT - 1)
                            + ", not " + correctIndex);
        }
    }

    /**
     * Convenience constructor for the question bank.
     *
     * @param answers the four answers, in the order they are shown
     */
    public Question(String id, Difficulty level, String text, int correctIndex, String... answers) {
        this(id, level, text, List.of(answers), correctIndex);
    }

    /**
     * @param index the position of an answer, 0 to 3
     * @return that answer
     * @throws IndexOutOfBoundsException if the index is not an answer of this question
     */
    public String answer(int index) {
        return answers.get(index);
    }

    /** @return the text of the correct answer, which the player is shown after answering (FR-36) */
    public String correctAnswer() {
        return answers.get(correctIndex);
    }

    /**
     * @param index the answer the player chose
     * @return {@code true} if that is the correct answer (FR-36); {@code false} for any other value,
     *         including one outside the four answers
     */
    public boolean isCorrect(int index) {
        return index == correctIndex;
    }

    /** @return the number of steps a right or wrong answer moves a piece at this level: 1, 2 or 3 (FR-35) */
    public int steps() {
        return level.questionSteps();
    }
}
