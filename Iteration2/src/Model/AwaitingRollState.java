package Model;

import java.util.Map;

/**
 * Concrete {@link TurnState}: the active player must roll, at the start of a turn or after a move with a Regular 6
 * (FR-15, FR-21). Task T2-13, owner: Rakan Ziyan.
 */
final class AwaitingRollState implements TurnState {

    @Override
    public TurnPhase phase() {
        return TurnPhase.AWAITING_ROLL;
    }

    @Override
    public boolean canRoll(StandardTurnManager turn) {
        return true;
    }

    /** Rolls with the Rule of 6 and waits for a move ({@link AwaitingMoveState}). */
    @Override
    public DiceRoll roll(StandardTurnManager turn) {
        return turn.rollDice();
    }

    @Override
    public Map<Piece, Move> legalMoves(StandardTurnManager turn) {
        return Map.of();
    }

    @Override
    public MoveResult move(StandardTurnManager turn, Piece piece) {
        throw TurnState.refused("No move is expected now", this);
    }

    /** Refused: the player has not rolled yet, or still has the extra roll of a 6 (FR-21). */
    @Override
    public String endTurn(StandardTurnManager turn) {
        turn.requireTurnCanEnd();
        return turn.passTurn(null); // not reached: requireTurnCanEnd always refuses in this state
    }
}
