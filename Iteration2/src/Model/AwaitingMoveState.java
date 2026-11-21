package Model;

import java.util.Map;

/**
 * Concrete {@link TurnState}: the dice have been rolled and the player must move one of the highlighted pieces
 * (FR-20, FR-24). Task T2-13, owner: Rakan Ziyan.
 *
 * <p>When no piece can move, the turn ends with a message (FR-23), unless the roll was a 6: then the player rolls
 * again (FR-21). A cancelled third 6 offers no move and ends the turn (FR-22).
 */
final class AwaitingMoveState implements TurnState {

    @Override
    public TurnPhase phase() {
        return TurnPhase.AWAITING_MOVE;
    }

    /** Only after a 6 with which no piece can move (FR-21). */
    @Override
    public boolean canRoll(StandardTurnManager turn) {
        return turn.bonusRoll() && legalMoves(turn).isEmpty();
    }

    @Override
    public DiceRoll roll(StandardTurnManager turn) {
        if (!canRoll(turn)) {
            throw TurnState.refused("Rolling is not allowed now", this);
        }
        return turn.rollDice();
    }

    @Override
    public Map<Piece, Move> legalMoves(StandardTurnManager turn) {
        return turn.computeLegalMoves();
    }

    /** Applies the move; the context chooses the next state (extra roll, end of turn or game over). */
    @Override
    public MoveResult move(StandardTurnManager turn, Piece piece) {
        return turn.applyMove(piece);
    }

    /** Allowed only without a legal move and without an extra roll; returns the FR-22 or FR-23 message. */
    @Override
    public String endTurn(StandardTurnManager turn) {
        turn.requireTurnCanEnd();
        return turn.passTurn(turn.noMoveMessage());
    }
}
