package model;

/**
 * Result of one roll (FR-16, FR-17).
 *
 * @param regular      regular die, 1-6
 * @param specialType  BOOST, ULTRA_BOOST or NONE
 * @param special      special die value, 0 when there is none
 * @param movement     regular, or regular x (special - 1)
 * @param surpriseRoll the extra Regular + UltraBoost roll granted by a surprise (outside the rule of 6)
 * @param cancelled    third 6 in a row: the roll is cancelled and the turn ends (FR-22)
 */
public record DiceRoll(int regular, DieType specialType, int special, int movement,
                       boolean surpriseRoll, boolean cancelled) {

    public boolean hasSpecial() {
        return specialType != DieType.NONE;
    }
}
