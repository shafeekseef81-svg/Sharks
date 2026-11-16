package Model;

import java.util.Objects;

/**
 * Iteration 1 home path and exact-finish rules (FR-18, FR-31). Task 18, owner: Shafeek Saif.
 *
 * <p>These rules answer only for a piece that moves by steps along its path. Leaving the castle on a Regular 6
 * (FR-19) is not a move by steps, so a piece in its castle always gets 0 here and the move engine handles it.
 * The path of each colour comes from {@link Board}, which also keeps every other colour off a home path (FR-33).
 */
public final class StandardHomePathRules implements HomePathRules {

    private final Board board;

    /**
     * @param board the game board (gives the home path start and the finishing index)
     * @throws NullPointerException if {@code board} is null
     */
    public StandardHomePathRules(Board board) {
        this.board = Objects.requireNonNull(board, "board");
    }

    /**
     * Steps for this roll: the Regular die on the home path (FR-18), otherwise the movement of the roll (FR-17).
     *
     * @return the number of steps; 0 for a piece in its castle or finished, a cancelled roll (FR-22), a movement
     *         of zero (FR-17) or a roll that would overshoot the finishing square (FR-31)
     * @throws NullPointerException if an argument is null
     */
    @Override
    public int stepsFor(Piece piece, DiceRoll roll) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(roll, "roll");
        if (roll.cancelled()) {
            return 0;
        }
        int steps = piece.progress() >= board.homeStartIndex() ? roll.regular() : roll.movement();
        return canReach(piece, steps) ? steps : 0;
    }

    /**
     * @return {@code true} if the piece is on its path and these forward steps do not pass the finishing square
     *         (FR-31); {@code false} for zero or negative steps
     * @throws NullPointerException if {@code piece} is null
     */
    @Override
    public boolean canReach(Piece piece, int steps) {
        Objects.requireNonNull(piece, "piece");
        return steps > 0 && piece.onBoard() && steps <= board.finishIndex() - piece.progress();
    }

    Board board() {
        return board;
    }
}
