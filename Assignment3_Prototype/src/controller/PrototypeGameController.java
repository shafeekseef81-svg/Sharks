package controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import javax.swing.Timer;

import components.Animations;
import components.BoardComponent;
import components.ModalOverlay;
import components.Theme;
import model.DiceRoll;
import model.Difficulty;
import model.GameHistory;
import model.GameRecord;
import model.GameSettings;
import model.GameState;
import model.MoveResult;
import model.Piece;
import model.Player;
import model.Question;
import model.QuestionBank;
import model.Square;
import model.SquareType;
import model.SurpriseOutcome;
import model.TurnPhase;
import view.GameBoardView;
import view.GameBoardView.StatusKind;
import view.QuestionDialog;
import view.SurpriseDialog;

/**
 * Lightweight game flow for the prototype: roll, highlight legal pieces, move, questions,
 * surprises, next turn, results. Rules live in {@link GameState}; this class connects them to the view.
 */
public final class PrototypeGameController {

    private final NavigationController nav;
    private final QuestionBank bank;
    private final GameHistory history;
    private GameState state;
    private GameBoardView view;
    private Timer clock;
    private Random random = new Random();
    private int demoLevel;
    private int demoSurprise;
    private boolean finished;
    private boolean active;
    private QuestionDialog lastQuestion;
    private SurpriseDialog lastSurprise;

    public PrototypeGameController(NavigationController nav, QuestionBank bank, GameHistory history) {
        this.nav = nav;
        this.bank = bank;
        this.history = history;
    }

    public GameBoardView newGame(GameSettings settings, Random random) {
        return newGame(settings, random, System::currentTimeMillis);
    }

    /** Creates a new game and its board view (NFR-02: built within a moment). */
    public GameBoardView newGame(GameSettings settings, Random random, LongSupplier clockSource) {
        stop();
        this.random = random;
        this.finished = false;
        state = new GameState(settings, bank, random, clockSource);
        view = new GameBoardView();
        view.setActions(this::roll, () -> nav.confirmLeaveGame(nav::showMainMenu), this::demoQuestion,
                this::demoSurprise, this::demoForceSix, this::demoResults);
        view.board().setPieceListener(this::pieceClicked);
        view.bind(state);
        state.addListener(s -> view.refresh());
        clock = new Timer(1000, e -> tick());
        clock.start();
        tick();
        active = true;
        startTurn(null, StatusKind.INFO);
        return view;
    }

    /** Stops the clock; pending animations and delayed steps of this game are ignored from now on. */
    public void stop() {
        active = false;
        if (clock != null) {
            clock.stop();
        }
    }

    /** Wraps a delayed step so it only runs while the same game is still active. */
    private Runnable guarded(Runnable action) {
        GameState game = state;
        return () -> {
            if (active && game == state) {
                action.run();
            }
        };
    }

    private void later(int delayMs, Runnable action) {
        Animations.later(delayMs, guarded(action));
    }

    public GameState state() {
        return state;
    }

    public GameBoardView view() {
        return view;
    }

    /** Question dialog opened last (used by the self-test and the screenshot tool). */
    public QuestionDialog lastQuestion() {
        return lastQuestion;
    }

    public SurpriseDialog lastSurprise() {
        return lastSurprise;
    }

    private void tick() {
        view.setElapsed(GameRecord.formatDuration(Duration.ofMillis(state.elapsedMillis())));
    }

    /** Updates the clock label now (the timer does it every second). */
    public void refreshClock() {
        tick();
    }

    private void setPhase(TurnPhase phase) {
        state.setPhase(phase);
        boolean idle = phase == TurnPhase.AWAITING_ROLL;
        view.setControls(idle, idle, idle || phase == TurnPhase.AWAITING_MOVE);
    }

    private boolean isIdle() {
        return state != null && !finished && state.phase() == TurnPhase.AWAITING_ROLL
                && ModalOverlay.openCount() == 0;
    }

    // ------------------------------------------------------------------ turn flow

    private void startTurn(String prefix, StatusKind kind) {
        setPhase(TurnPhase.AWAITING_ROLL);
        view.board().clearTargets();
        view.resetDice();
        Player p = state.currentPlayer();
        StringBuilder text = new StringBuilder();
        if (prefix != null) {
            text.append(prefix).append(' ');
        }
        text.append(p.name()).append(", it's your turn: roll the dice!");
        if (p.pieces().stream().allMatch(piece -> piece.inCastle() || piece.finished())) {
            text.append(" You need a 6 to bring a piece out of the castle.");
        }
        view.setStatus(text.toString(), kind);
    }

    /** Roll Dice (FR-15..FR-17); only the active player can roll. */
    public void roll() {
        if (!isIdle()) {
            return;
        }
        setPhase(TurnPhase.ROLLING);
        DiceRoll r = state.roll();
        int[] pending = {r.hasSpecial() ? 2 : 1};
        Runnable done = guarded(() -> {
            if (--pending[0] == 0) {
                afterRoll(r);
            }
        });
        view.regularDie().roll(r.regular(), done);
        if (r.hasSpecial()) {
            view.specialDie().setType(r.specialType());
            view.specialDie().roll(r.special(), done);
        }
    }

    private void afterRoll(DiceRoll r) {
        view.showRoll(r);
        Player p = state.currentPlayer();
        if (r.cancelled()) {
            setPhase(TurnPhase.RESOLVING);
            view.board().showBanner("Roll cancelled", Theme.WARNING);
            view.setStatus("Third 6 in a row! This roll is cancelled and the turn passes to " + nextName() + ".",
                    StatusKind.WARNING);
            later(1800, this::nextTurn);
            return;
        }
        Map<Piece, Integer> targets = state.legalTargets();
        if (targets.isEmpty()) {
            setPhase(TurnPhase.RESOLVING);
            if (state.hasBonusRoll()) {
                view.setStatus("No piece can move with this roll, but you rolled a 6: roll again!", StatusKind.WARNING);
                later(1300, this::bonusTurn);
            } else {
                String reason;
                if (p.pieces().stream().allMatch(piece -> piece.inCastle() || piece.finished())) {
                    reason = "A 6 is needed to bring a piece out of the castle.";
                } else if (r.movement() == 0) {
                    reason = "This roll gives 0 steps.";
                } else {
                    reason = "No piece can move exactly this many steps.";
                }
                view.setStatus("No legal move. " + reason + " The turn passes to " + nextName() + ".",
                        StatusKind.WARNING);
                later(2000, this::nextTurn);
            }
            return;
        }
        setPhase(TurnPhase.AWAITING_MOVE);
        view.board().setTargets(targets);
        boolean canExit = targets.keySet().stream().anyMatch(Piece::inCastle);
        view.setStatus(canExit
                ? "You rolled a 6! Click a glowing piece: bring a new piece out, or move one on the board."
                : "Click a glowing piece to move it. Point at a piece to preview where it lands.", StatusKind.INFO);
    }

    /** A highlighted piece was clicked (FR-20, FR-24). */
    public void pieceClicked(Piece piece) {
        if (state.phase() != TurnPhase.AWAITING_MOVE || !state.legalTargets().containsKey(piece)) {
            return;
        }
        view.board().clearTargets();
        setPhase(TurnPhase.MOVING);
        MoveResult result = state.movePiece(piece);
        animate(result, () -> afterMove(result));
    }

    private void animate(MoveResult r, Runnable whenDone) {
        Runnable done = guarded(whenDone);
        BoardComponent board = view.board();
        Piece p = r.piece();
        for (MoveResult.Capture c : r.captures()) {
            board.holdPiece(c.piece(), board.cellCenter(c.from()));
        }
        Runnable afterMain = () -> animateCaptures(r.captures(), done);
        switch (r.kind()) {
            case STEPS -> board.animateAlong(p, r.cells(), afterMain);
            case EXIT_CASTLE -> board.animateJump(p, board.castleSpot(p), board.cellCenter(r.cells().get(0)), afterMain);
            case TO_CASTLE -> board.animateJump(p, board.cellCenter(r.fromCell()), board.castleSpot(p), afterMain);
            case TO_FINISH -> board.animateJump(p, board.cellCenter(r.fromCell()), board.finishSpot(p), afterMain);
            default -> done.run();
        }
    }

    private void animateCaptures(List<MoveResult.Capture> captures, Runnable done) {
        if (captures.isEmpty()) {
            done.run();
            return;
        }
        BoardComponent board = view.board();
        int[] left = {captures.size()};
        for (MoveResult.Capture c : captures) {
            board.animateJump(c.piece(), board.cellCenter(c.from()), board.castleSpot(c.piece()), () -> {
                if (--left[0] == 0) {
                    done.run();
                }
            });
        }
    }

    private void afterMove(MoveResult r) {
        announce(r);
        if (r.gameOver()) {
            later(1000, () -> finishGame(false));
            return;
        }
        Square square = r.landedOn();
        if (square != null && square.type() == SquareType.QUESTION) {
            later(250, () -> askQuestion(r.piece(), square.questionLevel(), false, this::continueTurn));
        } else if (square != null && square.type() == SquareType.SURPRISE) {
            later(250, () -> surprise(r.piece(), SurpriseOutcome.draw(random), false, this::continueTurn));
        } else {
            continueTurn();
        }
    }

    private void announce(MoveResult r) {
        Player owner = state.playerOf(r.piece());
        if (!r.captures().isEmpty()) {
            Player victim = state.playerOf(r.captures().get(0).piece());
            view.board().showBanner("Captured!", Theme.DANGER);
            view.setStatus(owner.name() + " captured a piece of " + victim.name() + ". It goes back to its castle.",
                    StatusKind.SUCCESS);
        }
        if (r.playerFinished()) {
            view.board().showBanner(owner.name() + " finished in place " + owner.rank() + "!", Theme.GOLD);
            view.setStatus(owner.name() + " brought all four pieces home and finished in place " + owner.rank() + ".",
                    StatusKind.SUCCESS);
        } else if (r.pieceFinished()) {
            view.board().showBanner("Piece home!", Theme.SUCCESS);
            view.setStatus(owner.name() + " brought a piece to the finish (" + owner.finishedCount() + "/4).",
                    StatusKind.SUCCESS);
        }
    }

    private void continueTurn() {
        if (state.isGameOver()) {
            finishGame(false);
        } else if (state.hasBonusRoll()) {
            bonusTurn();
        } else {
            nextTurn();
        }
    }

    /** Same player rolls again: a 6 (FR-21) or the surprise extra roll. */
    private void bonusTurn() {
        setPhase(TurnPhase.AWAITING_ROLL);
        view.board().clearTargets();
        Player p = state.currentPlayer();
        boolean ultra = state.isSurpriseRollPending();
        view.board().showBanner(ultra ? "Extra roll!" : "Roll again!", Theme.player(p.color()));
        view.setStatus(ultra
                ? p.name() + ", roll again now with the Regular + UltraBoost dice."
                : p.name() + " rolled a 6: roll again!", StatusKind.SUCCESS);
        view.refresh();
    }

    private void nextTurn() {
        List<Player> skipped = state.endTurn();
        if (skipped.isEmpty()) {
            startTurn(null, StatusKind.INFO);
        } else {
            String names = skipped.stream().map(Player::name).collect(Collectors.joining(", "));
            startTurn(names + (skipped.size() == 1 ? " skips" : " skip") + " this turn (surprise effect).",
                    StatusKind.WARNING);
        }
    }

    private String nextName() {
        List<Player> players = state.players();
        int i = players.indexOf(state.currentPlayer());
        for (int k = 1; k <= players.size(); k++) {
            Player p = players.get((i + k) % players.size());
            if (p.rank() == 0) {
                return p.name();
            }
        }
        return state.currentPlayer().name();
    }

    // ------------------------------------------------------------------ questions (FR-34..FR-38)

    private void askQuestion(Piece piece, Difficulty level, boolean demo, Runnable then) {
        setPhase(TurnPhase.RESOLVING);
        Question question = state.nextQuestion(level);
        Player player = state.currentPlayer();
        ModalOverlay[] holder = new ModalOverlay[1];
        lastQuestion = new QuestionDialog(question, player, demo, piece != null && piece.onBoard(), correct -> {
            holder[0].close();
            applyQuestion(piece, level, correct, then);
        });
        holder[0] = nav.overlay(lastQuestion);
    }

    private void applyQuestion(Piece piece, Difficulty level, boolean correct, Runnable then) {
        int steps = level.questionSteps();
        if (piece == null || !piece.onBoard()) {
            view.setStatus(correct ? "Correct answer! (No piece on the board to move.)"
                    : "Wrong answer. (No piece on the board to move.)", correct ? StatusKind.SUCCESS : StatusKind.WARNING);
            then.run();
            return;
        }
        MoveResult r = state.shiftPiece(piece, correct ? steps : -steps);
        if (r.kind() == MoveResult.Kind.NONE) {
            view.setStatus(correct ? "Correct! The piece cannot go further forward (an exact finish is needed)."
                    : "Wrong answer, but the piece is already on its start square.", correct ? StatusKind.SUCCESS
                    : StatusKind.WARNING);
            then.run();
            return;
        }
        int moved = Math.abs(r.toProgress() - r.fromProgress());
        view.setStatus(correct ? "Correct! The piece moves " + moved + (moved == 1 ? " step" : " steps") + " forward."
                : "Wrong answer. The piece moves " + moved + (moved == 1 ? " step" : " steps") + " back.",
                correct ? StatusKind.SUCCESS : StatusKind.WARNING);
        animate(r, () -> {
            announce(r);
            if (r.gameOver()) {
                finishGame(false);
            } else {
                then.run();
            }
        });
    }

    // ------------------------------------------------------------------ surprises (FR-39..FR-42)

    private void surprise(Piece piece, SurpriseOutcome outcome, boolean demo, Runnable then) {
        setPhase(TurnPhase.RESOLVING);
        Player player = state.currentPlayer();
        boolean hasPiece = piece != null && piece.onBoard();
        String detail = null;
        String note = null;
        Piece victim = null;
        int back = 0;
        switch (outcome) {
            case OPPONENT_BACK -> {
                victim = state.leadingOpponentPiece(player.color());
                int rolled = 1 + random.nextInt(6);
                back = (rolled + 1) / 2;
                detail = victim == null
                        ? "No opponent piece is on a shared track, so this surprise is cancelled."
                        : "Regular die: " + rolled + ". The leading piece of " + state.playerOf(victim).name()
                        + " moves back " + back + (back == 1 ? " square." : " squares.");
                note = "Prototype: the opponent piece is chosen automatically (the most advanced one).";
            }
            case JUMP_TO_FINISH, BACK_TO_CASTLE, BACK_THREE -> {
                if (!hasPiece) {
                    note = "Prototype demo: " + player.name() + " has no piece on the board, so nothing moves.";
                }
            }
            case SKIP_TURN -> {
                if (demo) {
                    note = "Prototype demo: " + player.name() + " will miss the next turn.";
                }
            }
            default -> {
                // extra roll: no extra text
            }
        }
        Piece fVictim = victim;
        int fBack = back;
        ModalOverlay[] holder = new ModalOverlay[1];
        lastSurprise = new SurpriseDialog(outcome, player, detail, note, demo, () -> {
            holder[0].close();
            applySurprise(outcome, player, hasPiece ? piece : null, fVictim, fBack, then);
        });
        holder[0] = nav.overlay(lastSurprise);
    }

    private void applySurprise(SurpriseOutcome outcome, Player player, Piece piece, Piece victim, int back,
                               Runnable then) {
        switch (outcome) {
            case JUMP_TO_FINISH -> {
                if (piece == null) {
                    then.run();
                    return;
                }
                MoveResult r = state.jumpToFinish(piece);
                view.setStatus("The piece jumps straight to the finishing square!", StatusKind.SUCCESS);
                animate(r, () -> {
                    announce(r);
                    if (r.gameOver()) {
                        finishGame(false);
                    } else {
                        then.run();
                    }
                });
            }
            case OPPONENT_BACK -> {
                if (victim == null) {
                    view.setStatus("No opponent piece on a shared track: the surprise is cancelled.", StatusKind.INFO);
                    then.run();
                    return;
                }
                MoveResult r = state.shiftPiece(victim, -back);
                int moved = Math.abs(r.toProgress() - r.fromProgress());
                view.setStatus(state.playerOf(victim).name() + "'s piece moves back " + moved
                        + (moved == 1 ? " square." : " squares."), StatusKind.SUCCESS);
                animate(r, () -> {
                    announce(r);
                    then.run();
                });
            }
            case BACK_TO_CASTLE -> {
                if (piece == null) {
                    then.run();
                    return;
                }
                MoveResult r = state.sendToCastle(piece);
                view.setStatus("The piece goes back to its castle.", StatusKind.WARNING);
                animate(r, then);
            }
            case BACK_THREE -> {
                if (piece == null) {
                    then.run();
                    return;
                }
                MoveResult r = state.shiftPiece(piece, -3);
                int moved = Math.abs(r.toProgress() - r.fromProgress());
                view.setStatus(moved == 0 ? "The piece is already on its start square."
                        : "The piece moves " + moved + (moved == 1 ? " step" : " steps") + " back.", StatusKind.WARNING);
                animate(r, () -> {
                    announce(r);
                    then.run();
                });
            }
            case SKIP_TURN -> {
                state.skipNextTurn(player);
                view.setStatus(player.name() + " will skip the next turn.", StatusKind.WARNING);
                then.run();
            }
            case EXTRA_ROLL -> {
                state.grantSurpriseRoll();
                then.run();
            }
        }
    }

    // ------------------------------------------------------------------ prototype demo shortcuts

    /** Demo: opens a question for the active player (levels cycle Easy, Medium, Hard). Easy boards have none. */
    public void demoQuestion() {
        if (!isIdle() || state.difficulty().questionSquares() == 0) {
            return;
        }
        Difficulty level = Difficulty.values()[demoLevel++ % 3];
        askQuestion(state.leadingPiece(state.currentPlayer()), level, true, this::resumeAfterDemo);
    }

    /** Demo: triggers a surprise for the active player (the six outcomes in turn). Easy boards have none. */
    public void demoSurprise() {
        if (!isIdle() || state.difficulty().surpriseSquares() == 0) {
            return;
        }
        SurpriseOutcome outcome = SurpriseOutcome.values()[demoSurprise++ % SurpriseOutcome.values().length];
        surprise(state.leadingPiece(state.currentPlayer()), outcome, true, this::resumeAfterDemo);
    }

    /** Demo: the next regular die shows a 6, so a piece can leave the castle. */
    public void demoForceSix() {
        if (!isIdle()) {
            return;
        }
        state.forceNextRegular(6);
        view.setStatus("Prototype demo: the next roll of the regular die will show a 6. Press Roll Dice.",
                StatusKind.INFO);
    }

    /** Demo: ends the game now and opens the results screen. */
    public void demoResults() {
        if (!isIdle()) {
            return;
        }
        finishGame(true);
    }

    private void resumeAfterDemo() {
        if (state.isGameOver()) {
            finishGame(false);
            return;
        }
        if (state.isSurpriseRollPending()) {
            bonusTurn();
            return;
        }
        setPhase(TurnPhase.AWAITING_ROLL);
        view.board().clearTargets();
        view.refresh();
    }

    private void finishGame(boolean simulated) {
        if (finished) {
            return;
        }
        finished = true;
        stop();
        List<Player> ranking = simulated && !state.isGameOver() ? state.finishNow() : state.ranking();
        Duration duration = Duration.ofMillis(state.elapsedMillis());
        GameRecord record = history.add(state.difficulty(), ranking.stream().map(Player::name).toList(), duration);
        nav.showResults(ranking, state.difficulty(), duration, simulated, record);
    }
}
