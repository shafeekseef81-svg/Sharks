package Model;

import java.util.List;

/**
 * Outcome of moving a piece, used by the view to animate it.
 *
 * @param piece          the piece that moved
 * @param kind           how it moved
 * @param fromProgress   progress before the move (-1 = castle)
 * @param toProgress     progress after the move
 * @param cells          for STEPS: every cell from the old to the new position (inclusive)
 * @param fromCell       for jumps: the cell the piece left (null when it left the castle)
 * @param captures       opponent pieces sent back to their castle
 * @param landedOn       the shared-track square reached, or null (home path, finish, no move)
 * @param pieceFinished  the piece reached the finishing square
 * @param playerFinished the owner has now finished all four pieces
 * @param gameOver       all but one player have finished
 */
public record MoveResult(Piece piece, Kind kind, int fromProgress, int toProgress, List<Cell> cells,
                         Cell fromCell, List<Capture> captures, Square landedOn, boolean pieceFinished,
                         boolean playerFinished, boolean gameOver) {

    /** Type of movement. */
    public enum Kind { STEPS, EXIT_CASTLE, TO_CASTLE, TO_FINISH, NONE }

    /** A captured opponent piece and the cell it was captured on. */
    public record Capture(Piece piece, Cell from) {
    }

    static MoveResult none(Piece piece) {
        return new MoveResult(piece, Kind.NONE, piece.progress(), piece.progress(), List.of(), null,
                List.of(), null, false, false, false);
    }
}
