package Model;

import java.util.Objects;

/**
 * What came of answering one question: whether the player was right, which answer was correct, and how far
 * the piece moves because of it (SRS FR-35, FR-36). Task T2-08, owner: Mohamed Zereny.
 *
 * <p>Produced by {@link QuestionFactory#judge(Question, int)}. It carries everything the dialog of T2-11
 * needs in order to show the result, and everything the effect queue of T2-10 needs in order to move the
 * piece, without either of them having to know the rule that turned the answer into steps.
 *
 * @param question the question that was asked
 * @param chosenIndex the answer the player chose, or {@link #NO_ANSWER} if the player did not answer
 * @param correct whether that was the correct answer (FR-36)
 * @param steps how far the piece moves: forward for a correct answer, backward (a negative number) for a
 *        wrong one, by 1, 2 or 3 according to the level of the question (FR-35)
 */
public record QuestionResult(Question question, int chosenIndex, boolean correct, int steps) {

    /** Stands for "the player did not answer", which counts as a wrong answer (FR-35). */
    public static final int NO_ANSWER = -1;

    /**
     * @throws NullPointerException if {@code question} is null
     * @throws IllegalArgumentException if the parts contradict each other: the correctness does not match
     *         the chosen answer, or the steps are not the level's steps in the right direction
     */
    public QuestionResult {
        Objects.requireNonNull(question, "question");
        if (correct != question.isCorrect(chosenIndex)) {
            throw new IllegalArgumentException("Answer " + chosenIndex + " of " + question.id()
                    + " is " + (correct ? "not " : "") + "the correct one");
        }
        int expected = correct ? question.steps() : -question.steps();
        if (steps != expected) {
            throw new IllegalArgumentException("A " + (correct ? "correct" : "wrong") + " answer to "
                    + question.id() + " moves " + expected + " steps (FR-35), not " + steps);
        }
    }

    /** @return the text of the correct answer, to show the player after answering (FR-36) */
    public String correctAnswer() {
        return question.correctAnswer();
    }

    /** @return the answer the player chose, or null if the player did not answer */
    public String chosenAnswer() {
        return chosenIndex >= 0 && chosenIndex < Question.ANSWER_COUNT ? question.answer(chosenIndex) : null;
    }

    /** @return true if the piece moves forward (a correct answer), false if it moves backward */
    public boolean forward() {
        return correct;
    }
}
