package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A player with a unique name, a unique colour and four pieces (FR-06, FR-07, FR-13). */
public final class Player {
    private final String name;
    private final PlayerColor color;
    private final List<Piece> pieces = new ArrayList<>();
    private boolean skipNextTurn;
    private int rank;

    public Player(String name, PlayerColor color, int finishIndex) {
        this.name = name;
        this.color = color;
        for (int i = 0; i < 4; i++) {
            pieces.add(new Piece(color, i, finishIndex));
        }
    }

    public String name() {
        return name;
    }

    public PlayerColor color() {
        return color;
    }

    public List<Piece> pieces() {
        return Collections.unmodifiableList(pieces);
    }

    public int finishedCount() {
        return (int) pieces.stream().filter(Piece::finished).count();
    }

    public boolean allFinished() {
        return finishedCount() == pieces.size();
    }

    public boolean skipNextTurn() {
        return skipNextTurn;
    }

    public void setSkipNextTurn(boolean skipNextTurn) {
        this.skipNextTurn = skipNextTurn;
    }

    /** Final rank (1 = winner), or 0 while the player is still playing. */
    public int rank() {
        return rank;
    }

    void setRank(int rank) {
        this.rank = rank;
    }
}
