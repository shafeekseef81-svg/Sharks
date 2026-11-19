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
 * <p><b>The subject and the active player are two different things (FR-42).</b> An effect may move an
 * <i>opponent's</i> piece onto a surprise square, and that square is then activated while the same player is
 * still taking their turn. So:
 * <ul>
 *   <li>{@link #active()} is the player whose <b>turn</b> it is. FR-40 gives the choice to that player, and
 *       {@link #targets()} therefore holds the pieces of <i>that</i> player's opponents.</li>
 *   <li>{@link #subject()} is the piece that <b>landed</b> on the square, which may belong to somebody else;
 *       {@link #subjectOwner()} names its owner and {@link #opponentTriggered()} says whether the two
 *       differ.</li>
 * </ul>
 * In an ordinary chain, where the active player's own piece landed on the square, the two are the same.
 *
 * <p>Creating a prompt rolls no further dice and moves nothing; it is a plain description.
 *
 * @param outcome the outcome that was drawn (FR-39)
 * @param subject the piece that landed on the surprise square; always on the board
 * @param active the colour of the player whose turn it is, which need not own {@code subject} (FR-42)
 * @param regularRoll the Regular die rolled for a push, 1 to 6 for {@link SurpriseOutcome#OPPONENT_BACK} and
 *        0 for every other outcome (FR-40)
 * @param targets the pieces of the active player's opponents that may be chosen, in a fixed order; empty for
 *        every outcome other than {@link SurpriseOutcome#OPPONENT_BACK}, and also for a push when no such
 *        piece is on the shared track
 */
public record SurprisePrompt(SurpriseOutcome outcome, Piece subject, PlayerColor active,
                             int regularRoll, List<Piece> targets) {

    /** The note of a push that is cancelled because no opponent piece can be reached (FR-40). */
    public static final String NO_TARGET_NOTE =
            "No opponent piece is on the shared track, so this surprise does nothing.";

    /**
     * @throws NullPointerException if an argument or a target is null
     * @throws IllegalArgumentException if the piece is not on the board, if the Regular die does not match
     *         the outcome, or if a target is not a piece of an opponent of the active player
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

    /** @return the owner of the piece that landed on the surprise square */
    public PlayerColor subjectOwner() {
        return subject.color();
    }

    /**
     * @return true when the piece that landed on the square belongs to another player than the one whose turn
     *         it is, which an effect chain can cause (FR-42)
     */
    public boolean opponentTriggered() {
        return subject.color() != active;
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
     * push for which no eligible opponent piece is on the shared track.
     *
     * <p>A push with no eligible piece does nothing at all, which the Product Owner has confirmed against the
     * recovered specification of FR-40 and which PR #87 of T2-10 already expects (its
     * {@code EffectQueue.skip()}).
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
            case JUMP_TO_FINISH -> new SurpriseResult(outcome, active, subject, subject,
                    SurpriseResult.SurpriseAction.JUMP_TO_FINISH, 0, 0, null);
            case BACK_TO_CASTLE -> new SurpriseResult(outcome, active, subject, subject,
                    SurpriseResult.SurpriseAction.RETURN_TO_CASTLE, 0, 0, null);
            case BACK_THREE -> new SurpriseResult(outcome, active, subject, subject,
                    SurpriseResult.SurpriseAction.MOVE_STEPS,
                    -SurpriseOutcome.BACK_THREE_STEPS, 0, null);
            case SKIP_TURN -> new SurpriseResult(outcome, active, subject, null,
                    SurpriseResult.SurpriseAction.SKIP_NEXT_TURN, 0, 0, null);
            case EXTRA_ROLL -> new SurpriseResult(outcome, active, subject, null,
                    SurpriseResult.SurpriseAction.EXTRA_ROLL, 0, 0, null);
            case OPPONENT_BACK -> new SurpriseResult(outcome, active, subject, null,
                    SurpriseResult.SurpriseAction.NOTHING, 0, regularRoll, NO_TARGET_NOTE);
        };
    }

    /**
     * Finishes the push of FR-40 with the opponent piece the active player chose.
     *
     * <p>The piece must be one of {@link #targets()} and must still be on the board when the player answers:
     * a chain can capture a piece back to its castle between the moment the list was made and the moment the
     * player picks, and a stale pick is refused rather than moved. The list was made against the board as it
     * was; {@link SurpriseFactory#resolve(SurprisePrompt, Piece)} checks the pick against the board as it
     * <i>is</i>, and is the call T2-10 should prefer for that reason.
     *
     * @param target one of {@link #targets()}, the piece the player picked
     * @return an instruction to move that piece back half the Regular die, rounded up
     * @throws IllegalStateException if this outcome asks for no choice
     * @throws NullPointerException if {@code target} is null
     * @throws IllegalArgumentException if the piece is not one of the pieces the player may choose, which
     *         includes the active player's own pieces and any piece off the shared track, or if it has left
     *         the board since the list was made (FR-40)
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
        if (!target.onBoard()) {
            throw new IllegalArgumentException(target + " has left the board since it was offered, so it "
                    + "cannot be pushed back (FR-40)");
        }
        return new SurpriseResult(outcome, active, subject, target,
                SurpriseResult.SurpriseAction.MOVE_STEPS, -backwardSteps(), regularRoll, null);
    }
}
