package Model;

/** The four player colours (SRS FR-07), listed in clockwise order around the board. */
public enum PlayerColor {
    RED("Red"),
    GREEN("Green"),
    YELLOW("Yellow"),
    BLUE("Blue");

    private final String label;

    PlayerColor(String label) {
        this.label = label;
    }

    /** Human-readable colour name. */
    public String label() {
        return label;
    }
}
