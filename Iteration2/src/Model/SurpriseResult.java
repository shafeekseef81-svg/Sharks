package Model;

import java.util.Objects;

/**
 * What a surprise square has decided must happen, said once and plainly, so that the part of the game that
 * carries it out does not have to know the surprise rules (SRS FR-39, FR-40). Task T2-09, owner:
 * Mohamed Zereny.
 *
 * <p><b>Why this exists.</b> Drawing the outcome, rolling the die for a push and checking the player's choice
 * are surprise rules and belong to T2-09. Moving pieces with the movement rules, the Double Blocks, the
 * captures, the order of a chain of special squares and the turn itself are owned by the effect commands of
 * T2-10 and the turn of T2-13. This record is the line between the two: T2-09 fills it in, and T2-10 reads
 * {@link #action()} and builds its own command. Nothing is moved, and no turn state is changed, by creating
 * one of these.
 *
 * <p><b>Two different players (FR-42).</b> An effect may move an <i>opponent's</i> piece onto another special
 * square, and that square is then activated during the active player's turn
 * ({@code EffectQueue.Activation} of T2-10 carries the moved piece, whoever owns it). So a surprise has two
 * players in it, and they are kept apart on purpose:
 * <ul>
 *   <li>{@link #active()} — the player whose <b>turn</b> it is. FR-40 says this is the player who chooses the
 *       opponent piece to push, and FR-34 says this is the player who answers a question.</li>
 *   <li>{@link #triggerOwner()} — the owner of the piece that <b>landed</b> on the special square. In an
 *       ordinary chain the two are the same; after a push they need not be, and
 *       {@link #opponentTriggered()} says which case this is.</li>
 * </ul>
 *
 * <p><b>Which of the two a turn outcome falls on was Product Owner decision D2, approved 21/11/2026:</b> a
 * {@code SKIP_NEXT_TURN} or {@code EXTRA_ROLL} always falls on {@link #active()}, never on
 * {@link #triggerOwner()}, even when an opponent's piece set the chain off. See {@link #affected()} and
 * {@link #turnAffected()}. The outcomes that move a piece are unaffected by D2: they still act on the piece
 * they name.
 *
 * <p><b>How T2-10 uses it.</b> One {@code switch} over {@link #action()}:
 * <pre>{@code
 * switch (result.action()) {
 *     case JUMP_TO_FINISH  -> queue.run(new JumpToFinishEffect(result.piece()));
 *     case RETURN_TO_CASTLE -> queue.run(new ReturnToCastleEffect(result.piece()));
 *     case MOVE_STEPS      -> queue.run(new MoveStepsEffect(result.piece(), result.steps()));
 *     case SKIP_NEXT_TURN  -> turn.skipNextTurn(result.turnAffected());   // always the active player (D2)
 *     case EXTRA_ROLL      -> turn.grantSurpriseRoll();                   // to the active player (D2)
 *     case NOTHING         -> queue.skip();
 * }
 * }</pre>
 * {@link #piece()} is already the right piece for each action: the piece that landed on the square, or the
 * opponent piece the player chose for a push (FR-40). {@link #steps()} is already signed and never 0 for
 * {@link SurpriseAction#MOVE_STEPS}, so it fits {@code MoveStepsEffect} as it is.
 *
 * @param outcome the outcome that was drawn (FR-39)
 * @param active the colour of the player whose turn it is
 * @param trigger the piece that landed on the surprise square; its owner may be another player (FR-42)
 * @param piece the piece the action moves, or null when the action moves no piece
 * @param action what must happen
 * @param steps signed steps for {@link SurpriseAction#MOVE_STEPS} (negative = backward), otherwise 0
 * @param regularRoll the Regular die rolled for a push (FR-40), 1 to 6, or 0 when no die was rolled
 * @param note why nothing happens, when {@code action} is {@link SurpriseAction#NOTHING}; otherwise null
 */
public record SurpriseResult(SurpriseOutcome outcome, PlayerColor active, Piece trigger, Piece piece,
                             SurpriseAction action, int steps, int regularRoll, String note) {

    /**
     * The kinds of thing a surprise can ask for. They are deliberately not {@code SquareEffect} commands of
     * T2-10: T2-09 must compile and be tested on its own, and T2-10 stays the only owner of how an effect is
     * carried out.
     */
    public enum SurpriseAction {
        /** Send the piece straight to its finishing square. */
        JUMP_TO_FINISH,
        /** Send the piece back to its castle. */
        RETURN_TO_CASTLE,
        /** Move the piece {@link SurpriseResult#steps()} steps, forward if positive, backward if negative. */
        MOVE_STEPS,
        /** The active player misses the next turn (D2); no piece moves. */
        SKIP_NEXT_TURN,
        /** The active player rolls again at once with the Regular + UltraBoost dice; no piece moves. */
        EXTRA_ROLL,
        /** Nothing happens; {@link SurpriseResult#note()} says why. */
        NOTHING
    }

    /**
     * @throws NullPointerException if {@code outcome}, {@code active}, {@code trigger} or {@code action} is
     *         null
     * @throws IllegalArgumentException if the parts contradict each other, for instance steps without a
     *         {@link SurpriseAction#MOVE_STEPS} action, a moving action without a piece, a push that moves
     *         the active player's own piece, or a Regular die value outside 1-6
     */
    public SurpriseResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(active, "active");
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(action, "action");
        if (!trigger.onBoard()) {
            throw new IllegalArgumentException("A surprise square is reached by a piece on the board, not by "
                    + trigger);
        }
        boolean movesPiece = action == SurpriseAction.JUMP_TO_FINISH
                || action == SurpriseAction.RETURN_TO_CASTLE
                || action == SurpriseAction.MOVE_STEPS;
        if (movesPiece == (piece == null)) {
            throw new IllegalArgumentException(movesPiece
                    ? action + " needs the piece it moves"
                    : action + " moves no piece, so it must not carry one");
        }
        if (piece != null) {
            if (outcome.needsOwnPiece() && piece != trigger) {
                throw new IllegalArgumentException(outcome
                        + " moves the piece that landed on the square, not " + piece);
            }
            if (outcome.needsOpponentPiece() && piece.color() == active) {
                throw new IllegalArgumentException("A push moves an opponent of the active player "
                        + active.label() + ", not " + piece + " (FR-40)");
            }
            if (!piece.onBoard()) {
                throw new IllegalArgumentException(piece + " is not on the board, so it cannot be moved");
            }
        }
        if ((action == SurpriseAction.MOVE_STEPS) == (steps == 0)) {
            throw new IllegalArgumentException(action == SurpriseAction.MOVE_STEPS
                    ? "MOVE_STEPS moves at least one step"
                    : action + " moves no steps, so steps must be 0, not " + steps);
        }
        if (regularRoll != 0 && (regularRoll < 1 || regularRoll > DieType.REGULAR.faces())) {
            throw new IllegalArgumentException("A Regular die shows 1-" + DieType.REGULAR.faces()
                    + " or 0 for no roll, not " + regularRoll);
        }
        if ((action == SurpriseAction.NOTHING) == (note == null)) {
            throw new IllegalArgumentException(action == SurpriseAction.NOTHING
                    ? "NOTHING must say why nothing happens"
                    : action + " does happen, so it must not carry a note");
        }
    }

    /**
     * The player the outcome falls on: the owner of the piece that is moved, or, when no piece moves, the
     * player whose turn it is.
     *
     * <p><b>Product Owner decision D2, approved 21/11/2026.</b> A turn outcome
     * ({@link SurpriseAction#SKIP_NEXT_TURN}, {@link SurpriseAction#EXTRA_ROLL}) always falls on the
     * <b>active</b> player - the player whose turn it is - even when an opponent's piece set the chain off
     * ({@link #opponentTriggered()}). So if Red is taking the turn and Red's push moves Green's piece onto a
     * surprise square that draws "skip the next turn", <b>Red</b> misses the next turn, not Green; and an
     * extra roll likewise goes to Red. This follows FR-34, which gives the active player the question in the
     * same situation, and keeps every turn-level consequence with the player who caused it.
     *
     * <p>A turn outcome moves no piece, so {@code piece} is null for both of them and this method returns
     * {@link #active()}. The rule is therefore structural rather than a special case, and
     * {@link #changesTurn()} tells a caller when it applies. The branch of this method that returns
     * {@code piece.color()} is only reached by the outcomes that <i>move a piece</i>, where the owner of the
     * moved piece is the player affected - that is unchanged by D2.
     *
     * @return the colour of the player the outcome falls on; always {@link #active()} when
     *         {@link #changesTurn()} is true (D2)
     */
    public PlayerColor affected() {
        return piece == null ? active : piece.color();
    }

    /**
     * The player a turn outcome falls on, by Product Owner decision D2 (21/11/2026): always the active
     * player. Use this where the intent is specifically a turn outcome, so the D2 rule is named at the call
     * site instead of being inferred from {@code piece == null}.
     *
     * @return the colour of the active player
     * @throws IllegalStateException if this outcome does not change the turn, i.e. {@link #changesTurn()} is
     *         false; the caller wants {@link #affected()} in that case
     */
    public PlayerColor turnAffected() {
        if (!changesTurn()) {
            throw new IllegalStateException(action + " does not change the turn, so it has no turn target");
        }
        return active;
    }

    /** @return the owner of the piece that landed on the surprise square */
    public PlayerColor triggerOwner() {
        return trigger.color();
    }

    /**
     * @return true when the piece that landed on the square belongs to another player than the one whose turn
     *         it is, which an effect chain can cause (FR-42)
     */
    public boolean opponentTriggered() {
        return trigger.color() != active;
    }

    /** @return true when the action moves a piece and so becomes a {@code SquareEffect} command of T2-10 */
    public boolean movesPiece() {
        return piece != null;
    }

    /** @return true when nothing happens at all, so the effect queue of T2-10 only skips this square */
    public boolean cancelled() {
        return action == SurpriseAction.NOTHING;
    }

    /** @return true when the outcome changes the turn instead of a piece, which T2-13 owns */
    public boolean changesTurn() {
        return action == SurpriseAction.SKIP_NEXT_TURN || action == SurpriseAction.EXTRA_ROLL;
    }

    @Override
    public String toString() {
        return outcome.name() + " -> " + action
                + (piece == null ? "" : " on " + piece)
                + (steps == 0 ? "" : " by " + steps)
                + (regularRoll == 0 ? "" : " (Regular die " + regularRoll + ")")
                + " [turn of " + active.label()
                + (opponentTriggered() ? ", set off by " + trigger : "") + "]"
                + (note == null ? "" : ": " + note);
    }
}
