package Model;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The settings of every game level, from SRS Table 1 (NFR-19: level settings in one configuration class).
 * Task 07, owner: Shafeek Saif.
 *
 * <p>{@link Difficulty} only identifies a level; the values belong here. Read them with
 * {@link #of(Difficulty)}. The surprise probabilities of SRS Table 2, also named by NFR-19, are added here with
 * the surprise rules in Iteration 2.
 */
public final class LevelConfig {

    /** Home path squares of each colour, counting the finishing square (SRS Table 1). */
    private static final int HOME_PATH_SQUARES = 6;
    /** One starting square per colour (SRS Table 1). */
    private static final int STARTING_SQUARES = 4;
    /** Cells per side of the classic board that Easy uses. */
    private static final int CLASSIC_GRID_SIZE = 15;

    private static final Map<Difficulty, LevelConfig> LEVELS = new EnumMap<>(Difficulty.class);

    static {
        LEVELS.put(Difficulty.EASY, new LevelConfig(15, new int[] {52}, 4, 4, 0, 0, DieType.NONE, 1));
        LEVELS.put(Difficulty.MEDIUM, new LevelConfig(17, new int[] {52, 64}, 8, 8, 6, 2, DieType.BOOST, 2));
        LEVELS.put(Difficulty.HARD, new LevelConfig(19, new int[] {52, 64, 72}, 12, 12, 9, 3, DieType.ULTRA_BOOST, 3));
    }

    private final int gridSize;
    private final int[] trackSizes;
    private final int protectionSquares;
    private final int arrowSquares;
    private final int questionSquares;
    private final int surpriseSquares;
    private final DieType specialDie;
    private final int questionSteps;

    private LevelConfig(int gridSize, int[] trackSizes, int protectionSquares, int arrowSquares,
                        int questionSquares, int surpriseSquares, DieType specialDie, int questionSteps) {
        this.gridSize = gridSize;
        this.trackSizes = trackSizes;
        this.protectionSquares = protectionSquares;
        this.arrowSquares = arrowSquares;
        this.questionSquares = questionSquares;
        this.surpriseSquares = surpriseSquares;
        this.specialDie = specialDie;
        this.questionSteps = questionSteps;
    }

    /**
     * @param difficulty a game level
     * @return the settings of that level
     * @throws NullPointerException if {@code difficulty} is null
     */
    public static LevelConfig of(Difficulty difficulty) {
        return LEVELS.get(Objects.requireNonNull(difficulty, "difficulty"));
    }

    /** @return the board grid size: 15, 17 or 19 cells per side */
    public int gridSize() {
        return gridSize;
    }

    /** @return the number of extra rings drawn around the classic 15x15 board */
    public int outerRings() {
        return (gridSize - CLASSIC_GRID_SIZE) / 2;
    }

    /** @return a copy of the size of each shared track, innermost first */
    public int[] trackSizes() {
        return trackSizes.clone();
    }

    /** @return the number of shared-track squares on the whole board */
    public int totalTrackSquares() {
        return Arrays.stream(trackSizes).sum();
    }

    /** @return for example "52 + 64 = 116", or "52" for Easy */
    public String trackFormula() {
        if (trackSizes.length == 1) {
            return String.valueOf(trackSizes[0]);
        }
        return Arrays.stream(trackSizes).mapToObj(String::valueOf).collect(Collectors.joining(" + "))
                + " = " + totalTrackSquares();
    }

    /** @return the home path squares of each colour, counting the finishing square */
    public int homePathSquares() {
        return HOME_PATH_SQUARES;
    }

    /** @return the number of starting squares on the board */
    public int startingSquares() {
        return STARTING_SQUARES;
    }

    /** @return the number of protection squares on the board */
    public int protectionSquares() {
        return protectionSquares;
    }

    /** @return the number of arrow squares on the board */
    public int arrowSquares() {
        return arrowSquares;
    }

    /** @return the number of question squares on the board */
    public int questionSquares() {
        return questionSquares;
    }

    /** @return the question squares of each question level (2 on Medium, 3 on Hard) */
    public int questionsPerLevel() {
        return questionSquares / 3;
    }

    /** @return the number of surprise squares on the board */
    public int surpriseSquares() {
        return surpriseSquares;
    }

    /** @return Boost, UltraBoost or NONE (Easy uses the Regular die only) */
    public DieType specialDie() {
        return specialDie;
    }

    /** @return the steps moved for a correct (forward) or wrong (backward) answer at this level (FR-35) */
    public int questionSteps() {
        return questionSteps;
    }

    /** @return a description of the dice of this level */
    public String diceDescription() {
        return specialDie == DieType.NONE ? "Regular die" : "Regular + " + specialDie.label() + " dice";
    }
}
