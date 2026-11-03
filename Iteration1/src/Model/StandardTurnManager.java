package Model;

import java.util.List;
import java.util.Map;

/**
 * Iteration 1 turn management (FR-15, FR-16 partial, FR-17 partial, FR-21..FR-23).
 *
 * <p>Owner: Rakan Ziyan. <b>Phase A placeholder</b>: only the initial state is available. The first player in
 * setup order is active and no roll has been made. Rolling and moving are implemented in Tasks 11, 12 and 16.
 */
public final class StandardTurnManager implements TurnManager {

    private final List<Player> players;
    private final Dice dice;
    private final MoveEngine moveEngine;
    private final RankingTracker ranking;
    private final Difficulty level;

    /**
     * @param players    the players in setup order (FR-15)
     * @param level      the game level
     * @param dice       the dice
     * @param moveEngine the move engine
     * @param ranking    the finishing order tracker
     */
    public StandardTurnManager(List<Player> players, Difficulty level, Dice dice, MoveEngine moveEngine,
                               RankingTracker ranking) {
        this.players = List.copyOf(players);
        this.level = level;
        this.dice = dice;
        this.moveEngine = moveEngine;
        this.ranking = ranking;
    }

    @Override
    public Player current() {
        return players.get(0); // Phase A: initial state only (the first player in setup order)
    }

    @Override
    public TurnPhase phase() {
        return TurnPhase.AWAITING_ROLL;
    }

    @Override
    public DiceRoll lastRoll() {
        return null;
    }

    @Override
    public DiceRoll roll() {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Tasks 11-12 (Rakan)");
    }

    @Override
    public Map<Piece, Move> legalMoves() {
        return Map.of(); // Phase A: no roll yet, so no legal moves
    }

    @Override
    public MoveResult move(Piece piece) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 16 (Rakan)");
    }

    @Override
    public boolean hasBonusRoll() {
        return false;
    }

    @Override
    public String endTurn() {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Tasks 11-12 (Rakan)");
    }

    Dice dice() {
        return dice;
    }

    MoveEngine moveEngine() {
        return moveEngine;
    }

    RankingTracker ranking() {
        return ranking;
    }

    Difficulty level() {
        return level;
    }
}
