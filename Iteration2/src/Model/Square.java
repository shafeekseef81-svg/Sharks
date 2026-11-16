package Model;

/** One square of a shared track or of a home path. */
public final class Square {
    private final Cell cell;
    private final int ring;
    private SquareType type = SquareType.TRACK;
    private PlayerColor owner;
    private Direction arrowDirection;
    private Difficulty questionLevel;
    private boolean tinted;

    Square(Cell cell, int ring) {
        this.cell = cell;
        this.ring = ring;
    }

    public Cell cell() {
        return cell;
    }

    /** 0 = classic 52-square track, 1 = the 64-square ring, 2 = the 72-square ring, -1 = home path. */
    public int ring() {
        return ring;
    }

    public SquareType type() {
        return type;
    }

    /** Owning colour of a start square, arrow, home path square or tinted cell; otherwise null. */
    public PlayerColor owner() {
        return owner;
    }

    public Direction arrowDirection() {
        return arrowDirection;
    }

    /** Level of a question square (its colour on the board). */
    public Difficulty questionLevel() {
        return questionLevel;
    }

    /**
     * True for the classic start cell on the Medium/Hard boards: the official diagrams still draw it
     * in the player's colour, but it has no special function there.
     */
    public boolean isTinted() {
        return tinted;
    }

    public boolean isProtected() {
        return type.isProtected();
    }

    void setType(SquareType type) {
        this.type = type;
    }

    void setOwner(PlayerColor owner) {
        this.owner = owner;
    }

    void setArrowDirection(Direction arrowDirection) {
        this.arrowDirection = arrowDirection;
    }

    void setQuestionLevel(Difficulty questionLevel) {
        this.questionLevel = questionLevel;
    }

    void setTinted(boolean tinted) {
        this.tinted = tinted;
    }
}
