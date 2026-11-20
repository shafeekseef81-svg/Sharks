package Model;

import java.util.Random;

/**
 * <b>Concrete Creator</b> of the {@link QuestionFactory} <i>Factory Method</i> pattern: the Easy questions
 * (SRS FR-34, FR-35, FR-37). Task T2-08, owner: Mohamed Zereny.
 *
 * <p>It owns the {@link QuestionDeck} of the Easy level and deals from it, so no Easy question comes up
 * again until all of them have been asked. A right answer to one of its questions moves a piece 1 steps
 * forward, and a wrong answer the same 1 steps back (FR-35).
 */
public final class EasyQuestionFactory extends QuestionFactory {

    private final QuestionDeck deck;

    /**
     * @param bank the questions of the game
     * @param random the random source of the game; a fixed seed repeats the order
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the bank holds no Easy question (FR-34)
     */
    public EasyQuestionFactory(QuestionBank bank, Random random) {
        this.deck = new QuestionDeck(Difficulty.EASY, bank.byLevel(Difficulty.EASY), random);
    }

    @Override
    public Difficulty level() {
        return Difficulty.EASY;
    }

    /**
     * The factory method: deals the next Easy question of this game (FR-37).
     *
     * @return a Easy question that has not been asked since the deck was last shuffled
     */
    @Override
    protected Question createQuestion() {
        return deck.deal();
    }

    /** @return the deck this creator deals from, so a test can see how far through the level it is */
    QuestionDeck deck() {
        return deck;
    }
}
