package components;

import java.util.function.DoubleConsumer;

import javax.swing.Timer;

/** Small timer-based animation helpers. Tests and the screenshot tool switch them to instant mode. */
public final class Animations {

    /** When true, animations jump to their end and delays run immediately. */
    public static volatile boolean instant = false;

    private Animations() {
    }

    /**
     * Calls {@code onFrame} with a progress from 0 to 1 on the Swing thread for {@code durationMs},
     * then runs {@code onDone}.
     */
    public static Timer animate(int durationMs, DoubleConsumer onFrame, Runnable onDone) {
        if (instant || durationMs <= 0) {
            onFrame.accept(1.0);
            if (onDone != null) {
                onDone.run();
            }
            return null;
        }
        long start = System.nanoTime();
        Timer timer = new Timer(15, null);
        timer.addActionListener(e -> {
            double p = Math.min(1.0, (System.nanoTime() - start) / 1e6 / durationMs);
            onFrame.accept(p);
            if (p >= 1.0) {
                timer.stop();
                if (onDone != null) {
                    onDone.run();
                }
            }
        });
        timer.start();
        return timer;
    }

    /** Runs {@code action} once after a delay (immediately in instant mode). */
    public static void later(int delayMs, Runnable action) {
        if (instant) {
            action.run();
            return;
        }
        Timer timer = new Timer(delayMs, e -> action.run());
        timer.setRepeats(false);
        timer.start();
    }

    public static double easeOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    public static double easeInOut(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }
}
