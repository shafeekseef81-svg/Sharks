package model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A generated board: grid size, track/home squares and the movement path of every colour.
 * Built by {@link BoardFactory}.
 */
public final class Board {

    /** Board corners in clockwise order; each colour owns the castle in one corner. */
    public enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

    private final Difficulty difficulty;
    private final int size;
    private final int offset;
    private final Map<Cell, Square> squares;
    private final Map<PlayerColor, List<Cell>> paths;
    private final Map<PlayerColor, Corner> corners;

    Board(Difficulty difficulty, int size, int offset, Map<Cell, Square> squares,
          Map<PlayerColor, List<Cell>> paths, Map<PlayerColor, Corner> corners) {
        this.difficulty = difficulty;
        this.size = size;
        this.offset = offset;
        this.squares = squares;
        this.paths = paths;
        this.corners = corners;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    /** Cells per side (15, 17 or 19). */
    public int size() {
        return size;
    }

    /** Number of outer rings around the classic board (0, 1 or 2). */
    public int offset() {
        return offset;
    }

    public Collection<Square> squares() {
        return Collections.unmodifiableCollection(squares.values());
    }

    /** The square at a cell, or null for castles, the centre and the finish. */
    public Square squareAt(Cell cell) {
        return squares.get(cell);
    }

    /** Full path of a colour: start square ... home path ... finish (last element). */
    public List<Cell> path(PlayerColor color) {
        return Collections.unmodifiableList(paths.get(color));
    }

    /** Progress value of the finishing square (same for every colour). */
    public int finishIndex() {
        return paths.get(PlayerColor.RED).size() - 1;
    }

    /** First progress value on the home path (5 coloured squares before the finish). */
    public int homeStartIndex() {
        return finishIndex() - 5;
    }

    /** Cell for a progress value, or null while the piece is in its castle. */
    public Cell cellAt(PlayerColor color, int progress) {
        return progress < 0 ? null : paths.get(color).get(Math.min(progress, finishIndex()));
    }

    public Cell startCell(PlayerColor color) {
        return paths.get(color).get(0);
    }

    public Corner corner(PlayerColor color) {
        return corners.get(color);
    }

    public PlayerColor colorAt(Corner corner) {
        for (Map.Entry<PlayerColor, Corner> e : corners.entrySet()) {
            if (e.getValue() == corner) {
                return e.getKey();
            }
        }
        throw new IllegalStateException("No colour at " + corner);
    }

    public long count(SquareType type) {
        return squares.values().stream().filter(s -> s.type() == type).count();
    }

    public long countQuestions(Difficulty level) {
        return squares.values().stream()
                .filter(s -> s.type() == SquareType.QUESTION && s.questionLevel() == level).count();
    }

    /** Number of shared-track squares (all rings plus the classic track). */
    public int trackSquareCount() {
        return (int) squares.values().stream().filter(s -> s.type() != SquareType.HOME_PATH).count();
    }
}
