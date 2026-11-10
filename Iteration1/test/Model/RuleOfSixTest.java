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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests for Task 12: the Rule of 6, the cancelled third 6, the no-move pass and the turn flow of the
 * {@link GameModel} facade (FR-20..FR-23, FR-43, FR-44). The move engine (Task 16) and the ranking (Task 23)
 * are replaced by stubs, so only the turn rules are tested here. The independent tests of Task 14 are written
 * by Shafeek Saif.
 *
 * <p>Owner: Rakan Ziyan.
 */
class RuleOfSixTest {

    /** Engine stub: the test decides whether the active player has a legal move and what a move reports. */
    private static final class ScriptedEngine implements MoveEngine {
        boolean movable;
        boolean nextMoveFinishesPiece;
        boolean nextMoveCaptures;
        int applied;

        @Override
        public Map<Piece, Move> legalMoves(Player player, DiceRoll roll) {
            Map<Piece, Move> moves = new LinkedHashMap<>();
            if (movable) {
                Piece p = player.pieces().get(0);
                moves.put(p, new Move(p, roll.movement()));
            }
            return moves;
        }

        @Override
        public MoveResult apply(Move move) {
            applied++;
            List<MoveResult.Capture> captures = nextMoveCaptures
                    ? List.of(new MoveResult.Capture(move.piece(), new Cell(6, 1))) : List.of();
            return new MoveResult(move.piece(), MoveResult.Kind.STEPS, 0, move.targetProgress(), List.of(), null,
                    captures, null, nextMoveFinishesPiece, false, false);
        }

        @Override
        public boolean isPassable(Cell cell, Player player) {
            return true;
        }
    }

    /** Ranking stub: records finished pieces; the test decides who has finished and when the game ends. */
    private static final class StubRanking implements RankingTracker {
        final List<Player> piecesFinished = new ArrayList<>();
        final Set<Player> finished = new HashSet<>();
        boolean over;

        @Override
        public void onPieceFinished(Player player) {
            piecesFinished.add(player);
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

    private final ScriptedEngine engine = new ScriptedEngine();
    private final StubRanking ranking = new StubRanking();
    private final List<Player> players = List.of(
            new Player("Ana", PlayerColor.RED, 56), new Player("Ben", PlayerColor.GREEN, 56),
            new Player("Dan", PlayerColor.BLUE, 56));

    private StandardTurnManager turns(int... rolls) {
        return new StandardTurnManager(players, Difficulty.EASY, new FixedDice(rolls), engine, ranking);
    }

    private Piece firstPiece(StandardTurnManager t) {
        return t.current().pieces().get(0);
    }

    // ---- FR-21 -------------------------------------------------------------------------------------

    @Test
    @DisplayName("FR-21: after a 6 and a move the same player rolls again, and the turn cannot end first")
    void sixGivesExtraRollAfterMove() {
        engine.movable = true;
        StandardTurnManager t = turns(6, 3);
        t.roll();
        assertTrue(t.hasBonusRoll());
        assertFalse(t.canRoll(), "a legal move must be made before the extra roll (FR-20)");
        t.move(firstPiece(t));
        assertSame(players.get(0), t.current());
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase());
        assertThrows(IllegalStateException.class, t::endTurn, "the extra roll is not optional");

        assertEquals(3, t.roll().regular());
        assertFalse(t.hasBonusRoll());
        t.move(firstPiece(t));
        assertEquals(TurnPhase.MOVING, t.phase());
        assertNull(t.endTurn(), "a turn with a move passes without a message");
        assertSame(players.get(1), t.current());
    }

    @Test
    @DisplayName("FR-21: a 6 gives an extra roll even when no piece can move")
    void sixGivesExtraRollWithoutMove() {
        engine.movable = false;
        StandardTurnManager t = turns(6, 2);
        t.roll();
        assertTrue(t.legalMoves().isEmpty());
        assertTrue(t.hasBonusRoll());
        assertTrue(t.canRoll());
        assertThrows(IllegalStateException.class, t::endTurn);
        t.roll();
        assertSame(players.get(0), t.current(), "still the same player");
        assertFalse(t.hasBonusRoll());
        assertNotNull(t.endTurn());
        assertSame(players.get(1), t.current());
    }

    @Test
    @DisplayName("FR-21: capturing or finishing grants no extra roll; only a 6 does")
    void captureOrFinishGivesNoExtraRoll() {
        engine.movable = true;
        engine.nextMoveCaptures = true;
        engine.nextMoveFinishesPiece = true;
        StandardTurnManager t = turns(4, 6);
        t.roll();
        t.move(firstPiece(t));
        assertFalse(t.hasBonusRoll(), "a 4 that captures and finishes gives no extra roll");
        assertEquals(TurnPhase.MOVING, t.phase());
        t.endTurn();

        t.roll(); // Ben rolls a 6 that captures
        t.move(firstPiece(t));
        assertTrue(t.hasBonusRoll(), "the 6 still gives its extra roll");
    }

    // ---- FR-22 -------------------------------------------------------------------------------------

    @Test
    @DisplayName("FR-22: the third 6 in a row is cancelled: no move, the turn ends with a message")
    void thirdSixIsCancelled() {
        engine.movable = true;
        StandardTurnManager t = turns(6, 6, 6);
        t.roll();
        t.move(firstPiece(t));
        t.roll();
        t.move(firstPiece(t));
        DiceRoll third = t.roll();
        assertTrue(third.cancelled());
        assertEquals(6, third.regular());
        assertTrue(t.legalMoves().isEmpty(), "a cancelled roll moves no piece, even when moves exist");
        assertFalse(t.hasBonusRoll());
        assertThrows(IllegalArgumentException.class, () -> t.move(firstPiece(t)));
        assertEquals(2, engine.applied);

        String message = t.endTurn();
        assertTrue(message.contains("third 6"), message);
        assertSame(players.get(1), t.current());
    }

    @Test
    @DisplayName("FR-22: also when no piece could move after the first two 6s")
    void thirdSixWithoutMoves() {
        engine.movable = false;
        StandardTurnManager t = turns(6, 6, 6);
        assertFalse(t.roll().cancelled());
        assertFalse(t.roll().cancelled());
        assertTrue(t.roll().cancelled());
        assertTrue(t.endTurn().contains("third 6"));
    }

    @Test
    @DisplayName("FR-22: the count of 6s starts again in every turn")
    void sixCountResetsEachTurn() {
        engine.movable = false;
        StandardTurnManager t = turns(6, 6, 2, 6, 6, 1);
        t.roll();
        t.roll();
        t.roll(); // Ana: 6, 6, 2
        t.endTurn();
        assertFalse(t.roll().cancelled(), "Ben's first 6 is not Ana's third");
        assertFalse(t.roll().cancelled());
        t.roll();
        t.endTurn();
        assertSame(players.get(2), t.current());
    }

    // ---- FR-20 / FR-23 ---------------------------------------------------------------------------

    @Test
    @DisplayName("FR-20: a legal move may not be skipped, and only a legal piece may move")
    void legalMoveCannotBeSkipped() {
        engine.movable = true;
        StandardTurnManager t = turns(3);
        assertThrows(IllegalStateException.class, () -> t.move(firstPiece(t)), "no roll yet");
        t.roll();
        assertThrows(IllegalStateException.class, t::endTurn);
        Piece other = t.current().pieces().get(1);
        assertThrows(IllegalArgumentException.class, () -> t.move(other));
        assertEquals(0, engine.applied, "a refused move changes nothing");
        t.move(firstPiece(t));
        assertThrows(IllegalStateException.class, () -> t.move(firstPiece(t)), "one move per roll");
    }

    @Test
    @DisplayName("FR-23: no legal move and no extra roll: the turn passes with a message")
    void noMovePassesWithMessage() {
        engine.movable = false;
        StandardTurnManager t = turns(4);
        t.roll();
        assertFalse(t.canRoll());
        String message = t.endTurn();
        assertTrue(message.contains("Ana") && message.contains("no legal move") && message.contains("4"), message);
        assertSame(players.get(1), t.current());
        assertEquals(TurnPhase.AWAITING_ROLL, t.phase());
    }

    // ---- FR-43 / FR-44 ---------------------------------------------------------------------------

    @Test
    @DisplayName("FR-43: a finished piece is reported; a player who finished loses the extra roll")
    void finishedPlayerLosesExtraRoll() {
        engine.movable = true;
        engine.nextMoveFinishesPiece = true;
        StandardTurnManager t = turns(6);
        t.roll();
        ranking.finished.add(players.get(0)); // this move finishes Ana's last piece
        t.move(firstPiece(t));
        assertEquals(List.of(players.get(0)), ranking.piecesFinished);
        assertFalse(t.hasBonusRoll());
        assertEquals(TurnPhase.MOVING, t.phase());
        t.endTurn();
        assertSame(players.get(1), t.current());
    }

    @Test
    @DisplayName("FR-44: when the ranking reports the end after a move, the game is over")
    void gameOverAfterMove() {
        engine.movable = true;
        engine.nextMoveFinishesPiece = true;
        StandardTurnManager t = turns(6);
        t.roll();
        ranking.over = true;
        t.move(firstPiece(t));
        assertEquals(TurnPhase.GAME_OVER, t.phase());
        assertFalse(t.hasBonusRoll());
        assertThrows(IllegalStateException.class, t::roll);
        assertThrows(IllegalStateException.class, t::endTurn);
    }

    // ---- GameModel facade ------------------------------------------------------------------------

    private GameModel facade(int... rolls) {
        GameSettings s = new GameSettings(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Ana", PlayerColor.RED),
                new GameSettings.PlayerSetup("Ben", PlayerColor.GREEN)));
        return new GameModel(s, new Random(1), new FixedDice(rolls), () -> 0L, (b, p, sr, hr) -> engine);
    }

    private static List<GameEvent.Type> types(List<GameEvent> events) {
        return events.stream().map(GameEvent::type).toList();
    }

    @Test
    @DisplayName("Facade, FR-23: no legal move fires ROLLED, TURN_PASSED with the message, TURN_CHANGED")
    void facadeNoMove() {
        engine.movable = false;
        GameModel m = facade(2);
        List<GameEvent> ev = new ArrayList<>();
        m.addListener(ev::add);
        Player ana = m.currentPlayer();
        m.roll();
        assertEquals(List.of(GameEvent.Type.ROLLED, GameEvent.Type.TURN_PASSED, GameEvent.Type.TURN_CHANGED),
                types(ev));
        assertSame(ana, ev.get(0).player(), "ROLLED names the player who rolled");
        assertSame(ana, ev.get(1).player());
        assertTrue(ev.get(1).message().contains("no legal move"));
        assertSame(m.players().get(1), ev.get(2).player());
        assertSame(m.players().get(1), m.currentPlayer());
    }

    @Test
    @DisplayName("Facade, FR-21: a 6 with no legal move keeps the turn; the player rolls again")
    void facadeSixWithoutMove() {
        engine.movable = false;
        GameModel m = facade(6, 1);
        List<GameEvent> ev = new ArrayList<>();
        m.addListener(ev::add);
        m.roll();
        assertEquals(List.of(GameEvent.Type.ROLLED), types(ev));
        assertTrue(m.hasBonusRoll());
        assertSame(m.players().get(0), m.currentPlayer());
        m.roll();
        assertEquals(GameEvent.Type.TURN_PASSED, ev.get(2).type());
        assertSame(m.players().get(1), m.currentPlayer());
    }

    @Test
    @DisplayName("Facade, FR-20: a move fires MOVED then TURN_CHANGED; after a 6 the turn stays")
    void facadeMove() {
        engine.movable = true;
        GameModel m = facade(6, 4);
        List<GameEvent> ev = new ArrayList<>();
        m.addListener(ev::add);
        m.roll();
        assertEquals(1, m.legalMoves().size());
        m.move(m.currentPlayer().pieces().get(0));
        assertEquals(List.of(GameEvent.Type.ROLLED, GameEvent.Type.MOVED), types(ev), "6: no turn change");
        m.roll();
        m.move(m.currentPlayer().pieces().get(0));
        assertEquals(List.of(GameEvent.Type.ROLLED, GameEvent.Type.MOVED, GameEvent.Type.ROLLED,
                GameEvent.Type.MOVED, GameEvent.Type.TURN_CHANGED), types(ev));
        assertSame(m.players().get(1), m.currentPlayer());
    }

    @Test
    @DisplayName("Facade, FR-22: the third 6 fires TURN_PASSED with the cancel message")
    void facadeThirdSix() {
        engine.movable = false;
        GameModel m = facade(6, 6, 6);
        List<GameEvent> ev = new ArrayList<>();
        m.addListener(ev::add);
        m.roll();
        m.roll();
        m.roll();
        GameEvent passed = ev.get(ev.size() - 2);
        assertEquals(GameEvent.Type.TURN_PASSED, passed.type());
        assertTrue(passed.roll().cancelled());
        assertTrue(passed.message().contains("third 6"));
        assertSame(m.players().get(1), m.currentPlayer());
    }
}
