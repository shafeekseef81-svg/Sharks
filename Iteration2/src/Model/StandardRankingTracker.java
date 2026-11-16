package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Iteration 1 finishing order and ranking (FR-43..FR-45). Task 23, owner: Shafeek Saif.
 *
 * <p>A player finishes when all four pieces are home, and is ranked in the order of finishing. When all but one
 * player have finished the game is over and the remaining player takes the last rank (FR-44, staff Q49).
 */
public final class StandardRankingTracker implements RankingTracker {

    private final List<Player> players;
    private final List<Player> finished = new ArrayList<>();

    /**
     * @param players all players, in turn order
     * @throws NullPointerException if {@code players} is null
     */
    public StandardRankingTracker(List<Player> players) {
        this.players = List.copyOf(players);
    }

    /**
     * Records the player as finished once all four pieces are home (FR-43). Later calls for a recorded player
     * change nothing.
     *
     * @throws NullPointerException if {@code player} is null
     * @throws IllegalArgumentException if the player is not in this game
     */
    @Override
    public void onPieceFinished(Player player) {
        Objects.requireNonNull(player, "player");
        if (!players.contains(player)) {
            throw new IllegalArgumentException(player.name() + " is not in this game");
        }
        if (!player.allFinished() || finished.contains(player)) {
            return;
        }
        finished.add(player);
        player.setRank(finished.size());
        if (isGameOver()) {
            remaining().forEach(last -> last.setRank(players.size()));
        }
    }

    @Override
    public boolean isGameOver() {
        return finished.size() >= players.size() - 1;
    }

    /** @return the players in finishing order, with the last player once the game is over; cannot be modified */
    @Override
    public List<Player> ranking() {
        List<Player> result = new ArrayList<>(finished);
        if (isGameOver()) {
            result.addAll(remaining());
        }
        return Collections.unmodifiableList(result);
    }

    /** @return {@code true} only for a player who finished all pieces; the last-ranked player has not */
    @Override
    public boolean hasFinished(Player player) {
        return finished.contains(player);
    }

    private List<Player> remaining() {
        List<Player> result = new ArrayList<>(players);
        result.removeAll(finished);
        return result;
    }

    List<Player> players() {
        return players;
    }
}
