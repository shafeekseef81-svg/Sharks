package Model;

/**
 * One legal move offered to the active player: which piece moves and to which progress index
 * (see {@link Piece}). {@link MoveEngine#legalMoves} produces these, and the view highlights the pieces (FR-24).
 *
 * <p>Shared contract, frozen at checkpoint CP1. Owner: Rakan Ziyan.
 *
 * @param piece          the piece to move
 * @param targetProgress the progress index the piece will reach
 */
public record Move(Piece piece, int targetProgress) {
}
