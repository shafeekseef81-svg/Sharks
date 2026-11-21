package Model;

import java.util.Objects;

/**
 * Concrete command of the {@link SquareEffect} pattern: the surprise "return the piece to its castle" (SRS
 * Table 2). Task T2-10, owner: Shafeek Saif.
 *
 * @param piece the piece to send back; it must be on the board when the command runs
 */
public record ReturnToCastleEffect(Piece piece) implements SquareEffect {

    /** @throws NullPointerException if {@code piece} is null */
    public ReturnToCastleEffect {
        Objects.requireNonNull(piece, "piece");
    }

    /** @return the result of {@link EffectMover#returnToCastle} */
    @Override
    public MoveResult execute(EffectMover mover) {
        return mover.returnToCastle(piece);
    }
}
