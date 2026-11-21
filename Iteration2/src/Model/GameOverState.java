package Model;

import java.util.Map;

/**
 * Concrete {@link TurnState}: all but one player have finished (FR-44); nothing more can be done. Task T2-13,
 * owner: Rakan Ziyan.
 */
final class GameOverState implements TurnState {

    @Override
    public TurnPhase phase() {
        return TurnPhase.GAME_OVER;
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

    @Override
    public String endTurn(StandardTurnManager turn) {
        throw new IllegalStateException("The game is over");
    }
}
