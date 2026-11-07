package Model;

import java.util.List;

/**
 * Iteration 1 finishing order and ranking (FR-43..FR-45).
 *
 * <p>Owner: Shafeek Saif. <b>Phase A placeholder</b>: implemented in Task 23.
 */
public final class StandardRankingTracker implements RankingTracker {

    private final List<Player> players;

    /** @param players all players, in turn order */
    public StandardRankingTracker(List<Player> players) {
        this.players = List.copyOf(players);
    }

    @Override
    public void onPieceFinished(Player player) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 23 (Shafeek)");
    }

    @Override
    public boolean isGameOver() {
        return false; // Phase A: no game can end yet; replaced in Task 23
    }

    @Override
    public List<Player> ranking() {
        return List.of(); // Phase A: no player has finished yet; replaced in Task 23
    }

    @Override
    public boolean hasFinished(Player player) {
        return false; // Phase A: replaced in Task 23
    }

    List<Player> players() {
        return players;
    }
}
