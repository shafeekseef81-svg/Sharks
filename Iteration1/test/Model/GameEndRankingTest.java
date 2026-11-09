package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Task 25: independent unit tests for game end and ranking (SRS FR-43, FR-44, FR-45), written from the SRS text
 * by Rakan Ziyan, who did not implement {@link StandardRankingTracker} (Shafeek Saif, Task 23; SDP 5.3.4,
 * Definition of Done). The tracker is tested alone and through the real {@link GameModel}, with the real move
 * engine and turn manager.
 *
 * <p>FR-46 (elapsed time as HH:MM:SS) belongs to {@link GameClock}, Task 24. Its tests are added here once Task 24
 * is merged.
 */
class GameEndRankingTest {

    private static final int FINISH = 56; // Easy: 51 track squares + 5 home path squares, then the finish

    private static List<Player> players(int n) {
        PlayerColor[] colors = PlayerColor.values();
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Player("P" + (i + 1), colors[i], FINISH));
        }
        return list;
    }

    private static void finishAll(Player p) {
        for (Piece piece : p.pieces()) {
            piece.setProgress(piece.finishIndex());
        }
    }

    // ------------------------------------------------------------------ tracker alone

    @Test
    @DisplayName("FR-43: a player with three pieces home has not finished; the fourth piece finishes the player")
    void finishNeedsAllFourPieces() {
        List<Player> ps = players(3);
        RankingTracker t = new StandardRankingTracker(ps);
        Player p = ps.get(0);
        for (int i = 0; i < 3; i++) {
            p.pieces().get(i).setProgress(FINISH);
            t.onPieceFinished(p);
        }
        assertFalse(t.hasFinished(p));
        assertTrue(t.ranking().isEmpty());
        p.pieces().get(3).setProgress(FINISH);
        t.onPieceFinished(p);
        assertTrue(t.hasFinished(p));
        assertEquals(1, p.rank());
    }

    @Test
    @DisplayName("FR-44: with two players the game ends when one finishes, and the other takes the last rank")
    void twoPlayersEndAfterTheFirst() {
        List<Player> ps = players(2);
        RankingTracker t = new StandardRankingTracker(ps);
        assertFalse(t.isGameOver());
        finishAll(ps.get(1));
        t.onPieceFinished(ps.get(1));
        assertTrue(t.isGameOver());
        assertEquals(List.of(ps.get(1), ps.get(0)), t.ranking());
        assertEquals(1, ps.get(1).rank());
        assertEquals(2, ps.get(0).rank());
        assertFalse(t.hasFinished(ps.get(0)), "the last player did not finish their pieces");
    }

    @Test
    @DisplayName("FR-44, FR-45: with four players the game goes on until three have finished; ranks follow the finishing order")
    void fourPlayersRankedByFinishingOrder() {
        List<Player> ps = players(4);
        RankingTracker t = new StandardRankingTracker(ps);
        int[] order = {2, 0, 3};
        for (int k = 0; k < order.length; k++) {
            assertFalse(t.isGameOver(), "after " + k + " finished");
            finishAll(ps.get(order[k]));
            t.onPieceFinished(ps.get(order[k]));
        }
        assertTrue(t.isGameOver());
        assertEquals(List.of(ps.get(2), ps.get(0), ps.get(3), ps.get(1)), t.ranking(),
                "FR-45: finishing order, not setup order");
        for (int r = 0; r < 4; r++) {
            assertEquals(r + 1, t.ranking().get(r).rank());
        }
    }

    @Test
    @DisplayName("FR-45: reporting a finished player again changes nothing")
    void repeatedReportIsIgnored() {
        List<Player> ps = players(3);
        RankingTracker t = new StandardRankingTracker(ps);
        finishAll(ps.get(1));
        t.onPieceFinished(ps.get(1));
        t.onPieceFinished(ps.get(1));
        assertEquals(List.of(ps.get(1)), t.ranking());
        assertEquals(1, ps.get(1).rank());
    }

    @Test
    @DisplayName("FR-45: the ranking cannot be changed from outside, and a stranger cannot be reported")
    void rankingIsProtected() {
        List<Player> ps = players(2);
        RankingTracker t = new StandardRankingTracker(ps);
        finishAll(ps.get(0));
        t.onPieceFinished(ps.get(0));
        assertThrows(UnsupportedOperationException.class, () -> t.ranking().clear());
        assertThrows(IllegalArgumentException.class,
                () -> t.onPieceFinished(new Player("X", PlayerColor.BLUE, FINISH)));
    }

    // ------------------------------------------------------------------ through the real game

    private static GameSettings settings(int n) {
        PlayerColor[] colors = PlayerColor.values();
        List<GameSettings.PlayerSetup> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new GameSettings.PlayerSetup("P" + (i + 1), colors[i]));
        }
        return new GameSettings(Difficulty.EASY, list);
    }

    /** Three pieces home, the fourth two squares before the finish. */
    private static void almostDone(Player p) {
        for (int i = 0; i < 3; i++) {
            p.pieces().get(i).setProgress(p.pieces().get(i).finishIndex());
        }
        Piece last = p.pieces().get(3);
        last.setProgress(last.finishIndex() - 2);
    }

    @Test
    @DisplayName("FR-43 in a real game: the last piece home finishes the player, who is skipped from then on")
    void finishedPlayerIsSkipped() {
        GameModel m = new GameModel(settings(3), new Random(3), new FixedDice(2, 1, 1, 1), () -> 0L);
        Player p1 = m.players().get(0);
        Player p2 = m.players().get(1);
        Player p3 = m.players().get(2);
        almostDone(p1);
        List<GameEvent.Type> events = new ArrayList<>();
        m.addListener(e -> events.add(e.type()));

        m.roll();
        m.move(p1.pieces().get(3));
        assertTrue(p1.allFinished());
        assertTrue(events.contains(GameEvent.Type.PLAYER_FINISHED));
        assertFalse(m.isGameOver(), "two players are still playing");
        assertSame(p2, m.currentPlayer());

        m.roll(); // P2: a 1, nothing can leave the castle (FR-23)
        assertSame(p3, m.currentPlayer());
        m.roll(); // P3: a 1, passes
        assertSame(p2, m.currentPlayer(), "FR-43: P1 has finished and takes no more turns");
    }

    @Test
    @DisplayName("FR-44 in a real game: when all but one have finished the game is over and the ranking is complete")
    void realGameEnds() {
        GameModel m = new GameModel(settings(2), new Random(3), new FixedDice(2), () -> 0L);
        Player p1 = m.players().get(0);
        Player p2 = m.players().get(1);
        almostDone(p1);
        List<GameEvent.Type> events = new ArrayList<>();
        m.addListener(e -> events.add(e.type()));

        m.roll();
        m.move(p1.pieces().get(3));
        assertTrue(m.isGameOver());
        assertEquals(TurnPhase.GAME_OVER, m.phase());
        assertEquals(List.of(p1, p2), m.ranking());
        assertEquals(2, p2.rank(), "FR-44: the remaining player is ranked last");
        assertEquals(GameEvent.Type.GAME_OVER, events.get(events.size() - 1));
        assertFalse(events.contains(GameEvent.Type.TURN_CHANGED), "no turn after the game is over");
    }
}
