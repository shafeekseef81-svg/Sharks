package Controller;

import java.util.Random;

import Model.GameModel;
import Model.GameSettings;
import Model.Piece;
import View.GameBoardView;
import View.GameBoardView.StatusKind;

/**
 * Connects the game board screen to the game model. It calls the model, and it runs the dice and piece
 * animations in order. It contains no game rules: rules live in the {@code Model} package (NFR-08).
 *
 * <p>Owner: Rakan Ziyan (task N02, integration).
 *
 * <p><b>Phase A placeholder</b>: a game can be created and the board is shown, but rolling and moving are
 * not connected yet. They are added in Phase B (Tasks 11, 12, 16), and the full turn flow in task N02.
 */
public final class GameController {

    private final NavigationController nav;
    private GameModel model;
    private GameBoardView view;

    /** @param nav the navigation controller (screens and dialogs) */
    public GameController(NavigationController nav) {
        this.nav = nav;
    }

    /**
     * Creates a new game and its board screen. The settings are already validated (FR-08) and cannot change
     * from now on (FR-09).
     *
     * @param settings validated settings
     * @param random   random source for the board and the dice
     * @return the board screen of the new game
     */
    public GameBoardView newGame(GameSettings settings, Random random) {
        stop();
        model = new GameModel(settings, random);
        view = new GameBoardView();
        view.setActions(this::roll, () -> nav.confirmLeaveGame(nav::showMainMenu));
        view.board().setPieceListener(this::pieceSelected);
        view.bind(model);
        model.addListener(e -> view.refresh());
        view.setElapsed("00:00:00");
        view.setControls(true, true);
        view.setStatus(model.currentPlayer().name() + ", roll the dice.", StatusKind.INFO);
        return view;
    }

    /** Stops the running game (the clock and any pending animation). Phase A: nothing is running yet. */
    public void stop() {
        // Phase B (Task 24 / N02): stop the Swing timer that updates the game clock.
    }

    /** Roll button or Space key (FR-15). */
    private void roll() {
        view.setStatus("Rolling is not available yet: it is added in Phase B (Tasks 11-12).", StatusKind.WARNING);
    }

    /** A highlighted piece was clicked (FR-20, FR-24). */
    private void pieceSelected(Piece piece) {
        view.setStatus("Moving is not available yet: it is added in Phase B (Task 16).", StatusKind.WARNING);
    }

    /** @return the model of the current game, or {@code null} before the first game */
    public GameModel model() {
        return model;
    }

    /** @return the board screen of the current game, or {@code null} before the first game */
    public GameBoardView view() {
        return view;
    }
}
