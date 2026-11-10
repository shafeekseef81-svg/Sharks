package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link StandardRankingTracker} (Task 23; SRS FR-43, FR-44, FR-45; staff Q49, Q103). */
class StandardRankingTrackerTest {

    private static final int FINISH = 56;

    private static List<Player> players(int count) {
        PlayerColor[] colors = PlayerColor.values();
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Player("P" + (i + 1), colors[i], FINISH));
        }
        return list;
    }

    /** Brings one more piece home and tells the tracker, as the turn manager does. */
    private static void finishPiece(RankingTracker tracker, Player player, int index) {
        player.pieces().get(index).setProgress(FINISH);
        tracker.onPieceFinished(player);
    }

    private static void finishAll(RankingTracker tracker, Player player) {
        for (int i = 0; i < 4; i++) {
            finishPiece(tracker, player, i);
        }
    }

    @Test
    @DisplayName("FR-43: at the start nobody has finished and the game is not over")
    void initialState() {
        List<Player> list = players(4);
        RankingTracker tracker = new StandardRankingTracker(list);
        assertFalse(tracker.isGameOver());
        assertTrue(tracker.ranking().isEmpty());
        for (Player p : list) {
            assertFalse(tracker.hasFinished(p));
            assertEquals(0, p.rank());
        }
    }

    @Test
    @DisplayName("FR-43: three pieces home is not finished; the fourth piece finishes the player")
    void playerFinishesWithFourthPiece() {
        List<Player> list = players(3);
        RankingTracker tracker = new StandardRankingTracker(list);
        Player player = list.get(0);
        for (int i = 0; i < 3; i++) {
            finishPiece(tracker, player, i);
            assertFalse(tracker.hasFinished(player), "pieces home: " + (i + 1));
            assertEquals(0, player.rank(), "pieces home: " + (i + 1));
        }
        finishPiece(tracker, player, 3);
        assertTrue(tracker.hasFinished(player));
        assertEquals(1, player.rank());
    }

    @Test
    @DisplayName("FR-45: ranks follow the order of finishing, not the setup order")
    void ranksFollowFinishingOrder() {
        List<Player> list = players(4);
        RankingTracker tracker = new StandardRankingTracker(list);
        finishAll(tracker, list.get(2));
        finishAll(tracker, list.get(0));
        assertEquals(List.of(list.get(2), list.get(0)), tracker.ranking());
        assertEquals(1, list.get(2).rank());
        assertEquals(2, list.get(0).rank());
    }

    @Test
    @DisplayName("FR-44: with four players the game ends when the third player finishes")
    void gameEndsWhenAllButOneFinished() {
        List<Player> list = players(4);
        RankingTracker tracker = new StandardRankingTracker(list);
        finishAll(tracker, list.get(1));
        assertFalse(tracker.isGameOver());
        finishAll(tracker, list.get(3));
        assertFalse(tracker.isGameOver());
        finishAll(tracker, list.get(0));
        assertTrue(tracker.isGameOver());
    }

    @Test
    @DisplayName("FR-44: with two players the game ends when the first player finishes")
    void twoPlayerGame() {
        List<Player> list = players(2);
        RankingTracker tracker = new StandardRankingTracker(list);
        finishAll(tracker, list.get(1));
        assertTrue(tracker.isGameOver());
        assertEquals(List.of(list.get(1), list.get(0)), tracker.ranking());
        assertEquals(1, list.get(1).rank());
        assertEquals(2, list.get(0).rank());
    }

    @Test
    @DisplayName("FR-44, Q49: the remaining player is ranked last, without having finished")
    void remainingPlayerIsLast() {
        List<Player> list = players(3);
        RankingTracker tracker = new StandardRankingTracker(list);
        Player last = list.get(0);
        finishPiece(tracker, last, 0);
        finishAll(tracker, list.get(2));
        finishAll(tracker, list.get(1));

        assertEquals(List.of(list.get(2), list.get(1), last), tracker.ranking());
        assertEquals(3, last.rank());
        assertFalse(tracker.hasFinished(last));
    }

    @Test
    @DisplayName("FR-45: before the game is over the ranking holds only the players who finished")
    void rankingBeforeGameOver() {
        List<Player> list = players(3);
        RankingTracker tracker = new StandardRankingTracker(list);
        finishAll(tracker, list.get(1));
        assertEquals(List.of(list.get(1)), tracker.ranking());
    }

    @Test
    @DisplayName("FR-45: reporting a finished player again does not change the ranking")
    void repeatedReportIsIgnored() {
        List<Player> list = players(3);
        RankingTracker tracker = new StandardRankingTracker(list);
        finishAll(tracker, list.get(0));
        tracker.onPieceFinished(list.get(0));
        assertEquals(List.of(list.get(0)), tracker.ranking());
        assertEquals(1, list.get(0).rank());
    }

    @Test
    @DisplayName("NFR-11: a null player or a player from another game is refused")
    void invalidPlayersAreRefused() {
        RankingTracker tracker = new StandardRankingTracker(players(2));
        assertThrows(NullPointerException.class, () -> tracker.onPieceFinished(null));
        assertThrows(IllegalArgumentException.class, () -> tracker.onPieceFinished(players(2).get(0)));
        assertThrows(NullPointerException.class, () -> new StandardRankingTracker(null));
    }

    @Test
    @DisplayName("NFR-08: the ranking cannot be modified by a caller")
    void rankingIsReadOnly() {
        RankingTracker tracker = new StandardRankingTracker(players(2));
        assertThrows(UnsupportedOperationException.class, () -> tracker.ranking().clear());
    }
}
