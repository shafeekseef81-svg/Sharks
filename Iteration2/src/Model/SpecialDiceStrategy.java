package Model;

import java.util.Random;

/**
 * Common part of the two concrete {@link DiceStrategy strategies} that roll a special die next to the Regular
 * die (FR-16, FR-17). Task T2-04, owner: Shafeek Saif.
 *
 * <p>Both special dice use the same movement formula, Regular x (special - 1): a special 1 gives a movement of 0,
 * which moves nothing (FR-17). The subclasses only say which die they roll.
 */
public abstract class SpecialDiceStrategy implements DiceStrategy {

    /** Package-private: the concrete strategies are {@link BoostDiceStrategy} and {@link UltraBoostDiceStrategy}. */
    SpecialDiceStrategy() {
    }

    /**
     * @return Regular x (special - 1) (FR-17)
     * @throws IllegalArgumentException if a value is not a face of its die
     */
    @Override
    public final int movement(int regular, int special) {
        RegularDiceStrategy.requireRegular(regular);
        int faces = specialDie().faces();
        if (special < 1 || special > faces) {
            throw new IllegalArgumentException("A " + specialDie().label() + " die shows 1-" + faces
                    + ", not " + special);
        }
        return regular * (special - 1);
    }

    @Override
    public final DiceRoll roll(Random random) {
        int regular = RegularDiceStrategy.rollRegular(random);
        int special = random.nextInt(specialDie().faces()) + 1;
        return new DiceRoll(regular, specialDie(), special, movement(regular, special), false, false);
    }

    @Override
    public String toString() {
        return "Regular + " + specialDie().label() + " dice";
    }
}
