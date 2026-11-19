package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The rules of the surprise "push an opponent back" (SRS FR-40): which opponent pieces the active player may
 * choose, and how far the chosen piece goes back. Task T2-09, owner: Mohamed Zereny.
 *
 * <p><b>What FR-40 asks for.</b> A Regular die is rolled and <i>an opponent piece chosen by the active
 * player</i> moves back half of that value, rounded up. The Assignment 3 prototype could not ask the player,
 * so it picked the most advanced opponent piece by itself
 * ({@code GameState.leadingOpponentPiece}); {@code PROTOTYPE_TRACEABILITY.md} lists FR-40 as <i>Partial</i>
 * for exactly that reason. This class closes that gap on the model side: it only offers the choice, and the
 * piece that is pushed is always the one that was handed back to it. Nothing here ever picks the leader.
 *
 * <p><b>Eligibility.</b> A piece may be chosen when all of the following hold:
 * <ul>
 *   <li>it belongs to another player, never to the active player;</li>
 *   <li>it is on the board: not in its castle and not finished;</li>
 *   <li>it is still on the shared track, that is its progress is below
 *       {@link Board#homeStartIndex()}. A piece that has turned into its own home path cannot be reached from
 *       the shared track, and the same test is what the prototype used.</li>
 * </ul>
 * A piece that stands on its own start square stays eligible: it is on the shared track, and a backward move
 * from there simply stops where it is (FR-32). Stopping it is the work of the effect mover of T2-10, not of
 * this class.
 *
 * <p>This class holds no state and moves nothing. The movement itself, the Double Blocks on the way (FR-29,
 * FR-41) and any capture (FR-26, FR-27) belong to the effect commands of T2-10.
 */
public final class OpponentTargets {

    private OpponentTargets() {
    }

    /**
     * How far a pushed piece goes back: half the Regular die, rounded up, that is &#8968;R/2&#8969; (FR-40).
     *
     * <p>Worked out as {@code (regular + 1) / 2} in whole numbers, which avoids rounding a fraction:
     * 1 and 2 give 1, 3 and 4 give 2, 5 and 6 give 3.
     *
     * @param regular the Regular die value, 1 to 6
     * @return the number of squares the chosen piece moves backward, 1 to 3
     * @throws IllegalArgumentException if the value is not a face of the Regular die
     */
    public static int backwardSteps(int regular) {
        if (regular < 1 || regular > DieType.REGULAR.faces()) {
            throw new IllegalArgumentException("A Regular die shows 1-" + DieType.REGULAR.faces()
                    + ", not " + regular);
        }
        return (regular + 1) / 2;
    }

    /**
     * Every opponent piece the active player may choose (FR-40), in player order and then in piece order, so
     * that the dialog of T2-11 always lists them the same way.
     *
     * @param players the players of the game
     * @param board the board, for {@link Board#homeStartIndex()}
     * @param active the colour of the active player, whose own pieces are left out
     * @return the eligible pieces, possibly empty; the list cannot be modified
     * @throws NullPointerException if an argument or a list entry is null
     */
    public static List<Piece> eligible(List<Player> players, Board board, PlayerColor active) {
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(active, "active");
        List<Piece> targets = new ArrayList<>();
        for (Player player : players) {
            Objects.requireNonNull(player, "player");
            if (player.color() == active) {
                continue;
            }
            for (Piece piece : player.pieces()) {
                if (isOnSharedTrack(piece, board)) {
                    targets.add(piece);
                }
            }
        }
        return Collections.unmodifiableList(targets);
    }

    /**
     * Whether one piece may be chosen as the target of this surprise (FR-40).
     *
     * @param piece the candidate piece, or null
     * @param board the board, for {@link Board#homeStartIndex()}
     * @param active the colour of the active player
     * @return true only if the piece is not null, belongs to another player and is on the shared track
     * @throws NullPointerException if {@code board} or {@code active} is null
     */
    public static boolean isEligible(Piece piece, Board board, PlayerColor active) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(active, "active");
        return piece != null && piece.color() != active && isOnSharedTrack(piece, board);
    }

    /**
     * Checks a target the player chose and gives it back, so that a wrong choice fails at once instead of
     * pushing the wrong piece.
     *
     * @param piece the piece the active player chose
     * @param board the board
     * @param active the colour of the active player
     * @return the same piece
     * @throws NullPointerException if {@code piece}, {@code board} or {@code active} is null
     * @throws IllegalArgumentException if the piece belongs to the active player or is not on the shared track
     */
    public static Piece requireEligible(Piece piece, Board board, PlayerColor active) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(active, "active");
        if (piece.color() == active) {
            throw new IllegalArgumentException("A surprise pushes back an opponent piece, not " + piece
                    + " of the active player (FR-40)");
        }
        if (!isOnSharedTrack(piece, board)) {
            throw new IllegalArgumentException(piece + " is not on the shared track, so it cannot be pushed "
                    + "back (FR-40)");
        }
        return piece;
    }

    /** On the board and still before the owner's home path, which is the reachable part of the track. */
    private static boolean isOnSharedTrack(Piece piece, Board board) {
        return piece != null && piece.onBoard() && piece.progress() < board.homeStartIndex();
    }
}
