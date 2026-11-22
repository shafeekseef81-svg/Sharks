package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Moves pieces for question and surprise effects: the <i>receiver</i> of the {@link SquareEffect} Command
 * pattern (FR-26, FR-27, FR-31, FR-32, FR-35, FR-41). Task T2-10, owner: Shafeek Saif.
 *
 * <p>An effect moves one given piece; the player does not choose among pieces as with a dice roll. The rules of
 * a dice move still apply (FR-41):
 * <ul>
 *   <li>Steps follow the piece's own path from {@link Board#path(PlayerColor)}, forward or backward (FR-25).</li>
 *   <li>An opponent Double Block can be neither landed on nor passed, in either direction
 *       ({@link MoveEngine#isPassable}, FR-29). The piece has no other piece to move instead, so it stops on the
 *       cell just before the first block in its direction.</li>
 *   <li>Backward movement stops at the owner's starting square and never crosses it (FR-32).</li>
 *   <li>Forward movement needs exact steps to the finish (FR-31, {@link HomePathRules#canReach}); a move that
 *       would overshoot does not happen.</li>
 *   <li>Landing captures a lone opponent on an unprotected square, after any kind of movement (FR-26); protected
 *       squares let pieces of different players stay together (FR-27). Both come from {@link SquareRules}.</li>
 * </ul>
 * The only exception is the "jump to the finishing square" surprise, which ignores exact steps and blocks
 * (FR-41, SRS Table 2): {@link #jumpToFinish}.
 *
 * <p>A movement of zero steps returns {@link MoveResult.Kind#NONE}, so the {@link EffectQueue} does not activate
 * the square again (FR-42, staff answer Q81).
 */
public final class EffectMover {

    private final Board board;
    private final List<Player> players;
    private final SquareRules squareRules;
    private final HomePathRules homePathRules;
    private final MoveEngine moveEngine;

    /**
     * @param board         the game board
     * @param players       all players of the game
     * @param squareRules   capture and protection rules (FR-26, FR-27)
     * @param homePathRules exact-finish rule (FR-31)
     * @param moveEngine    the engine of the game; only its {@link MoveEngine#isPassable} is used, so a Double
     *                      Block means the same for effects as for dice moves (FR-28, FR-29)
     * @throws NullPointerException if an argument is null
     */
    public EffectMover(Board board, List<Player> players, SquareRules squareRules, HomePathRules homePathRules,
                       MoveEngine moveEngine) {
        this.board = Objects.requireNonNull(board, "board");
        this.players = List.copyOf(players);
        this.squareRules = Objects.requireNonNull(squareRules, "squareRules");
        this.homePathRules = Objects.requireNonNull(homePathRules, "homePathRules");
        this.moveEngine = Objects.requireNonNull(moveEngine, "moveEngine");
    }

    /**
     * Moves a piece forward (a right answer) or backward (a wrong answer, three steps back, or an opponent pushed
     * back) with the movement and blocking rules (FR-29, FR-31, FR-32, FR-35, FR-40, FR-41).
     *
     * @param piece a piece on the board (not in its castle and not finished)
     * @param steps positive to move forward, negative to move backward; not 0
     * @return a {@link MoveResult.Kind#STEPS} result with the cells in the order they are passed, or
     *         {@link MoveResult.Kind#NONE} when the piece cannot move at all (a block right next to it, already
     *         on its starting square, or a forward move that would overshoot the finish)
     * @throws IllegalArgumentException if {@code steps} is 0 or the piece is not on the board of this game
     * @throws IllegalStateException if {@link SquareRules} returns a piece that cannot be captured; nothing
     *         changes
     */
    public MoveResult moveSteps(Piece piece, int steps) {
        Player owner = requireOnBoard(piece);
        if (steps == 0) {
            throw new IllegalArgumentException("An effect moves at least one step: " + piece);
        }
        int from = piece.progress();
        int to = steps > 0 ? forwardTarget(piece, owner, steps) : backwardTarget(piece, owner, -steps);
        if (to == from) {
            return MoveResult.none(piece);
        }
        PlayerColor color = piece.color();
        Cell target = board.cellAt(color, to);
        boolean onSharedTrack = to < board.homeStartIndex();
        List<MoveResult.Capture> captures = onSharedTrack ? capturesAt(piece, target) : List.of();

        piece.setProgress(to);
        for (MoveResult.Capture c : captures) {
            c.piece().setProgress(Piece.IN_CASTLE); // FR-26
        }

        int direction = to > from ? 1 : -1;
        List<Cell> cells = new ArrayList<>();
        for (int p = from; p != to + direction; p += direction) {
            cells.add(board.cellAt(color, p));
        }
        Square landedOn = onSharedTrack ? board.squareAt(target) : null;
        boolean pieceFinished = piece.finished();
        return new MoveResult(piece, MoveResult.Kind.STEPS, from, to, List.copyOf(cells), board.cellAt(color, from),
                List.copyOf(captures), landedOn, pieceFinished, pieceFinished && owner.allFinished(),
                isGameOver());
    }

    /**
     * Sends a piece back to its castle (surprise "return the piece to its castle", SRS Table 2).
     *
     * @param piece a piece on the board
     * @return a {@link MoveResult.Kind#TO_CASTLE} result
     * @throws IllegalArgumentException if the piece is not on the board of this game
     */
    public MoveResult returnToCastle(Piece piece) {
        requireOnBoard(piece);
        int from = piece.progress();
        Cell fromCell = board.cellAt(piece.color(), from);
        piece.setProgress(Piece.IN_CASTLE);
        return new MoveResult(piece, MoveResult.Kind.TO_CASTLE, from, Piece.IN_CASTLE, List.of(), fromCell,
                List.of(), null, false, false, isGameOver());
    }

    /**
     * Moves a piece straight to its finishing square, ignoring exact steps and Double Blocks (surprise "jump to
     * the finishing square", SRS Table 2; the exception of FR-41). Nothing is captured: the finishing square is
     * protected (FR-27).
     *
     * @param piece a piece on the board
     * @return a {@link MoveResult.Kind#TO_FINISH} result
     * @throws IllegalArgumentException if the piece is not on the board of this game
     */
    public MoveResult jumpToFinish(Piece piece) {
        Player owner = requireOnBoard(piece);
        int from = piece.progress();
        Cell fromCell = board.cellAt(piece.color(), from);
        piece.setProgress(piece.finishIndex());
        return new MoveResult(piece, MoveResult.Kind.TO_FINISH, from, piece.finishIndex(), List.of(), fromCell,
                List.of(), null, true, owner.allFinished(), isGameOver());
    }

    /** The progress a forward effect reaches: the target, the cell before the first block, or no move. */
    private int forwardTarget(Piece piece, Player owner, int steps) {
        int from = piece.progress();
        if (!homePathRules.canReach(piece, steps)) {
            return from; // FR-31: overshooting the finish is no move
        }
        int target = from + steps;
        for (int p = from + 1; p <= target; p++) {
            if (!passable(owner, p)) {
                return p - 1; // FR-29: stop before the block
            }
        }
        return target;
    }

    /** The progress a backward effect reaches: never behind the starting square (FR-32), never past a block. */
    private int backwardTarget(Piece piece, Player owner, int steps) {
        int from = piece.progress();
        int target = Math.max(0, from - steps);
        for (int p = from - 1; p >= target; p--) {
            if (!passable(owner, p)) {
                return p + 1; // FR-29 in the other direction: stop before the block
            }
        }
        return target;
    }

    private boolean passable(Player owner, int progress) {
        return moveEngine.isPassable(board.cellAt(owner.color(), progress), owner);
    }

    /** The captures of landing on {@code target}, checked before anything changes (FR-26, FR-27). */
    private List<MoveResult.Capture> capturesAt(Piece mover, Cell target) {
        List<MoveResult.Capture> captures = new ArrayList<>();
        for (Piece victim : squareRules.capturesOnLanding(mover, target)) {
            boolean valid = victim != null && victim.color() != mover.color() && victim.onBoard()
                    && target.equals(board.cellAt(victim.color(), victim.progress()))
                    && players.stream().anyMatch(p -> p.pieces().contains(victim))
                    && captures.stream().noneMatch(c -> c.piece() == victim);
            if (!valid) {
                throw new IllegalStateException("SquareRules returned a piece that cannot be captured on " + target
                        + ": " + victim);
            }
            captures.add(new MoveResult.Capture(victim, target));
        }
        return captures;
    }

    /** @return the owner of a piece that is on the board of this game */
    private Player requireOnBoard(Piece piece) {
        Objects.requireNonNull(piece, "piece");
        for (Player player : players) {
            if (player.pieces().contains(piece)) {
                if (!piece.onBoard()) {
                    throw new IllegalArgumentException("An effect moves only a piece on the board: " + piece);
                }
                return player;
            }
        }
        throw new IllegalArgumentException("Piece is not in this game: " + piece);
    }

    /** All but one player have finished (FR-44). */
    private boolean isGameOver() {
        return players.stream().filter(p -> !p.allFinished()).count() <= 1;
    }
}
