package Model;

import java.util.Map;

/**
 * Concrete state of the <b>State</b> pattern ({@link TurnState}): a question or surprise square waits for its
 * effect ({@link TurnPhase#RESOLVING}). Task T2-10 part 2, owner: Shafeek Saif, on the hook that T2-13 (Rakan
 * Ziyan) left for it.
 *
 * <p>It is entered from {@link AwaitingMoveState}, through {@link StandardTurnManager#applyMove}, when a dice move
 * stops on a question or surprise square, and it is left when the effect queue of T2-10 is empty. Each effect can
 * reach a new special square, so the state may stay for a whole chain (FR-42).
 *
 * <p>While it lasts the player answers or confirms and nothing else: rolling, moving with the dice and ending the
 * turn are refused (FR-34, FR-39). When the chain is over, the context picks the next state as after any move: a
 * pending roll from a 6 or an extra roll from a surprise, the end of the turn, or the end of the game.
 */
final class ResolvingState implements TurnState {

    @Override
    public TurnPhase phase() {
        return TurnPhase.RESOLVING;
    }

    @Override
    public boolean canRoll(StandardTurnManager turn) {
        return false;
    }

    @Override
    public DiceRoll roll(StandardTurnManager turn) {
        throw TurnState.refused("Resolve the special square first", this);
    }

    @Override
    public Map<Piece, Move> legalMoves(StandardTurnManager turn) {
        return Map.of();
    }

    @Override
    public MoveResult move(StandardTurnManager turn, Piece piece) {
        throw TurnState.refused("Resolve the special square first", this);
    }

    @Override
    public String endTurn(StandardTurnManager turn) {
        throw TurnState.refused("The turn cannot end before the special square is resolved", this);
    }

    @Override
    public MoveResult runEffect(StandardTurnManager turn, SquareEffect effect) {
        return turn.runQueuedEffect(effect);
    }

    @Override
    public void skipEffect(StandardTurnManager turn) {
        turn.skipQueuedEffect();
    }

    @Override
    public void skipNextTurn(StandardTurnManager turn, Player player) {
        turn.markSkip(player);
    }

    @Override
    public void grantSurpriseRoll(StandardTurnManager turn) {
        turn.addSurpriseRoll();
    }
}
