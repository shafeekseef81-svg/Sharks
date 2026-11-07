package Model;

import java.util.Random;

/**
 * Production dice: a uniformly random Regular die (1-6) and, from Iteration 2, the special dice (FR-16).
 *
 * <p>Owner: Rakan Ziyan. <b>Phase A placeholder</b>: implemented in Task 11.
 */
public final class RandomDice implements Dice {

    private final Random random;

    /** @param random the random generator (a fixed seed makes games repeatable in tests) */
    public RandomDice(Random random) {
        this.random = random;
    }

    @Override
    public DiceRoll roll(Difficulty level) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 11 (Rakan)");
    }

    /** @return the random generator used by these dice */
    Random random() {
        return random;
    }
}
