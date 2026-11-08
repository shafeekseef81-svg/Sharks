package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A player with a name, a colour and four pieces that start in the castle (FR-06, FR-07, FR-09, FR-13).
 * The name and colour cannot change once the player exists. Task 04, owner: Rakan Ziyan.
 */
public final class Player {

    /** Pieces per player (FR-13). */
    public static final int PIECES_PER_PLAYER = 4;

    private final String name;
    private final PlayerColor color;
    private final List<Piece> pieces;
    private boolean skipNextTurn;
    private int rank;

    /**
     * @param finishIndex progress index of the finish square on this player's path (must be positive)
     * @throws IllegalArgumentException if the name is blank or the finish index is not positive
     * @throws NullPointerException if the name or colour is null
     */
    public Player(String name, PlayerColor color, int finishIndex) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A player needs a name.");
        }
        if (finishIndex <= 0) {
            throw new IllegalArgumentException("finishIndex must be positive: " + finishIndex);
        }
        this.name = name;
        this.color = color;
        List<Piece> list = new ArrayList<>();
        for (int i = 0; i < PIECES_PER_PLAYER; i++) {
            list.add(new Piece(color, i, finishIndex));
        }
        this.pieces = Collections.unmodifiableList(list);
    }

    public String name() {
        return name;
    }

    public PlayerColor color() {
        return color;
    }

    /** The four pieces, in castle order. The list cannot be modified. */
    public List<Piece> pieces() {
        return pieces;
    }

    public int finishedCount() {
        return (int) pieces.stream().filter(Piece::finished).count();
    }

    public boolean allFinished() {
        return finishedCount() == pieces.size();
    }

    /** Always false in Iteration 1; set only by the surprise rules of Iteration 2. */
    public boolean skipNextTurn() {
        return skipNextTurn;
    }

    /** Package-private: only model rule classes may change it (NFR-08). */
    void setSkipNextTurn(boolean skipNextTurn) {
        this.skipNextTurn = skipNextTurn;
    }

    /** Final rank (1 = winner), or 0 while the player is still playing. */
    public int rank() {
        return rank;
    }

    /**
     * Package-private: set by the ranking rules (Task 23).
     *
     * @throws IllegalArgumentException if the rank is below 1
     */
    void setRank(int rank) {
        if (rank < 1) {
            throw new IllegalArgumentException("rank must be 1 or more: " + rank);
        }
        this.rank = rank;
    }

    @Override
    public String toString() {
        return name + " (" + color.label() + ")";
    }
}
