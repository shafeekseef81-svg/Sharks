package Model;

import java.util.Objects;

/**
 * Concrete command of the {@link SquareEffect} pattern: the surprise "jump to the finishing square, ignoring exact
 * steps and blocks" (SRS Table 2, the exception of FR-41). Task T2-10, owner: Shafeek Saif.
 *
 * @param piece the piece that jumps; it must be on the board when the command runs
 */
public record JumpToFinishEffect(Piece piece) implements SquareEffect {

    /** @throws NullPointerException if {@code piece} is null */
    public JumpToFinishEffect {
        Objects.requireNonNull(piece, "piece");
    }

    /** @return the result of {@link EffectMover#jumpToFinish} */
    @Override
    public MoveResult execute(EffectMover mover) {
        return mover.jumpToFinish(piece);
    }
}
