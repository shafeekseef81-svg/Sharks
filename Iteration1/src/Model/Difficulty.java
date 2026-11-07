package Model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Level configuration from SRS Table 1 (NFR-19: all level settings in one place).
 * The same enum is used for the game level and for the level of a question.
 */
public enum Difficulty {
    EASY("Easy", 15, new int[] {52}, 4, 4, 0, 0, DieType.NONE, 1),
    MEDIUM("Medium", 17, new int[] {52, 64}, 8, 8, 6, 2, DieType.BOOST, 2),
    HARD("Hard", 19, new int[] {52, 64, 72}, 12, 12, 9, 3, DieType.ULTRA_BOOST, 3);

    private final String label;
    private final int gridSize;
    private final int[] trackSizes;
    private final int protectionSquares;
    private final int arrowSquares;
    private final int questionSquares;
    private final int surpriseSquares;
    private final DieType specialDie;
    private final int questionSteps;

    Difficulty(String label, int gridSize, int[] trackSizes, int protectionSquares, int arrowSquares,
               int questionSquares, int surpriseSquares, DieType specialDie, int questionSteps) {
        this.label = label;
        this.gridSize = gridSize;
        this.trackSizes = trackSizes;
        this.protectionSquares = protectionSquares;
        this.arrowSquares = arrowSquares;
        this.questionSquares = questionSquares;
        this.surpriseSquares = surpriseSquares;
        this.specialDie = specialDie;
        this.questionSteps = questionSteps;
    }

    public String label() {
        return label;
    }

    /** Board grid size: 15, 17 or 19 cells per side. */
    public int gridSize() {
        return gridSize;
    }

    /** Number of extra rings drawn around the classic 15x15 board. */
    public int outerRings() {
        return (gridSize - 15) / 2;
    }

    public int[] trackSizes() {
        return trackSizes.clone();
    }

    public int totalTrackSquares() {
        return Arrays.stream(trackSizes).sum();
    }

    /** For example "52 + 64 = 116", or "52" for Easy. */
    public String trackFormula() {
        if (trackSizes.length == 1) {
            return String.valueOf(trackSizes[0]);
        }
        return Arrays.stream(trackSizes).mapToObj(String::valueOf).collect(Collectors.joining(" + "))
                + " = " + totalTrackSquares();
    }

    public int protectionSquares() {
        return protectionSquares;
    }

    public int arrowSquares() {
        return arrowSquares;
    }

    public int questionSquares() {
        return questionSquares;
    }

    /** Question squares of each question level on this board (2 on Medium, 3 on Hard). */
    public int questionsPerLevel() {
        return questionSquares / 3;
    }

    public int surpriseSquares() {
        return surpriseSquares;
    }

    /** Boost, UltraBoost or NONE (Easy uses the regular die only). */
    public DieType specialDie() {
        return specialDie;
    }

    /** Steps moved for a correct (forward) or wrong (backward) answer at this question level (FR-35). */
    public int questionSteps() {
        return questionSteps;
    }

    public String diceDescription() {
        return specialDie == DieType.NONE ? "Regular die" : "Regular + " + specialDie.label() + " dice";
    }
}
