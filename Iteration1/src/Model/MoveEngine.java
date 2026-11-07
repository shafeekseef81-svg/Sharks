package Model;

import java.util.Map;

/**
 * Computes and applies legal moves (FR-19 partial, FR-20, FR-25 partial).
 * Shared contract, frozen at checkpoint CP1.
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
     * Always {@code true} in Iteration 1. In Iteration 2, an opponent Double Block returns {@code false}
     * (FR-29, SDP risk R1).
     *
     * @param cell   the cell to check
     * @param player the moving player
     * @return {@code true} if the cell can be passed or entered
     */
    boolean isPassable(Cell cell, Player player);
}
