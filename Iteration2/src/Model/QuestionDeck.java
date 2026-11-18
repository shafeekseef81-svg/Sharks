package Model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * The questions of one level, shuffled, dealt one at a time: no question comes up again until every
 * question of that level has been asked (SRS FR-37). Task T2-08, owner: Mohamed Zereny.
 *
 * <p>When the last question of the round has been dealt, the deck is shuffled again and a new round
 * begins, so a long game never runs out of questions.
 *
 * <p>The shuffling uses the {@link Random} it is given, so a game started with a fixed seed asks the same
 * questions in the same order, which is what makes the tests of this task repeatable.
 *
 * <p>This is a reworked version of {@code QuestionBank.Deck} from the Assignment 3 prototype, taken through
 * the reuse gate (SDP 6.1). Two things changed. The prototype fell back to the questions of <i>all</i>
 * levels when a level had none of its own, which quietly breaks FR-34; this deck refuses an empty level
 * when it is built, so the problem shows up at the start of a game instead of in the middle of one. And a
 * deck here holds one level only, which is what the {@link QuestionFactory} of each level needs.
 */
public final class QuestionDeck {

    private final Difficulty level;
    private final List<Question> questions;
    private final Random random;
    private final Deque<Question> remaining = new ArrayDeque<>();
    private int round;

    /**
     * @param level the level of every question in this deck
     * @param questions the questions of that level, at least one
     * @param random the random source used to shuffle; a fixed seed repeats the order
     * @throws NullPointerException if an argument or a question is null
     * @throws IllegalArgumentException if there are no questions, or one of them belongs to another level
     */
    public QuestionDeck(Difficulty level, List<Question> questions, Random random) {
        this.level = Objects.requireNonNull(level, "level");
        this.random = Objects.requireNonNull(random, "random");
        Objects.requireNonNull(questions, "questions");
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("There is no " + level.label() + " question to ask (FR-34)");
        }
        for (Question q : questions) {
            if (Objects.requireNonNull(q, "question").level() != level) {
                throw new IllegalArgumentException(
                        "Question " + q.id() + " is " + q.level().label() + ", not " + level.label());
            }
        }
        this.questions = List.copyOf(questions);
    }

    /** @return the level of this deck */
    public Difficulty level() {
        return level;
    }

    /** @return how many different questions this level has */
    public int size() {
        return questions.size();
    }

    /** @return how many questions are left before the deck is shuffled again */
    public int remaining() {
        return remaining.size();
    }

    /**
     * How many full rounds through the level have been dealt. It is 0 before the first question and goes up
     * by one every time the deck is shuffled again, which lets a test see that FR-37 held for a whole round.
     *
     * @return the number of shuffles so far
     */
    public int round() {
        return round;
    }

    /**
     * Deals the next question of this level (FR-37).
     *
     * @return a question that has not been asked since the deck was last shuffled
     */
    public Question deal() {
        if (remaining.isEmpty()) {
            List<Question> shuffled = new ArrayList<>(questions);
            Collections.shuffle(shuffled, random);
            remaining.addAll(shuffled);
            round++;
        }
        return remaining.poll();
    }
}
