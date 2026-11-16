package Model;

import java.util.Locale;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Measures the elapsed game time and formats it as HH:MM:SS (FR-46).
 * The time source is injectable, so tests can use a fake clock.
 *
 * <p>Owner: Mohamed Zereny (Task 24).
 *
 * <h2>Behaviour</h2>
 * <ul>
 *   <li>A new clock reads {@code 0} until {@link #start()} is called.</li>
 *   <li>{@link #start()} begins a measurement. Calling it again while the clock runs does nothing, so an
 *       extra call cannot restart the time of a game that is already being played. After {@link #stop()}
 *       it begins a <b>new</b> measurement from zero.</li>
 *   <li>{@link #stop()} freezes the elapsed time; from then on {@link #elapsedMillis()} keeps returning the
 *       same value. Stopping twice, or stopping a clock that never ran, does nothing.</li>
 *   <li>The reading never becomes negative and never goes backwards, even if the time source does. The
 *       default source of {@link GameModel} is {@code System::currentTimeMillis}, which is the wall clock
 *       and can jump backwards when the computer synchronises its time; a jump like that would otherwise
 *       show the game time running backwards in front of the players.</li>
 * </ul>
 *
 * <p>The class holds no GUI code (NFR-08): it only measures and formats. Showing the time and refreshing
 * it belongs to the View and the Controller.
 *
 * <p>Not thread-safe: the game calls it from the Swing event thread only.
 */
public final class GameClock {

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 3600L;

    private final LongSupplier timeSource;

    /** Reading of the time source when the current measurement started. */
    private long startedAt;
    /** Elapsed time of the finished measurement, returned once the clock has been stopped. */
    private long frozenMillis;
    /** Largest elapsed time seen in this measurement; keeps the reading from going backwards. */
    private long highWaterMillis;
    private boolean running;

    /**
     * @param timeSource the current time in milliseconds, e.g. {@code System::currentTimeMillis}
     * @throws NullPointerException if {@code timeSource} is null
     */
    public GameClock(LongSupplier timeSource) {
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }

    /**
     * Starts measuring when the game begins. Does nothing while the clock is already running; after
     * {@link #stop()} it starts a new measurement from zero.
     */
    public void start() {
        if (running) {
            return;
        }
        startedAt = timeSource.getAsLong();
        frozenMillis = 0;
        highWaterMillis = 0;
        running = true;
    }

    /**
     * Stops measuring when the game ends (FR-44). The elapsed time stays readable afterwards. Calling it
     * on a clock that is not running does nothing, so it is safe to stop a game twice or to stop a game
     * that never started.
     */
    public void stop() {
        if (!running) {
            return;
        }
        frozenMillis = elapsedMillis();
        running = false;
    }

    /** @return {@code true} while the clock is measuring */
    public boolean running() {
        return running;
    }

    /**
     * @return the elapsed game time in milliseconds: {@code 0} before the first {@link #start()}, the time
     *         running while the clock runs, and the time of the finished game after {@link #stop()}. The
     *         value is never negative and never smaller than a value returned earlier in the same
     *         measurement.
     */
    public long elapsedMillis() {
        if (!running) {
            return frozenMillis;
        }
        long sinceStart = timeSource.getAsLong() - startedAt;
        if (sinceStart > highWaterMillis) {
            highWaterMillis = sinceStart;
        }
        return highWaterMillis;
    }

    /**
     * Formats a duration for the board and the results screen (FR-46, FR-47). Hours are not wrapped at 24,
     * so a very long game still reads correctly. A negative duration is shown as {@code 00:00:00}.
     *
     * @param millis a duration in milliseconds
     * @return the duration as HH:MM:SS (FR-46)
     */
    public static String format(long millis) {
        long totalSeconds = Math.max(0L, millis) / MILLIS_PER_SECOND;
        long hours = totalSeconds / SECONDS_PER_HOUR;
        long minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE;
        long seconds = totalSeconds % SECONDS_PER_MINUTE;
        // Locale.ROOT: the digits must stay 0-9 whatever language the computer is set to (NFR-24).
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    LongSupplier timeSource() {
        return timeSource;
    }
}
