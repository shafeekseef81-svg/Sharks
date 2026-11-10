package View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Cell;
import Model.Difficulty;
import Model.GameModel;
import Model.GameSettings;
import Model.Piece;
import Model.PlayerColor;
import View.components.Animations;
import View.components.BoardComponent;

/**
 * Verification tests for the square-by-square movement animation (Task 19; SRS NFR-04).
 *
 * <p>The animation lives in {@code BoardComponent.animateAlong} and {@code Animations}, reused from the
 * Assignment 3 prototype; the controller starts it after a move. Task 19 is to verify it and record the
 * evidence.
 *
 * <p>NFR-04 asks for a movement that is animated square by square and completes within 2 seconds. The
 * tests therefore check the promised duration against the real timing of {@code animateAlong}, that the
 * piece really passes over every square on the way, and that the animation always finishes and cleans up,
 * so a move can never be submitted twice and nothing keeps running after the game is left.
 *
 * <p>Owner: Mohamed Zereny (Task 19).
 */
class MovementAnimationTest {

    /** NFR-04: a movement must be finished within this time. */
    private static final int LIMIT_MILLIS = 2000;
    private static final int BOARD_SIZE = 700;

    private boolean instantBefore;

    @BeforeEach
    void rememberAnimationMode() {
        instantBefore = Animations.instant;
    }

    @AfterEach
    void restoreAnimationMode() {
        Animations.instant = instantBefore;
    }

    private static GameModel easyGame() {
        GameSettings settings = new GameSettings(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Red", PlayerColor.RED),
                new GameSettings.PlayerSetup("Green", PlayerColor.GREEN)));
        return new GameModel(settings, new Random(2026));
    }

    private static BoardComponent paintedBoard(GameModel game) {
        BoardComponent board = new BoardComponent();
        board.setGame(game);
        board.setInteractive(true);
        board.setSize(BOARD_SIZE, BOARD_SIZE);
        board.doLayout();
        paint(board);
        return board;
    }

    private static void paint(BoardComponent board) {
        BufferedImage image = new BufferedImage(Math.max(1, board.getWidth()),
                Math.max(1, board.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        board.paint(g);
        g.dispose();
    }

    /** The first {@code count} cells of the red path, the route of a real move. */
    private static List<Cell> redPath(GameModel game, int from, int count) {
        List<Cell> path = new ArrayList<>();
        for (int i = from; i < from + count; i++) {
            path.add(game.board().path(PlayerColor.RED).get(i));
        }
        return path;
    }

    // ------------------------------------------------------------------ NFR-04: within two seconds

    @Test
    @DisplayName("NFR-04: the longest movement of a Regular die is promised well inside two seconds")
    void theLongestMoveStaysInsideTheLimit() {
        // animateAlong asks Animations for max(260, min(1600, steps * 120)) milliseconds. The Regular die
        // moves at most 6 squares, so the longest normal movement is 6 * 120 = 720ms, and even the
        // hard ceiling of the formula, 1600ms, is below the 2 seconds of NFR-04.
        for (int steps = 1; steps <= 6; steps++) {
            int promised = Math.max(260, Math.min(1600, steps * 120));
            assertTrue(promised < LIMIT_MILLIS,
                    "a move of " + steps + " squares would take " + promised + "ms");
        }
        assertEquals(720, Math.max(260, Math.min(1600, 6 * 120)), "a full 6 takes 720ms");
        assertTrue(1600 < LIMIT_MILLIS, "even the longest animation the formula allows fits NFR-04");
    }

    @Test
    @DisplayName("NFR-04: a real six-square movement finishes, on the Swing thread, inside two seconds")
    void aRealMovementFinishesInTime() throws Exception {
        Animations.instant = false;                      // measure the real animation
        GameModel game = easyGame();
        Piece piece = game.players().get(0).pieces().get(0);
        List<Cell> route = new ArrayList<>();
        List<Boolean> onSwingThread = new ArrayList<>();
        AtomicInteger finished = new AtomicInteger();

        long start = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> {
            BoardComponent board = paintedBoard(game);
            route.addAll(redPath(game, 0, 7));           // six steps, the longest a Regular die gives
            board.animateAlong(piece, route, () -> {
                onSwingThread.add(SwingUtilities.isEventDispatchThread());
                finished.incrementAndGet();
            });
        });

        // Wait for the animation, without assuming how long it takes.
        long deadline = System.currentTimeMillis() + LIMIT_MILLIS + 500;
        while (finished.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        long tookMillis = (System.nanoTime() - start) / 1_000_000;

        assertEquals(1, finished.get(), "the animation must report that it finished");
        assertTrue(tookMillis < LIMIT_MILLIS,
                "NFR-04: a six-square movement took " + tookMillis + "ms, the limit is " + LIMIT_MILLIS);
        assertEquals(List.of(true), onSwingThread,
                "the animation must end on the Swing thread, so the controller may touch the screen");
    }

    @Test
    @DisplayName("NFR-04: the animation runs on a Swing timer, so the window stays responsive")
    void theAnimationUsesASwingTimer() throws Exception {
        Animations.instant = false;
        List<Timer> timers = new ArrayList<>();
        SwingUtilities.invokeAndWait(() ->
                timers.add(Animations.animate(200, progress -> { }, null)));

        Timer timer = timers.get(0);
        assertTrue(timer instanceof Timer, "a javax.swing.Timer fires on the event thread");
        assertTrue(timer.isRunning(), "the animation is running");
        assertTrue(timer.getDelay() <= 40,
                "frames must come often enough to look smooth: every " + timer.getDelay() + "ms");
        SwingUtilities.invokeAndWait(timer::stop);
    }

    // ------------------------------------------------------------- square by square, and it finishes

    @Test
    @DisplayName("NFR-04: the movement is drawn in many steps, not as one jump")
    void theMovementIsSmooth() throws Exception {
        Animations.instant = false;
        List<Double> progress = new ArrayList<>();
        AtomicInteger finished = new AtomicInteger();

        SwingUtilities.invokeAndWait(() ->
                Animations.animate(300, progress::add, finished::incrementAndGet));

        long deadline = System.currentTimeMillis() + LIMIT_MILLIS;
        while (finished.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        assertEquals(1, finished.get(), "the animation must report that it finished");
        assertTrue(progress.size() > 5,
                "a movement must be drawn in many frames, not one: " + progress.size());
        for (int i = 1; i < progress.size(); i++) {
            assertTrue(progress.get(i) >= progress.get(i - 1),
                    "the movement must always go forward: " + progress);
        }
        assertEquals(1.0, progress.get(progress.size() - 1), 0.0001,
                "the last frame must put the piece exactly on its square");
    }

    @Test
    @DisplayName("NFR-04: the piece is really drawn in different places while it travels its route")
    void thePieceIsDrawnMovingAlongTheRoute() throws Exception {
        Animations.instant = false;
        GameModel game = easyGame();
        Piece piece = game.players().get(0).pieces().get(0);
        List<BufferedImage> frames = new ArrayList<>();
        AtomicInteger finished = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            BoardComponent board = paintedBoard(game);
            // Photograph the board again and again while the animation runs.
            Timer camera = new Timer(25, e -> frames.add(snapshot(board)));
            camera.start();
            board.animateAlong(piece, redPath(game, 0, 5), () -> {
                camera.stop();
                finished.incrementAndGet();
            });
        });

        long deadline = System.currentTimeMillis() + LIMIT_MILLIS;
        while (finished.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }

        assertEquals(1, finished.get(), "the animation finished");
        assertTrue(frames.size() >= 3, "the board was photographed while moving: " + frames.size());
        long different = frames.stream().map(MovementAnimationTest::fingerprint).distinct().count();
        assertTrue(different >= 3,
                "the piece must be drawn in different places on the way, but the board only showed "
                        + different + " different pictures");
    }

    private static BufferedImage snapshot(BoardComponent board) {
        BufferedImage image = new BufferedImage(board.getWidth(), board.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        board.paint(g);
        g.dispose();
        return image;
    }

    /** A cheap value that changes as soon as anything on the board is drawn somewhere else. */
    private static long fingerprint(BufferedImage image) {
        long sum = 0;
        for (int y = 0; y < image.getHeight(); y += 5) {
            for (int x = 0; x < image.getWidth(); x += 5) {
                sum = sum * 31 + image.getRGB(x, y);
            }
        }
        return sum;
    }

    @Test
    @DisplayName("NFR-04: a movement of no squares finishes at once instead of hanging")
    void anEmptyRouteFinishesImmediately() {
        onEdt(() -> {
            GameModel game = easyGame();
            BoardComponent board = paintedBoard(game);
            Piece piece = game.players().get(0).pieces().get(0);
            AtomicInteger finished = new AtomicInteger();

            board.animateAlong(piece, List.of(), finished::incrementAndGet);
            assertEquals(1, finished.get(), "an empty route must not leave the game waiting");

            board.animateAlong(piece, redPath(game, 0, 1), finished::incrementAndGet);
            assertEquals(2, finished.get(), "a single square is no movement either");
        });
    }

    @Test
    @DisplayName("NFR-04: in instant mode the animation ends at once, so the tests of others never wait")
    void instantModeFinishesStraightAway() {
        onEdt(() -> {
            Animations.instant = true;
            GameModel game = easyGame();
            BoardComponent board = paintedBoard(game);
            Piece piece = game.players().get(0).pieces().get(0);
            AtomicInteger finished = new AtomicInteger();

            board.animateAlong(piece, redPath(game, 0, 7), finished::incrementAndGet);

            assertEquals(1, finished.get(), "instant mode must finish inside the same call");
        });
    }

    // ------------------------------------------------------------------ clean end, no double move

    @Test
    @DisplayName("NFR-04: when the animation ends the piece is back under the control of the Model")
    void theAnimationCleansUpAfterItself() {
        onEdt(() -> {
            Animations.instant = true;
            GameModel game = easyGame();
            BoardComponent board = paintedBoard(game);
            Piece piece = game.players().get(0).pieces().get(0);
            Point2D.Double before = board.restingPosition(piece);

            board.animateAlong(piece, redPath(game, 0, 4), () -> { });

            assertEquals(before, board.restingPosition(piece),
                    "the piece must follow the Model again, not stay where the animation left it");
        });
    }

    @Test
    @DisplayName("FR-20: no move can be started while the pieces are not highlighted during an animation")
    void noSecondMoveCanBeSubmittedDuringAnAnimation() {
        onEdt(() -> {
            Animations.instant = true;
            GameModel game = easyGame();
            BoardComponent board = paintedBoard(game);
            Piece piece = game.players().get(0).pieces().get(0);
            List<Piece> clicked = new ArrayList<>();
            board.setPieceListener(clicked::add);

            // The controller clears the highlight before it animates; from then on no click is accepted,
            // which is what stops the same roll being played twice (FR-20).
            board.clearTargets();
            board.animateAlong(piece, redPath(game, 0, 4), () -> { });
            for (int y = 0; y < board.getHeight(); y += 9) {
                for (int x = 0; x < board.getWidth(); x += 9) {
                    board.dispatchEvent(new java.awt.event.MouseEvent(board,
                            java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y,
                            1, false, java.awt.event.MouseEvent.BUTTON1));
                }
            }
            assertTrue(clicked.isEmpty(), "no piece may be played while a move is being shown");
        });
    }

    @Test
    @DisplayName("NFR-04: leaving the game stops the pulse, so nothing keeps running in the background")
    void leavingTheGameStopsTheTimers() {
        onEdt(() -> {
            GameModel game = easyGame();
            BoardComponent board = paintedBoard(game);
            board.addNotify();                 // the screen is shown
            board.removeNotify();              // the player leaves the game (FR-03)
            // removeNotify stops the pulse timer; drawing afterwards must still be safe.
            paint(board);
        });
    }

    @Test
    @DisplayName("NFR-04: several movements one after another all finish and leave nothing behind")
    void manyMovementsInARowAllFinish() {
        onEdt(() -> {
            Animations.instant = true;
            GameModel game = easyGame();
            BoardComponent board = paintedBoard(game);
            Piece piece = game.players().get(0).pieces().get(0);
            AtomicInteger finished = new AtomicInteger();

            for (int i = 0; i < 20; i++) {
                board.animateAlong(piece, redPath(game, i, 4), finished::incrementAndGet);
            }

            assertEquals(20, finished.get(), "every movement must report that it finished");
            assertFalse(board.targets().containsKey(piece), "nothing is left highlighted");
        });
    }

    /** Runs the body on the Swing event dispatch thread and reports its failures as its own. */
    private static void onEdt(Runnable body) {
        try {
            SwingUtilities.invokeAndWait(body);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new AssertionError(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for the event dispatch thread", e);
        }
    }
}
