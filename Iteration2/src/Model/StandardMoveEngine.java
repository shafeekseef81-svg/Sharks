package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Move engine (FR-19, FR-20, FR-25 partial, FR-28..FR-30). Task 16 (Iteration 1) and T2-06 (Iteration 2, Double
 * Blocks), owner: Rakan Ziyan.
 *
 * <p>A piece's position is its progress along the owner's path from {@link Board#path(PlayerColor)}: that path
 * already runs clockwise from the owner's start square, passes over the arrows of the other colours and turns
 * only at the owner's own arrow into its home path (FR-25). This class therefore never looks at arrows itself.
 *
 * <p>Rule split, as frozen at CP1:
 * <ul>
 *   <li>Leaving the castle (FR-19): a piece in the castle may move only on a Regular 6, onto its start
 *       square. This class decides it.</li>
 *   <li>Steps and exact finish (FR-17, FR-18, FR-31): asked from {@link HomePathRules} (Task 18).</li>
 *   <li>Captures and protected squares (FR-26, FR-27): asked from {@link SquareRules} (Task 17). This class
 *       only sends the returned pieces back to their castles.</li>
 *   <li>Double Blocks (FR-28..FR-30, T2-06): two or more pieces of one colour on one cell form a Double
 *       Block. It is worked out from the positions of the pieces every time it is needed and never stored, so
 *       it lasts as long as the pieces stay (FR-30) and is gone as soon as one of them moves. Every cell of a
 *       move, and the start square of a castle exit (FR-19), is checked with {@link #isPassable}.</li>
 * </ul>
 *
 * <p>Stopping before a Double Block (project description, "Double Block"): a piece whose way is blocked stops on
 * the cell just before the block, even without using all its steps, but only when it is the only movement the
 * player can make. So {@link #legalMoves} first looks for moves that use the whole roll (a castle exit on a
 * Regular 6 counts as one); only when there is none does it offer each blocked piece a move to the cell before
 * the first block. A block right next to the piece leaves it without a move (zero steps are no move).
 *
 * <p>Only a move returned by the latest {@link #legalMoves} call can be applied, and only once: a roll moves
 * exactly one piece (FR-20). A refused move changes nothing.
 */
public final class StandardMoveEngine implements MoveEngine {

    /** Regular die value that lets a piece leave its castle (FR-19). */
    static final int EXIT_ROLL = 6;

    private final Board board;
    private final List<Player> players;
    private final SquareRules squareRules;
    private final HomePathRules homePathRules;

    /** Moves offered by the latest {@link #legalMoves} call; cleared once one of them is applied. */
    private Map<Piece, Move> offered = Map.of();

    /**
     * @param board         the game board
     * @param players       all players of the game (needed for captures)
     * @param squareRules   capture and protection rules
     * @param homePathRules home path and exact-finish rules
     * @throws NullPointerException if an argument is null
     */
    public StandardMoveEngine(Board board, List<Player> players, SquareRules squareRules,
                              HomePathRules homePathRules) {
        this.board = Objects.requireNonNull(board, "board");
        this.players = List.copyOf(players);
        this.squareRules = Objects.requireNonNull(squareRules, "squareRules");
        this.homePathRules = Objects.requireNonNull(homePathRules, "homePathRules");
    }

    /**
     * {@inheritDoc}
     *
     * <p>The pieces are in castle order. A cancelled roll (FR-22) and a player who has finished have no moves.
     * The previous offer is withdrawn as soon as the arguments are not null, so a call that then fails leaves
     * no move that can still be applied.
     *
     * @throws IllegalArgumentException if the player is not in this game
     */
    @Override
    public Map<Piece, Move> legalMoves(Player player, DiceRoll roll) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(roll, "roll");
        offered = Map.of(); // only a move from the latest call may be applied, also if this call fails
        requireInGame(player);
        Map<Piece, Move> moves = new LinkedHashMap<>();
        if (!roll.cancelled()) {
            for (Piece piece : player.pieces()) {
                Move move = moveFor(piece, player, roll);
                if (move != null) {
                    moves.put(piece, move);
                }
            }
            if (moves.isEmpty()) {
                // No piece can use the whole roll: a blocked piece may stop before the Double Block.
                for (Piece piece : player.pieces()) {
                    Move move = stopBeforeBlock(piece, player, roll);
                    if (move != null) {
                        moves.put(piece, move);
                    }
                }
            }
        }
        offered = Collections.unmodifiableMap(moves);
        return offered;
    }

    /** The one move this piece can make with the whole roll, or {@code null}. */
    private Move moveFor(Piece piece, Player owner, DiceRoll roll) {
        if (piece.finished()) {
            return null;
        }
        if (piece.inCastle()) {
            // FR-19: a Regular 6 places the piece on its start square; the special die is ignored. An opponent
            // Double Block on the start square keeps the piece in its castle (FR-29).
            boolean canExit = roll.regular() == EXIT_ROLL && isPassable(board.startCell(owner.color()), owner);
            return canExit ? new Move(piece, 0) : null;
        }
        int target = targetOf(piece, roll);
        if (target < 0) {
            return null;
        }
        return firstBlocked(piece, owner, target) < 0 ? new Move(piece, target) : null;
    }

    /**
     * The move to the cell just before the first opponent Double Block on the piece's way, or {@code null} if the
     * way is free, the piece has no steps, or the block is on the very next cell.
     */
    private Move stopBeforeBlock(Piece piece, Player owner, DiceRoll roll) {
        if (piece.finished() || piece.inCastle()) {
            return null; // a castle exit is a single jump: it is either possible or not
        }
        int target = targetOf(piece, roll);
        if (target < 0) {
            return null;
        }
        int blocked = firstBlocked(piece, owner, target);
        int stop = blocked - 1;
        return blocked >= 0 && stop > piece.progress() ? new Move(piece, stop) : null;
    }

    /** The progress the roll takes this board piece to, or -1 when it has no steps or would overshoot (FR-31). */
    private int targetOf(Piece piece, DiceRoll roll) {
        int steps = homePathRules.stepsFor(piece, roll);
        if (steps <= 0 || !homePathRules.canReach(piece, steps)) {
            return -1; // FR-31: an overshooting piece has no move
        }
        int target = piece.progress() + steps;
        if (target > piece.finishIndex()) {
            // HomePathRules allowed a step past the finish: refuse it instead of corrupting the piece.
            throw new IllegalStateException("HomePathRules allowed " + piece + " to move past the finish");
        }
        return target;
    }

    /** The first progress after the piece, up to {@code target}, that the owner may not enter, or -1 (FR-29). */
    private int firstBlocked(Piece piece, Player owner, int target) {
        for (int p = piece.progress() + 1; p <= target; p++) {
            if (!isPassable(board.cellAt(owner.color(), p), owner)) {
                return p;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A normal move is {@link MoveResult.Kind#STEPS} (also when it reaches the finish, with
     * {@code pieceFinished}); leaving the castle is {@link MoveResult.Kind#EXIT_CASTLE}. The other kinds are
     * used by the jumps of Iteration 2.
     *
     * @throws IllegalArgumentException if the move is not one returned by the latest {@link #legalMoves} call,
     *         or was already applied; nothing changes
     * @throws IllegalStateException if {@link SquareRules} returns a piece that cannot be captured; nothing
     *         changes
     */
    @Override
    public MoveResult apply(Move move) {
        Objects.requireNonNull(move, "move");
        Move expected = offered.get(move.piece());
        if (expected == null || expected.targetProgress() != move.targetProgress()) {
            throw new IllegalArgumentException("Not a legal move for the current roll: " + move);
        }
        Piece piece = move.piece();
        Player owner = ownerOf(piece);
        int from = piece.progress();
        int to = move.targetProgress();
        PlayerColor color = piece.color();
        Cell target = board.cellAt(color, to);
        boolean onSharedTrack = to < board.homeStartIndex();

        // Work out every change first, so that a rule error leaves the game as it was.
        List<MoveResult.Capture> captures = new ArrayList<>();
        if (onSharedTrack) {
            for (Piece victim : squareRules.capturesOnLanding(piece, target)) {
                requireCapturable(victim, piece, target);
                for (MoveResult.Capture earlier : captures) {
                    if (earlier.piece() == victim) {
                        throw new IllegalStateException("SquareRules returned the same piece twice: " + victim);
                    }
                }
                captures.add(new MoveResult.Capture(victim, target));
            }
        }

        piece.setProgress(to);
        for (MoveResult.Capture c : captures) {
            c.piece().setProgress(Piece.IN_CASTLE); // FR-26
        }
        offered = Map.of(); // one move per roll (FR-20)

        MoveResult.Kind kind = from == Piece.IN_CASTLE ? MoveResult.Kind.EXIT_CASTLE : MoveResult.Kind.STEPS;
        List<Cell> cells = new ArrayList<>();
        for (int p = Math.max(from, 0); p <= to; p++) {
            cells.add(board.cellAt(color, p));
        }
        Square landedOn = onSharedTrack ? board.squareAt(target) : null;
        Cell fromCell = from == Piece.IN_CASTLE ? null : board.cellAt(color, from);
        boolean pieceFinished = piece.finished();
        return new MoveResult(piece, kind, from, to, List.copyOf(cells), fromCell, List.copyOf(captures),
                landedOn, pieceFinished, pieceFinished && owner.allFinished(), isGameOver());
    }

    /**
     * {@inheritDoc}
     *
     * <p>A cell is not passable when two or more pieces of one opponent colour stand on it (FR-28, FR-29). Pieces
     * in their castle or on the finishing square do not count. The player's own pieces never block it, and the
     * answer does not depend on the direction, so backward moves (questions, surprises) can use it as well.
     *
     * @throws NullPointerException if an argument is null
     */
    @Override
    public boolean isPassable(Cell cell, Player player) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(player, "player");
        for (Player other : players) {
            if (other.color() == player.color()) {
                continue;
            }
            int onCell = 0;
            for (Piece piece : other.pieces()) {
                if (piece.onBoard() && cell.equals(board.cellAt(piece.color(), piece.progress()))) {
                    onCell++;
                }
            }
            if (onCell >= 2) {
                return false;
            }
        }
        return true;
    }

    /** All but one player have finished (FR-44). */
    private boolean isGameOver() {
        long playing = players.stream().filter(p -> !p.allFinished()).count();
        return playing <= 1;
    }

    private void requireCapturable(Piece victim, Piece mover, Cell target) {
        if (victim == null || victim.color() == mover.color() || !victim.onBoard()
                || !target.equals(board.cellAt(victim.color(), victim.progress()))) {
            throw new IllegalStateException("SquareRules returned a piece that is not an opponent on " + target
                    + ": " + victim);
        }
        boolean inGame = players.stream().anyMatch(p -> p.pieces().contains(victim));
        if (!inGame) {
            // A rule error, as the Javadoc of apply says, not a bad argument from the caller.
            throw new IllegalStateException("SquareRules returned a piece that is not in this game: " + victim);
        }
    }

    private void requireInGame(Player player) {
        for (Player p : players) {
            if (p == player) {
                return;
            }
        }
        throw new IllegalArgumentException("Player is not in this game: " + player);
    }

    private Player ownerOf(Piece piece) {
        for (Player p : players) {
            if (p.pieces().contains(piece)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Piece is not in this game: " + piece);
    }

    Board board() {
        return board;
    }

    List<Player> players() {
        return players;
    }

    SquareRules squareRules() {
        return squareRules;
    }

    HomePathRules homePathRules() {
        return homePathRules;
    }
}
