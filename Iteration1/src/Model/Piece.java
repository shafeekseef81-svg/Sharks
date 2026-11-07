package Model;

/**
 * A game piece. Its position is a progress index along the owner's path:
 * -1 = in the castle, 0 = start square, {@code finishIndex} = finished.
 */
public final class Piece {
    private final PlayerColor color;
    private final int index;
    private final int finishIndex;
    private int progress = -1;

    public Piece(PlayerColor color, int index, int finishIndex) {
        this.color = color;
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

    /**
     * Sets the position. Package-private: only model rule classes (e.g. the move engine) may move pieces,
     * so views and controllers cannot change the game state directly (NFR-08).
     */
    void setProgress(int progress) {
        this.progress = Math.max(-1, Math.min(progress, finishIndex));
    }

    public boolean inCastle() {
        return progress < 0;
    }

    public boolean finished() {
        return progress >= finishIndex;
    }

    public boolean onBoard() {
        return progress >= 0 && progress < finishIndex;
    }
}
