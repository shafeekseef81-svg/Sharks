package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of {@link GameClock}: the elapsed game time and its HH:MM:SS format (Task 24; SRS FR-46, FR-47).
 *
 * <p>Every test drives a fake time source, so the results do not depend on how fast the computer is and no
 * test ever has to sleep. Time only moves when a test moves it.
 *
 * <p>Owner: Mohamed Zereny (Task 24).
 */
class GameClockTest {

    /** A time source a test can move by hand. */
    private static final class FakeTime implements LongSupplier {
        private long now;

        FakeTime(long start) {
            this.now = start;
        }

        void advance(long millis) {
            now += millis;
        }

        void set(long millis) {
            now = millis;
        }

        @Override
        public long getAsLong() {
            return now;
        }
    }

    private FakeTime time;
    private GameClock clock;

    @BeforeEach
    void setUp() {
        // A large start value, so a test cannot pass by accident just because the source begins at 0.
        time = new FakeTime(1_700_000_000_000L);
        clock = new GameClock(time);
    }

    // ------------------------------------------------------------------ initial state

    @Test
    @DisplayName("FR-46: a new clock reads zero and is not running")
    void initialState() {
        assertFalse(clock.running(), "a new clock does not measure yet");
        assertEquals(0L, clock.elapsedMillis(), "a game that has not started took no time");
        assertEquals("00:00:00", GameClock.format(clock.elapsedMillis()));
    }

    @Test
    @DisplayName("FR-46: time passing before the start does not count")
    void timeBeforeTheStartIsIgnored() {
        time.advance(90_000);
        assertEquals(0L, clock.elapsedMillis(), "the clock was not started yet");
        clock.start();
        assertEquals(0L, clock.elapsedMillis(), "the measurement begins at the start, not earlier");
    }

    // ------------------------------------------------------------------ start

    @Test
    @DisplayName("FR-46: a running clock reports the time that passed since the start")
    void runningClockFollowsTheTimeSource() {
        clock.start();
        assertTrue(clock.running());
        time.advance(1_500);
        assertEquals(1_500L, clock.elapsedMillis());
        time.advance(58_500);
        assertEquals(60_000L, clock.elapsedMillis(), "one minute of game time");
        assertEquals("00:01:00", GameClock.format(clock.elapsedMillis()));
    }

    @Test
    @DisplayName("FR-46: starting a clock that already runs does not restart the game time")
    void startIsIgnoredWhileRunning() {
        clock.start();
        time.advance(30_000);
        clock.start();
        clock.start();
        assertEquals(30_000L, clock.elapsedMillis(), "an extra start may not throw the game time away");
        time.advance(5_000);
        assertEquals(35_000L, clock.elapsedMillis(), "and the clock keeps running normally");
    }

    @Test
    @DisplayName("FR-46: after a stop, start begins a new measurement from zero")
    void startAfterStopMeasuresAgainFromZero() {
        clock.start();
        time.advance(42_000);
        clock.stop();
        assertEquals(42_000L, clock.elapsedMillis());

        time.advance(10_000);        // time between the two games does not count
        clock.start();
        assertTrue(clock.running());
        assertEquals(0L, clock.elapsedMillis(), "a new game starts at zero");
        time.advance(7_000);
        assertEquals(7_000L, clock.elapsedMillis());
    }

    // ------------------------------------------------------------------ stop

    @Test
    @DisplayName("FR-44, FR-46: stopping freezes the elapsed time")
    void stopFreezesTheElapsedTime() {
        clock.start();
        time.advance(125_000);
        clock.stop();

        assertFalse(clock.running());
        assertEquals(125_000L, clock.elapsedMillis());
        time.advance(600_000);       // the game is over; the clock must not move on
        assertEquals(125_000L, clock.elapsedMillis(), "a finished game keeps its duration");
    }

    @Test
    @DisplayName("FR-46: stopping twice keeps the first duration (stop is idempotent)")
    void stopIsIdempotent() {
        clock.start();
        time.advance(90_000);
        clock.stop();
        long afterFirstStop = clock.elapsedMillis();

        time.advance(45_000);
        clock.stop();
        clock.stop();

        assertEquals(afterFirstStop, clock.elapsedMillis(), "a second stop may not change the duration");
        assertEquals(90_000L, clock.elapsedMillis());
        assertFalse(clock.running());
    }

    @Test
    @DisplayName("FR-46: stopping a clock that never started is allowed and leaves it at zero")
    void stopBeforeStartDoesNothing() {
        clock.stop();
        assertFalse(clock.running());
        assertEquals(0L, clock.elapsedMillis());

        clock.start();               // and the clock still works afterwards
        time.advance(3_000);
        assertEquals(3_000L, clock.elapsedMillis());
    }

    // ------------------------------------------------------------------ a time source that misbehaves

    @Test
    @DisplayName("FR-46: the game time never goes backwards when the system clock jumps back")
    void aClockJumpingBackwardsNeverShowsLessTime() {
        clock.start();
        time.advance(120_000);
        assertEquals(120_000L, clock.elapsedMillis());

        time.advance(-30_000);       // the computer corrected its clock backwards
        assertEquals(120_000L, clock.elapsedMillis(), "the shown game time may not run backwards");

        time.advance(45_000);        // 15s further than the highest point so far
        assertEquals(135_000L, clock.elapsedMillis(), "and it goes on from the highest value");
    }

    @Test
    @DisplayName("FR-46: a backwards jump right after the start still reads zero, never a negative time")
    void aBackwardsJumpNeverGivesANegativeTime() {
        clock.start();
        time.advance(-5_000);
        assertEquals(0L, clock.elapsedMillis(), "a negative duration is impossible");
        clock.stop();
        assertEquals(0L, clock.elapsedMillis());
        assertEquals("00:00:00", GameClock.format(clock.elapsedMillis()));
    }

    // ------------------------------------------------------------------ format (FR-46)

    @Test
    @DisplayName("FR-46: a duration is shown as HH:MM:SS")
    void formatsAsHoursMinutesSeconds() {
        assertEquals("00:00:00", GameClock.format(0));
        assertEquals("00:00:00", GameClock.format(999), "part of a second is not a second yet");
        assertEquals("00:00:01", GameClock.format(1_000));
        assertEquals("00:00:59", GameClock.format(59_999));
        assertEquals("00:01:00", GameClock.format(60_000));
        assertEquals("00:59:59", GameClock.format(3_599_000));
        assertEquals("01:00:00", GameClock.format(3_600_000));
        assertEquals("02:03:04", GameClock.format(2 * 3_600_000 + 3 * 60_000 + 4_000));
    }

    @Test
    @DisplayName("FR-46: every field is padded to two digits")
    void everyFieldIsPadded() {
        String text = GameClock.format(5 * 60_000 + 7_000);
        assertEquals("00:05:07", text);
        assertEquals(8, text.length(), "HH:MM:SS is always eight characters");
        assertEquals(2, text.chars().filter(c -> c == ':').count());
    }

    @Test
    @DisplayName("FR-46: hours are not wrapped at 24, so a very long game still reads correctly")
    void hoursDoNotWrap() {
        assertEquals("24:00:00", GameClock.format(24L * 3_600_000));
        assertEquals("100:00:00", GameClock.format(100L * 3_600_000));
    }

    @Test
    @DisplayName("FR-46: a negative duration is shown as zero instead of a broken string")
    void negativeDurationsAreShownAsZero() {
        assertEquals("00:00:00", GameClock.format(-1));
        assertEquals("00:00:00", GameClock.format(-3_600_000));
    }

    @Test
    @DisplayName("NFR-24: the digits stay 0-9 whatever language the computer is set to")
    void theFormatDoesNotDependOnTheLanguageOfTheComputer() {
        Locale original = Locale.getDefault();
        try {
            // Locales whose default digits are not 0-9; the clock must still read 01:02:03.
            for (Locale locale : new Locale[] {new Locale("ar", "EG"), new Locale("hi", "IN"),
                    new Locale("he", "IL")}) {
                Locale.setDefault(locale);
                assertEquals("01:02:03", GameClock.format(3_600_000 + 2 * 60_000 + 3_000),
                        "wrong digits under " + locale);
            }
        } finally {
            Locale.setDefault(original);
        }
    }

    // ------------------------------------------------------------------ wiring

    @Test
    @DisplayName("NFR-11: a clock without a time source is refused at once")
    void theTimeSourceIsRequired() {
        assertThrows(NullPointerException.class, () -> new GameClock(null));
    }

    @Test
    @DisplayName("FR-46: a game exposes its clock, ready to be started by the controller")
    void theGameModelExposesAWorkingClock() {
        GameSettings settings = new GameSettings(Difficulty.EASY, java.util.List.of(
                new GameSettings.PlayerSetup("Red", PlayerColor.RED),
                new GameSettings.PlayerSetup("Green", PlayerColor.GREEN)));
        FakeTime gameTime = new FakeTime(0);
        GameModel model = new GameModel(settings, new java.util.Random(7), new FixedDice(6), gameTime);

        GameClock gameClock = model.clock();
        assertFalse(gameClock.running(), "a new game has not started its clock yet");
        gameClock.start();
        gameTime.advance(65_000);
        assertEquals("00:01:05", GameClock.format(gameClock.elapsedMillis()));
        gameClock.stop();
        gameTime.advance(10_000);
        assertEquals("00:01:05", GameClock.format(gameClock.elapsedMillis()));
    }

    @AfterEach
    void tearDown() {
        clock = null;
        time = null;
    }
}
