package View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Board;
import Model.Cell;
import Model.Difficulty;
import Model.GameModel;
import Model.GameSettings;
import Model.Piece;
import Model.PlayerColor;
import Model.Square;
import Model.SquareType;
import View.components.BoardComponent;
import View.components.Theme;

/**
 * Verification tests for the Easy board drawing (Task 09; SRS FR-14 partial, NFR-23).
 *
 * <p>The drawing itself was reused from the Assignment 3 prototype through the reuse gate (SDP 6.1) and
 * had no automated test. These tests record that it really draws the board of the Model and really scales
 * with the window, which is what Task 09 owes.
 *
 * <p>They render the component into an image instead of opening a window, so they also run headless, and
 * they check geometry and the colours that must appear rather than exact pixels, so a change of style does
 * not break them.
 *
 * <p>Owner: Mohamed Zereny (Task 09).
 */
class BoardComponentRenderTest {

    /** NFR-23 names 1280x720 as the smallest screen; this is the board area inside that window. */
    private static final int MIN_SCREEN_BOARD = 600;

    private static GameModel easyGame() {
        GameSettings settings = new GameSettings(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Red", PlayerColor.RED),
                new GameSettings.PlayerSetup("Green", PlayerColor.GREEN),
                new GameSettings.PlayerSetup("Yellow", PlayerColor.YELLOW),
                new GameSettings.PlayerSetup("Blue", PlayerColor.BLUE)));
        return new GameModel(settings, new Random(2026));
    }

    /**
     * A board of the given size, painted once. The component works out its geometry while it paints, the
     * same as on screen, so the drawing places can only be asked for after a first paint.
     */
    private static BoardComponent boardAt(GameModel game, int size) {
        BoardComponent board = new BoardComponent();
        board.setGame(game);
        board.setSize(size, size);
        board.doLayout();
        render(board);
        return board;
    }

    /** Paints the component and returns the image, exactly as Swing would paint it on screen. */
    private static BufferedImage render(BoardComponent board) {
        BufferedImage image = new BufferedImage(Math.max(1, board.getWidth()),
                Math.max(1, board.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        board.paint(g);
        g.dispose();
        return image;
    }

    /**
     * Smallest rectangle of the image that is not the white background, as {x, y, width, height}. This is
     * the board as it was really painted, which is the only way to see from outside how big it is drawn:
     * {@code cellCenter} and friends answer in cell units, and the conversion to pixels is private.
     */
    private static int[] drawnBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        int white = Color.WHITE.getRGB();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != white) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        assertTrue(maxX >= 0, "nothing was painted at all");
        return new int[] {minX, minY, maxX - minX + 1, maxY - minY + 1};
    }

    private static boolean contains(BufferedImage image, Color wanted, int tolerance) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color c = new Color(image.getRGB(x, y));
                if (Math.abs(c.getRed() - wanted.getRed()) <= tolerance
                        && Math.abs(c.getGreen() - wanted.getGreen()) <= tolerance
                        && Math.abs(c.getBlue() - wanted.getBlue()) <= tolerance) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ FR-14: the board is drawn

    @Test
    @DisplayName("FR-14: the board drawn is the board of the Model, 15x15 cells on Easy")
    void theBoardOfTheModelIsDrawn() {
        onEdt(() -> {
            GameModel game = easyGame();
            assertEquals(15, game.board().size(), "the Easy board is 15x15 (FR-10)");
            BoardComponent board = boardAt(game, 700);

            int n = game.board().size();
            Set<Point2D.Double> centres = new HashSet<>();
            for (int row = 0; row < n; row++) {
                for (int col = 0; col < n; col++) {
                    // The positions are in cell units: the board runs from 0 to n on both axes.
                    Point2D.Double centre = board.cellCenter(new Cell(row, col));
                    assertTrue(centre.x > 0 && centre.x < n && centre.y > 0 && centre.y < n,
                            "cell " + row + "," + col + " falls outside the board: " + centre);
                    centres.add(centre);
                }
            }
            assertEquals(n * n, centres.size(), "every cell of the Model gets its own place on the board");
        });
    }

    @Test
    @DisplayName("FR-14: the drawing is not blank and shows all four player colours (castles and pieces)")
    void allFourColoursAreDrawn() {
        onEdt(() -> {
            BufferedImage image = render(boardAt(easyGame(), 700));
            Set<Integer> distinct = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y += 3) {
                for (int x = 0; x < image.getWidth(); x += 3) {
                    distinct.add(image.getRGB(x, y));
                }
            }
            assertTrue(distinct.size() > 50, "the board is not a blank area: " + distinct.size() + " colours");
            for (PlayerColor colour : PlayerColor.values()) {
                assertTrue(contains(image, Theme.player(colour), 12),
                        colour + " does not appear on the board (castle and pieces, FR-13, FR-14)");
            }
        });
    }

    @Test
    @DisplayName("FR-11, FR-14: the Easy board really carries start, arrow, protection and home-path squares")
    void everySquareTypeOfTheEasyBoardHasAPlaceOnScreen() {
        onEdt(() -> {
            GameModel game = easyGame();
            BoardComponent board = boardAt(game, 700);
            Board model = game.board();
            Set<SquareType> drawn = new HashSet<>();
            for (Square square : model.squares()) {
                Point2D.Double centre = board.cellCenter(square.cell());
                assertTrue(centre.x > 0 && centre.y > 0, square.type() + " square is off the board");
                drawn.add(square.type());
            }
            assertTrue(drawn.contains(SquareType.START), "start squares");
            assertTrue(drawn.contains(SquareType.ARROW), "arrows");
            assertTrue(drawn.contains(SquareType.PROTECTION), "protection squares (FR-12)");
            assertTrue(drawn.contains(SquareType.HOME_PATH), "home paths (FR-33)");
            assertTrue(drawn.contains(SquareType.TRACK), "plain track squares");
            assertFalse(drawn.contains(SquareType.QUESTION), "Easy has no question squares (Table 1)");
            assertFalse(drawn.contains(SquareType.SURPRISE), "Easy has no surprise squares (Table 1)");
        });
    }

    @Test
    @DisplayName("FR-13: every piece starts in its own castle and has a drawing place there")
    void piecesRestInTheirCastles() {
        onEdt(() -> {
            GameModel game = easyGame();
            BoardComponent board = boardAt(game, 700);
            int n = game.board().size();
            Set<Point2D.Double> spots = new HashSet<>();
            for (var player : game.players()) {
                for (Piece piece : player.pieces()) {
                    assertTrue(piece.inCastle(), "a new game keeps every piece in its castle (FR-13)");
                    Point2D.Double spot = board.restingPosition(piece);
                    assertTrue(spot.x > 0 && spot.x < n && spot.y > 0 && spot.y < n,
                            piece + " is drawn outside the board: " + spot);
                    spots.add(spot);
                }
            }
            assertEquals(16, spots.size(), "all 16 pieces have their own place, none drawn on top of another");
        });
    }

    @Test
    @DisplayName("FR-14: the finishing place of every colour is inside the centre of the board")
    void everyColourHasAFinishingPlaceInTheCentre() {
        onEdt(() -> {
            GameModel game = easyGame();
            BoardComponent board = boardAt(game, 700);
            double middle = game.board().size() / 2.0;          // cell units: 7.5 on the Easy board
            for (var player : game.players()) {
                Point2D.Double finish = board.finishSpot(player.pieces().get(0));
                assertTrue(Math.abs(finish.x - middle) < 2.0 && Math.abs(finish.y - middle) < 2.0,
                        player.color() + " finishes away from the centre " + middle + ": " + finish);
            }
        });
    }

    // ------------------------------------------------------------------ NFR-23: it scales

    @Test
    @DisplayName("NFR-23: the whole board is drawn inside a 1280x720 screen, without scrolling")
    void theBoardFitsTheSmallestSupportedScreen() {
        onEdt(() -> {
            BoardComponent board = boardAt(easyGame(), MIN_SCREEN_BOARD);
            int[] drawn = drawnBounds(render(board));

            assertTrue(drawn[0] >= 0 && drawn[1] >= 0, "the board starts inside the component");
            assertTrue(drawn[0] + drawn[2] <= MIN_SCREEN_BOARD && drawn[1] + drawn[3] <= MIN_SCREEN_BOARD,
                    "the board runs past the edge, so it would need scrolling: "
                            + drawn[2] + "x" + drawn[3] + " at " + drawn[0] + "," + drawn[1]);
            assertTrue(drawn[2] > MIN_SCREEN_BOARD * 0.8 && drawn[3] > MIN_SCREEN_BOARD * 0.8,
                    "the board should use the room it has: " + drawn[2] + "x" + drawn[3]);
            assertEquals(drawn[2], drawn[3], 4, "the board is drawn square");
        });
    }

    @Test
    @DisplayName("NFR-23: the board grows and shrinks with the window, keeping its proportions")
    void theBoardScalesWithTheWindow() {
        onEdt(() -> {
            GameModel game = easyGame();
            int small = drawnBounds(render(boardAt(game, 400)))[2];
            int medium = drawnBounds(render(boardAt(game, 700)))[2];
            int large = drawnBounds(render(boardAt(game, 1000)))[2];

            assertTrue(small < medium && medium < large,
                    "the board must grow with the window: " + small + ", " + medium + ", " + large);
            double ratio = (double) large / small;
            assertTrue(ratio > 2.0 && ratio < 3.0,
                    "2.5 times the room should give about 2.5 times the board, ratio was " + ratio);
        });
    }

    @Test
    @DisplayName("NFR-23: a window that is not square keeps the board square, using the shorter side")
    void aWideWindowStillDrawsASquareBoard() {
        onEdt(() -> {
            BoardComponent board = new BoardComponent();
            board.setGame(easyGame());
            board.setSize(1200, 600);
            board.doLayout();
            int[] drawn = drawnBounds(render(board));

            assertEquals(drawn[2], drawn[3], 4, "the board stays square in a wide window");
            assertTrue(drawn[3] <= 600, "the board must fit the shorter side without scrolling (NFR-23)");
            assertTrue(drawn[0] > 100, "and it is centred, not stuck to the left edge: x=" + drawn[0]);
        });
    }

    @Test
    @DisplayName("NFR-23: a board too small to read is skipped instead of drawing an unreadable mess")
    void aTinyBoardIsSkippedSafely() {
        onEdt(() -> {
            GameModel game = easyGame();
            BoardComponent board = new BoardComponent();
            board.setGame(game);
            for (int size : new int[] {0, 1, 49}) {
                board.setSize(size, size);
                board.doLayout();
                render(board);        // below 50px the component draws nothing and must not throw
            }
            board.setSize(60, 60);
            board.doLayout();
            render(board);
            Point2D.Double centre = board.cellCenter(new Cell(7, 7));
            assertTrue(centre.x > 0 && centre.y > 0, "from 50px upwards it draws again: " + centre);
        });
    }

    // ------------------------------------------------------------------ state

    @Test
    @DisplayName("NFR-08: the board only draws the Model; a new game resets what was shown")
    void settingANewGameResetsTheView() {
        onEdt(() -> {
            BoardComponent board = boardAt(easyGame(), 700);
            GameModel second = easyGame();
            board.setGame(second);
            assertTrue(board.targets().isEmpty(), "a new game starts with nothing highlighted");
            render(board);        // the new game draws without failing
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
