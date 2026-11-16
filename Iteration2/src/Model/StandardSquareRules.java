package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Iteration 1 capture and protection rules (FR-26 partial, FR-27 partial). Task 17, owner: Shafeek Saif.
 *
 * <p>These rules only decide; they never change a piece. As {@link SquareRules} states, the caller sends the
 * returned pieces back to their castles (staff Q41). Double Blocks (FR-28..FR-30) are Iteration 2.
 */
public final class StandardSquareRules implements SquareRules {

    private final Board board;
    private final List<Player> players;

    /**
     * @param board   the game board
     * @param players all players of the game
     * @throws NullPointerException if an argument is null
     */
    public StandardSquareRules(Board board, List<Player> players) {
        this.board = Objects.requireNonNull(board, "board");
        this.players = List.copyOf(players);
    }

    /**
     * Protection, starting and surprise squares, and the finishing square, are protected (FR-27).
     *
     * @param square the square; {@code null} stands for the finishing square
     * @return {@code true} if the square is protected
     */
    @Override
    public boolean isProtected(Square square) {
        return square == null || square.isProtected();
    }

    /**
     * Finds the piece captured by landing on a cell (FR-26). A capture needs exactly one opponent piece on an
     * unprotected shared-track square; two or more opponent pieces are not captured, because FR-26 names one.
     * Nothing is captured on a home path, which only ever holds its own colour (FR-33).
     *
     * @return the captured piece, or an empty list; the list cannot be modified
     * @throws NullPointerException if an argument is null
     */
    @Override
    public List<Piece> capturesOnLanding(Piece mover, Cell target) {
        Objects.requireNonNull(mover, "mover");
        Objects.requireNonNull(target, "target");
        Square square = board.squareAt(target);
        if (isProtected(square) || square.type() == SquareType.HOME_PATH) {
            return List.of();
        }
        List<Piece> opponents = new ArrayList<>();
        for (Player player : players) {
            for (Piece piece : player.pieces()) {
                if (piece.color() != mover.color() && piece.onBoard()
                        && target.equals(board.cellAt(piece.color(), piece.progress()))) {
                    opponents.add(piece);
                }
            }
        }
        return opponents.size() == 1 ? List.copyOf(opponents) : List.of();
    }

    Board board() {
        return board;
    }

    List<Player> players() {
        return players;
    }
}
