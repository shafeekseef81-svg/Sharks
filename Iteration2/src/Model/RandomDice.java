package Model;

import java.util.Objects;
import java.util.Random;

/**
 * Production dice (FR-16, FR-17): the <i>context</i> of the {@link DiceStrategy} pattern.
 * Task 11, owner: Rakan Ziyan; Iteration 2 Strategy (T2-04): Shafeek Saif.
 *
 * <p>This class owns the random generator of the game. For every roll it takes the strategy of the game level
 * from {@link LevelConfig#diceStrategy()} and lets it roll: the Regular die alone on Easy, Regular + Boost on
 * Medium and Regular + UltraBoost on Hard. It contains no level-specific code, so a new die needs a new strategy
 * and no change here.
 */
public final class RandomDice implements Dice {

    private final Random random;

    /**
     * @param random the random generator (a fixed seed makes games repeatable in tests)
     * @throws NullPointerException if {@code random} is null
     */
    public RandomDice(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Rolls the dice of the level with the strategy of that level.
     *
     * @param level the game level
     * @return a roll with a Regular value of 1-6, the special die of the level and the movement of FR-17
     * @throws NullPointerException if {@code level} is null
     */
    @Override
    public DiceRoll roll(Difficulty level) {
        Objects.requireNonNull(level, "level");
        return rollWith(level.config().diceStrategy());
    }

    /**
     * Rolls with a given strategy, whatever the level of the game. Used for the level dice above and available
     * for a roll that does not follow the level, such as the extra Regular + UltraBoost roll of a surprise.
     *
     * @param strategy the dice to roll
     * @return the roll of that strategy
     * @throws NullPointerException if {@code strategy} is null or returns no roll
     */
    public DiceRoll rollWith(DiceStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");
        return Objects.requireNonNull(strategy.roll(random), "the dice strategy returned no roll");
    }

    /** @return the random generator used by these dice */
    Random random() {
        return random;
    }
}
