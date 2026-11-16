package Model;

import java.util.Map;

/**
 * Turn order, dice and the Rule of 6 (FR-15, FR-16 partial, FR-17 partial, FR-21, FR-22, FR-23).
 * Shared contract, frozen at checkpoint CP1.
 *
 * <p>Owner: Rakan Ziyan (Tasks 11 and 12). In Iteration 2 the turn phases become the State pattern (NFR-09).
 */
public interface TurnManager {

    /** @return the active player; only this player may roll (FR-15) */
    Player current();

    /** @return the current turn phase */
    TurnPhase phase();

    /** @return the last roll of the current turn, or {@code null} before the first roll */
    DiceRoll lastRoll();

    /**
     * Rolls for the active player and applies the Rule of 6: an extra roll after a 6, and a third 6 in a row
     * is cancelled (FR-21, FR-22).
     *
     * @return the roll
     */
    DiceRoll roll();

    /** @return the legal moves for the last roll, by piece (FR-20, FR-24); empty if there is none (FR-23) */
    Map<Piece, Move> legalMoves();

    /**
     * Moves the chosen piece with the last roll.
     *
     * @param piece a piece contained in {@link #legalMoves()}
     * @return the result of the move
     */
    MoveResult move(Piece piece);

    /** @return {@code true} if the active player rolls again (FR-21) */
    boolean hasBonusRoll();

    /**
     * Ends the turn and passes it to the next player who has not finished (FR-15, FR-43).
     *
     * @return a message to show when the turn passed without a move (FR-23), or {@code null}
     */
    String endTurn();
}
