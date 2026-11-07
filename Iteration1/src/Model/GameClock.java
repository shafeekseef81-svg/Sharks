package Model;

import java.util.function.LongSupplier;

/**
 * Measures the elapsed game time and formats it as HH:MM:SS (FR-46).
 * The time source is injectable, so tests can use a fake clock.
 *
 * <p>Owner: Mohamed Zereny. <b>Phase A placeholder</b>: implemented in Task 24.
 */
public final class GameClock {

    private final LongSupplier timeSource;

    /** @param timeSource the current time in milliseconds, e.g. {@code System::currentTimeMillis} */
    public GameClock(LongSupplier timeSource) {
        this.timeSource = timeSource;
    }

    /** Starts measuring when the game begins. */
    public void start() {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 24 (Mohamed)");
    }

    /** Stops measuring when the game ends (FR-44). */
    public void stop() {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 24 (Mohamed)");
    }

    /** @return the elapsed game time in milliseconds */
    public long elapsedMillis() {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 24 (Mohamed)");
    }

    /**
     * @param millis a duration in milliseconds
     * @return the duration as HH:MM:SS (FR-46)
     */
    public static String format(long millis) {
        throw new UnsupportedOperationException("Not implemented yet - Phase B, Task 24 (Mohamed)");
    }

    LongSupplier timeSource() {
        return timeSource;
    }
}
