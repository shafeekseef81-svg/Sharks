package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

/**
 * The game model facade used by controllers and views. It holds the immutable settings (FR-09), the board,
 * the players and the rule objects, and it notifies {@link GameListener}s (Observer pattern, NFR-09).
 * Views read from it and never change it (NFR-08). Controllers change it only through {@link #roll()} and
 * {@link #move(Piece)}.
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
        this.settings = settings;
        this.board = BoardFactory.create(settings.difficulty(), random);
        List<Player> list = new ArrayList<>();
        for (GameSettings.PlayerSetup p : settings.players()) {
            list.add(new Player(p.name(), p.color(), board.finishIndex()));
        }
        this.players = Collections.unmodifiableList(list);
        SquareRules squareRules = new StandardSquareRules(board, players);
        HomePathRules homePathRules = new StandardHomePathRules(board);
        MoveEngine moveEngine = new StandardMoveEngine(board, players, squareRules, homePathRules);
        this.ranking = new StandardRankingTracker(players);
        this.turns = new StandardTurnManager(players, settings.difficulty(), dice, moveEngine, ranking);
        this.clock = new GameClock(timeSource);
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
     * Rolls for the active player (FR-15, FR-16) and notifies the listeners.
     *
     * @return the roll
     */
    public DiceRoll roll() {
        DiceRoll roll = turns.roll();
        fire(new GameEvent(GameEvent.Type.ROLLED, turns.current(), roll, null, null));
        return roll;
    }

    /**
     * Moves a highlighted piece (FR-20) and notifies the listeners.
     *
     * @param piece a piece contained in {@link #legalMoves()}
     * @return the result of the move
     */
    public MoveResult move(Piece piece) {
        MoveResult result = turns.move(piece);
        fire(new GameEvent(GameEvent.Type.MOVED, turns.current(), null, result, null));
        return result;
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
