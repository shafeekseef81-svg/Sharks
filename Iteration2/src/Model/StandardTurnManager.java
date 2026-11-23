package Model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turn management (FR-15, FR-16, FR-17, FR-20, FR-21, FR-22, FR-23, FR-43, FR-44) and the <i>context</i> of the
 * {@link TurnState} pattern. Owner: Rakan Ziyan (Tasks 11 and 12; State pattern T2-13).
 *
 * <p><b>Turn order (Task 11):</b> players take turns in setup order; only the active player rolls (FR-15).
 * {@link #endTurn()} passes the turn to the next player who has not finished (FR-43).
 *
 * <p><b>Rule of 6 (Task 12):</b>
 * <ul>
 *   <li>A Regular 6 grants an extra roll, also when no piece can move or the movement is zero (FR-21). The
 *       player first makes a legal move if one exists, then rolls again.</li>
 *   <li>Capturing or finishing a piece grants no extra roll of its own (FR-21); only the 6 does.</li>
 *   <li>The third Regular 6 in a row within one turn is cancelled: no piece moves and the turn ends (FR-22).</li>
 *   <li>A legal move may not be skipped (FR-20): the turn cannot end while one is available.</li>
 *   <li>If no legal move exists and no extra roll is granted, {@link #endTurn()} returns the message to show
 *       and passes the turn (FR-23).</li>
 *   <li>A player who has finished all pieces takes no more turns and loses a pending extra roll (FR-43).</li>
 * </ul>
 *
 * <p><b>State pattern (T2-13):</b> every public operation is delegated to the current {@link TurnState}, which
 * allows only what its phase allows. States: {@link AwaitingRollState} → roll → {@link AwaitingMoveState} → move →
 * {@link AwaitingRollState} (extra roll) or {@link MovedState} (waiting for {@link #endTurn()});
 * {@link GameOverState} when the ranking reports the end (FR-44). This class keeps the data of the turn and the
 * steps the states share. The behaviour is the same as the Iteration 1 version, which switched on a phase value.
 * Legal moves and moving come from the {@link MoveEngine} (Task 16).
 */
public final class StandardTurnManager implements TurnManager {

    /** Number of Regular 6s in a row whose last one is cancelled (FR-22). */
    static final int SIXES_LIMIT = 3;

    private final List<Player> players;
    private final Dice dice;
    private final MoveEngine moveEngine;
    private final RankingTracker ranking;
    private final Difficulty level;

    /** The concrete states; they hold no data, so one instance of each is enough. */
    private final TurnState awaitingRoll = new AwaitingRollState();
    private final TurnState awaitingMove = new AwaitingMoveState();
    private final TurnState moved = new MovedState();
    private final TurnState gameOver = new GameOverState();

    private TurnState state = awaitingRoll;
    private int currentIndex;
    private DiceRoll lastRoll;
    private int sixesInRow;
    private boolean bonusRoll;
    private Map<Piece, Move> cachedMoves;

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

    // ------------------------------------------------------------------ TurnManager, delegated to the state

    @Override
    public Player current() {
        return players.get(currentIndex);
    }

    @Override
    public TurnPhase phase() {
        return state.phase();
    }

    @Override
    public DiceRoll lastRoll() {
        return lastRoll;
    }

    /**
     * Rolls for the active player (FR-15, FR-16) and applies the Rule of 6 (FR-21, FR-22). Allowed at the start
     * of a turn, after a move that earned an extra roll, and after a 6 with which no piece could move.
     *
     * @return the roll; {@link DiceRoll#cancelled()} is true for the third 6 in a row
     * @throws IllegalStateException if rolling is not allowed now
     */
    @Override
    public DiceRoll roll() {
        return state.roll(this);
    }

    /** @return {@code true} if the active player may roll now */
    public boolean canRoll() {
        return state.canRoll(this);
    }

    /**
     * @return the legal moves for the last roll, by piece (FR-20, FR-24); empty before a roll, after a move,
     *         and for a cancelled roll (FR-22)
     */
    @Override
    public Map<Piece, Move> legalMoves() {
        return state.legalMoves(this);
    }

    /**
     * Moves a legal piece with the last roll (FR-20). A finished piece is reported to the ranking (FR-43).
     * Afterwards the player rolls again if the roll was a 6 (FR-21); otherwise the phase is {@code MOVING}
     * until {@link #endTurn()} is called.
     *
     * @param piece a piece contained in {@link #legalMoves()}
     * @return the result reported by the move engine
     * @throws IllegalStateException if no move is expected now
     * @throws IllegalArgumentException if the piece has no legal move
     */
    @Override
    public MoveResult move(Piece piece) {
        return state.move(this, piece);
    }

    /** @return {@code true} if the active player must roll again before the turn can end (FR-21) */
    @Override
    public boolean hasBonusRoll() {
        return bonusRoll && state != gameOver;
    }

    /**
     * Ends the turn and passes it to the next player in setup order who has not finished (FR-15, FR-43).
     *
     * @return the message to show when the turn passed without a move: the cancelled third 6 (FR-22) or no
     *         legal move (FR-23); otherwise {@code null}
     * @throws IllegalStateException if the player has not rolled, still has a legal move (FR-20) or an extra
     *         roll (FR-21), or the game is over
     */
    @Override
    public String endTurn() {
        return state.endTurn(this);
    }

    // ------------------------------------------------------------------ steps shared by the states

    /** The Rule of 6 on a new roll (FR-21, FR-22); then a move is awaited. */
    DiceRoll rollDice() {
        DiceRoll roll = Objects.requireNonNull(dice.roll(level), "the dice returned no roll");
        sixesInRow = roll.regular() == 6 ? sixesInRow + 1 : 0;
        if (sixesInRow == SIXES_LIMIT) {
            roll = new DiceRoll(roll.regular(), roll.specialType(), roll.special(), roll.movement(),
                    roll.surpriseRoll(), true);
            bonusRoll = false;
        } else {
            bonusRoll = roll.regular() == 6;
        }
        lastRoll = roll;
        cachedMoves = null;
        state = awaitingMove;
        return roll;
    }

    /** The legal moves of the last roll, asked once from the move engine; none for a cancelled roll (FR-22). */
    Map<Piece, Move> computeLegalMoves() {
        if (lastRoll == null || lastRoll.cancelled()) {
            return Map.of();
        }
        if (cachedMoves == null) {
            Map<Piece, Move> moves = moveEngine.legalMoves(current(), lastRoll);
            cachedMoves = Collections.unmodifiableMap(new LinkedHashMap<>(moves));
        }
        return cachedMoves;
    }

    /** Applies a legal move and chooses the next state: game over, extra roll or end of turn (FR-21, FR-43). */
    MoveResult applyMove(Piece piece) {
        Move move = computeLegalMoves().get(piece);
        if (move == null) {
            throw new IllegalArgumentException(piece + " has no legal move with this roll");
        }
        MoveResult result = Objects.requireNonNull(moveEngine.apply(move), "the move engine returned no result");
        cachedMoves = null;
        Player mover = current();
        if (result.pieceFinished()) {
            ranking.onPieceFinished(mover);
        }
        if (ranking.isGameOver()) {
            state = gameOver;
            bonusRoll = false;
        } else if (ranking.hasFinished(mover)) {
            bonusRoll = false; // a finished player takes no more turns (FR-43)
            state = moved;
        } else {
            state = bonusRoll ? awaitingRoll : moved;
        }
        return result;
    }

    /** @throws IllegalStateException if the player has not rolled, must still move (FR-20) or roll again (FR-21) */
    void requireTurnCanEnd() {
        if (lastRoll == null) {
            throw new IllegalStateException("The active player has not rolled yet");
        }
        if (!legalMoves().isEmpty()) {
            throw new IllegalStateException("A legal move must be made; it may not be skipped (FR-20)");
        }
        if (hasBonusRoll()) {
            throw new IllegalStateException("The player rolled a 6 and rolls again (FR-21)");
        }
    }

    /** @return the message for a turn that ends without a move: a cancelled third 6 (FR-22) or no move (FR-23) */
    String noMoveMessage() {
        Player p = current();
        return lastRoll.cancelled()
                ? p.name() + " rolled a third 6 in a row: the roll is cancelled and the turn ends."
                : p.name() + " has no legal move with a " + lastRoll.movement() + "; the turn passes.";
    }

    /** Clears the turn and passes it to the next player who has not finished (FR-15, FR-43, FR-44). */
    String passTurn(String message) {
        lastRoll = null;
        cachedMoves = null;
        sixesInRow = 0;
        bonusRoll = false;
        if (ranking.isGameOver()) {
            state = gameOver;
            return message;
        }
        for (int step = 1; step <= players.size(); step++) {
            int next = (currentIndex + step) % players.size();
            if (!ranking.hasFinished(players.get(next))) {
                currentIndex = next;
                state = awaitingRoll;
                return message;
            }
        }
        state = gameOver; // nobody is left to play
        return message;
    }

    boolean bonusRoll() {
        return bonusRoll;
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
