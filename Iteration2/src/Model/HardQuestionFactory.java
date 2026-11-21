package Model;

import java.util.Random;

/**
 * <b>Concrete Creator</b> of the {@link QuestionFactory} <i>Factory Method</i> pattern: the Hard questions
 * (SRS FR-34, FR-35, FR-37). Task T2-08, owner: Mohamed Zereny.
 *
 * <p>It owns the {@link QuestionDeck} of the Hard level and deals from it, so no Hard question comes up
 * again until all of them have been asked. A right answer to one of its questions moves a piece 3 steps
 * forward, and a wrong answer the same 3 steps back (FR-35).
 */
public final class HardQuestionFactory extends QuestionFactory {

    private final QuestionDeck deck;

    /**
     * @param bank the questions of the game
     * @param random the random source of the game; a fixed seed repeats the order
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the bank holds no Hard question (FR-34)
     */
    public HardQuestionFactory(QuestionBank bank, Random random) {
        this.deck = new QuestionDeck(Difficulty.HARD, bank.byLevel(Difficulty.HARD), random);
    }

    @Override
    public Difficulty level() {
        return Difficulty.HARD;
    }

    /**
     * The factory method: deals the next Hard question of this game (FR-37).
     *
     * @return a Hard question that has not been asked since the deck was last shuffled
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
