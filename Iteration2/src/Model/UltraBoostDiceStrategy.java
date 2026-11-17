package Model;

/**
 * Concrete strategy of the {@link DiceStrategy} pattern for the Hard level: the Regular die and the UltraBoost
 * die (1-6); movement = Regular x (UltraBoost - 1), so 0 to 5 times the Regular value (FR-16, FR-17).
 * Task T2-04, owner: Shafeek Saif.
 *
 * <p>The extra Regular + UltraBoost roll that a surprise can grant on any level (Iteration 2, T2-09) can reuse
 * this strategy.
 */
public final class UltraBoostDiceStrategy extends SpecialDiceStrategy {

    @Override
    public DieType specialDie() {
        return DieType.ULTRA_BOOST;
    }
}
