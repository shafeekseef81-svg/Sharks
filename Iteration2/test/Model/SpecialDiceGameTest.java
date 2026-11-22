package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests of T2-04 through the {@link GameModel} facade with all the real rule classes: the Boost and
 * UltraBoost dice in Medium and Hard games (FR-16, FR-17), their effect on the existing rules (FR-18, FR-19,
 * FR-21, FR-23) and the colour's own route (FR-25). Owner: Shafeek Saif.
 *
 * <p>Iteration 2 squares (questions, surprises) and Double Blocks have no effect yet; they come with T2-06 and
 * T2-08..T2-10.
 */
class SpecialDiceGameTest {

    private static final long BOARD_SEED = 11;

    private static GameSettings settings(Difficulty level, int players) {
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            setup.add(new GameSettings.PlayerSetup("P" + (i + 1), PlayerColor.values()[i]));
        }
        return new GameSettings(level, setup);
    }

    /** A two-player game whose dice show the scripted faces (Regular, special, Regular, special, ...). */
    private static GameModel scripted(Difficulty level, int... faces) {
        return new GameModel(settings(level, 2), new Random(BOARD_SEED), new RandomDice(new ScriptedRandom(faces)),
                () -> 0L);
    }

    private static Piece firstPiece(GameModel model) {
        return model.currentPlayer().pieces().get(0);
    }

    @Test
    @DisplayName("FR-17: a Boost 1 gives a movement of 0 - a piece on the track cannot move and the turn passes")
    void zeroMovementMovesNothing() {
        GameModel model = scripted(Difficulty.MEDIUM, 5, 1);
        Player first = model.currentPlayer();
        firstPiece(model).setProgress(10);
        DiceRoll roll = model.roll();
        assertEquals(0, roll.movement());
        assertTrue(model.legalMoves().isEmpty(), "nothing moves with a movement of 0");
        assertEquals(10, first.pieces().get(0).progress());
        assertNotEquals(first, model.currentPlayer(), "FR-23: no legal move, the turn passes");
    }

    @Test
    @DisplayName("FR-17, FR-25: Medium Regular 4 x (Boost 3 - 1) moves 8 squares along the colour's own route")
    void boostMovesAlongOwnRoute() {
        GameModel model = scripted(Difficulty.MEDIUM, 4, 3);
        Piece piece = firstPiece(model);
        piece.setProgress(10);
        DiceRoll roll = model.roll();
        assertEquals(8, roll.movement());
        Move move = model.legalMoves().get(piece);
        assertEquals(18, move.targetProgress());
        model.move(piece);
        assertEquals(18, piece.progress());
        assertEquals(model.board().path(piece.color()).get(18), model.board().cellAt(piece.color(), 18));
    }

    @Test
    @DisplayName("FR-17, FR-21: Hard Regular 6 x (UltraBoost 6 - 1) moves 30 squares and still grants an extra roll")
    void ultraBoostSixKeepsTheExtraRoll() {
        GameModel model = scripted(Difficulty.HARD, 6, 6, 1, 2);
        Player first = model.currentPlayer();
        Piece piece = firstPiece(model);
        piece.setProgress(10);
        assertEquals(30, model.roll().movement());
        assertEquals(40, model.legalMoves().get(piece).targetProgress());
        model.move(piece);
        assertEquals(40, piece.progress());
        assertSame(first, model.currentPlayer(), "FR-21: a Regular 6 rolls again");
        assertTrue(model.hasBonusRoll(), "FR-21: the extra roll depends on the Regular 6, not on the movement");
        assertEquals(TurnPhase.AWAITING_ROLL, model.phase());
        assertEquals(new DiceRoll(1, DieType.ULTRA_BOOST, 2, 1, false, false), model.roll());
    }

    @Test
    @DisplayName("FR-19: a Regular 6 with a special 1 (movement 0) still brings a piece out; track pieces stay")
    void castleExitWithZeroMovement() {
        GameModel model = scripted(Difficulty.HARD, 6, 1);
        Player first = model.currentPlayer();
        Piece onTrack = first.pieces().get(0);
        onTrack.setProgress(10);
        model.roll();
        Map<Piece, Move> moves = model.legalMoves();
        assertFalse(moves.containsKey(onTrack), "movement 0: the piece on the track has no move");
        assertEquals(3, moves.size(), "the three castle pieces may leave");
        for (Map.Entry<Piece, Move> e : moves.entrySet()) {
            assertTrue(e.getKey().inCastle());
            assertEquals(0, e.getValue().targetProgress(), "FR-19: onto the start square");
        }
    }

    @Test
    @DisplayName("FR-18: on the home path the Regular die counts, not the FR-17 movement")
    void homePathUsesTheRegularDie() {
        GameModel model = scripted(Difficulty.MEDIUM, 2, 3);
        Piece piece = firstPiece(model);
        int homeStart = model.board().homeStartIndex();
        piece.setProgress(homeStart);
        assertEquals(4, model.roll().movement());
        assertEquals(homeStart + 2, model.legalMoves().get(piece).targetProgress(), "2 squares, not 2 x (3 - 1)");
    }

    @Test
    @DisplayName("FR-25: on every level and for every colour, a route changes ring only at its own arrows")
    void routesTurnOnlyAtOwnArrows() {
        for (Difficulty level : Difficulty.values()) {
            Board board = BoardFactory.create(level, new Random(BOARD_SEED));
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> path = board.path(color);
                String where = level.label() + ", " + color;
                int ownArrows = 0;
                int foreignArrows = 0;
                // The last cell is the finishing square in the centre, which has no Square; stop on the step before.
                for (int i = 0; i + 2 < path.size(); i++) {
                    Square here = board.squareAt(path.get(i));
                    Square next = board.squareAt(path.get(i + 1));
                    boolean arrow = here.type() == SquareType.ARROW;
                    boolean own = arrow && here.owner() == color;
                    ownArrows += own ? 1 : 0;
                    foreignArrows += arrow && !own ? 1 : 0;
                    if (next.ring() != here.ring()) {
                        assertTrue(own, where + ": the route leaves ring " + here.ring() + " at " + here.cell()
                                + ", which is not one of its own arrows");
                        assertEquals(here.ring() - 1, next.ring(), where + ": an arrow leads one ring inward");
                    } else {
                        assertFalse(own, where + ": its own arrow at " + here.cell() + " must turn it inward");
                    }
                }
                assertEquals(level.outerRings() + 1, ownArrows, where + ": one own arrow per ring");
                if (level != Difficulty.EASY) {
                    assertTrue(foreignArrows > 0, where + ": the route passes other colours' arrows without turning");
                }
            }
        }
    }

    @Test
    @DisplayName("FR-16..FR-19, FR-43..FR-45: complete 2-4 player Medium and Hard games with the real dice end correctly")
    void completeMediumAndHardGames() {
        int games = 0;
        for (Difficulty level : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            int zeroRolls = 0;
            for (int players = 2; players <= 4; players++) {
                for (long seed = 0; seed < 8; seed++) {
                    zeroRolls += playToEnd(level, players, seed);
                    games++;
                }
            }
            assertTrue(zeroRolls > 0, level.label() + ": rolls with a movement of 0 happened");
        }
        assertEquals(48, games);
    }

    /**
     * Plays one game to the end with random choices and checks every offered move against FR-17..FR-19.
     *
     * @return the number of rolls with a movement of 0
     */
    private static int playToEnd(Difficulty level, int players, long seed) {
        String where = level.label() + ", " + players + " players, seed " + seed;
        GameModel model = new GameModel(settings(level, players), new Random(seed));
        Random choice = new Random(seed * 31 + 7);
        int homeStart = model.board().homeStartIndex();
        int zeroRolls = 0;
        for (int rolls = 0; !model.isGameOver(); rolls++) {
            assertTrue(rolls < 200_000, "the game must end: " + where);
            DiceRoll roll = model.roll();
            assertEquals(level.specialDie(), roll.specialType(), where);
            zeroRolls += roll.movement() == 0 ? 1 : 0;
            Map<Piece, Move> moves = model.legalMoves();
            for (Map.Entry<Piece, Move> e : moves.entrySet()) {
                Piece p = e.getKey();
                int target = e.getValue().targetProgress();
                if (p.inCastle()) {
                    assertEquals(6, roll.regular(), "FR-19: " + where);
                    assertEquals(0, target, where);
                } else if (p.progress() >= homeStart) {
                    assertEquals(p.progress() + roll.regular(), target, "FR-18: " + where);
                } else {
                    assertTrue(roll.movement() > 0, "FR-17: movement 0 moves nothing, " + where);
                    int full = p.progress() + roll.movement();
                    if (target != full) {
                        // T2-06 (FR-29): a blocked piece stops just before an opponent Double Block, and only
                        // when no offered move uses the whole roll.
                        assertTrue(target > p.progress() && target < full, "FR-17: " + where);
                        assertTrue(opponentBlockAt(model, p, target + 1), "FR-29: stop before a block, " + where);
                        assertTrue(moves.entrySet().stream().noneMatch(m -> usesWholeRoll(m, roll, homeStart)),
                                "FR-29: a stop only without a full move, " + where);
                    }
                }
            }
            if (!moves.isEmpty()) {
                List<Piece> movable = new ArrayList<>(moves.keySet());
                model.move(movable.get(choice.nextInt(movable.size())));
            }
        }
        List<Player> ranking = model.ranking();
        assertEquals(players, ranking.size(), where);
        for (int i = 0; i < players; i++) {
            assertEquals(i + 1, ranking.get(i).rank(), where);
            assertEquals(i < players - 1, ranking.get(i).allFinished(), "FR-44: " + where);
        }
        return zeroRolls;
    }

    /** Whether a move uses the whole roll: a castle exit, the Regular die on the home path, else the movement. */
    private static boolean usesWholeRoll(Map.Entry<Piece, Move> m, DiceRoll roll, int homeStart) {
        Piece p = m.getKey();
        int target = m.getValue().targetProgress();
        if (p.inCastle()) {
            return true;
        }
        int steps = p.progress() >= homeStart ? roll.regular() : roll.movement();
        return target == p.progress() + steps;
    }

    /** Whether two or more pieces of one opponent of {@code mover} stand on the mover's path at {@code progress}. */
    private static boolean opponentBlockAt(GameModel model, Piece mover, int progress) {
        Cell cell = model.board().cellAt(mover.color(), progress);
        for (Player other : model.players()) {
            if (other.color() == mover.color()) {
                continue;
            }
            long here = other.pieces().stream()
                    .filter(q -> q.onBoard() && cell.equals(model.board().cellAt(q.color(), q.progress()))).count();
            if (here >= 2) {
                return true;
            }
        }
        return false;
    }
}
