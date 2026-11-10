package Model;

import java.util.Objects;

/**
 * A game piece. Its position is a progress index along the owner's path:
 * {@value #IN_CASTLE} = in the castle, 0 = start square, {@code finishIndex} = finished.
 * Task 04, owner: Rakan Ziyan.
 */
public final class Piece {

    /** Progress value of a piece in its castle (FR-13). */
    public static final int IN_CASTLE = -1;

    private final PlayerColor color;
    private final int index;
    private final int finishIndex;
    private int progress = IN_CASTLE;

    /**
     * @param index 0..3 within the owner's pieces
     * @param finishIndex progress index of the finish square (must be positive)
     * @throws IllegalArgumentException if the index or finish index is out of range
     * @throws NullPointerException if the colour is null
     */
    public Piece(PlayerColor color, int index, int finishIndex) {
        this.color = Objects.requireNonNull(color, "color");
        if (index < 0 || index >= Player.PIECES_PER_PLAYER) {
            throw new IllegalArgumentException("index must be 0.." + (Player.PIECES_PER_PLAYER - 1) + ": " + index);
        }
        if (finishIndex <= 0) {
            throw new IllegalArgumentException("finishIndex must be positive: " + finishIndex);
        }
        this.index = index;
        this.finishIndex = finishIndex;
    }

    public PlayerColor color() {
        return color;
    }

    /** 0..3 within the owner's four pieces (also its spot in the castle). */
    public int index() {
        return index;
    }

    public int progress() {
        return progress;
    }

    /** Progress index of the finish square. */
    public int finishIndex() {
        return finishIndex;
    }

    /**
     * Sets the position. Package-private: only model rule classes (e.g. the move engine) may move pieces,
     * so views and controllers cannot change the game state directly (NFR-08).
     *
     * @throws IllegalArgumentException if the value is not between {@value #IN_CASTLE} and the finish index,
     *         so that a wrong rule calculation fails at once instead of being hidden
     */
    void setProgress(int progress) {
        if (progress < IN_CASTLE || progress > finishIndex) {
            throw new IllegalArgumentException(
                    "progress must be " + IN_CASTLE + ".." + finishIndex + ": " + progress);
        }
        this.progress = progress;
    }

    public boolean inCastle() {
        return progress == IN_CASTLE;
    }

    public boolean finished() {
        return progress == finishIndex;
    }

    public boolean onBoard() {
        return progress >= 0 && progress < finishIndex;
    }

    @Override
    public String toString() {
        return color.label() + " piece " + (index + 1);
    }
}
