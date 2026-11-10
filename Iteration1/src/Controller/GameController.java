package Controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.Timer;

import Model.DiceRoll;
import Model.GameClock;
import Model.GameEvent;
import Model.GameModel;
import Model.GameSettings;
import Model.Move;
import Model.MoveResult;
import Model.Piece;
import Model.Player;
import View.GameBoardView;
import View.GameBoardView.StatusKind;
import View.components.Animations;
import View.components.BoardComponent;
import View.components.Theme;

/**
 * Connects the game board screen to the game model (task N02, integration). It calls the model, runs the dice
 * and piece animations in order, and shows the model's messages. It contains no game rules: legal moves,
 * extra rolls, the rule of 6, captures, finishing and ranking are all decided in the {@code Model} package
 * (NFR-08). The model also ends turns by itself and reports it with {@link GameEvent.Type#TURN_PASSED}.
 *
 * <p>Owner: Rakan Ziyan. All methods run on the Swing event thread.
 */
public final class GameController {

    /** Pause that lets the players read a "turn passes" message before the next turn starts. */
    static final int PASS_DELAY_MS = 1800;
    /** Pause after the last move before the results screen. */
    static final int GAME_OVER_DELAY_MS = 1000;

    private final NavigationController nav;
    private GameModel model;
    private GameBoardView view;
    private Timer clockTimer;
    /** True while the dice or a piece are animated, so clicks and rolls are ignored (FR-15, FR-20). */
    private boolean busy;
    /** Message of the last {@link GameEvent.Type#TURN_PASSED} event (FR-22, FR-23), or null. */
    private String passMessage;
    /** Increases with every new game, so delayed actions of an old game do nothing. */
    private int generation;

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
        generation++;
        busy = false;
        passMessage = null;
        model = new GameModel(settings, random);
        view = new GameBoardView();
        view.setActions(this::roll, () -> nav.confirmLeaveGame(nav::showMainMenu));
        view.board().setPieceListener(this::pieceSelected);
        view.bind(model);
        model.addListener(this::onModelEvent);
        model.clock().start();
        clockTimer = new Timer(1000, e -> showClock());
        clockTimer.start();
        showClock();
        startTurn();
        return view;
    }

    /** Stops the running game: the clock and any pending delayed action. */
    public void stop() {
        generation++;
        if (clockTimer != null) {
            clockTimer.stop();
            clockTimer = null;
        }
        if (model != null) {
            model.clock().stop();
        }
    }

    private void onModelEvent(GameEvent event) {
        if (event.type() == GameEvent.Type.TURN_PASSED) {
            passMessage = event.message();
        }
        view.refresh();
    }

    private void showClock() {
        if (model != null && view != null) {
            view.setElapsed(GameClock.format(model.clock().elapsedMillis()));
        }
    }

    // ------------------------------------------------------------------ roll (FR-15..FR-17, FR-21..FR-23)

    /** Roll button or Space key (FR-15). Ignored while busy, after the game, or while a move must be chosen. */
    void roll() {
        if (busy || model.isGameOver() || !model.legalMoves().isEmpty()) {
            return;
        }
        busy = true;
        view.setControls(false, true);
        Player roller = model.currentPlayer();
        passMessage = null;
        DiceRoll roll;
        try {
            roll = model.roll();
        } catch (UnsupportedOperationException e) {
            // Medium and Hard dice come in Iteration 2; the setup screen should offer Easy only (issue #13).
            busy = false;
            view.setStatus("This level is not playable in Iteration 1. Please start an Easy game.",
                    StatusKind.ERROR);
            return;
        }
        int game = generation;
        view.regularDie().roll(roll.regular(), () -> {
            if (game == generation) {
                afterRoll(roller, roll);
            }
        });
    }

    private void afterRoll(Player roller, DiceRoll roll) {
        view.showRoll(roll);
        if (passMessage != null) {
            // The model already ended the turn: a cancelled third 6 (FR-22) or no legal move (FR-23).
            if (roll.cancelled()) {
                view.board().showBanner("Roll cancelled", Theme.WARNING);
            }
            view.setStatus(passMessage + " Next: " + model.currentPlayer().name() + ".", StatusKind.WARNING);
            later(PASS_DELAY_MS, this::startTurn);
            return;
        }
        Map<Piece, Move> moves = model.legalMoves();
        if (moves.isEmpty()) {
            // A 6 without a legal move still gives another roll (FR-21).
            busy = false;
            view.setControls(true, true);
            view.setStatus(roller.name() + ": no piece can move, but you rolled a 6. Roll again!",
                    StatusKind.WARNING);
            return;
        }
        Map<Piece, Integer> targets = new LinkedHashMap<>();
        for (Map.Entry<Piece, Move> e : moves.entrySet()) {
            targets.put(e.getKey(), e.getValue().targetProgress());
        }
        view.board().setTargets(targets);
        view.board().setInteractive(true);
        busy = false;
        boolean canExit = targets.keySet().stream().anyMatch(Piece::inCastle);
        view.setStatus(roller.name() + (canExit
                ? ": you rolled a 6. Click a glowing piece: bring one out, or move one on the board."
                : ": click a glowing piece to move it."), StatusKind.INFO);
    }

    // ------------------------------------------------------------------ move (FR-20, FR-24, FR-26, FR-31)

    /** A highlighted piece was clicked (FR-20, FR-24). Pieces without a legal move are ignored. */
    void pieceSelected(Piece piece) {
        if (busy || model.isGameOver() || !model.legalMoves().containsKey(piece)) {
            return;
        }
        busy = true;
        BoardComponent board = view.board();
        board.clearTargets();
        board.setInteractive(false);
        Player mover = model.currentPlayer();
        MoveResult result = model.move(piece);
        int game = generation;
        animate(result, () -> {
            if (game == generation) {
                afterMove(mover, result);
            }
        });
    }

    private void animate(MoveResult r, Runnable done) {
        BoardComponent board = view.board();
        Piece p = r.piece();
        for (MoveResult.Capture c : r.captures()) {
            board.holdPiece(c.piece(), board.cellCenter(c.from()));
        }
        Runnable afterMain = () -> animateCaptures(r, done);
        switch (r.kind()) {
            case STEPS -> board.animateAlong(p, r.cells(), afterMain);
            case EXIT_CASTLE -> board.animateJump(p, board.castleSpot(p), board.cellCenter(r.cells().get(0)),
                    afterMain);
            case TO_CASTLE -> board.animateJump(p, board.cellCenter(r.fromCell()), board.castleSpot(p), afterMain);
            case TO_FINISH -> board.animateJump(p, board.cellCenter(r.fromCell()), board.finishSpot(p), afterMain);
            default -> afterMain.run();
        }
    }

    private void animateCaptures(MoveResult r, Runnable done) {
        if (r.captures().isEmpty()) {
            done.run();
            return;
        }
        BoardComponent board = view.board();
        int[] left = {r.captures().size()};
        for (MoveResult.Capture c : r.captures()) {
            board.animateJump(c.piece(), board.cellCenter(c.from()), board.castleSpot(c.piece()), () -> {
                if (--left[0] == 0) {
                    done.run();
                }
            });
        }
    }

    private void afterMove(Player mover, MoveResult r) {
        view.refresh();
        String news = announce(mover, r);
        if (r.gameOver() || model.isGameOver()) {
            view.setStatus(news + "Game over!", StatusKind.SUCCESS);
            later(GAME_OVER_DELAY_MS, this::finishGame);
            return;
        }
        if (model.currentPlayer() == mover) {
            // Same player again: the move was made with a 6 (FR-21).
            view.board().showBanner("Roll again!", Theme.player(mover.color()));
            startTurn(news + mover.name() + " rolled a 6: roll again!", StatusKind.SUCCESS);
        } else {
            startTurn(news, news.isEmpty() ? StatusKind.INFO : StatusKind.SUCCESS);
        }
    }

    /** Banner and text for a capture or a finish; the decision itself was made by the model. */
    private String announce(Player mover, MoveResult r) {
        StringBuilder text = new StringBuilder();
        if (!r.captures().isEmpty()) {
            view.board().showBanner("Captured!", Theme.DANGER);
            text.append(mover.name()).append(" captured a piece; it goes back to its castle. ");
        }
        if (r.playerFinished()) {
            view.board().showBanner(mover.name() + " finished in place " + mover.rank() + "!", Theme.GOLD);
            text.append(mover.name()).append(" brought all four pieces home (place ").append(mover.rank())
                    .append("). ");
        } else if (r.pieceFinished()) {
            view.board().showBanner("Piece home!", Theme.SUCCESS);
            text.append(mover.name()).append(" brought a piece home (").append(mover.finishedCount())
                    .append("/4). ");
        }
        return text.toString();
    }

    // ------------------------------------------------------------------ turns and end of game

    private void startTurn() {
        startTurn("", StatusKind.INFO);
    }

    private void startTurn(String prefix, StatusKind kind) {
        busy = false;
        passMessage = null;
        view.board().clearTargets();
        view.board().setInteractive(false);
        view.resetDice();
        view.setControls(true, true);
        view.setStatus(prefix + model.currentPlayer().name() + ", roll the dice.", kind);
        view.refresh();
    }

    private void finishGame() {
        stop();
        nav.showResults(model.ranking(), model.difficulty(), GameClock.format(model.clock().elapsedMillis()));
    }

    private void later(int delayMs, Runnable action) {
        int game = generation;
        Animations.later(delayMs, () -> {
            if (game == generation) {
                action.run();
            }
        });
    }

    /** @return the model of the current game, or {@code null} before the first game */
    public GameModel model() {
        return model;
    }

    /** @return the board screen of the current game, or {@code null} before the first game */
    public GameBoardView view() {
        return view;
    }

    /** @return {@code true} while the dice or a piece are being animated */
    boolean isBusy() {
        return busy;
    }
}
