package model;

/** A board grid cell (row, column) with the origin at the top-left corner. */
public record Cell(int row, int col) {

    /** Returns this cell rotated 90 degrees clockwise {@code times} times on an n x n grid. */
    public Cell rotate(int times, int n) {
        int r = row;
        int c = col;
        for (int i = 0; i < Math.floorMod(times, 4); i++) {
            int nr = c;
            int nc = n - 1 - r;
            r = nr;
            c = nc;
        }
        return new Cell(r, c);
    }
}
