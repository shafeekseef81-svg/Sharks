package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests for Task 11: turn order and rolling (FR-15). The Rule of 6 (Task 12) and moving (Task 16)
 * are not tested here.
 *
 * <p>Owner: Rakan Ziyan.
 */
class TurnOrderTest {

    /** Ranking stub: the test decides who has finished and whether the game is over. */
    private static final class StubRanking implements RankingTracker {
        final Set<Player> finished = new HashSet<>();
        boolean over;

        @Override
        public void onPieceFinished(Player player) {
        }

        @Override
        public boolean isGameOver() {
            return over;
        }

        @Override
        public List<Player> ranking() {
            return List.of();
        }

        @Override
        public boolean hasFinished(Player player) {
            return finished.contains(player);
        }
    }

    /** Move-engine stub that records the roll it was asked about. */
    private static final class RecordingEngine implements MoveEngine {
        Player askedPlayer;
        DiceRoll askedRoll;

        @Override
        public Map<Piece, Move> legalMoves(Player player, DiceRoll roll) {
            askedPlayer = player;
            askedRoll = roll;
            return Map.of();
        }

        @Override
        public MoveResult apply(Move move) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPassable(Cell cell, Player player) {
            return true;
        }
    }

    private static List<Player> players(int n) {
        PlayerColor[] colors = PlayerColor.values();
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Player("P" + (i + 1), colors[i], 56));
        }
        return list;
    }

    @Test
    @DisplayName("FR-15: the first player in setup order starts, no roll has been made")
    void initialTurn() {
        List<Player> ps = players(3);
        StandardTurnManager t = new StandardTurnManager(ps, Difficulty.EASY, new FixedDice(4),
                new RecordingEngine(), new StubRanking());
        assertSame(ps.get(0), t.current());
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase());
        assertNull(t.lastRoll());
        assertTrue(t.legalMoves().isEmpty());
        assertFalse(t.hasBonusRoll());
    }

    @Test
    @DisplayName("FR-15/FR-16: a roll uses the dice, is stored for the active player and waits for a move")
    void rollIsStored() {
        List<Player> ps = players(2);
        RecordingEngine engine = new RecordingEngine();
        StandardTurnManager t = new StandardTurnManager(ps, Difficulty.EASY, new FixedDice(4), engine,
                new StubRanking());
        DiceRoll r = t.roll();
        assertEquals(4, r.regular());
        assertSame(r, t.lastRoll());
        assertEquals(TurnPhase.AWAITING_MOVE, t.phase());

        t.legalMoves();
        assertSame(ps.get(0), engine.askedPlayer, "legal moves are asked for the active player");
        assertSame(r, engine.askedRoll);
    }

    @Test
    @DisplayName("FR-15: the active player rolls once per turn (extra rolls are Task 12)")
    void oneRollPerTurn() {
        StandardTurnManager t = new StandardTurnManager(players(2), Difficulty.EASY, new FixedDice(3, 5),
                new RecordingEngine(), new StubRanking());
        t.roll();
        assertThrows(IllegalStateException.class, t::roll);
    }

    @Test
    @DisplayName("FR-15: turns follow the setup order and wrap around, for 2, 3 and 4 players")
    void setupOrderWraps() {
        for (int n = 2; n <= 4; n++) {
            List<Player> ps = players(n);
            StandardTurnManager t = new StandardTurnManager(ps, Difficulty.EASY,
                    new FixedDice(1, 2, 3, 4, 5, 1, 2, 3, 4), new RecordingEngine(), new StubRanking());
            for (int turn = 0; turn < 2 * n; turn++) {
                assertSame(ps.get(turn % n), t.current(), n + " players, turn " + turn);
                t.roll();
                assertNotNull(t.endTurn(), "no legal move in this stub: the turn passes with a message (FR-23)");
                assertNull(t.lastRoll(), "a new turn starts without a roll");
                assertEquals(TurnPhase.AWAITING_ROLL, t.phase());
            }
        }
    }

    @Test
    @DisplayName("FR-15: a turn cannot end before the active player has rolled")
    void cannotEndTurnWithoutRoll() {
        StandardTurnManager t = new StandardTurnManager(players(2), Difficulty.EASY, new FixedDice(2),
                new RecordingEngine(), new StubRanking());
        assertThrows(IllegalStateException.class, t::endTurn);
    }

    @Test
    @DisplayName("FR-43: finished players are skipped in the turn order")
    void finishedPlayersAreSkipped() {
        List<Player> ps = players(4);
        StubRanking ranking = new StubRanking();
        StandardTurnManager t = new StandardTurnManager(ps, Difficulty.EASY, new FixedDice(1, 2, 3),
                new RecordingEngine(), ranking);
        ranking.finished.add(ps.get(1));
        ranking.finished.add(ps.get(2));
        t.roll();
        t.endTurn();
        assertSame(ps.get(3), t.current(), "P2 and P3 have finished");
        t.roll();
        t.endTurn();
        assertSame(ps.get(0), t.current());
    }

    @Test
    @DisplayName("FR-44: when the ranking reports the end, the game is over and nobody can roll")
    void gameOver() {
        StubRanking ranking = new StubRanking();
        StandardTurnManager t = new StandardTurnManager(players(2), Difficulty.EASY, new FixedDice(2, 2),
                new RecordingEngine(), ranking);
        t.roll();
        ranking.over = true;
        t.endTurn();
        assertEquals(TurnPhase.GAME_OVER, t.phase());
        assertThrows(IllegalStateException.class, t::roll);
        assertThrows(IllegalStateException.class, t::endTurn);
        assertTrue(t.legalMoves().isEmpty());
    }

    @Test
    @DisplayName("Invalid configuration: fewer than two players or missing parts are rejected")
    void invalidConfiguration() {
        RecordingEngine e = new RecordingEngine();
        StubRanking r = new StubRanking();
        FixedDice d = new FixedDice(1);
        assertThrows(IllegalArgumentException.class,
                () -> new StandardTurnManager(players(1), Difficulty.EASY, d, e, r));
        assertThrows(NullPointerException.class,
                () -> new StandardTurnManager(players(2), null, d, e, r));
        assertThrows(NullPointerException.class,
                () -> new StandardTurnManager(players(2), Difficulty.EASY, null, e, r));
        assertThrows(NullPointerException.class,
                () -> new StandardTurnManager(players(2), Difficulty.EASY, d, null, r));
        assertThrows(NullPointerException.class,
                () -> new StandardTurnManager(players(2), Difficulty.EASY, d, e, null));
    }

    @Test
    @DisplayName("FR-15 through the facade: GameModel.roll() rolls for the active player and fires ROLLED")
    void facadeFiresRolled() {
        GameSettings s = new GameSettings(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Ana", PlayerColor.RED),
                new GameSettings.PlayerSetup("Ben", PlayerColor.BLUE)));
        GameModel m = new GameModel(s, new Random(5), new FixedDice(5), () -> 0L,
                (b, p, sr, hr) -> new RecordingEngine());
        List<GameEvent> events = new ArrayList<>();
        m.addListener(events::add);
        Player roller = m.currentPlayer();
        DiceRoll r = m.roll();
        assertEquals(5, r.regular());
        assertEquals(GameEvent.Type.ROLLED, events.get(0).type());
        assertSame(roller, events.get(0).player());
        assertSame(r, events.get(0).roll());
    }

    @Test
    @DisplayName("FR-16: the real game uses RandomDice; an Easy game can roll without a test double")
    void realGameRolls() {
        GameSettings s = new GameSettings(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Ana", PlayerColor.RED),
                new GameSettings.PlayerSetup("Ben", PlayerColor.BLUE)));
        GameModel m = new GameModel(s, new Random(9), new RandomDice(new Random(9)), () -> 0L,
                (b, p, sr, hr) -> new RecordingEngine());
        int v = m.roll().regular();
        assertTrue(v >= 1 && v <= 6);
    }
}
