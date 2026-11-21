package Model;

import java.util.List;
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

    // ------------------------------------------------------------------ special squares (T2-10 part 2)

    /**
     * @return the question or surprise square that waits for its effect, with the piece that reached it, while
     *         the phase is {@link TurnPhase#RESOLVING}; otherwise {@code null} (FR-34, FR-39, FR-42)
     */
    EffectQueue.Activation pendingEffect();

    /**
     * Carries out the effect of the waiting square. A piece it brings home counts for its owner (FR-43); when
     * the chain is over the turn goes on as after a move.
     *
     * @param effect the command made from the answer or the surprise
     * @return the result of the command
     * @throws IllegalStateException if no special square waits
     */
    MoveResult runEffect(SquareEffect effect);

    /**
     * Resolves the waiting square without moving a piece: a push with no opponent to push (FR-40), or one of the
     * two turn surprises after {@link #skipNextTurn} or {@link #grantSurpriseRoll}.
     *
     * @throws IllegalStateException if no special square waits
     */
    void skipEffect();

    /**
     * The surprise "skip the next turn" (SRS Table 2, Q56): the player misses their next turn, and a pending roll
     * from a 6 of the active player is cancelled. Call {@link #skipEffect()} afterwards.
     *
     * @param player the player who misses the turn
     * @throws IllegalStateException if no special square waits
     */
    void skipNextTurn(Player player);

    /**
     * The surprise "extra roll" (SRS Table 2, Q27): one more roll with the Regular and UltraBoost dice, outside
     * the rule of 6, before a pending roll from a 6. Call {@link #skipEffect()} afterwards.
     *
     * @throws IllegalStateException if no special square waits
     */
    void grantSurpriseRoll();

    /** @return the players whose turn was passed over by the last change of turn (Q56); possibly empty */
    List<Player> skippedTurns();
}
