package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Factory for the three boards (SRS FR-10..FR-12, NFR-09 "Factory Method (boards)").
 *
 * <p>Geometry follows the official diagrams in the project description: Easy is the classic
 * 15x15 board; Medium adds one ring of 64 squares (17x17) and Hard two rings of 64 and 72
 * squares (19x19). On Medium/Hard the start squares are the outer corners next to each castle,
 * and the arrows of a colour form a "staircase" (e.g. red on Hard: (7,0), (8,1), (9,2)) that
 * turns the piece inward ring by ring after one full lap. Everything is defined once for the
 * colour in the top-left corner and rotated for the other three corners.
 */
public final class BoardFactory {

    private BoardFactory() {
    }

    /** Builds a board; start squares and arrows are fixed, special squares are placed at random. */
    public static Board create(Difficulty difficulty, Random random) {
        int n = difficulty.gridSize();
        int o = difficulty.outerRings();
        Map<Cell, Square> squares = new LinkedHashMap<>();
        for (int depth = 0; depth < o; depth++) {
            for (Cell c : ring(n, depth)) {
                squares.put(c, new Square(c, o - depth));
            }
        }
        for (Cell c : innerTrack(o)) {
            squares.put(c, new Square(c, 0));
        }

        Map<PlayerColor, Board.Corner> corners = cornersFor(difficulty);
        Map<PlayerColor, List<Cell>> paths = new EnumMap<>(PlayerColor.class);
        List<Cell> basePath = topLeftPath(n, o);
        for (PlayerColor color : PlayerColor.values()) {
            int k = corners.get(color).ordinal();
            List<Cell> path = new ArrayList<>(basePath.size());
            for (Cell c : basePath) {
                path.add(c.rotate(k, n));
            }
            paths.put(color, path);

            Square start = squares.get(path.get(0));
            start.setType(SquareType.START);
            start.setOwner(color);

            for (int depth = 0; depth <= o; depth++) {
                Square arrow = squares.get(new Cell(7 + depth, depth).rotate(k, n));
                arrow.setType(SquareType.ARROW);
                arrow.setOwner(color);
                arrow.setArrowDirection(Direction.EAST.rotateClockwise(k));
            }
            if (o > 0) {
                Square classicStart = squares.get(new Cell(o + 6, o + 1).rotate(k, n));
                classicStart.setTinted(true);
                classicStart.setOwner(color);
            }
            for (int i = 1; i <= 5; i++) {
                Cell h = new Cell(7 + o, o + i).rotate(k, n);
                Square home = new Square(h, -1);
                home.setType(SquareType.HOME_PATH);
                home.setOwner(color);
                squares.put(h, home);
            }
        }
        placeSpecialSquares(difficulty, squares, random);
        return new Board(difficulty, n, o, squares, paths, corners);
    }

    /** Colour per corner, as drawn in the official diagrams (Easy: green top-left; Medium/Hard: red top-left). */
    static Map<PlayerColor, Board.Corner> cornersFor(Difficulty difficulty) {
        Map<PlayerColor, Board.Corner> corners = new EnumMap<>(PlayerColor.class);
        PlayerColor[] clockwise = PlayerColor.values();
        int first = difficulty == Difficulty.EASY ? PlayerColor.GREEN.ordinal() : PlayerColor.RED.ordinal();
        for (int i = 0; i < 4; i++) {
            corners.put(clockwise[(first + i) % 4], Board.Corner.values()[i]);
        }
        return corners;
    }

    /** Path of the top-left colour: outer ring(s), the classic track, the home path and the finish. */
    static List<Cell> topLeftPath(int n, int o) {
        List<Cell> path = new ArrayList<>();
        for (int depth = 0; depth < o; depth++) {
            List<Cell> ring = ring(n, depth);
            Cell entry = depth == 0 ? new Cell(0, 0) : new Cell(6 + depth, depth);
            Cell arrow = new Cell(7 + depth, depth);
            int i = ring.indexOf(entry);
            while (true) {
                Cell c = ring.get(i % ring.size());
                path.add(c);
                if (c.equals(arrow)) {
                    break;
                }
                i++;
            }
        }
        List<Cell> inner = innerTrack(o);
        if (o > 0) {
            path.add(inner.get(51));
        }
        for (int i = 0; i <= 50; i++) {
            path.add(inner.get(i));
        }
        for (int i = 1; i <= 5; i++) {
            path.add(new Cell(7 + o, o + i));
        }
        path.add(new Cell(7 + o, o + 6));
        return path;
    }

    /** Ring at a depth (0 = outermost), clockwise from its top-left cell. */
    static List<Cell> ring(int n, int depth) {
        int a = depth;
        int b = n - 1 - depth;
        List<Cell> cells = new ArrayList<>();
        for (int c = a; c <= b; c++) {
            cells.add(new Cell(a, c));
        }
        for (int r = a + 1; r <= b; r++) {
            cells.add(new Cell(r, b));
        }
        for (int c = b - 1; c >= a; c--) {
            cells.add(new Cell(b, c));
        }
        for (int r = b - 1; r > a; r--) {
            cells.add(new Cell(r, a));
        }
        return cells;
    }

    /** The classic 52-square track, clockwise from the top-left colour's classic start (6,1). */
    static List<Cell> innerTrack(int o) {
        List<Cell> t = new ArrayList<>(52);
        line(t, 6, 1, 6, 5);
        line(t, 5, 6, 0, 6);
        line(t, 0, 7, 0, 7);
        line(t, 0, 8, 5, 8);
        line(t, 6, 9, 6, 14);
        line(t, 7, 14, 7, 14);
        line(t, 8, 14, 8, 9);
        line(t, 9, 8, 14, 8);
        line(t, 14, 7, 14, 7);
        line(t, 14, 6, 9, 6);
        line(t, 8, 5, 8, 0);
        line(t, 7, 0, 7, 0);
        line(t, 6, 0, 6, 0);
        List<Cell> shifted = new ArrayList<>(t.size());
        for (Cell c : t) {
            shifted.add(new Cell(c.row() + o, c.col() + o));
        }
        return shifted;
    }

    private static void line(List<Cell> out, int r0, int c0, int r1, int c1) {
        int dr = Integer.signum(r1 - r0);
        int dc = Integer.signum(c1 - c0);
        int r = r0;
        int c = c0;
        while (true) {
            out.add(new Cell(r, c));
            if (r == r1 && c == c1) {
                break;
            }
            r += dr;
            c += dc;
        }
    }

    /**
     * Random protection, question and surprise squares on plain shared-track squares (FR-12):
     * never overlapping, never on start squares, arrows or home paths, and not adjacent to each
     * other when that is possible.
     */
    private static void placeSpecialSquares(Difficulty d, Map<Cell, Square> squares, Random random) {
        List<Square> candidates = new ArrayList<>();
        for (Square s : squares.values()) {
            if (s.type() == SquareType.TRACK && !s.isTinted()) {
                candidates.add(s);
            }
        }
        Collections.shuffle(candidates, random);

        List<Object> wanted = new ArrayList<>();
        for (int i = 0; i < d.protectionSquares(); i++) {
            wanted.add(SquareType.PROTECTION);
        }
        for (int i = 0; i < d.questionSquares(); i++) {
            wanted.add(Difficulty.values()[i % 3]);
        }
        for (int i = 0; i < d.surpriseSquares(); i++) {
            wanted.add(SquareType.SURPRISE);
        }
        Collections.shuffle(wanted, random);

        List<Square> chosen = new ArrayList<>();
        for (Square s : candidates) {
            if (chosen.size() == wanted.size()) {
                break;
            }
            boolean spaced = chosen.stream().noneMatch(c -> Math.abs(c.cell().row() - s.cell().row()) <= 1
                    && Math.abs(c.cell().col() - s.cell().col()) <= 1);
            if (spaced) {
                chosen.add(s);
            }
        }
        for (Square s : candidates) {
            if (chosen.size() == wanted.size()) {
                break;
            }
            if (!chosen.contains(s)) {
                chosen.add(s);
            }
        }
        for (int i = 0; i < chosen.size(); i++) {
            Square s = chosen.get(i);
            Object kind = wanted.get(i);
            if (kind instanceof Difficulty level) {
                s.setType(SquareType.QUESTION);
                s.setQuestionLevel(level);
            } else {
                s.setType((SquareType) kind);
            }
        }
    }
}
