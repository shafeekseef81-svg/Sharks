package Model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Iteration 1 turn management (FR-15, FR-16 partial, FR-17 partial, FR-21..FR-23). Owner: Rakan Ziyan.
 *
 * <p><b>Task 11 (this version):</b> turn order and the Regular die.
 * <ul>
 *   <li>Players take turns in setup order; only the active player rolls, once per turn (FR-15).</li>
 *   <li>A roll uses the game's dice for the game's level (FR-16, FR-17).</li>
 *   <li>{@link #endTurn()} passes the turn to the next player in setup order who has not finished (FR-15,
 *       FR-43), and ends the game when the ranking says so (FR-44).</li>
 * </ul>
 * <p><b>Not in Task 11:</b> the extra roll after a 6 and the cancelled third 6 (FR-21, FR-22) and the
 * no-move message (FR-23) are Task 12. Legal moves and moving are Task 16; until then
 * {@link #legalMoves()} after a roll and {@link #move(Piece)} reach the Phase A placeholder of the move engine.
 */
public final class StandardTurnManager implements TurnManager {

    private final List<Player> players;
    private final Dice dice;
    private final MoveEngine moveEngine;
    private final RankingTracker ranking;
    private final Difficulty level;

    private int currentIndex;
    private TurnPhase phase = TurnPhase.AWAITING_ROLL;
    private DiceRoll lastRoll;

    /**
     * @param players    the players in setup order (FR-15), at least two
     * @param level      the game level
     * @param dice       the dice
     * @param moveEngine the move engine
     * @param ranking    the finishing order tracker
     * @throws IllegalArgumentException if fewer than two players are given
     * @throws NullPointerException if an argument is null
     */
    public StandardTurnManager(List<Player> players, Difficulty level, Dice dice, MoveEngine moveEngine,
                               RankingTracker ranking) {
        this.players = List.copyOf(players);
        if (this.players.size() < 2) {
            throw new IllegalArgumentException("A game needs at least two players");
        }
        this.level = Objects.requireNonNull(level, "level");
        this.dice = Objects.requireNonNull(dice, "dice");
        this.moveEngine = Objects.requireNonNull(moveEngine, "moveEngine");
        this.ranking = Objects.requireNonNull(ranking, "ranking");
    }

    @Override
    public Player current() {
        return players.get(currentIndex);
    }

    @Override
    public TurnPhase phase() {
        return phase;
    }

    @Override
    public DiceRoll lastRoll() {
        return lastRoll;
    }

    /**
     * Rolls for the active player (FR-15, FR-16). Allowed once per turn, while the phase is
     * {@link TurnPhase#AWAITING_ROLL}; afterwards the phase is {@link TurnPhase#AWAITING_MOVE}.
     *
     * @throws IllegalStateException if the active player already rolled this turn or the game is over
     */
    @Override
    public DiceRoll roll() {
        if (phase != TurnPhase.AWAITING_ROLL) {
            throw new IllegalStateException("Rolling is not allowed now (phase " + phase + ")");
        }
        DiceRoll roll = Objects.requireNonNull(dice.roll(level), "the dice returned no roll");
        lastRoll = roll;
        phase = TurnPhase.AWAITING_MOVE;
        return roll;
    }

    /** @return the legal moves for the last roll; empty before the active player has rolled */
    @Override
    public Map<Piece, Move> legalMoves() {
        if (phase != TurnPhase.AWAITING_MOVE || lastRoll == null) {
            return Map.of();
        }
        return moveEngine.legalMoves(current(), lastRoll);
    }

    @Override
    public MoveResult move(Piece piece) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 16 (Rakan)");
    }

    /** Always {@code false} until Task 12 adds the Rule of 6 (FR-21). */
    @Override
    public boolean hasBonusRoll() {
        return false;
    }

    /**
     * Passes the turn to the next player in setup order who has not finished (FR-15, FR-43). If the ranking
     * reports that the game is over, the phase becomes {@link TurnPhase#GAME_OVER} (FR-44).
     *
     * @return {@code null}; the no-move message of FR-23 is added in Task 12
     * @throws IllegalStateException if the active player has not rolled yet this turn, or the game is over
     */
    @Override
    public String endTurn() {
        if (phase == TurnPhase.GAME_OVER) {
            throw new IllegalStateException("The game is over");
        }
        if (lastRoll == null) {
            throw new IllegalStateException("The active player has not rolled yet");
        }
        lastRoll = null;
        if (ranking.isGameOver()) {
            phase = TurnPhase.GAME_OVER;
            return null;
        }
        for (int step = 1; step <= players.size(); step++) {
            int next = (currentIndex + step) % players.size();
            if (!ranking.hasFinished(players.get(next))) {
                currentIndex = next;
                phase = TurnPhase.AWAITING_ROLL;
                return null;
            }
        }
        phase = TurnPhase.GAME_OVER; // nobody is left to play
        return null;
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
