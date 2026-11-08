package Model;

import java.util.Objects;
import java.util.Random;

/**
 * Production dice (FR-16, FR-17). Task 11, owner: Rakan Ziyan.
 *
 * <p>Iteration 1 supports the Easy level only: one uniformly random Regular die (1-6), and the movement equals
 * the Regular die (FR-17). The Boost and UltraBoost dice of Medium and Hard are added in Iteration 2; until then
 * a roll for those levels is refused instead of returning an invented result.
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
     * Rolls the Regular die.
     *
     * @param level the game level
     * @return a roll with a Regular value of 1-6, no special die, movement = Regular value
     * @throws NullPointerException if {@code level} is null
     * @throws UnsupportedOperationException for Medium and Hard, whose special dice are Iteration 2 work
     */
    @Override
    public DiceRoll roll(Difficulty level) {
        Objects.requireNonNull(level, "level");
        if (level.specialDie() != DieType.NONE) {
            throw new UnsupportedOperationException(
                    "The " + level.specialDie().label() + " die of the " + level.label()
                            + " level is added in Iteration 2; Iteration 1 plays Easy only.");
        }
        int regular = random.nextInt(DieType.REGULAR.faces()) + 1;
        return DiceRoll.regularOnly(regular);
    }

    /** @return the random generator used by these dice */
    Random random() {
        return random;
    }
}
