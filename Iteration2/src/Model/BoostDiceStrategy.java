package Model;

/**
 * Concrete strategy of the {@link DiceStrategy} pattern for the Medium level: the Regular die and the Boost die
 * (1-3); movement = Regular x (Boost - 1), so 0, 1 or 2 times the Regular value (FR-16, FR-17).
 * Task T2-04, owner: Shafeek Saif.
 */
public final class BoostDiceStrategy extends SpecialDiceStrategy {

    @Override
    public DieType specialDie() {
        return DieType.BOOST;
    }
}
