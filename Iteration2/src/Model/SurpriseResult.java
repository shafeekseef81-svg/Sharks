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
 * @param piece the piece the action moves, or null when the action moves no piece
 * @param affected the colour of the player the outcome falls on: the active player, or the owner of the
 *        pushed piece for {@link SurpriseOutcome#OPPONENT_BACK}
 * @param action what must happen
 * @param steps signed steps for {@link SurpriseAction#MOVE_STEPS} (negative = backward), otherwise 0
 * @param regularRoll the Regular die rolled for a push (FR-40), 1 to 6, or 0 when no die was rolled
 * @param note why nothing happens, when {@code action} is {@link SurpriseAction#NOTHING}; otherwise null
 */
public record SurpriseResult(SurpriseOutcome outcome, Piece piece, PlayerColor affected,
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
     * @throws NullPointerException if {@code outcome}, {@code affected} or {@code action} is null
     * @throws IllegalArgumentException if the parts contradict each other, for instance steps without a
     *         {@link SurpriseAction#MOVE_STEPS} action, a moving action without a piece, or a Regular die
     *         value outside 1-6
     */
    public SurpriseResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(affected, "affected");
        Objects.requireNonNull(action, "action");
        boolean movesPiece = action == SurpriseAction.JUMP_TO_FINISH
                || action == SurpriseAction.RETURN_TO_CASTLE
                || action == SurpriseAction.MOVE_STEPS;
        if (movesPiece == (piece == null)) {
            throw new IllegalArgumentException(movesPiece
                    ? action + " needs the piece it moves"
                    : action + " moves no piece, so it must not carry one");
        }
        if (piece != null && piece.color() != affected) {
            throw new IllegalArgumentException(piece + " does not belong to " + affected.label());
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
                + (note == null ? "" : ": " + note);
    }
}
