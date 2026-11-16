package Model;

/** Dice used by the game (SRS FR-16). */
public enum DieType {
    NONE("None", 0),
    REGULAR("Regular", 6),
    BOOST("Boost", 3),
    ULTRA_BOOST("UltraBoost", 6);

    private final String label;
    private final int faces;

    DieType(String label, int faces) {
        this.label = label;
        this.faces = faces;
    }

    /** Display name of the die. */
    public String label() {
        return label;
    }

    /** Number of faces (values 1..faces). */
    public int faces() {
        return faces;
    }
}
