package Model;

/**
 * Identifies a game level. The same enum is used for the game level and for the level of a question.
 *
 * <p>The values of a level are defined only in {@link LevelConfig} (NFR-19). The accessors below read them
 * from there, so code that asks a {@code Difficulty} for its settings keeps working.
 */
public enum Difficulty {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** @return the settings of this level */
    public LevelConfig config() {
        return LevelConfig.of(this);
    }

    /** Board grid size: 15, 17 or 19 cells per side. */
    public int gridSize() {
        return config().gridSize();
    }

    /** Number of extra rings drawn around the classic 15x15 board. */
    public int outerRings() {
        return config().outerRings();
    }

    public int[] trackSizes() {
        return config().trackSizes();
    }

    public int totalTrackSquares() {
        return config().totalTrackSquares();
    }

    /** For example "52 + 64 = 116", or "52" for Easy. */
    public String trackFormula() {
        return config().trackFormula();
    }

    public int protectionSquares() {
        return config().protectionSquares();
    }

    public int arrowSquares() {
        return config().arrowSquares();
    }

    public int questionSquares() {
        return config().questionSquares();
    }

    /** Question squares of each question level on this board (2 on Medium, 3 on Hard). */
    public int questionsPerLevel() {
        return config().questionsPerLevel();
    }

    public int surpriseSquares() {
        return config().surpriseSquares();
    }

    /** Boost, UltraBoost or NONE (Easy uses the regular die only). */
    public DieType specialDie() {
        return config().specialDie();
    }

    /** Steps moved for a correct (forward) or wrong (backward) answer at this question level (FR-35). */
    public int questionSteps() {
        return config().questionSteps();
    }

    public String diceDescription() {
        return config().diceDescription();
    }
}
