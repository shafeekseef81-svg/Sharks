package Model;

import java.util.Map;

/**
 * Computes and applies legal moves (FR-19, FR-20, FR-25 partial, FR-28..FR-30).
 * Shared contract, frozen at checkpoint CP1; Iteration 2 (T2-06) filled in {@link #isPassable} without changing it.
 *
 * <p>Owner: Rakan Ziyan (Task 16). It delegates capture and protection to {@link SquareRules} (Shafeek),
 * and the home path and exact finish to {@link HomePathRules} (Shafeek).
 */
public interface MoveEngine {

    /**
     * Returns every legal move of the player for the roll. Each piece appears at most once,
     * because a roll moves exactly one piece and steps cannot be split (FR-20).
     *
     * @param player the active player
     * @param roll   the current roll
     * @return the legal moves by piece; empty when no move is possible (FR-23)
     */
    Map<Piece, Move> legalMoves(Player player, DiceRoll roll);

    /**
     * Applies a legal move, including any capture, and reports what happened so it can be animated.
     *
     * @param move a move returned by {@link #legalMoves}
     * @return the result of the move
     */
    MoveResult apply(Move move);

    /**
     * Whether a piece of the player may land on or pass the cell.
     * An opponent Double Block (two or more pieces of one opponent colour on the cell) returns {@code false}, for
     * forward and backward movement alike (FR-28, FR-29, SDP risk R1). Iteration 1 always returned {@code true}.
     *
     * @param cell   the cell to check
     * @param player the moving player
     * @return {@code true} if the cell can be passed or entered
     */
    boolean isPassable(Cell cell, Player player);
}
