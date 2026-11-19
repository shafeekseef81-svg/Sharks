package Model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * <b>Factory Method</b> design pattern (GoF): the <i>Creator</i> that a question square asks for a question
 * (SRS FR-34..FR-37). Task T2-08, owner: Mohamed Zereny.
 *
 * <p><b>Problem.</b> A question square carries a level of its own, drawn in its own colour: an Easy, a
 * Medium or a Hard question, whatever the level of the board (SRS Table 1). When a piece stops on such a
 * square the game needs a question <i>of that level</i>, taken from that level's own questions, and a right
 * or wrong answer then moves the piece by that level's own number of steps: 1, 2 or 3 (FR-35). The game
 * must not care how a question of a level is produced, only that it gets one.
 *
 * <p><b>Roles.</b>
 * <ul>
 *   <li><i>Product</i>: {@link Question}.</li>
 *   <li><i>Creator</i>: this class. It declares the factory method {@link #createQuestion()} and uses it in
 *       {@link #ask()}, its own operation, which is the same for every level.</li>
 *   <li><i>Concrete Creators</i>: {@link EasyQuestionFactory}, {@link MediumQuestionFactory} and
 *       {@link HardQuestionFactory}. Each one decides which question it makes: it owns the
 *       {@link QuestionDeck} of its level and deals from it, so FR-37 holds per level.</li>
 * </ul>
 *
 * <p><b>Why not a simple factory.</b> A single {@code static Question create(Difficulty level)} with a
 * {@code switch} over the three levels would also return the right question today, but every new kind of
 * question would mean editing that one method, and the levels could never differ in more than the data they
 * carry. Here the client holds a {@code QuestionFactory} and calls {@link #ask()} without ever naming a
 * level, and a new kind of question is a new subclass that changes nothing that already exists. The
 * {@code switch} is gone from the game and appears exactly once, in {@link #forBank(QuestionBank, Random)},
 * which builds the three creators when a game starts.
 *
 * <p>A factory is tied to one game: it holds that game's {@link Random}, so a seeded game asks the same
 * questions in the same order.
 */
public abstract class QuestionFactory {

    /** Package-private: the concrete creators of this iteration live in this package. */
    QuestionFactory() {
    }

    /**
     * @return the level of every question this creator makes, which is the colour of the square that asks
     */
    public abstract Difficulty level();

    /**
     * <b>The factory method.</b> A concrete creator overrides it to produce the question it is responsible
     * for. It is not called directly by the game: {@link #ask()} calls it.
     *
     * @return a question of this creator's level
     */
    protected abstract Question createQuestion();

    /**
     * The operation of the creator: asks for the next question of this level (FR-34, FR-37). It is the same
     * for every level, and it is the only thing the rest of the game calls.
     *
     * @return the question to put in front of the player
     * @throws IllegalStateException if a concrete creator produced no question, or one of another level
     */
    public final Question ask() {
        Question question = createQuestion();
        if (question == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " produced no question");
        }
        if (question.level() != level()) {
            throw new IllegalStateException(getClass().getSimpleName() + " produced a "
                    + question.level().label() + " question, not " + level().label());
        }
        return question;
    }

    /**
     * Turns the player's answer into the result of the question: whether it was right, which answer was
     * right, and how far the piece moves for it (FR-35, FR-36).
     *
     * <p>A correct answer moves the piece forward by the steps of the question's level, a wrong answer the
     * same number of steps backward, and not answering counts as a wrong answer
     * ({@link QuestionResult#NO_ANSWER}). Carrying the piece out is the work of the effect queue of T2-10;
     * this only says what should happen.
     *
     * @param question the question that was asked, from {@link #ask()}
     * @param chosenIndex the answer the player chose, 0 to 3, or {@link QuestionResult#NO_ANSWER}
     * @return the result of this question
     * @throws NullPointerException if {@code question} is null
     * @throws IllegalArgumentException if the question is not of this creator's level, or the chosen answer
     *         is neither one of the four answers nor {@code NO_ANSWER}
     */
    public final QuestionResult judge(Question question, int chosenIndex) {
        Objects.requireNonNull(question, "question");
        if (question.level() != level()) {
            throw new IllegalArgumentException("A " + level().label() + " square cannot judge the "
                    + question.level().label() + " question " + question.id());
        }
        if (chosenIndex != QuestionResult.NO_ANSWER
                && (chosenIndex < 0 || chosenIndex >= Question.ANSWER_COUNT)) {
            throw new IllegalArgumentException("Answer " + chosenIndex + " is not one of the "
                    + Question.ANSWER_COUNT + " answers of " + question.id());
        }
        boolean correct = question.isCorrect(chosenIndex);
        return new QuestionResult(question, chosenIndex, correct,
                correct ? question.steps() : -question.steps());
    }

    /**
     * Builds the creator of every level for one game: the only place that names the three concrete
     * creators. A game keeps the map and looks up the creator of a square by
     * {@link Square#questionLevel()}; nothing else in the game chooses between levels.
     *
     * @param bank the questions of the game (FR-34)
     * @param random the random source of the game; a fixed seed repeats the order of the questions
     * @return one creator per level
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the bank has no question of some level
     */
    public static Map<Difficulty, QuestionFactory> forBank(QuestionBank bank, Random random) {
        Objects.requireNonNull(bank, "bank");
        Objects.requireNonNull(random, "random");
        Map<Difficulty, QuestionFactory> factories = new EnumMap<>(Difficulty.class);
        factories.put(Difficulty.EASY, new EasyQuestionFactory(bank, random));
        factories.put(Difficulty.MEDIUM, new MediumQuestionFactory(bank, random));
        factories.put(Difficulty.HARD, new HardQuestionFactory(bank, random));
        return Map.copyOf(factories);
    }

    @Override
    public String toString() {
        return level().label() + " questions";
    }
}
