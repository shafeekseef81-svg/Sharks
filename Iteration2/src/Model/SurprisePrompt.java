package Model;

import java.util.List;
import java.util.Objects;

/**
 * A surprise that has been drawn but not yet carried out: the outcome, the piece that landed on the square,
 * and, for the push of FR-40, the Regular die that was rolled together with the opponent pieces the active
 * player may choose from. Task T2-09, owner: Mohamed Zereny.
 *
 * <p><b>Why a step in between.</b> FR-40 says the active player chooses the opponent piece, so the model
 * cannot turn that outcome into an instruction in one go: it must first say what there is to choose. This
 * record is what the surprise dialog of T2-11 shows, and {@link #resolve(Piece)} turns the player's answer
 * into the {@link SurpriseResult} that T2-10 carries out. The five other outcomes need no choice and go
 * straight through {@link #resolve()}.
 *
 * <p>Creating a prompt rolls no further dice and moves nothing; it is a plain description.
 *
 * @param outcome the outcome that was drawn (FR-39)
 * @param subject the piece that landed on the surprise square; always on the board
 * @param active the colour of the active player, the owner of {@code subject}
 * @param regularRoll the Regular die rolled for a push, 1 to 6 for {@link SurpriseOutcome#OPPONENT_BACK} and
 *        0 for every other outcome (FR-40)
 * @param targets the opponent pieces the active player may choose, in a fixed order; empty for every outcome
 *        other than {@link SurpriseOutcome#OPPONENT_BACK}, and also for a push when no opponent piece is on
 *        the shared track
 */
public record SurprisePrompt(SurpriseOutcome outcome, Piece subject, PlayerColor active,
                             int regularRoll, List<Piece> targets) {

    /** The note of a push that is cancelled because no opponent piece can be reached (FR-40). */
    public static final String NO_TARGET_NOTE =
            "No opponent piece is on the shared track, so this surprise does nothing.";

    /**
     * @throws NullPointerException if an argument or a target is null
     * @throws IllegalArgumentException if the piece is not on the board or not the active player's, if the
     *         Regular die does not match the outcome, or if a target is not an eligible opponent piece
     */
    public SurprisePrompt {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(active, "active");
        Objects.requireNonNull(targets, "targets");
        if (!subject.onBoard()) {
            throw new IllegalArgumentException("A surprise square is reached by a piece on the board, not by "
                    + subject);
        }
        if (subject.color() != active) {
            throw new IllegalArgumentException(subject + " is not a piece of the active player "
                    + active.label());
        }
        boolean push = outcome.needsOpponentPiece();
        if (push ? regularRoll < 1 || regularRoll > DieType.REGULAR.faces() : regularRoll != 0) {
            throw new IllegalArgumentException(push
                    ? "A push rolls a Regular die of 1-" + DieType.REGULAR.faces() + ", not " + regularRoll
                    : outcome + " rolls no Regular die, so the value must be 0, not " + regularRoll);
        }
        if (!push && !targets.isEmpty()) {
            throw new IllegalArgumentException(outcome + " chooses no opponent piece (FR-40)");
        }
        targets = List.copyOf(targets);
        for (Piece target : targets) {
            if (target.color() == active) {
                throw new IllegalArgumentException(target
                        + " belongs to the active player and cannot be pushed back (FR-40)");
            }
            if (!target.onBoard()) {
                throw new IllegalArgumentException(target + " is not on the board (FR-40)");
            }
        }
    }

    /**
     * @return how far the chosen piece goes back, half the Regular die rounded up (FR-40), or 0 when this
     *         outcome pushes nobody
     */
    public int backwardSteps() {
        return regularRoll == 0 ? 0 : OpponentTargets.backwardSteps(regularRoll);
    }

    /**
     * @return true when the active player still has to choose an opponent piece, that is for a push with at
     *         least one eligible piece (FR-40). When it is false, {@link #resolve()} finishes the surprise.
     */
    public boolean needsChoice() {
        return outcome.needsOpponentPiece() && !targets.isEmpty();
    }

    /**
     * Finishes a surprise that needs no choice from the player: the five outcomes other than the push, and a
     * push for which no opponent piece is on the shared track.
     *
     * <p>A push with no eligible piece does nothing at all, as in the Assignment 3 prototype and as PR #87
     * of T2-10 expects (its {@code EffectQueue.skip()}). This reading still needs the Product Owner's
     * confirmation: the SRS text of FR-40 is not in the repository.
     *
     * @return what must happen
     * @throws IllegalStateException if the player must still choose a piece; call {@link #resolve(Piece)}
     */
    public SurpriseResult resolve() {
        if (needsChoice()) {
            throw new IllegalStateException(outcome + " needs the opponent piece the player chose (FR-40); "
                    + "use resolve(Piece)");
        }
        return switch (outcome) {
            case JUMP_TO_FINISH -> new SurpriseResult(outcome, subject, active,
                    SurpriseResult.SurpriseAction.JUMP_TO_FINISH, 0, 0, null);
            case BACK_TO_CASTLE -> new SurpriseResult(outcome, subject, active,
                    SurpriseResult.SurpriseAction.RETURN_TO_CASTLE, 0, 0, null);
            case BACK_THREE -> new SurpriseResult(outcome, subject, active,
                    SurpriseResult.SurpriseAction.MOVE_STEPS,
                    -SurpriseOutcome.BACK_THREE_STEPS, 0, null);
            case SKIP_TURN -> new SurpriseResult(outcome, null, active,
                    SurpriseResult.SurpriseAction.SKIP_NEXT_TURN, 0, 0, null);
            case EXTRA_ROLL -> new SurpriseResult(outcome, null, active,
                    SurpriseResult.SurpriseAction.EXTRA_ROLL, 0, 0, null);
            case OPPONENT_BACK -> new SurpriseResult(outcome, null, active,
                    SurpriseResult.SurpriseAction.NOTHING, 0, regularRoll, NO_TARGET_NOTE);
        };
    }

    /**
     * Finishes the push of FR-40 with the opponent piece the active player chose.
     *
     * @param target one of {@link #targets()}, the piece the player picked
     * @return an instruction to move that piece back half the Regular die, rounded up
     * @throws IllegalStateException if this outcome asks for no choice
     * @throws NullPointerException if {@code target} is null
     * @throws IllegalArgumentException if the piece is not one of the pieces the player may choose, which
     *         includes the active player's own pieces and any piece off the shared track (FR-40)
     */
    public SurpriseResult resolve(Piece target) {
        if (!outcome.needsOpponentPiece()) {
            throw new IllegalStateException(outcome + " chooses no opponent piece (FR-40); use resolve()");
        }
        Objects.requireNonNull(target, "target");
        if (targets.stream().noneMatch(candidate -> candidate == target)) {
            throw new IllegalArgumentException(target + " is not one of the "
                    + targets.size() + " pieces the player may choose (FR-40)");
        }
        return new SurpriseResult(outcome, target, target.color(),
                SurpriseResult.SurpriseAction.MOVE_STEPS, -backwardSteps(), regularRoll, null);
    }
}
