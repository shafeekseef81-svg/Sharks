package Model;

import java.util.Map;

/**
 * <b>State</b> design pattern (GoF): one phase of a turn and what the active player may do in it (NFR-09 "State
 * (turn phases)", SDP deliverable D3). Task T2-13, owner: Rakan Ziyan.
 *
 * <p><b>Problem.</b> Whether a player may roll, move or end the turn depends on the phase of the turn, and Iteration
 * 2 adds phases (question and surprise effects). Written as checks on a phase value inside every method of the
 * turn manager, each new phase would change every method and each method would repeat the same conditions.
 *
 * <p><b>Roles.</b>
 * <ul>
 *   <li><i>State</i>: this interface. Every operation the player can ask for in a turn is a method.</li>
 *   <li><i>Concrete states</i>: {@link AwaitingRollState}, {@link AwaitingMoveState}, {@link MovedState} and
 *       {@link GameOverState}; each allows only what its phase allows and chooses the next state.</li>
 *   <li><i>Context</i>: {@link StandardTurnManager}. It keeps the data of the turn (player, roll, Rule-of-6
 *       counters), delegates every request to its current state and offers the states the shared steps.</li>
 * </ul>
 *
 * <p>The states hold no data of their own: the context keeps one instance of each. A refused request throws
 * {@link IllegalStateException} and changes nothing.
 *
 * <p><b>Iteration 2 hook (T2-10):</b> the effect queue of question and surprise squares adds a state for
 * {@link TurnPhase#RESOLVING}, entered from {@link AwaitingMoveState} after a move that lands on such a square and
 * left when the queue is empty. No other state has to change.
 */
interface TurnState {

    /** @return the phase this state stands for, as reported by {@link TurnManager#phase()} */
    TurnPhase phase();

    /**
     * @param turn the context
     * @return {@code true} if the active player may roll now
     */
    boolean canRoll(StandardTurnManager turn);

    /**
     * Rolls for the active player.
     *
     * @param turn the context
     * @return the roll
     * @throws IllegalStateException if rolling is not allowed in this state
     */
    DiceRoll roll(StandardTurnManager turn);

    /**
     * @param turn the context
     * @return the legal moves for the last roll; empty when no move is expected in this state
     */
    Map<Piece, Move> legalMoves(StandardTurnManager turn);

    /**
     * Moves a piece with the last roll.
     *
     * @param turn  the context
     * @param piece the chosen piece
     * @return the result of the move
     * @throws IllegalStateException if no move is expected in this state
     * @throws IllegalArgumentException if the piece has no legal move
     */
    MoveResult move(StandardTurnManager turn, Piece piece);

    /**
     * Ends the turn and passes it on.
     *
     * @param turn the context
     * @return a message to show when the turn passed without a move, or {@code null}
     * @throws IllegalStateException if the turn may not end in this state
     */
    String endTurn(StandardTurnManager turn);

    /** Builds the exception for a request this state refuses. */
    static IllegalStateException refused(String what, TurnState state) {
        return new IllegalStateException(what + " (phase " + state.phase() + ")");
    }
}
