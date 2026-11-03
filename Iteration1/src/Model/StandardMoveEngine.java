package Model;

import java.util.List;
import java.util.Map;

/**
 * Iteration 1 move engine (FR-19 partial, FR-20, FR-25 partial).
 *
 * <p>Owner: Rakan Ziyan. <b>Phase A placeholder</b>: implemented in Task 16.
 */
public final class StandardMoveEngine implements MoveEngine {

    private final Board board;
    private final List<Player> players;
    private final SquareRules squareRules;
    private final HomePathRules homePathRules;

    /**
     * @param board         the game board
     * @param players       all players of the game (needed for captures)
     * @param squareRules   capture and protection rules
     * @param homePathRules home path and exact-finish rules
     */
    public StandardMoveEngine(Board board, List<Player> players, SquareRules squareRules,
                              HomePathRules homePathRules) {
        this.board = board;
        this.players = List.copyOf(players);
        this.squareRules = squareRules;
        this.homePathRules = homePathRules;
    }

    @Override
    public Map<Piece, Move> legalMoves(Player player, DiceRoll roll) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 16 (Rakan)");
    }

    @Override
    public MoveResult apply(Move move) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 16 (Rakan)");
    }

    @Override
    public boolean isPassable(Cell cell, Player player) {
        return true; // Iteration 1 has no Double Blocks (FR-28..FR-30 come in Iteration 2)
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
