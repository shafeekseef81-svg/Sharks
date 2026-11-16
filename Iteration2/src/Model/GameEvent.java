package Model;

/**
 * A change in the game that views observe (Observer pattern, NFR-09). Shared contract, frozen at checkpoint CP1.
 *
 * <p>Owner: Rakan Ziyan.
 *
 * @param type    what happened
 * @param player  the player concerned (may be {@code null} for {@link Type#STATE_CHANGED})
 * @param roll    the roll, for {@link Type#ROLLED}; otherwise {@code null}
 * @param move    the move result, for {@link Type#MOVED}; otherwise {@code null}
 * @param message a text for the player, for example why a turn was passed (FR-23); may be {@code null}
 */
public record GameEvent(Type type, Player player, DiceRoll roll, MoveResult move, String message) {

    /** Kinds of game events. */
    public enum Type {
        /** Generic refresh: something in the model changed. */
        STATE_CHANGED,
        /** The active player rolled (FR-16). */
        ROLLED,
        /** A piece moved (FR-20). */
        MOVED,
        /** The turn passed without a move (FR-22, FR-23). */
        TURN_PASSED,
        /** A new player is active (FR-15). */
        TURN_CHANGED,
        /** A player finished all four pieces (FR-43). */
        PLAYER_FINISHED,
        /** The game is over (FR-44). */
        GAME_OVER
    }

    /**
     * @param player the player concerned, may be {@code null}
     * @return a generic refresh event
     */
    public static GameEvent stateChanged(Player player) {
        return new GameEvent(Type.STATE_CHANGED, player, null, null, null);
    }
}
