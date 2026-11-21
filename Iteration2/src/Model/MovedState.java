package Model;

import java.util.Map;

/**
 * Concrete {@link TurnState}: the player has moved and has no extra roll; the turn waits for
 * {@link TurnManager#endTurn()} while the move is shown ({@link TurnPhase#MOVING}). Task T2-13, owner: Rakan Ziyan.
 */
final class MovedState implements TurnState {

    @Override
    public TurnPhase phase() {
        return TurnPhase.MOVING;
    }

    @Override
    public boolean canRoll(StandardTurnManager turn) {
        return false;
    }

    @Override
    public DiceRoll roll(StandardTurnManager turn) {
        throw TurnState.refused("Rolling is not allowed now", this);
    }

    @Override
    public Map<Piece, Move> legalMoves(StandardTurnManager turn) {
        return Map.of();
    }

    @Override
    public MoveResult move(StandardTurnManager turn, Piece piece) {
        throw TurnState.refused("No move is expected now", this);
    }

    /** Passes the turn to the next player who has not finished (FR-15, FR-43); no message. */
    @Override
    public String endTurn(StandardTurnManager turn) {
        turn.requireTurnCanEnd();
        return turn.passTurn(null);
    }
}
