package Model;

import java.util.Objects;
import java.util.Random;

/**
 * Concrete strategy of the {@link DiceStrategy} pattern for the Easy level: the Regular die alone, and the
 * movement equals the Regular value (FR-16, FR-17). Task T2-04, owner: Shafeek Saif.
 *
 * <p>It draws exactly one value per roll, as the Iteration 1 dice did, so a seeded Easy game rolls the same
 * values as before.
 */
public final class RegularDiceStrategy implements DiceStrategy {

    @Override
    public DieType specialDie() {
        return DieType.NONE;
    }

    /**
     * @return the Regular value (FR-17)
     * @throws IllegalArgumentException if {@code regular} is not 1-6 or {@code special} is not 0
     */
    @Override
    public int movement(int regular, int special) {
        requireRegular(regular);
        if (special != 0) {
            throw new IllegalArgumentException("The Regular die alone has no special value, not " + special);
        }
        return regular;
    }

    @Override
    public DiceRoll roll(Random random) {
        return DiceRoll.regularOnly(rollRegular(random));
    }

    @Override
    public String toString() {
        return "Regular die";
    }

    /**
     * Rolls the Regular die; shared by every strategy (FR-16).
     *
     * @return a value from 1 to 6
     */
    static int rollRegular(Random random) {
        return Objects.requireNonNull(random, "random").nextInt(REGULAR_FACES) + 1;
    }

    /** @throws IllegalArgumentException if the value is not a face of the Regular die */
    static void requireRegular(int regular) {
        if (regular < 1 || regular > REGULAR_FACES) {
            throw new IllegalArgumentException("A Regular die shows 1-" + REGULAR_FACES + ", not " + regular);
        }
    }
}
