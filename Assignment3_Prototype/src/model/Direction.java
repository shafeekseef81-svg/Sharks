package model;

/** Arrow directions, in clockwise order. */
public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    /** Returns the direction rotated 90 degrees clockwise {@code times} times. */
    public Direction rotateClockwise(int times) {
        return values()[Math.floorMod(ordinal() + times, 4)];
    }
}
