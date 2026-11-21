package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

/**
 * The game model facade used by controllers and views. It holds the immutable settings (FR-09), the board,
 * the players and the rule objects, and it notifies {@link GameListener}s (Observer pattern, NFR-09).
 * Views read from it and never change it (NFR-08). Controllers change it only through {@link #roll()} and
 * {@link #move(Piece)}, and, while a special square waits ({@link TurnPhase#RESOLVING}), through
 * {@link #applyAnswer} and {@link #applySurprise}.
 *
 * <p><b>Special squares (T2-10 part 2, Shafeek Saif).</b> A move that stops on a question or surprise square
 * leaves the turn in {@code RESOLVING} and fires {@code EFFECT_PENDING}. The controller then shows the square:
 * {@link #pendingQuestion()} and {@link #judgeAnswer(int)} for a question (T2-08), {@link #pendingSurprise()} and
 * {@link #resolveSurprise} for a surprise (T2-09). Those only read and judge. {@link #applyAnswer} and
 * {@link #applySurprise} carry the effect out through the effect queue (T2-10), which may reach the next special
 * square (FR-42). The active player answers and chooses, whoever owns the piece that reached the square (FR-34,
 * FR-40).
 *
 * <p>Shared contract, frozen at checkpoint CP1. Owner: Rakan Ziyan.
 */
public final class GameModel {

    private final GameSettings settings;
    private final Board board;
    private final List<Player> players;
    private final TurnManager turns;
    private final RankingTracker ranking;
    private final GameClock clock;
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();
    /** The question creators of this game, one per level (T2-08). */
    private final Map<Difficulty, QuestionFactory> questions;
    /** The surprise creator of this game (T2-09). */
    private final SurpriseFactory surprises;
    /** The waiting square that {@link #question} or {@link #surprise} was made for. */
    private EffectQueue.Activation preparedFor;
    private Question question;
    private SurprisePrompt surprise;

    /**
     * Creates a game with random dice and the system clock.
     *
     * @param settings validated game settings
     * @param random   random source for the board and the dice
     */
    public GameModel(GameSettings settings, Random random) {
        this(settings, random, new RandomDice(random), System::currentTimeMillis);
    }

    /**
     * Creates a game with an explicit dice source and clock (used by tests).
     *
     * @param settings   validated game settings
     * @param random     random source for the board
     * @param dice       the dice
     * @param timeSource the time source of the game clock
     */
    public GameModel(GameSettings settings, Random random, Dice dice, LongSupplier timeSource) {
        this(settings, random, dice, timeSource, StandardMoveEngine::new);
    }

    /** Creates the move engine of a game; lets tests replace the engine (package-private, not a contract). */
    @FunctionalInterface
    interface EngineFactory {
        MoveEngine create(Board board, List<Player> players, SquareRules squareRules, HomePathRules homePathRules);
    }

    /**
     * Creates a game with an explicit move engine. Package-private: used by tests of the turn flow.
     */
    GameModel(GameSettings settings, Random random, Dice dice, LongSupplier timeSource, EngineFactory engines) {
        this.settings = settings;
        this.board = BoardFactory.create(settings.difficulty(), random);
        List<Player> list = new ArrayList<>();
        for (GameSettings.PlayerSetup p : settings.players()) {
            list.add(new Player(p.name(), p.color(), board.finishIndex()));
        }
        this.players = Collections.unmodifiableList(list);
        SquareRules squareRules = new StandardSquareRules(board, players);
        HomePathRules homePathRules = new StandardHomePathRules(board);
        MoveEngine moveEngine = engines.create(board, players, squareRules, homePathRules);
        this.ranking = new StandardRankingTracker(players);
        EffectQueue effects = new EffectQueue(new EffectMover(board, players, squareRules, homePathRules,
                moveEngine));
        this.turns = new StandardTurnManager(players, settings.difficulty(), dice, moveEngine, ranking, effects);
        this.clock = new GameClock(timeSource);
        // Same random source as the board: a seeded game asks the same questions and surprises (NFR-13).
        this.questions = QuestionFactory.forBank(QuestionBank.ofCourseQuestions(), random);
        this.surprises = new SurpriseFactory(board, players, random);
    }

    // ------------------------------------------------------------------ observer

    /** @param listener a listener notified after every change */
    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    /** @param listener a listener to remove */
    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    private void fire(GameEvent event) {
        for (GameListener l : listeners) {
            l.onGameEvent(event);
        }
    }

    // ------------------------------------------------------------------ commands (controller only)

    /**
     * Rolls for the active player (FR-15, FR-16) and notifies the listeners with {@code ROLLED}. If the turn
     * cannot go on, it ends here (Task 12): a cancelled third 6 (FR-22), or no legal move and no extra roll
     * (FR-23), fires {@code TURN_PASSED} with the message to show, then {@code TURN_CHANGED}.
     *
     * @return the roll
     */
    public DiceRoll roll() {
        Player roller = turns.current();
        DiceRoll roll = turns.roll();
        fire(new GameEvent(GameEvent.Type.ROLLED, roller, roll, null, null));
        if (turns.legalMoves().isEmpty() && !turns.hasBonusRoll()) {
            String message = turns.endTurn();
            fire(new GameEvent(GameEvent.Type.TURN_PASSED, roller, roll, null, message));
            afterTurnEnded();
        }
        return roll;
    }

    /**
     * Moves a highlighted piece (FR-20) and notifies the listeners with {@code MOVED}, then
     * {@code PLAYER_FINISHED} if the mover has finished (FR-43) and {@code GAME_OVER} at the end (FR-44).
     * A move that stops on a question or surprise square fires {@code EFFECT_PENDING} and the turn waits for it
     * (T2-10). Otherwise, without an extra roll (FR-21), the turn passes and {@code TURN_CHANGED} follows.
     *
     * @param piece a piece contained in {@link #legalMoves()}
     * @return the result of the move
     */
    public MoveResult move(Piece piece) {
        Player mover = turns.current();
        boolean finishedBefore = ranking.hasFinished(mover);
        MoveResult result = turns.move(piece);
        fire(new GameEvent(GameEvent.Type.MOVED, mover, null, result, null));
        if (!finishedBefore && ranking.hasFinished(mover)) {
            fire(new GameEvent(GameEvent.Type.PLAYER_FINISHED, mover, null, result, null));
        }
        continueTurn(result);
        return result;
    }

    /** After a move or an effect: the end of the game, the next special square, another roll or the next turn. */
    private void continueTurn(MoveResult result) {
        if (turns.phase() == TurnPhase.GAME_OVER) {
            fire(new GameEvent(GameEvent.Type.GAME_OVER, turns.current(), null, result, null));
        } else if (turns.phase() == TurnPhase.RESOLVING) {
            fire(new GameEvent(GameEvent.Type.EFFECT_PENDING, turns.current(), null, result, null));
        } else if (!turns.hasBonusRoll()) {
            turns.endTurn();
            afterTurnEnded();
        }
    }

    // ------------------------------------------------------------------ special squares (T2-10 part 2)

    /**
     * @return the question or surprise square that waits for its effect, with the piece that reached it, or
     *         {@code null} when none waits (FR-34, FR-39, FR-42)
     */
    public EffectQueue.Activation pendingEffect() {
        return turns.pendingEffect();
    }

    /**
     * The question of the waiting question square, of the square's own level (FR-34, FR-37). It is asked once:
     * calling again returns the same question until it is answered.
     *
     * @return the question to show to the active player
     * @throws IllegalStateException if no question square waits
     */
    public Question pendingQuestion() {
        EffectQueue.Activation waiting = requireWaiting(SquareType.QUESTION);
        if (preparedFor != waiting || question == null) {
            preparedFor = waiting;
            surprise = null;
            question = questions.get(waiting.square().questionLevel()).ask();
        }
        return question;
    }

    /**
     * Judges an answer to {@link #pendingQuestion()} without changing the game (FR-35, FR-36): what the dialog
     * shows before the player continues.
     *
     * @param chosenIndex the answer, 0 to 3, or {@link QuestionResult#NO_ANSWER}
     * @return whether it was right, the right answer and the signed steps
     * @throws IllegalStateException if no question square waits
     */
    public QuestionResult judgeAnswer(int chosenIndex) {
        Question asked = pendingQuestion();
        return questions.get(asked.level()).judge(asked, chosenIndex);
    }

    /**
     * Carries out the answer: the piece that reached the square moves by the signed steps (FR-35), with the
     * movement and blocking rules (FR-41). It may reach the next special square (FR-42). A capture made on the
     * square before the question stays (FR-38).
     *
     * @param result the result from {@link #judgeAnswer(int)} for the waiting question
     * @return the result of the move
     * @throws IllegalStateException if no question square waits
     * @throws IllegalArgumentException if the result is not for the waiting question
     */
    public MoveResult applyAnswer(QuestionResult result) {
        Objects.requireNonNull(result, "result");
        EffectQueue.Activation waiting = requireWaiting(SquareType.QUESTION);
        if (preparedFor != waiting || result.question() != question) {
            throw new IllegalArgumentException("This is not the answer to the waiting question");
        }
        clearPrepared();
        MoveResult moved = turns.runEffect(new MoveStepsEffect(waiting.piece(), result.steps()));
        afterEffect(moved);
        return moved;
    }

    /**
     * The surprise of the waiting surprise square, drawn once (FR-39): calling again returns the same surprise
     * until it is carried out. For a push it lists the opponent pieces the active player may choose (FR-40).
     *
     * @return the surprise to show to the active player
     * @throws IllegalStateException if no surprise square waits
     */
    public SurprisePrompt pendingSurprise() {
        EffectQueue.Activation waiting = requireWaiting(SquareType.SURPRISE);
        if (preparedFor != waiting || surprise == null) {
            preparedFor = waiting;
            question = null;
            surprise = surprises.draw(waiting.piece(), turns.current().color());
        }
        return surprise;
    }

    /**
     * Checks the opponent piece the active player chose for a push against the board as it is now, without
     * changing the game (FR-40).
     *
     * @param prompt the surprise from {@link #pendingSurprise()}
     * @param target the chosen piece
     * @return what must happen
     * @throws IllegalArgumentException if the piece may not be chosen
     */
    public SurpriseResult resolveSurprise(SurprisePrompt prompt, Piece target) {
        return surprises.resolve(prompt, target);
    }

    /**
     * Carries out a surprise (FR-39..FR-42): a move of the piece that reached the square or of the chosen
     * opponent piece through the effect queue, or a change of turn. "Skip the next turn" and "extra roll" fall
     * on the active player, the provisional answer of T2-09 ({@link SurpriseResult#affected()}) proposed on
     * issue #75.
     *
     * @param result the result for the waiting surprise: {@link SurprisePrompt#resolve()} or
     *               {@link #resolveSurprise}
     * @return the result of the move, or {@code null} when no piece moves
     * @throws IllegalStateException if no surprise square waits
     * @throws IllegalArgumentException if the result is not for the waiting surprise
     */
    public MoveResult applySurprise(SurpriseResult result) {
        Objects.requireNonNull(result, "result");
        EffectQueue.Activation waiting = requireWaiting(SquareType.SURPRISE);
        boolean drawnHere = preparedFor == waiting && surprise != null;
        if (result.trigger() != waiting.piece() || result.active() != turns.current().color()
                || drawnHere && (result.outcome() != surprise.outcome()
                || result.regularRoll() != surprise.regularRoll())) {
            throw new IllegalArgumentException("This is not the result of the waiting surprise");
        }
        clearPrepared();
        MoveResult moved = null;
        switch (result.action()) {
            case JUMP_TO_FINISH -> moved = turns.runEffect(new JumpToFinishEffect(result.piece()));
            case RETURN_TO_CASTLE -> moved = turns.runEffect(new ReturnToCastleEffect(result.piece()));
            case MOVE_STEPS -> moved = turns.runEffect(new MoveStepsEffect(result.piece(), result.steps()));
            case SKIP_NEXT_TURN -> {
                turns.skipNextTurn(playerOf(result.affected()));
                turns.skipEffect();
            }
            case EXTRA_ROLL -> {
                turns.grantSurpriseRoll();
                turns.skipEffect();
            }
            case NOTHING -> turns.skipEffect();
        }
        afterEffect(moved);
        return moved;
    }

    private EffectQueue.Activation requireWaiting(SquareType type) {
        EffectQueue.Activation waiting = turns.pendingEffect();
        if (waiting == null) {
            throw new IllegalStateException("No special square is waiting for an effect");
        }
        if (waiting.square().type() != type) {
            throw new IllegalStateException("The waiting square is a " + waiting.square().type()
                    + " square, not a " + type + " square");
        }
        return waiting;
    }

    private void clearPrepared() {
        preparedFor = null;
        question = null;
        surprise = null;
    }

    private void afterEffect(MoveResult moved) {
        if (moved != null) {
            Player owner = playerOf(moved.piece().color());
            fire(new GameEvent(GameEvent.Type.MOVED, owner, null, moved, null));
            if (moved.playerFinished()) {
                fire(new GameEvent(GameEvent.Type.PLAYER_FINISHED, owner, null, moved, null));
            }
        } else {
            fire(GameEvent.stateChanged(turns.current()));
        }
        continueTurn(moved);
    }

    private void afterTurnEnded() {
        for (Player passed : turns.skippedTurns()) {
            fire(new GameEvent(GameEvent.Type.TURN_SKIPPED, passed, null, null,
                    passed.name() + " misses this turn (surprise)."));
        }
        if (turns.phase() == TurnPhase.GAME_OVER) {
            fire(new GameEvent(GameEvent.Type.GAME_OVER, turns.current(), null, null, null));
        } else {
            fire(new GameEvent(GameEvent.Type.TURN_CHANGED, turns.current(), null, null, null));
        }
    }

    // ------------------------------------------------------------------ read-only state (views)

    /** @return the immutable settings of this game (FR-09) */
    public GameSettings settings() {
        return settings;
    }

    /** @return the level of this game */
    public Difficulty difficulty() {
        return settings.difficulty();
    }

    /** @return the board */
    public Board board() {
        return board;
    }

    /** @return the players in setup order (unmodifiable) */
    public List<Player> players() {
        return players;
    }

    /**
     * @param color a colour
     * @return the player with that colour, or {@code null} if no player uses it
     */
    public Player playerOf(PlayerColor color) {
        for (Player p : players) {
            if (p.color() == color) {
                return p;
            }
        }
        return null;
    }

    /** @return the active player (FR-15) */
    public Player currentPlayer() {
        return turns.current();
    }

    /** @return the current turn phase */
    public TurnPhase phase() {
        return turns.phase();
    }

    /** @return the last roll of the current turn, or {@code null} */
    public DiceRoll lastRoll() {
        return turns.lastRoll();
    }

    /** @return the legal moves for the last roll (FR-24) */
    public Map<Piece, Move> legalMoves() {
        return turns.legalMoves();
    }

    /** @return {@code true} if the active player rolls again (FR-21) */
    public boolean hasBonusRoll() {
        return turns.hasBonusRoll();
    }

    /** @return {@code true} when the game has ended (FR-44) */
    public boolean isGameOver() {
        return ranking.isGameOver();
    }

    /** @return the players in finishing order (FR-45) */
    public List<Player> ranking() {
        return ranking.ranking();
    }

    /** @return the game clock (FR-46) */
    public GameClock clock() {
        return clock;
    }

    /** Notifies the listeners that the state changed (for example when a view is first shown). */
    public void refresh() {
        fire(GameEvent.stateChanged(currentPlayer()));
    }
}
