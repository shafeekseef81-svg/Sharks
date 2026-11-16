package Model;

/**
 * Home path and finishing rules (FR-18, FR-31, FR-33). Shared contract, frozen at checkpoint CP1.
 *
 * <p>Owner: Shafeek Saif (Task 18).
 */
public interface HomePathRules {

    /**
     * Number of steps the piece may move with this roll. A piece on its home path moves by the Regular die
     * only (FR-18). On Easy, every piece moves by the Regular die (FR-17).
     *
     * @param piece the piece
     * @param roll  the roll
     * @return the number of steps, or 0 if the piece cannot move
     */
    int stepsFor(Piece piece, DiceRoll roll);

    /**
     * Whether the piece can move exactly this many steps without overshooting the finishing square (FR-31).
     *
     * @param piece the piece
     * @param steps the number of steps
     * @return {@code true} if the target is on the path and not beyond the finishing square
     */
    boolean canReach(Piece piece, int steps);
}
