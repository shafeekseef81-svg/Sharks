package model;

import java.util.Random;

/** Surprise outcomes with the probabilities of SRS Table 2 (FR-39..FR-42). */
public enum SurpriseOutcome {
    JUMP_TO_FINISH(10, "Jump to the finish!",
            "Your piece jumps straight to its finishing square, ignoring exact steps and blocks."),
    OPPONENT_BACK(10, "Push an opponent back",
            "A regular die is rolled: an opponent piece on the shared track moves back half of the result, rounded up."),
    BACK_TO_CASTLE(10, "Back to the castle",
            "Oh no! The piece that landed here returns to its castle."),
    BACK_THREE(30, "Three steps back",
            "The piece that landed here moves three steps backward."),
    SKIP_TURN(30, "Skip your next turn",
            "You will miss your next turn, and any extra roll from a 6 is cancelled."),
    EXTRA_ROLL(10, "Extra roll!",
            "Roll again right away with the Regular + UltraBoost dice. This roll does not count for the rule of 6.");

    private final int percent;
    private final String title;
    private final String description;

    SurpriseOutcome(int percent, String title, String description) {
        this.percent = percent;
        this.title = title;
        this.description = description;
    }

    public int percent() {
        return percent;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    /** True for outcomes that act on the piece that landed on the surprise square. */
    public boolean needsOwnPiece() {
        return this == JUMP_TO_FINISH || this == BACK_TO_CASTLE || this == BACK_THREE;
    }

    /** Draws one outcome with the Table 2 probabilities. */
    public static SurpriseOutcome draw(Random random) {
        int x = random.nextInt(100);
        int cumulative = 0;
        for (SurpriseOutcome o : values()) {
            cumulative += o.percent;
            if (x < cumulative) {
                return o;
            }
        }
        return BACK_THREE;
    }
}
