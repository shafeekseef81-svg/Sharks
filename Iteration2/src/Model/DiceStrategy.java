package Model;

import java.util.Random;

/**
 * <b>Strategy</b> design pattern (GoF): the dice of one game level and their movement formula (FR-16, FR-17).
 * Task T2-04, owner: Shafeek Saif.
 *
 * <p><b>Problem.</b> Each level rolls different dice and turns them into a movement differently: Easy rolls the
 * Regular die alone, Medium adds a Boost die (1-3), Hard adds an UltraBoost die (1-6), and with a special die the
 * movement is Regular x (special - 1) instead of the Regular value. Written as {@code if}/{@code switch} on the
 * level inside the dice, every new die would change that class and its tests.
 *
 * <p><b>Roles.</b>
 * <ul>
 *   <li><i>Strategy</i>: this interface.</li>
 *   <li><i>Concrete strategies</i>: {@link RegularDiceStrategy} (Easy), {@link BoostDiceStrategy} (Medium) and
 *       {@link UltraBoostDiceStrategy} (Hard); the two special dice share {@link SpecialDiceStrategy}.</li>
 *   <li><i>Context</i>: {@link RandomDice}. It owns the random generator and, for each roll, uses the strategy
 *       of the game level.</li>
 *   <li><i>Selection</i>: {@link LevelConfig#diceStrategy()}, so the dice of a level are configured with the
 *       other level settings (NFR-19).</li>
 * </ul>
 *
 * <p>Strategies hold no state: one instance per level is shared, and the random generator is passed in by the
 * context, so a fixed seed still makes a whole game repeatable.
 */
public interface DiceStrategy {

    /** Number of faces of the Regular die, rolled on every level (FR-16). */
    int REGULAR_FACES = DieType.REGULAR.faces();

    /**
     * @return the special die rolled together with the Regular die, or {@link DieType#NONE} when there is none
     */
    DieType specialDie();

    /**
     * The movement formula of these dice (FR-17).
     *
     * @param regular the Regular die value, 1-6
     * @param special the special die value, or 0 when there is no special die
     * @return the number of steps the roll moves a piece on the shared track; 0 moves nothing
     */
    int movement(int regular, int special);

    /**
     * Rolls these dice once: the Regular die first, then the special die if there is one. Every face of every die
     * is equally likely (FR-16, NFR-13).
     *
     * @param random the random generator of the game
     * @return the roll, with the movement of {@link #movement(int, int)}
     * @throws NullPointerException if {@code random} is null
     */
    DiceRoll roll(Random random);
}
