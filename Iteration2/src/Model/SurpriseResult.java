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
 * <p><b>How T2-10 uses it.</b> One {@code switch} over {@link #action()}:
 * <pre>{@code
 * switch (result.action()) {
 *     case JUMP_TO_FINISH  -> queue.run(new JumpToFinishEffect(result.piece()));
 *     case RETURN_TO_CASTLE -> queue.run(new ReturnToCastleEffect(result.piece()));
 *     case MOVE_STEPS      -> queue.run(new MoveStepsEffect(result.piece(), result.steps()));
 *     case SKIP_NEXT_TURN  -> turn.skipNextTurn(result.affected());
 *     case EXTRA_ROLL      -> turn.grantSurpriseRoll();
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
        /** The player of {@link SurpriseResult#affected()} misses the next turn; no piece moves. */
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
     * <p><b>Open decision for the team.</b> For {@link SurpriseAction#SKIP_NEXT_TURN} and
     * {@link SurpriseAction#EXTRA_ROLL} in a chain that an opponent's piece set off ({@link
     * #opponentTriggered()}), the approved requirements do not say whether the turn effect falls on the
     * player whose turn it is or on the owner of the piece that landed. This method returns the player whose
     * turn it is, as a <b>provisional</b> answer, and {@link #turnTargetUndecided()} is {@code true} in
     * exactly that case so the open question is visible in code instead of hidden. Changing the answer is a
     * one-line change here.
     *
     * @return the colour of the player the outcome falls on
     */
    public PlayerColor affected() {
        return piece == null ? active : piece.color();
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

    /**
     * @return true when this is a turn effect in a chain that an opponent's piece set off, so which player it
     *         falls on is still an open decision for the team; see {@link #affected()}
     */
    public boolean turnTargetUndecided() {
        return changesTurn() && opponentTriggered();
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
