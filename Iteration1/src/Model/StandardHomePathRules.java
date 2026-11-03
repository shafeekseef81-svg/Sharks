package Model;

/**
 * Iteration 1 home path and exact-finish rules (FR-18, FR-31, FR-33).
 *
 * <p>Owner: Shafeek Saif. <b>Phase A placeholder</b>: implemented in Task 18.
 */
public final class StandardHomePathRules implements HomePathRules {

    private final Board board;

    /** @param board the game board (gives the home path start and the finishing index) */
    public StandardHomePathRules(Board board) {
        this.board = board;
    }

    @Override
    public int stepsFor(Piece piece, DiceRoll roll) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 18 (Shafeek)");
    }

    @Override
    public boolean canReach(Piece piece, int steps) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 18 (Shafeek)");
    }

    Board board() {
        return board;
    }
}
