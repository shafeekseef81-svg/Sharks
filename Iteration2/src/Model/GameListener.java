package Model;

/**
 * Observer of the game model (Observer pattern, NFR-09). Views register through
 * {@link GameModel#addListener} and refresh when an event arrives. Shared contract, frozen at checkpoint CP1.
 *
 * <p>Owner: Rakan Ziyan.
 */
@FunctionalInterface
public interface GameListener {

    /** @param event what changed in the game */
    void onGameEvent(GameEvent event);
}
