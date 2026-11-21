package Model;

import java.util.Random;

/**
 * The six things a surprise square can do, with the probability of each one (SRS FR-39). Task T2-09,
 * owner: Mohamed Zereny.
 *
 * <p><b>Reuse gate (SDP 6.1).</b> The six outcomes, their texts and their probabilities are reused from the
 * Assignment 3 prototype ({@code Assignment3_Prototype/src/model/SurpriseOutcome.java}). What is new here is
 * the split between <i>drawing</i> an outcome and <i>carrying it out</i>: the prototype moved the pieces
 * itself inside its controller, while this enum only names the outcome. {@link SurpriseFactory} draws it,
 * {@link SurpriseResult} says what must happen, and the effect queue of T2-10 does it.
 *
 * <p><b>Probabilities.</b> The table below matches the prototype's table, which
 * {@code Assignment3_Prototype/PROTOTYPE_TRACEABILITY.md} records as the probabilities of SRS Table 2. The
 * Product Owner has since checked these six values against the full submitted Sharks SRS and the original
 * project specification and confirmed them. Those documents are <b>not</b> committed to this repository: the
 * {@code SRS_Sharks.pdf} and {@code SRS_Sharks.docx} that are here are version 1.0 and stop before the
 * requirement tables, so the confirmation rests on the Product Owner's reading of documents held outside the
 * repository, not on anything a reader can check from a clone.
 *
 * <table border="1">
 *   <caption>Outcome probabilities (FR-39)</caption>
 *   <tr><th>Outcome</th><th>Chance</th><th>Draw range of {@link #at(int)}</th></tr>
 *   <tr><td>{@link #JUMP_TO_FINISH}</td><td>10%</td><td>0-9</td></tr>
 *   <tr><td>{@link #OPPONENT_BACK}</td><td>10%</td><td>10-19</td></tr>
 *   <tr><td>{@link #BACK_TO_CASTLE}</td><td>10%</td><td>20-29</td></tr>
 *   <tr><td>{@link #BACK_THREE}</td><td>30%</td><td>30-59</td></tr>
 *   <tr><td>{@link #SKIP_TURN}</td><td>30%</td><td>60-89</td></tr>
 *   <tr><td>{@link #EXTRA_ROLL}</td><td>10%</td><td>90-99</td></tr>
 * </table>
 *
 * <p>The percentages add up to exactly {@value #PROBABILITY_TOTAL}; the class refuses to load otherwise, so a
 * later edit of the table cannot quietly leave a gap in which no outcome would be drawn.
 */
public enum SurpriseOutcome {

    /** The piece jumps straight to its finishing square, ignoring exact steps and Double Blocks (FR-31, FR-41). */
    JUMP_TO_FINISH(10, "Jump to the finish!",
            "Your piece jumps straight to its finishing square, ignoring exact steps and blocks."),

    /**
     * A Regular die is rolled and an opponent piece chosen by the active player moves back half of it,
     * rounded up (FR-40).
     */
    OPPONENT_BACK(10, "Push an opponent back",
            "A regular die is rolled: an opponent piece on the shared track moves back half of the result, "
                    + "rounded up. You choose which piece."),

    /** The piece that landed on the square goes back to its castle. */
    BACK_TO_CASTLE(10, "Back to the castle",
            "Oh no! The piece that landed here returns to its castle."),

    /** The piece that landed on the square moves three steps backward, stopping at its start square (FR-32). */
    BACK_THREE(30, "Three steps back",
            "The piece that landed here moves three steps backward."),

    /** The active player misses the next turn; a bonus roll from a 6 is cancelled with it (FR-22). */
    SKIP_TURN(30, "Skip your next turn",
            "You will miss your next turn, and any extra roll from a 6 is cancelled."),

    /** The active player rolls again at once with the Regular + UltraBoost dice; it is not a Rule-of-6 roll. */
    EXTRA_ROLL(10, "Extra roll!",
            "Roll again right away with the Regular + UltraBoost dice. This roll does not count for the rule of 6.");

    /** The percentages of all outcomes together (FR-39). */
    public static final int PROBABILITY_TOTAL = 100;

    /** Steps the {@link #BACK_THREE} outcome moves the piece backward. */
    public static final int BACK_THREE_STEPS = 3;

    static {
        int sum = 0;
        for (SurpriseOutcome outcome : values()) {
            if (outcome.percent <= 0) {
                throw new IllegalStateException(outcome + " must have a positive chance, not " + outcome.percent);
            }
            sum += outcome.percent;
        }
        if (sum != PROBABILITY_TOTAL) {
            throw new IllegalStateException("The surprise probabilities must add up to "
                    + PROBABILITY_TOTAL + "% (FR-39), not " + sum + "%");
        }
    }

    private final int percent;
    private final String title;
    private final String description;

    SurpriseOutcome(int percent, String title, String description) {
        this.percent = percent;
        this.title = title;
        this.description = description;
    }

    /** @return the chance of this outcome in percent (FR-39) */
    public int percent() {
        return percent;
    }

    /** @return the short heading of the surprise dialog of T2-11 */
    public String title() {
        return title;
    }

    /** @return the sentence that explains the outcome to the player */
    public String description() {
        return description;
    }

    /** @return true for the outcomes that act on the piece that landed on the surprise square */
    public boolean needsOwnPiece() {
        return this == JUMP_TO_FINISH || this == BACK_TO_CASTLE || this == BACK_THREE;
    }

    /**
     * @return true for the one outcome that acts on an opponent piece the active player chooses (FR-40),
     *         which is {@link #OPPONENT_BACK}
     */
    public boolean needsOpponentPiece() {
        return this == OPPONENT_BACK;
    }

    /**
     * @return true for the outcomes that change the turn instead of moving a piece, which the turn of T2-13
     *         owns: {@link #SKIP_TURN} and {@link #EXTRA_ROLL}
     */
    public boolean affectsTurnOnly() {
        return this == SKIP_TURN || this == EXTRA_ROLL;
    }

    /**
     * The outcome of one point of the 0-99 probability range, the table above read as one line (FR-39).
     *
     * <p>{@link #draw(Random)} is this method with a random point. Keeping the two apart lets a test check
     * every boundary of the table exactly, without a random source at all.
     *
     * @param percentile a point of the range, 0 to {@value #PROBABILITY_TOTAL} - 1
     * @return the outcome whose share of the range contains that point
     * @throws IllegalArgumentException if the point is outside the range
     */
    public static SurpriseOutcome at(int percentile) {
        if (percentile < 0 || percentile >= PROBABILITY_TOTAL) {
            throw new IllegalArgumentException("A percentile is 0.." + (PROBABILITY_TOTAL - 1)
                    + ", not " + percentile);
        }
        int cumulative = 0;
        for (SurpriseOutcome outcome : values()) {
            cumulative += outcome.percent;
            if (percentile < cumulative) {
                return outcome;
            }
        }
        throw new AssertionError("The probabilities add up to " + PROBABILITY_TOTAL + "%, so "
                + percentile + " is covered");
    }

    /**
     * Draws one outcome with the probabilities of the table above (FR-39).
     *
     * @param random the random source of the game; a fixed seed draws the same surprises again, so a whole
     *        game is repeatable (NFR-13, SDP 6.2)
     * @return the drawn outcome
     * @throws NullPointerException if {@code random} is null
     */
    public static SurpriseOutcome draw(Random random) {
        return at(random.nextInt(PROBABILITY_TOTAL));
    }

    @Override
    public String toString() {
        return title;
    }
}
