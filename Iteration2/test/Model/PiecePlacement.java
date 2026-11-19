package Model;

/**
 * Test helper: puts a piece on a given progress value from outside the {@code Model} package.
 *
 * <p>{@link Piece#setProgress(int)} is package-private on purpose, so that only the model rule classes may
 * move a piece (NFR-08). Tests of the {@code View} and {@code Controller} packages sometimes need a board in a
 * particular position without playing a whole game to reach it, and this helper is the one door for that. It
 * lives in the test tree only and is never packaged into the JAR.
 *
 * <p>Test infrastructure for T2-11; owner: Mohamed Zereny. It follows the same pattern as {@link FixedDice}.
 */
public final class PiecePlacement {

    private PiecePlacement() {
    }

    /**
     * @param piece the piece to place
     * @param progress the progress value: {@link Piece#IN_CASTLE}, 0 for the start square, up to the finish
     * @return the same piece, so it can be used inline
     * @throws IllegalArgumentException if the value is outside the piece's path
     * @throws NullPointerException if {@code piece} is null
     */
    public static Piece place(Piece piece, int progress) {
        piece.setProgress(progress);
        return piece;
    }
}
