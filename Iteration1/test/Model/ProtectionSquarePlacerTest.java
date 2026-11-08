package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests for Task 08: random protection squares and the 100-board test (FR-11, FR-12 P, FR-13).
 *
 * <p>The tests build a fresh Easy layout with no special squares: the start squares, arrows and home paths are
 * copied from a board made by {@link BoardFactory}, and everything else is a plain track square. This keeps the
 * tests independent of how {@code BoardFactory} places special squares today (Task 07).
 *
 * <p>Owner: Rakan Ziyan.
 */
class ProtectionSquarePlacerTest {

    private static final int EASY_TRACK = 52;
    private static final int EASY_FREE = EASY_TRACK - 4 - 4; // minus 4 start squares and 4 arrows

    /** The Easy layout without special squares: shared track, start squares, arrows, home paths. */
    private static List<Square> easyLayout() {
        Board board = BoardFactory.create(Difficulty.EASY, new Random(0));
        List<Square> out = new ArrayList<>();
        for (Square original : board.squares()) {
            Square s = new Square(original.cell(), original.ring());
            SquareType t = original.type();
            boolean fixed = t == SquareType.START || t == SquareType.ARROW || t == SquareType.HOME_PATH;
            s.setType(fixed ? t : SquareType.TRACK);
            s.setOwner(original.owner());
            s.setTinted(original.isTinted());
            out.add(s);
        }
        return out;
    }

    private static long count(List<Square> squares, SquareType type) {
        return squares.stream().filter(s -> s.type() == type).count();
    }

    @Test
    @DisplayName("Test layout: Easy has 52 track squares, 4 start squares, 4 arrows and 20 home path squares")
    void layoutMatchesTable1() {
        List<Square> layout = easyLayout();
        assertEquals(EASY_TRACK, layout.stream().filter(s -> s.ring() >= 0).count());
        assertEquals(4, count(layout, SquareType.START));
        assertEquals(4, count(layout, SquareType.ARROW));
        assertEquals(20, count(layout, SquareType.HOME_PATH));
        assertEquals(0, count(layout, SquareType.PROTECTION));
        assertEquals(EASY_FREE, layout.stream().filter(ProtectionSquarePlacer::isFree).count());
    }

    @Test
    @DisplayName("FR-11/FR-12: 100 Easy boards each get exactly 4 protection squares on free shared-track squares")
    void hundredBoards() {
        Set<Set<Cell>> layouts = new HashSet<>();
        Set<Cell> everChosen = new HashSet<>();
        Set<Cell> freeCells = easyLayout().stream().filter(ProtectionSquarePlacer::isFree)
                .map(Square::cell).collect(Collectors.toSet());

        for (int game = 0; game < 100; game++) {
            List<Square> layout = easyLayout();
            List<Square> chosen = ProtectionSquarePlacer.place(Difficulty.EASY, layout, new Random(1000 + game));

            assertEquals(4, chosen.size(), "game " + game);
            assertEquals(4, count(layout, SquareType.PROTECTION), "game " + game);
            assertEquals(4, count(layout, SquareType.START), "start squares untouched");
            assertEquals(4, count(layout, SquareType.ARROW), "arrows untouched");
            assertEquals(20, count(layout, SquareType.HOME_PATH), "home paths untouched (FR-33)");

            Set<Cell> cells = chosen.stream().map(Square::cell).collect(Collectors.toSet());
            assertEquals(4, cells.size(), "no two protection squares on one cell (FR-12)");
            for (Square s : chosen) {
                assertTrue(freeCells.contains(s.cell()), "on a free shared-track square: " + s.cell());
                assertTrue(s.ring() >= 0, "not on a home path");
                assertTrue(s.isProtected(), "protection squares prevent capture (FR-27)");
            }
            layouts.add(cells);
            everChosen.addAll(cells);
        }
        assertTrue(layouts.size() >= 95, "a new random set in (almost) every game, got " + layouts.size());
        assertEquals(freeCells, everChosen, "every free track square can be chosen");
    }

    @Test
    @DisplayName("FR-12: the same seed gives the same board, a different seed a different one")
    void repeatableWithSeed() {
        Set<Cell> a = cells(ProtectionSquarePlacer.place(4, easyLayout(), new Random(42)));
        Set<Cell> b = cells(ProtectionSquarePlacer.place(4, easyLayout(), new Random(42)));
        Set<Cell> c = cells(ProtectionSquarePlacer.place(4, easyLayout(), new Random(43)));
        assertEquals(a, b);
        assertFalse(a.equals(c));
    }

    @Test
    @DisplayName("FR-12: a second placement never overlaps the first one")
    void noOverlapWithExistingSpecialSquares() {
        List<Square> layout = easyLayout();
        Set<Cell> first = cells(ProtectionSquarePlacer.place(4, layout, new Random(1)));
        Set<Cell> second = cells(ProtectionSquarePlacer.place(EASY_FREE - 4, layout, new Random(2)));
        assertTrue(first.stream().noneMatch(second::contains));
        assertEquals(EASY_FREE, count(layout, SquareType.PROTECTION));
    }

    @Test
    @DisplayName("Boundaries: 0 squares changes nothing; all free squares is allowed; one more is refused")
    void countBoundaries() {
        List<Square> layout = easyLayout();
        assertTrue(ProtectionSquarePlacer.place(0, layout, new Random(1)).isEmpty());
        assertEquals(0, count(layout, SquareType.PROTECTION));

        assertEquals(EASY_FREE, ProtectionSquarePlacer.place(EASY_FREE, easyLayout(), new Random(1)).size());

        List<Square> tooMany = easyLayout();
        assertThrows(IllegalArgumentException.class,
                () -> ProtectionSquarePlacer.place(EASY_FREE + 1, tooMany, new Random(1)));
        assertEquals(0, count(tooMany, SquareType.PROTECTION), "a refused placement changes no square");
    }

    @Test
    @DisplayName("Invalid configurations: negative count, null arguments and null squares are rejected")
    void invalidConfigurations() {
        assertThrows(IllegalArgumentException.class,
                () -> ProtectionSquarePlacer.place(-1, easyLayout(), new Random(1)));
        assertThrows(NullPointerException.class, () -> ProtectionSquarePlacer.place(4, null, new Random(1)));
        assertThrows(NullPointerException.class, () -> ProtectionSquarePlacer.place(4, easyLayout(), null));
        assertThrows(NullPointerException.class,
                () -> ProtectionSquarePlacer.place((Difficulty) null, easyLayout(), new Random(1)));
        List<Square> withNull = new ArrayList<>(easyLayout());
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> ProtectionSquarePlacer.place(4, withNull, new Random(1)));
        assertThrows(IllegalArgumentException.class,
                () -> ProtectionSquarePlacer.place(1, List.of(), new Random(1)), "no free squares");
    }

    @Test
    @DisplayName("Table 1: the level sets the number (Easy 4, Medium 8, Hard 12)")
    void countComesFromLevel() {
        assertEquals(4, Difficulty.EASY.protectionSquares());
        assertEquals(8, Difficulty.MEDIUM.protectionSquares());
        assertEquals(12, Difficulty.HARD.protectionSquares());
        assertEquals(4, ProtectionSquarePlacer.place(Difficulty.EASY, easyLayout(), new Random(7)).size());
    }

    @Test
    @DisplayName("FR-13: in 100 new games every player starts with four pieces in the castle")
    void hundredGamesStartInCastle() {
        List<GameSettings.PlayerSetup> setups = Arrays.asList(
                new GameSettings.PlayerSetup("A", PlayerColor.RED),
                new GameSettings.PlayerSetup("B", PlayerColor.GREEN),
                new GameSettings.PlayerSetup("C", PlayerColor.YELLOW),
                new GameSettings.PlayerSetup("D", PlayerColor.BLUE));
        GameSettings settings = new GameSettings(Difficulty.EASY, setups);
        for (int game = 0; game < 100; game++) {
            GameModel m = new GameModel(settings, new Random(game), new FixedDice(1), () -> 0L);
            for (Player p : m.players()) {
                assertEquals(4, p.pieces().size());
                assertTrue(p.pieces().stream().allMatch(Piece::inCastle), "game " + game + ", " + p.name());
            }
        }
    }

    private static Set<Cell> cells(List<Square> squares) {
        return squares.stream().map(Square::cell).collect(Collectors.toSet());
    }
}
