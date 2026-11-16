package Model;

import java.util.List;

/**
 * Finishing order, game end and ranking (FR-43, FR-44, FR-45). Shared contract, frozen at checkpoint CP1.
 *
 * <p>Owner: Shafeek Saif (Task 23).
 */
public interface RankingTracker {

    /**
     * Called after a piece finishes. Records the player as finished when all four pieces are home (FR-43).
     *
     * @param player the owner of the piece
     */
    void onPieceFinished(Player player);

    /** @return {@code true} when all but one player have finished (FR-44) */
    boolean isGameOver();

    /** @return the players in finishing order; once the game is over, the last player is included (FR-44, FR-45) */
    List<Player> ranking();

    /**
     * @param player a player
     * @return {@code true} if the player has finished and takes no more turns (FR-43)
     */
    boolean hasFinished(Player player);
}
