package Model;

/** Square types shown on the board (SRS FR-11, FR-14). */
public enum SquareType {
    TRACK, START, ARROW, PROTECTION, QUESTION, SURPRISE, HOME_PATH;

    /** Protected squares prevent capturing (FR-27); question squares are not protected. */
    public boolean isProtected() {
        return this == START || this == PROTECTION || this == SURPRISE;
    }
}
