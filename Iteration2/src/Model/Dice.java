package Model;

/**
 * Source of dice rolls (FR-16). Shared contract, frozen at checkpoint CP1.
 *
 * <p>Production code uses {@link RandomDice}. Unit tests use a fixed sequence of values, so that rule
 * tests are repeatable (SDP 6.2, Task 14).
 *
 * <p>Owner: Rakan Ziyan (Task 11).
 */
public interface Dice {

    /**
     * Rolls the dice for one throw.
     *
     * @param level the game level; it decides whether a special die is rolled (Easy: Regular die only)
     * @return the roll, including the movement value of FR-17
     */
    DiceRoll roll(Difficulty level);
}
