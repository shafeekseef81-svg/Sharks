package Model;

import java.util.List;

/**
 * Capture and protection rules (FR-26 partial, FR-27 partial). Shared contract, frozen at checkpoint CP1.
 *
 * <p>Owner: Shafeek Saif (Task 17).
 */
public interface SquareRules {

    /**
     * Whether pieces on this square are safe from capture. Protection, starting, surprise and finishing
     * squares are protected (FR-27).
     *
     * @param square the square; {@code null} stands for the finishing square
     * @return {@code true} if the square is protected
     */
    boolean isProtected(Square square);

    /**
     * Opponent pieces captured when {@code mover} lands on {@code target} (FR-26): a lone opponent piece on an
     * unprotected square. The caller sends the returned pieces back to their castles.
     *
     * @param mover  the piece that lands
     * @param target the cell it lands on
     * @return the captured pieces; empty if nothing is captured
     */
    List<Piece> capturesOnLanding(Piece mover, Cell target);
}
