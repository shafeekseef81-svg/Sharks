package Model;

import java.util.Objects;

/**
 * Result of one roll (FR-16, FR-17). Task 11, owner: Rakan Ziyan.
 *
 * <p>The constructor rejects impossible values, so a wrong roll fails at once instead of moving a piece.
 *
 * @param regular      regular die, 1-6
 * @param specialType  BOOST, ULTRA_BOOST or NONE
 * @param special      special die value (1..faces), 0 when there is none
 * @param movement     regular when there is no special die, otherwise regular x (special - 1) (FR-17)
 * @param surpriseRoll the extra Regular + UltraBoost roll granted by a surprise (Iteration 2)
 * @param cancelled    third 6 in a row: the roll is cancelled and the turn ends (FR-22, Task 12)
 */
public record DiceRoll(int regular, DieType specialType, int special, int movement,
                       boolean surpriseRoll, boolean cancelled) {

    /**
     * @throws IllegalArgumentException if a value is outside its die, the special type is REGULAR, or the
     *         movement does not follow FR-17
     * @throws NullPointerException if {@code specialType} is null
     */
    public DiceRoll {
        Objects.requireNonNull(specialType, "specialType");
        if (regular < 1 || regular > DieType.REGULAR.faces()) {
            throw new IllegalArgumentException("A Regular die shows 1-6, not " + regular);
        }
        if (specialType == DieType.REGULAR) {
            throw new IllegalArgumentException("The special die cannot be a Regular die");
        }
        if (specialType == DieType.NONE ? special != 0 : special < 1 || special > specialType.faces()) {
            throw new IllegalArgumentException("Invalid " + specialType.label() + " value: " + special);
        }
        int expected = specialType == DieType.NONE ? regular : regular * (special - 1);
        if (movement != expected) {
            throw new IllegalArgumentException("Movement must be " + expected + " (FR-17), not " + movement);
        }
    }

    /**
     * A roll of the Regular die only, as on the Easy level: movement = Regular value (FR-17).
     *
     * @param regular the Regular die value, 1-6
     */
    public static DiceRoll regularOnly(int regular) {
        return new DiceRoll(regular, DieType.NONE, 0, regular, false, false);
    }

    public boolean hasSpecial() {
        return specialType != DieType.NONE;
    }
}
