package View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Dice;
import Model.DiceRoll;
import Model.Difficulty;
import Model.DieType;
import Model.GameModel;
import Model.GameSettings;
import Model.Move;
import Model.Piece;
import Model.PlayerColor;
import View.components.BoardComponent;

/**
 * Verification tests for highlighting the pieces that may move (Task 13; SRS FR-24).
 *
 * <p>The highlight itself lives in {@code BoardComponent} and was reused from the Assignment 3 prototype;
 * the controller feeds it the legal moves of the Model. These tests record that the highlight really shows
 * the legal moves and nothing else, and that a piece that is not highlighted cannot be played.
 *
 * <p>The rules are never recomputed here: the legal moves come from the real {@code GameModel}, so the
 * View is only checked for showing what the Model decided (NFR-08).
 *
 * <p>Owner: Mohamed Zereny (Task 13).
 */
class LegalMoveHighlightTest {

    private static final int BOARD_SIZE = 700;

    /** A die that always shows the same Regular value, so the legal moves are the same every run. */
    private record FixedRegularDice(int value) implements Dice {
        @Override
        public DiceRoll roll(Difficulty level) {
            return new DiceRoll(value, DieType.NONE, 0, value, false, false);
        }
    }

    private static GameModel gameRolling(int regular) {
        GameSettings settings = new GameSettings(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Red", PlayerColor.RED),
                new GameSettings.PlayerSetup("Green", PlayerColor.GREEN)));
        GameModel game = new GameModel(settings, new Random(2026), new FixedRegularDice(regular), () -> 0L);
        game.roll();
        return game;
    }

    /** The legal moves of the Model in the form the board shows them: piece to destination. */
    private static Map<Piece, Integer> legalTargets(GameModel game) {
        Map<Piece, Integer> targets = new HashMap<>();
        for (Map.Entry<Piece, Move> entry : game.legalMoves().entrySet()) {
            targets.put(entry.getKey(), entry.getValue().targetProgress());
        }
        return targets;
    }

    private static BoardComponent paintedBoard(GameModel game) {
        BoardComponent board = new BoardComponent();
        board.setGame(game);
        board.setInteractive(true);
        board.setSize(BOARD_SIZE, BOARD_SIZE);
        board.doLayout();
        paint(board);            // the component works out its geometry while painting
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

    /**
     * Presses the mouse over the whole board and collects every piece the board reports as clicked. This
     * is how a player reaches a piece, so it shows exactly which pieces can really be played.
     */
    private static Set<Piece> piecesThatCanBeClicked(BoardComponent board) {
        Set<Piece> clicked = new HashSet<>();
        board.setPieceListener(clicked::add);
        for (int y = 0; y < board.getHeight(); y += 7) {
            for (int x = 0; x < board.getWidth(); x += 7) {
                board.dispatchEvent(new MouseEvent(board, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                        0, x, y, 1, false, MouseEvent.BUTTON1));
            }
        }
        return clicked;
    }

    // ------------------------------------------------------------------ FR-24: what is highlighted

    @Test
    @DisplayName("FR-24: after a roll the board highlights exactly the pieces the Model allows")
    void theHighlightShowsExactlyTheLegalMoves() {
        onEdt(() -> {
            GameModel game = gameRolling(6);
            Map<Piece, Integer> legal = legalTargets(game);
            assertEquals(4, legal.size(), "a 6 lets all four castle pieces out (FR-19)");

            BoardComponent board = paintedBoard(game);
            board.setTargets(legal);

            assertEquals(legal.keySet(), board.targets().keySet(),
                    "the highlighted pieces must be exactly the legal ones");
            assertEquals(legal, board.targets(), "and each one with the destination the Model gave");
        });
    }

    @Test
    @DisplayName("FR-24: a roll that moves nothing highlights nothing")
    void noLegalMovesHighlightsNothing() {
        onEdt(() -> {
            GameModel game = gameRolling(3);        // a 3 cannot open a castle
            Map<Piece, Integer> legal = legalTargets(game);
            assertTrue(legal.isEmpty(), "no piece can move with a 3 from the castle (FR-19)");

            BoardComponent board = paintedBoard(game);
            board.setTargets(legal);

            assertTrue(board.targets().isEmpty(), "nothing may be highlighted");
            assertTrue(piecesThatCanBeClicked(board).isEmpty(), "and no piece may be played");
        });
    }

    @Test
    @DisplayName("FR-24: only a highlighted piece can be clicked; the others cannot be played")
    void onlyHighlightedPiecesCanBePlayed() {
        onEdt(() -> {
            GameModel game = gameRolling(6);
            Map<Piece, Integer> legal = legalTargets(game);
            BoardComponent board = paintedBoard(game);

            // Highlight only one of the four legal pieces: the other three must become unreachable.
            Piece chosen = legal.keySet().iterator().next();
            board.setTargets(Map.of(chosen, legal.get(chosen)));

            Set<Piece> clickable = piecesThatCanBeClicked(board);
            assertEquals(Set.of(chosen), clickable,
                    "a piece that is not highlighted must not be playable: " + clickable);
        });
    }

    @Test
    @DisplayName("FR-24: no piece of another player is ever highlighted or playable")
    void onlyTheActivePlayerPiecesAreOffered() {
        onEdt(() -> {
            GameModel game = gameRolling(6);
            Map<Piece, Integer> legal = legalTargets(game);
            BoardComponent board = paintedBoard(game);
            board.setTargets(legal);

            PlayerColor active = game.currentPlayer().color();
            for (Piece piece : board.targets().keySet()) {
                assertEquals(active, piece.color(), "only the active player may be offered a move (FR-15)");
            }
            for (Piece piece : piecesThatCanBeClicked(board)) {
                assertEquals(active, piece.color(), "no opponent piece may be clickable");
            }
        });
    }

    // ------------------------------------------------------------------ FR-24: clearing

    @Test
    @DisplayName("FR-24: clearing the highlight leaves nothing marked and nothing playable")
    void clearingRemovesEveryHighlight() {
        onEdt(() -> {
            GameModel game = gameRolling(6);
            BoardComponent board = paintedBoard(game);
            board.setTargets(legalTargets(game));
            assertFalse(board.targets().isEmpty());

            board.clearTargets();

            assertTrue(board.targets().isEmpty(), "after a move the highlight must be gone");
            assertTrue(piecesThatCanBeClicked(board).isEmpty(),
                    "and no piece may be played until the next roll");
        });
    }

    @Test
    @DisplayName("FR-24: a new game clears the highlight of the previous one")
    void aNewGameClearsTheHighlight() {
        onEdt(() -> {
            GameModel first = gameRolling(6);
            BoardComponent board = paintedBoard(first);
            board.setTargets(legalTargets(first));
            assertFalse(board.targets().isEmpty());

            board.setGame(gameRolling(6));

            assertTrue(board.targets().isEmpty(), "the highlight of the old game may not stay on screen");
        });
    }

    @Test
    @DisplayName("FR-24: setting a new highlight replaces the old one completely")
    void aNewHighlightReplacesTheOldOne() {
        onEdt(() -> {
            GameModel game = gameRolling(6);
            Map<Piece, Integer> legal = legalTargets(game);
            BoardComponent board = paintedBoard(game);
            board.setTargets(legal);

            Piece only = legal.keySet().iterator().next();
            board.setTargets(Map.of(only, legal.get(only)));

            assertEquals(Set.of(only), board.targets().keySet(),
                    "the pieces of the previous roll may not stay highlighted");
        });
    }

    // ------------------------------------------------------------------ the View stays a View

    @Test
    @DisplayName("NFR-08: the board keeps its own copy of the highlight; the caller cannot change it")
    void theHighlightCannotBeChangedFromOutside() {
        onEdt(() -> {
            GameModel game = gameRolling(6);
            BoardComponent board = paintedBoard(game);
            Map<Piece, Integer> given = new HashMap<>(legalTargets(game));
            board.setTargets(given);
            int size = board.targets().size();

            given.clear();                                   // the caller changes its own map
            assertEquals(size, board.targets().size(), "the board must not follow the caller's map");
            assertThrows(UnsupportedOperationException.class, () -> board.targets().clear(),
                    "what the board hands out may not be changed either");
        });
    }

    @Test
    @DisplayName("FR-24: a board that is only decoration never offers a move")
    void aDecorativeBoardIsNotPlayable() {
        onEdt(() -> {
            GameModel game = gameRolling(6);
            BoardComponent board = new BoardComponent();
            board.setGame(game);
            board.setInteractive(false);                     // the preview on the main menu
            board.setSize(BOARD_SIZE, BOARD_SIZE);
            board.doLayout();
            paint(board);
            board.setTargets(legalTargets(game));

            assertTrue(piecesThatCanBeClicked(board).isEmpty(),
                    "the menu preview may never start a move");
        });
    }

    @Test
    @DisplayName("FR-24: the highlight survives being drawn, at every supported board size")
    void theHighlightIsDrawnWithoutFailing() {
        onEdt(() -> {
            GameModel game = gameRolling(6);
            Map<Piece, Integer> legal = legalTargets(game);
            for (int size : new int[] {300, 600, 900}) {
                BoardComponent board = new BoardComponent();
                board.setGame(game);
                board.setInteractive(true);
                board.setSize(size, size);
                board.doLayout();
                board.setTargets(legal);
                paint(board);                                // drawing the preview must not fail
                assertEquals(legal.size(), board.targets().size(), "at " + size + "px");
            }
        });
    }

    @Test
    @DisplayName("FR-24: the destinations offered are the ones the Model computed, never invented")
    void theDestinationsComeFromTheModel() {
        onEdt(() -> {
            GameModel game = gameRolling(6);
            Map<Piece, Move> fromModel = game.legalMoves();
            BoardComponent board = paintedBoard(game);
            board.setTargets(legalTargets(game));

            List<String> wrong = new ArrayList<>();
            for (Map.Entry<Piece, Integer> shown : board.targets().entrySet()) {
                Move expected = fromModel.get(shown.getKey());
                if (expected == null || expected.targetProgress() != shown.getValue()) {
                    wrong.add(shown.getKey() + " shown to " + shown.getValue()
                            + " but the Model says " + (expected == null ? "no move" : expected));
                }
            }
            assertTrue(wrong.isEmpty(), "the View may not invent a destination: " + wrong);
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
