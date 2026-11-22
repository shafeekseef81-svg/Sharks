package View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Board;
import Model.Cell;
import Model.Difficulty;
import Model.GameModel;
import Model.GameSettings;
import Model.Piece;
import Model.Player;
import Model.PlayerColor;
import Model.Square;
import Model.SquareType;
import View.components.BoardComponent;
import View.components.Theme;

/**
 * Drawing tests for the Medium and Hard boards and the square symbols (task T2-03, issue #54;
 * SRS FR-14 and NFR-23).
 *
 * <p>Owner: Mohamed Zereny. {@link BoardComponentRenderTest} covers the same ground for the Easy board;
 * nothing here repeats it. The drawing itself came from the Assignment 3 prototype through the reuse gate
 * (SDP 6.1) and had no automated test on these two levels, which is what this task owes.
 *
 * <p>The tests render into an image instead of opening a window, so they also run headless. They check
 * geometry, the symbols that must appear and the room the board takes, not exact pixels, so a change of
 * style does not break them.
 *
 * <p>A game on Medium or Hard can be built and drawn today: {@code RandomDice} refuses only to <em>roll</em>
 * for those levels until T2-04, and the view never rolls. Choosing them on the setup screen is T2-01
 * (issue #13) and is not part of this task.
 */
class MediumHardBoardRenderTest {

    /** NFR-23 names 1280x720 as the smallest screen that must work without scrolling. */
    private static final int MIN_SCREEN_W = 1280;
    private static final int MIN_SCREEN_H = 720;

    /** The board area inside that window, as {@link BoardComponentRenderTest} uses it. */
    private static final int MIN_SCREEN_BOARD = 600;

    /** The two levels of this task; Easy is covered by {@link BoardComponentRenderTest}. */
    private static final Difficulty[] LEVELS = {Difficulty.MEDIUM, Difficulty.HARD};

    /** Cells per side, from the official diagrams: Medium 17x17, Hard 19x19. */
    private static int expectedGrid(Difficulty level) {
        return level == Difficulty.MEDIUM ? 17 : 19;
    }

    private static GameModel game(Difficulty level) {
        GameSettings settings = new GameSettings(level, List.of(
                new GameSettings.PlayerSetup("Red", PlayerColor.RED),
                new GameSettings.PlayerSetup("Green", PlayerColor.GREEN),
                new GameSettings.PlayerSetup("Yellow", PlayerColor.YELLOW),
                new GameSettings.PlayerSetup("Blue", PlayerColor.BLUE)));
        return new GameModel(settings, new Random(2026));
    }

    /** A board of the given size, painted once; the component works out its geometry while it paints. */
    private static BoardComponent boardAt(GameModel game, int size) {
        BoardComponent board = new BoardComponent();
        board.setGame(game);
        board.setSize(size, size);
        board.doLayout();
        render(board);
        return board;
    }

    /** Paints a component into an image, exactly as Swing would paint it on screen. */
    private static BufferedImage render(Component component) {
        BufferedImage image = new BufferedImage(Math.max(1, component.getWidth()),
                Math.max(1, component.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        component.paint(g);
        g.dispose();
        return image;
    }

    /** Smallest rectangle of the image that is not the white background, as {x, y, width, height}. */
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

    private static Map<SquareType, Integer> squareTypes(Board board) {
        Map<SquareType, Integer> counts = new EnumMap<>(SquareType.class);
        for (Square s : board.squares()) {
            counts.merge(s.type(), 1, Integer::sum);
        }
        return counts;
    }

    /** Lays out a whole screen, including the nested panels, the way a real window does. */
    private static void layoutAll(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutAll(nested);
            }
        }
    }

    /** The game screen bound to a game and laid out at the smallest supported window. */
    private static GameBoardView screen(GameModel game) {
        GameBoardView view = new GameBoardView();
        view.bind(game);
        view.setSize(MIN_SCREEN_W, MIN_SCREEN_H);
        layoutAll(view);
        return view;
    }

    private static BoardComponent boardOf(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof BoardComponent board) {
                return board;
            }
            if (child instanceof Container nested) {
                BoardComponent found = boardOf(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Every piece of text shown on a screen, so the legend can be read back. */
    private static List<String> texts(Container root) {
        List<String> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && label.getText() != null) {
                found.add(label.getText());
            }
            if (child instanceof Container nested) {
                found.addAll(texts(nested));
            }
        }
        return found;
    }

    // ------------------------------------------------------------------ FR-14: the boards are drawn

    @Test
    @DisplayName("T2-03 FR-14: the board drawn is the board of the Model, 17x17 on Medium and 19x19 on Hard")
    void theLargerBoardsOfTheModelAreDrawn() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                GameModel game = game(level);
                int n = game.board().size();
                assertEquals(expectedGrid(level), n, level.label() + " board size (FR-10)");
                BoardComponent board = boardAt(game, 700);
                Set<Point2D.Double> centres = new HashSet<>();
                for (int row = 0; row < n; row++) {
                    for (int col = 0; col < n; col++) {
                        Point2D.Double centre = board.cellCenter(new Cell(row, col));
                        assertTrue(centre.x > 0 && centre.x < n && centre.y > 0 && centre.y < n,
                                level.label() + " cell " + row + "," + col + " falls outside the board: " + centre);
                        centres.add(centre);
                    }
                }
                assertEquals(n * n, centres.size(),
                        level.label() + ": every cell of the Model gets its own place on the board");
            }
        });
    }

    @Test
    @DisplayName("T2-03 FR-14: every square type of the level has its place on screen, question and surprise too")
    void everySquareTypeOfTheLargerBoardsIsDrawn() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                GameModel game = game(level);
                BoardComponent board = boardAt(game, 700);
                Board model = game.board();
                int n = model.size();
                Map<SquareType, Integer> counts = squareTypes(model);
                for (SquareType type : List.of(SquareType.START, SquareType.ARROW, SquareType.PROTECTION,
                        SquareType.QUESTION, SquareType.SURPRISE, SquareType.HOME_PATH, SquareType.TRACK)) {
                    assertTrue(counts.getOrDefault(type, 0) > 0,
                            level.label() + " has no " + type + " square to draw");
                }
                for (Square square : model.squares()) {
                    Point2D.Double centre = board.cellCenter(square.cell());
                    assertTrue(centre.x > 0 && centre.x < n && centre.y > 0 && centre.y < n,
                            level.label() + " " + square.type() + " square at " + square.cell()
                                    + " is drawn off the board: " + centre);
                }
                assertTrue(model.squares().stream().anyMatch(Square::isTinted),
                        level.label() + " must keep the classic start square of each colour coloured");
            }
        });
    }

    @Test
    @DisplayName("T2-03 FR-14: question squares carry their level colour and surprise squares the gift colour")
    void questionAndSurpriseSymbolsAreDrawnAndTellThemselvesApart() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                BufferedImage image = render(boardAt(game(level), 700));
                for (Difficulty questionLevel : Difficulty.values()) {
                    assertTrue(contains(image, Theme.level(questionLevel), 12),
                            level.label() + ": no " + questionLevel.label()
                                    + " question square is drawn in its own colour (FR-14)");
                }
                assertTrue(contains(image, Theme.SURPRISE, 12),
                        level.label() + ": the surprise squares are not drawn (FR-14)");
                assertTrue(contains(image, Theme.STAR, 16),
                        level.label() + ": the protection stars are not drawn (FR-14)");
                for (PlayerColor colour : PlayerColor.values()) {
                    assertTrue(contains(image, Theme.player(colour), 12),
                            level.label() + ": " + colour.label() + " does not appear on the board");
                }
            }
        });
    }

    @Test
    @DisplayName("T2-03 FR-14: the Easy board shows no question or surprise symbols, the larger boards do")
    void onlyTheLargerBoardsShowQuestionAndSurpriseSymbols() {
        onEdt(() -> {
            BufferedImage easy = render(boardAt(game(Difficulty.EASY), 700));
            assertFalse(contains(easy, Theme.SURPRISE, 12),
                    "an Easy board has no surprise squares (Table 1), so the gift colour must not appear");
            BufferedImage hard = render(boardAt(game(Difficulty.HARD), 700));
            assertTrue(contains(hard, Theme.SURPRISE, 12), "the Hard board does show surprise squares");
        });
    }

    @Test
    @DisplayName("T2-03 FR-13: all 16 pieces rest in their castles on the larger boards, each with its own place")
    void piecesRestInTheirCastlesOnTheLargerBoards() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                GameModel game = game(level);
                BoardComponent board = boardAt(game, 700);
                int n = game.board().size();
                Set<Point2D.Double> spots = new HashSet<>();
                for (Player player : game.players()) {
                    for (Piece piece : player.pieces()) {
                        assertTrue(piece.inCastle(), level.label() + ": a new game keeps every piece in its castle");
                        Point2D.Double spot = board.restingPosition(piece);
                        assertTrue(spot.x > 0 && spot.x < n && spot.y > 0 && spot.y < n,
                                level.label() + " " + piece + " is drawn outside the board: " + spot);
                        spots.add(spot);
                    }
                }
                assertEquals(16, spots.size(),
                        level.label() + ": all 16 pieces have their own place, none drawn on top of another");
            }
        });
    }

    @Test
    @DisplayName("T2-03 FR-14: a piece on the shared track is drawn on the cell the Model gives it")
    void aPieceOnTheTrackIsDrawnOnItsOwnCell() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                GameModel game = game(level);
                BoardComponent board = boardAt(game, 700);
                Board model = game.board();
                for (PlayerColor colour : PlayerColor.values()) {
                    for (int progress : new int[] {0, 1, model.homeStartIndex(), model.finishIndex() - 1}) {
                        Cell cell = model.cellAt(colour, progress);
                        Point2D.Double centre = board.cellCenter(cell);
                        assertEquals(cell.col() + 0.5, centre.x, 1e-9,
                                level.label() + " " + colour.label() + " progress " + progress + " column");
                        assertEquals(cell.row() + 0.5, centre.y, 1e-9,
                                level.label() + " " + colour.label() + " progress " + progress + " row");
                    }
                }
            }
        });
    }

    @Test
    @DisplayName("T2-03 FR-14: the finishing place of every colour stays in the centre of the larger boards")
    void everyColourFinishesInTheCentre() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                GameModel game = game(level);
                BoardComponent board = boardAt(game, 700);
                double middle = game.board().size() / 2.0;
                Set<Point2D.Double> finishes = new HashSet<>();
                for (Player player : game.players()) {
                    Point2D.Double finish = board.finishSpot(player.pieces().get(0));
                    assertTrue(Math.abs(finish.x - middle) < 2.0 && Math.abs(finish.y - middle) < 2.0,
                            level.label() + " " + player.color() + " finishes away from the centre " + middle
                                    + ": " + finish);
                    finishes.add(finish);
                }
                assertEquals(4, finishes.size(), level.label() + ": each colour finishes on its own side");
            }
        });
    }

    // ------------------------------------------------------------------ NFR-23: they fit the window

    @Test
    @DisplayName("T2-03 NFR-23: the whole 17x17 and 19x19 board is drawn inside a 1280x720 screen")
    void theLargerBoardsFitTheSmallestSupportedScreen() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                BoardComponent board = boardAt(game(level), MIN_SCREEN_BOARD);
                int[] drawn = drawnBounds(render(board));
                assertTrue(drawn[0] >= 0 && drawn[1] >= 0, level.label() + ": the board starts inside the component");
                assertTrue(drawn[0] + drawn[2] <= MIN_SCREEN_BOARD && drawn[1] + drawn[3] <= MIN_SCREEN_BOARD,
                        level.label() + ": the board runs past the edge, so it would need scrolling: "
                                + drawn[2] + "x" + drawn[3] + " at " + drawn[0] + "," + drawn[1]);
                assertTrue(drawn[2] > MIN_SCREEN_BOARD * 0.8 && drawn[3] > MIN_SCREEN_BOARD * 0.8,
                        level.label() + ": the board should use the room it has: " + drawn[2] + "x" + drawn[3]);
                assertEquals(drawn[2], drawn[3], 4, level.label() + ": the board is drawn square");
            }
        });
    }

    @Test
    @DisplayName("T2-03 NFR-23: the complete game screen at 1280x720 keeps the whole board visible")
    void theGameScreenShowsTheWholeBoardAtTheSmallestScreen() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                GameBoardView view = screen(game(level));
                BoardComponent board = boardOf(view);
                assertTrue(board != null, level.label() + ": the game screen has no board");
                assertTrue(board.getWidth() > 0 && board.getHeight() > 0,
                        level.label() + ": the board got no room on the screen");
                int[] drawn = drawnBounds(render(board));
                assertTrue(drawn[0] + drawn[2] <= board.getWidth() && drawn[1] + drawn[3] <= board.getHeight(),
                        level.label() + ": the board is cut off inside the game screen: " + drawn[2] + "x" + drawn[3]
                                + " in " + board.getWidth() + "x" + board.getHeight());
                assertEquals(drawn[2], drawn[3], 4, level.label() + ": the board stays square on the game screen");
                assertTrue(drawn[2] >= 500,
                        level.label() + ": the board is squeezed to " + drawn[2] + "px on a 1280x720 screen");
            }
        });
    }

    @Test
    @DisplayName("T2-03 NFR-23: a cell of the 19x19 board stays large enough to read at the smallest screen")
    void cellsStayReadableOnTheHardBoard() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                int side = drawnBounds(render(boardAt(game(level), MIN_SCREEN_BOARD)))[2];
                double cell = (double) side / expectedGrid(level);
                assertTrue(cell >= 24.0,
                        level.label() + ": a cell is only " + String.format("%.1f", cell)
                                + "px at the smallest supported screen, too small for its symbol (NFR-23)");
            }
        });
    }

    @Test
    @DisplayName("T2-03 NFR-23: the larger boards grow and shrink with the window, keeping their proportions")
    void theLargerBoardsScaleWithTheWindow() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                GameModel game = game(level);
                int small = drawnBounds(render(boardAt(game, 400)))[2];
                int medium = drawnBounds(render(boardAt(game, 700)))[2];
                int large = drawnBounds(render(boardAt(game, 1000)))[2];
                assertTrue(small < medium && medium < large,
                        level.label() + ": the board must grow with the window: "
                                + small + ", " + medium + ", " + large);
                double ratio = (double) large / small;
                assertTrue(ratio > 2.0 && ratio < 3.0,
                        level.label() + ": 2.5 times the room should give about 2.5 times the board, ratio was "
                                + ratio);
            }
        });
    }

    @Test
    @DisplayName("T2-03 NFR-23: a window that is not square keeps the larger boards square and centred")
    void aWideWindowStillDrawsASquareBoard() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                BoardComponent board = new BoardComponent();
                board.setGame(game(level));
                board.setSize(1200, 600);
                board.doLayout();
                int[] drawn = drawnBounds(render(board));
                assertEquals(drawn[2], drawn[3], 4, level.label() + ": the board stays square in a wide window");
                assertTrue(drawn[3] <= 600, level.label() + ": the board must fit the shorter side (NFR-23)");
                assertTrue(drawn[0] > 100, level.label() + ": and it is centred: x=" + drawn[0]);
            }
        });
    }

    // ------------------------------------------------------------------ FR-14: the legend explains the symbols

    @Test
    @DisplayName("T2-03 FR-14: the legend names every symbol of the level, with the count of SRS Table 1")
    void theLegendExplainsEverySymbolOfTheLevel() {
        onEdt(() -> {
            List<String> medium = texts(screen(game(Difficulty.MEDIUM)));
            assertTrue(medium.contains("Question E / M / H"), "Medium legend misses the question squares");
            assertTrue(medium.contains("Surprise (safe)"), "Medium legend misses the surprise squares");
            assertTrue(medium.contains("x8"), "Medium legend misses the count 8 (protection and arrows, Table 1)");
            assertTrue(medium.contains("x6"), "Medium legend misses the count 6 (question squares, Table 1)");
            assertTrue(medium.contains("x2"), "Medium legend misses the count 2 (surprise squares, Table 1)");

            List<String> hard = texts(screen(game(Difficulty.HARD)));
            assertTrue(hard.contains("x12"), "Hard legend misses the count 12 (protection and arrows, Table 1)");
            assertTrue(hard.contains("x9"), "Hard legend misses the count 9 (question squares, Table 1)");
            assertTrue(hard.contains("x3"), "Hard legend misses the count 3 (surprise squares, Table 1)");
        });
    }

    @Test
    @DisplayName("T2-03 FR-14: the legend explains the coloured classic start square, which Easy does not have")
    void theLegendExplainsTheOldStartSquareOnTheLargerBoardsOnly() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                assertTrue(texts(screen(game(level))).contains("Old start (no effect)"),
                        level.label() + ": the one coloured square with no function is not explained (FR-14)");
            }
            assertFalse(texts(screen(game(Difficulty.EASY))).contains("Old start (no effect)"),
                    "the Easy board has no such square, so the row must not be shown");
        });
    }

    @Test
    @DisplayName("T2-03 FR-14: the game screen names the level and its board size")
    void theScreenNamesTheLevelAndTheBoardSize() {
        onEdt(() -> {
            List<String> medium = texts(screen(game(Difficulty.MEDIUM)));
            assertTrue(medium.contains("MEDIUM"), "the level pill does not say MEDIUM");
            assertTrue(medium.contains("17 x 17 board"), "the screen does not give the Medium board size");
            assertTrue(medium.contains("Track: 52 + 64 = 116 squares"), "the Medium track formula is missing");

            List<String> hard = texts(screen(game(Difficulty.HARD)));
            assertTrue(hard.contains("HARD"), "the level pill does not say HARD");
            assertTrue(hard.contains("19 x 19 board"), "the screen does not give the Hard board size");
            assertTrue(hard.contains("Track: 52 + 64 + 72 = 188 squares"), "the Hard track formula is missing");
        });
    }

    // ------------------------------------------------------------------ NFR-08: the view only reads

    @Test
    @DisplayName("T2-03 NFR-08: drawing a larger board changes nothing in the Model")
    void drawingDoesNotChangeTheModel() {
        onEdt(() -> {
            for (Difficulty level : LEVELS) {
                GameModel game = game(level);
                Map<SquareType, Integer> before = squareTypes(game.board());
                GameBoardView view = screen(game);
                render(view);
                render(boardAt(game, 700));
                assertEquals(before, squareTypes(game.board()),
                        level.label() + ": drawing the board changed the squares of the Model");
                assertTrue(boardOf(view).targets().isEmpty(),
                        level.label() + ": a board that was only drawn highlights nothing");
                for (Player player : game.players()) {
                    for (Piece piece : player.pieces()) {
                        assertTrue(piece.inCastle(), level.label() + ": drawing moved a piece");
                    }
                }
            }
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
