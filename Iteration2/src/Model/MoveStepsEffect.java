package Model;

import java.util.Objects;

/**
 * Concrete command of the {@link SquareEffect} pattern: move one piece a number of steps forward or backward with
 * the movement and blocking rules (FR-29, FR-31, FR-32, FR-41). Task T2-10, owner: Shafeek Saif.
 *
 * <p>Used for an answer to a question (forward for a right answer, back for a wrong one: 1, 2 or 3 steps by the
 * question's level, FR-35), for the surprise "move the piece backward three steps", and for an opponent piece
 * pushed back half a Regular die, rounded up (FR-40).
 *
 * @param piece the piece to move; it must be on the board when the command runs
 * @param steps positive = forward, negative = backward; not 0
 */
public record MoveStepsEffect(Piece piece, int steps) implements SquareEffect {

    /**
     * @throws NullPointerException if {@code piece} is null
     * @throws IllegalArgumentException if {@code steps} is 0
     */
    public MoveStepsEffect {
        Objects.requireNonNull(piece, "piece");
        if (steps == 0) {
            throw new IllegalArgumentException("An effect moves at least one step");
        }
    }

    /** @return the result of {@link EffectMover#moveSteps} */
    @Override
    public MoveResult execute(EffectMover mover) {
        return mover.moveSteps(piece, steps);
    }
}
