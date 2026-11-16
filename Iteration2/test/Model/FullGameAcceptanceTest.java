package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Acceptance tests of US-1.5 "the game ends with rankings and duration" and of complete Easy games (Task 26:
 * FR-43..FR-47), written by the Product Owner. Games are played through the {@link GameModel} facade with the
 * real dice and all the real rule classes.
 *
 * <p><b>Not automated here:</b> the results screen (FR-47) is covered by the GameController integration tests (N02)
 * and a manual run. The two duration tests run against the real {@link GameClock} of Task 24.
 */
class FullGameAcceptanceTest {

    private static final int ROLL_LIMIT = 200_000;

    private static GameSettings settings(int count) {
        PlayerColor[] colors = PlayerColor.values();
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            setup.add(new GameSettings.PlayerSetup("P" + (i + 1), colors[i]));
        }
        return new GameSettings(Difficulty.EASY, setup);
    }

    /** Facts collected while one complete game is played. */
    private static final class Played {
        GameModel model;
        final List<Player> finishEvents = new ArrayList<>();
        int gameOverEvents;
        int captures;
        int rolls;
    }

    /** Plays a complete game, checking the rules that must hold after every step. */
    private static Played playToEnd(int count, long seed) {
        Played p = new Played();
        Random choice = new Random(seed * 31 + 7);
        GameModel model = new GameModel(settings(count), new Random(seed));
        p.model = model;
        model.addListener(e -> {
            if (e.type() == GameEvent.Type.PLAYER_FINISHED) {
                p.finishEvents.add(e.player());
            } else if (e.type() == GameEvent.Type.GAME_OVER) {
                p.gameOverEvents++;
            }
        });
        int finish = model.board().finishIndex();
        while (!model.isGameOver()) {
            assertTrue(p.rolls++ < ROLL_LIMIT, "the game must end, seed " + seed);
            Player active = model.currentPlayer();
            assertFalse(active.allFinished(), "FR-43: a finished player takes no more turns, seed " + seed);
            model.roll();
            List<Piece> movable = new ArrayList<>(model.legalMoves().keySet());
            if (movable.isEmpty()) {
                continue;
            }
            Piece chosen = movable.get(choice.nextInt(movable.size()));
            int before = chosen.progress();
            MoveResult r = model.move(chosen);
            p.captures += r.captures().size();
            assertTrue(chosen.progress() > before, "a piece only moves forward in Iteration 1");
            for (Player player : model.players()) {
                for (Piece piece : player.pieces()) {
                    assertTrue(piece.progress() >= Piece.IN_CASTLE && piece.progress() <= finish);
                }
            }
            for (MoveResult.Capture c : r.captures()) {
                assertTrue(c.piece().inCastle(), "FR-26: a captured piece is in its castle");
                assertFalse(c.piece().color() == chosen.color(), "FR-26: only opponents are captured");
                assertFalse(model.board().squareAt(c.from()).isProtected(), "FR-27: never on a protected square");
            }
        }
        return p;
    }

    @Test
    @DisplayName("T26 FR-43, FR-44, FR-45: complete 2, 3 and 4 player games end with a correct ranking")
    void completeGamesEndCorrectly() {
        int games = 0;
        long captures = 0;
        for (int count = 2; count <= 4; count++) {
            for (long seed = 0; seed < 40; seed++) {
                Played p = playToEnd(count, seed);
                GameModel m = p.model;
                String where = count + " players, seed " + seed;
                assertTrue(m.isGameOver(), where);
                assertEquals(TurnPhase.GAME_OVER, m.phase(), where);
                assertEquals(1, p.gameOverEvents, "GAME_OVER is announced once: " + where);

                List<Player> ranking = m.ranking();
                assertEquals(count, ranking.size(), "everyone is ranked: " + where);
                assertEquals(count, ranking.stream().distinct().count(), where);
                assertEquals(p.finishEvents, ranking.subList(0, count - 1), "FR-45: order of finishing, " + where);
                for (int i = 0; i < count; i++) {
                    assertEquals(i + 1, ranking.get(i).rank(), where);
                    assertEquals(i < count - 1, ranking.get(i).allFinished(), "FR-44: " + where);
                }
                games++;
                captures += p.captures;
            }
        }
        assertEquals(120, games);
        assertTrue(captures > 0, "captures happened in real play");
    }

    @Test
    @DisplayName("T26 FR-44: after the game is over nobody can roll")
    void noRollAfterGameOver() {
        Played p = playToEnd(2, 3);
        assertThrows(IllegalStateException.class, p.model::roll);
    }

    @Test
    @DisplayName("T26 FR-43: when a player finishes in a 3 player game the others keep playing, and he is skipped")
    void finishedPlayerIsSkipped() {
        Deque<Integer> values = new ArrayDeque<>(List.of(1, 1, 1, 1));
        Dice dice = level -> DiceRoll.regularOnly(values.remove());
        GameModel m = new GameModel(settings(3), new Random(9), dice, () -> 0L);
        Player first = m.players().get(0);
        Player second = m.players().get(1);
        Player third = m.players().get(2);
        int finish = m.board().finishIndex();
        for (int i = 0; i < 3; i++) {
            first.pieces().get(i).setProgress(finish);
        }
        first.pieces().get(3).setProgress(finish - 1);

        m.roll();
        MoveResult r = m.move(first.pieces().get(3));

        assertTrue(r.playerFinished());
        assertFalse(r.gameOver());
        assertFalse(m.isGameOver());
        assertEquals(List.of(first), m.ranking());
        assertSame(second, m.currentPlayer());
        m.roll();
        assertSame(third, m.currentPlayer());
        m.roll();
        assertSame(second, m.currentPlayer(), "the finished first player is skipped (FR-43)");
    }

    @Test
    @DisplayName("T26 FR-21, FR-43: a player who finishes on a 6 does not roll again")
    void finishingOnSixEndsThatPlayersTurns() {
        Deque<Integer> values = new ArrayDeque<>(List.of(6));
        Dice dice = level -> DiceRoll.regularOnly(values.remove());
        GameModel m = new GameModel(settings(3), new Random(9), dice, () -> 0L);
        Player first = m.players().get(0);
        int finish = m.board().finishIndex();
        for (int i = 0; i < 3; i++) {
            first.pieces().get(i).setProgress(finish);
        }
        first.pieces().get(3).setProgress(finish - 6);
        m.roll();
        m.move(first.pieces().get(3));
        assertFalse(m.hasBonusRoll());
        assertSame(m.players().get(1), m.currentPlayer());
    }

    @Test
    @DisplayName("T26 FR-46: the elapsed game time is measured")
    void clockMeasuresElapsedTime() {
        AtomicLong now = new AtomicLong(1_000);
        GameClock clock = new GameClock(now::get);
        clock.start();
        now.addAndGet(65_000);
        assertEquals(65_000, clock.elapsedMillis());
        clock.stop();
        now.addAndGet(10_000);
        assertEquals(65_000, clock.elapsedMillis(), "the time stops with the game");
    }

    @Test
    @DisplayName("T26 FR-46: the duration is shown as HH:MM:SS")
    void durationFormat() {
        assertEquals("00:00:00", GameClock.format(0));
        assertEquals("00:01:05", GameClock.format(65_000));
        assertEquals("01:00:01", GameClock.format(3_601_000));
    }
}
