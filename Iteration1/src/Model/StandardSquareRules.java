package Model;

import java.util.List;

/**
 * Iteration 1 capture and protection rules (FR-26 partial, FR-27 partial).
 *
 * <p>Owner: Shafeek Saif. <b>Phase A placeholder</b>: implemented in Task 17.
 */
public final class StandardSquareRules implements SquareRules {

    private final Board board;
    private final List<Player> players;

    /**
     * @param board   the game board
     * @param players all players of the game
     */
    public StandardSquareRules(Board board, List<Player> players) {
        this.board = board;
        this.players = List.copyOf(players);
    }

    @Override
    public boolean isProtected(Square square) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 17 (Shafeek)");
    }

    @Override
    public List<Piece> capturesOnLanding(Piece mover, Cell target) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 17 (Shafeek)");
    }

    Board board() {
        return board;
    }

    List<Player> players() {
        return players;
    }
}
