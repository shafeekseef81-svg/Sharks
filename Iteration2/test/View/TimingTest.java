package View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Board;
import Model.Cell;
import Model.Difficulty;
import Model.GameModel;
import Model.GameSettings;
import Model.Piece;
import Model.PlayerColor;
import View.components.Animations;
import View.components.BoardComponent;

/**
 * Timing tests of T2-14 (SRS NFR-02, NFR-04) for the Iteration 2 boards and dice. Owner: Shafeek Saif.
 *
 * <p><b>NFR-02:</b> "The game board shall appear within 2 seconds of starting a game, including random square
 * placement." Measured here from the start of a game to a laid-out and painted game screen: the model with its
 * random board (protection, question and surprise squares, T2-02), the screen of T2-03 bound to it, and one full
 * paint. Other tests may already have loaded the classes in the same run, so a cold start (the first game after
 * starting the program) is checked by hand on the two computers of the release (N2-06).
 *
 * <p><b>NFR-04:</b> "Each piece movement shall be animated square by square and complete within 2 seconds." Task 19
 * ({@code MovementAnimationTest}) measured a six-square move. Iteration 2 adds much longer moves: on Hard a Regular
 * 6 with an UltraBoost 6 moves 6 x 5 = 30 squares (FR-17), and the surprise jumps to the castle or the finish.
 *
 * <p>Both run off-screen, like the other {@code View} tests. The two computers of the release (N2-06) repeat
 * NFR-02 by hand on the real window. Measured on 19/11: the board in under 0.1 s; the 30-square move in about
 * 1.7 s, the closest to the limit, because {@code animateAlong} allows at most 1.6 s per move.
 */
class TimingTest {

    /** NFR-02 and NFR-04: two seconds. */
    private static final long LIMIT_MILLIS = 2000;
    private static final int SCREEN_W = 1280;
    private static final int SCREEN_H = 720;

    private boolean instantBefore;

    @BeforeEach
    void rememberAnimationMode() {
        instantBefore = Animations.instant;
    }

    @AfterEach
    void restoreAnimationMode() {
        Animations.instant = instantBefore;
    }

    private static GameSettings fourPlayers(Difficulty level) {
        List<GameSettings.PlayerSetup> setup = new ArrayList<>();
        for (PlayerColor color : PlayerColor.values()) {
            setup.add(new GameSettings.PlayerSetup(color.label(), color));
        }
        return new GameSettings(level, setup);
    }

    private static void layoutAll(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutAll(nested);
            }
        }
    }

    private static void paint(Component component) {
        BufferedImage image = new BufferedImage(Math.max(1, component.getWidth()), Math.max(1, component.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        component.paint(g);
        g.dispose();
    }

    /** Starts a game and returns the milliseconds until its screen is laid out and painted (NFR-02). */
    private static long startAndShow(Difficulty level, long seed) throws Exception {
        long[] took = new long[1];
        SwingUtilities.invokeAndWait(() -> {
            long start = System.nanoTime();
            GameModel game = new GameModel(fourPlayers(level), new Random(seed));
            GameBoardView view = new GameBoardView();
            view.bind(game);
            view.setSize(SCREEN_W, SCREEN_H);
            layoutAll(view);
            paint(view);
            took[0] = (System.nanoTime() - start) / 1_000_000;
        });
        return took[0];
    }

    // ------------------------------------------------------------------ NFR-02

    @Test
    @DisplayName("T2-14 NFR-02: a Hard game with four players shows its board within 2 seconds, over 21 boards")
    void hardBoardAppearsInTime() throws Exception {
        long worst = 0;
        for (long seed = 0; seed <= 20; seed++) {
            worst = Math.max(worst, startAndShow(Difficulty.HARD, seed));
        }
        assertTrue(worst < LIMIT_MILLIS, "NFR-02: the slowest of 21 Hard games took " + worst + "ms");
    }

    @Test
    @DisplayName("T2-14 NFR-02: Easy and Medium games with four players show their board within 2 seconds")
    void everyLevelAppearsInTime() throws Exception {
        for (Difficulty level : List.of(Difficulty.EASY, Difficulty.MEDIUM)) {
            for (long seed = 0; seed < 10; seed++) {
                long took = startAndShow(level, seed);
                assertTrue(took < LIMIT_MILLIS, "NFR-02: " + level.label() + " seed " + seed + " took " + took + "ms");
            }
        }
    }

    // ------------------------------------------------------------------ NFR-04

    /** Runs an animation for real and returns how long it took until its end callback ran (NFR-04). */
    private static long animate(GameModel game, BiConsumer<BoardComponent, Runnable> start) throws Exception {
        Animations.instant = false;
        AtomicInteger finished = new AtomicInteger();
        long begin = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> {
            BoardComponent board = new BoardComponent();
            board.setGame(game);
            board.setSize(700, 700);
            board.doLayout();
            paint(board);
            start.accept(board, finished::incrementAndGet);
        });
        long deadline = System.currentTimeMillis() + LIMIT_MILLIS + 1000;
        while (finished.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(1, finished.get(), "the animation must report that it finished");
        return (System.nanoTime() - begin) / 1_000_000;
    }

    @Test
    @DisplayName("T2-14 NFR-04: the longest Hard move, 30 squares (Regular 6 x (UltraBoost 6 - 1)), animates within 2 s")
    void longestHardMoveInTime() throws Exception {
        GameModel game = new GameModel(fourPlayers(Difficulty.HARD), new Random(5));
        Board board = game.board();
        assertTrue(board.homeStartIndex() > 31, "a 30-square move fits on the shared track");
        List<Cell> route = new ArrayList<>(board.path(PlayerColor.RED).subList(0, 31)); // 30 steps
        Piece piece = game.players().get(0).pieces().get(0);
        long took = animate(game, (component, done) -> component.animateAlong(piece, route, done));
        assertTrue(took < LIMIT_MILLIS, "NFR-04: a 30-square move took " + took + "ms");
    }

    @Test
    @DisplayName("T2-14 NFR-04: the surprise jumps to the castle and to the finish animate within 2 s")
    void surpriseJumpsInTime() throws Exception {
        GameModel game = new GameModel(fourPlayers(Difficulty.HARD), new Random(5));
        Piece piece = game.players().get(0).pieces().get(0);
        Cell far = game.board().path(PlayerColor.RED).get(40);
        Cell start = game.board().path(PlayerColor.RED).get(0);
        long toCastle = animate(game, (component, done) ->
                component.animateJump(piece, component.cellCenter(far), component.castleSpot(piece), done));
        long toFinish = animate(game, (component, done) ->
                component.animateJump(piece, component.cellCenter(start), component.finishSpot(piece), done));
        assertTrue(toCastle < LIMIT_MILLIS, "NFR-04: the jump to the castle took " + toCastle + "ms");
        assertTrue(toFinish < LIMIT_MILLIS, "NFR-04: the jump to the finish took " + toFinish + "ms");
    }
}
