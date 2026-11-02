package model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * Simulated game state for the prototype: turn order, dice, a light subset of the movement
 * rules, rankings and elapsed time. No GUI code (NFR-08).
 *
 * <p>Implemented: castle exit on a regular 6, the movement formula, home path by the regular die,
 * exact finish, extra roll on a 6 (third 6 cancelled), skipping finished players and skip flags,
 * capturing a lone opponent on an unprotected square. Not modelled (prototype scope): Double Blocks,
 * chained special squares and the full set of rules of SRS 3.2.1.5.
 */
public final class GameState {

    /** Observer notified after every change (NFR-09 "Observer"). */
    public interface Listener {
        void stateChanged(GameState state);
    }

    private final GameSettings settings;
    private final Board board;
    private final List<Player> players = new ArrayList<>();
    private final Random random;
    private final QuestionBank.Deck questions;
    private final List<Player> ranking = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private final LongSupplier clock;
    private final long startMillis;
    private long endMillis = -1;

    private int current;
    private TurnPhase phase = TurnPhase.AWAITING_ROLL;
    private DiceRoll lastRoll;
    private int consecutiveSixes;
    private boolean sixBonusPending;
    private boolean surpriseRollPending;
    private int forcedRegular;
    private int forcedSpecial;

    public GameState(GameSettings settings, QuestionBank bank, Random random) {
        this(settings, bank, random, System::currentTimeMillis);
    }

    public GameState(GameSettings settings, QuestionBank bank, Random random, LongSupplier clock) {
        this.settings = settings;
        this.random = random;
        this.clock = clock;
        this.board = BoardFactory.create(settings.difficulty(), random);
        for (GameSettings.PlayerSetup p : settings.players()) {
            players.add(new Player(p.name(), p.color(), board.finishIndex()));
        }
        this.questions = bank.newDeck(random);
        this.startMillis = clock.getAsLong();
    }

    // ------------------------------------------------------------------ observers

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    private void fire() {
        for (Listener l : List.copyOf(listeners)) {
            l.stateChanged(this);
        }
    }

    // ------------------------------------------------------------------ accessors

    public GameSettings settings() {
        return settings;
    }

    public Difficulty difficulty() {
        return settings.difficulty();
    }

    public Board board() {
        return board;
    }

    public List<Player> players() {
        return List.copyOf(players);
    }

    public Player currentPlayer() {
        return players.get(current);
    }

    public Player playerOf(PlayerColor color) {
        return players.stream().filter(p -> p.color() == color).findFirst().orElse(null);
    }

    public Player playerOf(Piece piece) {
        return playerOf(piece.color());
    }

    public TurnPhase phase() {
        return phase;
    }

    public void setPhase(TurnPhase phase) {
        this.phase = phase;
        fire();
    }

    public DiceRoll lastRoll() {
        return lastRoll;
    }

    public long elapsedMillis() {
        return (endMillis >= 0 ? endMillis : clock.getAsLong()) - startMillis;
    }

    public Question nextQuestion(Difficulty level) {
        return questions.next(level);
    }

    public Random random() {
        return random;
    }

    // ------------------------------------------------------------------ dice

    /** Prototype demo aid: the next regular die shows this value. */
    public void forceNextRegular(int value) {
        forcedRegular = value;
        fire();
    }

    /** Test and screenshot hook: fixes both dice of the next roll (special 0 = random). */
    public void forceNextRoll(int regular, int special) {
        forcedRegular = regular;
        forcedSpecial = special;
    }

    public boolean isNextRollForced() {
        return forcedRegular > 0;
    }

    /** Rolls the dice of the level (FR-16, FR-17) and applies the rule of 6 (FR-21, FR-22). */
    public DiceRoll roll() {
        boolean surprise = surpriseRollPending;
        surpriseRollPending = false;
        int regular = forcedRegular > 0 ? forcedRegular : 1 + random.nextInt(6);
        forcedRegular = 0;
        DieType type = surprise ? DieType.ULTRA_BOOST : settings.difficulty().specialDie();
        int special = type == DieType.NONE ? 0
                : forcedSpecial > 0 ? Math.min(forcedSpecial, type.faces()) : 1 + random.nextInt(type.faces());
        forcedSpecial = 0;
        int movement = type == DieType.NONE ? regular : regular * (special - 1);
        boolean cancelled = false;
        if (!surprise) {
            consecutiveSixes = regular == 6 ? consecutiveSixes + 1 : 0;
            cancelled = consecutiveSixes == 3;
            if (cancelled) {
                consecutiveSixes = 0;
            }
            sixBonusPending = regular == 6 && !cancelled;
        }
        lastRoll = new DiceRoll(regular, type, special, movement, surprise, cancelled);
        fire();
        return lastRoll;
    }

    /** True when the active player rolls again (a 6, or the surprise extra roll). */
    public boolean hasBonusRoll() {
        return sixBonusPending || surpriseRollPending;
    }

    public boolean isSurpriseRollPending() {
        return surpriseRollPending;
    }

    public void grantSurpriseRoll() {
        surpriseRollPending = true;
        fire();
    }

    /** Skip-turn surprise: the player misses the next turn and a pending 6 roll is cancelled. */
    public void skipNextTurn(Player player) {
        player.setSkipNextTurn(true);
        sixBonusPending = false;
        fire();
    }

    // ------------------------------------------------------------------ moves

    /** Legal pieces of the active player for the last roll, with their destination (FR-24). */
    public Map<Piece, Integer> legalTargets() {
        Map<Piece, Integer> targets = new LinkedHashMap<>();
        if (lastRoll == null || lastRoll.cancelled()) {
            return targets;
        }
        for (Piece p : currentPlayer().pieces()) {
            int t = target(p, lastRoll);
            if (t >= 0) {
                targets.put(p, t);
            }
        }
        return targets;
    }

    private int target(Piece p, DiceRoll roll) {
        if (p.finished()) {
            return -1;
        }
        if (p.inCastle()) {
            return roll.regular() == 6 ? 0 : -1;
        }
        int steps = p.progress() >= board.homeStartIndex() ? roll.regular() : roll.movement();
        if (steps <= 0) {
            return -1;
        }
        int t = p.progress() + steps;
        return t <= board.finishIndex() ? t : -1;
    }

    /** Moves a legal piece of the active player with the last roll. */
    public MoveResult movePiece(Piece piece) {
        Integer target = legalTargets().get(piece);
        if (target == null) {
            throw new IllegalArgumentException("This piece cannot move with the current roll.");
        }
        if (piece.inCastle()) {
            piece.setProgress(0);
            return finishMove(piece, MoveResult.Kind.EXIT_CASTLE, -1, List.of(board.cellAt(piece.color(), 0)), null);
        }
        return stepTo(piece, target);
    }

    /**
     * Moves a piece by {@code delta} squares for a question or surprise. Backward movement stops at
     * the start square (FR-32); a forward move that would pass the finish does not happen.
     */
    public MoveResult shiftPiece(Piece piece, int delta) {
        if (!piece.onBoard()) {
            return MoveResult.none(piece);
        }
        int target = piece.progress() + delta;
        if (delta < 0) {
            target = Math.max(0, target);
        }
        if (target > board.finishIndex() || target == piece.progress()) {
            return MoveResult.none(piece);
        }
        return stepTo(piece, target);
    }

    public MoveResult sendToCastle(Piece piece) {
        if (piece.inCastle() || piece.finished()) {
            return MoveResult.none(piece);
        }
        Cell from = board.cellAt(piece.color(), piece.progress());
        int fromProgress = piece.progress();
        piece.setProgress(-1);
        fire();
        return new MoveResult(piece, MoveResult.Kind.TO_CASTLE, fromProgress, -1, List.of(), from, List.of(),
                null, false, false, false);
    }

    public MoveResult jumpToFinish(Piece piece) {
        if (!piece.onBoard()) {
            return MoveResult.none(piece);
        }
        Cell from = board.cellAt(piece.color(), piece.progress());
        int fromProgress = piece.progress();
        piece.setProgress(board.finishIndex());
        return finishMove(piece, MoveResult.Kind.TO_FINISH, fromProgress, List.of(), from);
    }

    private MoveResult stepTo(Piece piece, int target) {
        int from = piece.progress();
        List<Cell> cells = new ArrayList<>();
        int dir = target > from ? 1 : -1;
        for (int i = from; ; i += dir) {
            cells.add(board.cellAt(piece.color(), i));
            if (i == target) {
                break;
            }
        }
        piece.setProgress(target);
        return finishMove(piece, MoveResult.Kind.STEPS, from, cells, null);
    }

    private MoveResult finishMove(Piece piece, MoveResult.Kind kind, int from, List<Cell> cells, Cell fromCell) {
        List<MoveResult.Capture> captures = new ArrayList<>();
        Square landed = null;
        if (!piece.finished()) {
            Cell cell = board.cellAt(piece.color(), piece.progress());
            Square square = board.squareAt(cell);
            if (square != null && square.type() != SquareType.HOME_PATH) {
                landed = square;
                if (!square.isProtected()) {
                    List<Piece> opponents = opponentPiecesAt(cell, piece.color());
                    if (opponents.size() == 1) {
                        Piece victim = opponents.get(0);
                        victim.setProgress(-1);
                        captures.add(new MoveResult.Capture(victim, cell));
                    }
                }
            }
        }
        boolean playerFinished = false;
        boolean gameOver = false;
        if (piece.finished()) {
            Player owner = playerOf(piece);
            if (owner.allFinished() && owner.rank() == 0) {
                ranking.add(owner);
                owner.setRank(ranking.size());
                playerFinished = true;
                // A finished player takes no further turns (FR-43), so pending extra rolls are dropped.
                sixBonusPending = false;
                surpriseRollPending = false;
                if (players.stream().filter(p -> p.rank() == 0).count() <= 1) {
                    completeRanking();
                    gameOver = true;
                }
            }
        }
        fire();
        return new MoveResult(piece, kind, from, piece.progress(), cells, fromCell, captures, landed,
                piece.finished(), playerFinished, gameOver);
    }

    private List<Piece> opponentPiecesAt(Cell cell, PlayerColor self) {
        List<Piece> found = new ArrayList<>();
        for (Player p : players) {
            if (p.color() == self) {
                continue;
            }
            for (Piece piece : p.pieces()) {
                if (piece.onBoard() && cell.equals(board.cellAt(piece.color(), piece.progress()))) {
                    found.add(piece);
                }
            }
        }
        return found;
    }

    /** Most advanced opponent piece on a shared track, for the "move an opponent" surprise (FR-40). */
    public Piece leadingOpponentPiece(PlayerColor self) {
        Piece best = null;
        for (Player p : players) {
            if (p.color() == self) {
                continue;
            }
            for (Piece piece : p.pieces()) {
                if (piece.onBoard() && piece.progress() < board.homeStartIndex()
                        && (best == null || piece.progress() > best.progress())) {
                    best = piece;
                }
            }
        }
        return best;
    }

    /** Most advanced piece of a player that is on the board, or null. */
    public Piece leadingPiece(Player player) {
        return player.pieces().stream().filter(Piece::onBoard)
                .max(Comparator.comparingInt(Piece::progress)).orElse(null);
    }

    // ------------------------------------------------------------------ turns and results

    /**
     * Ends the turn and activates the next player in setup order (FR-15), skipping finished
     * players and players hit by the skip-turn surprise.
     *
     * @return players whose turn was skipped
     */
    public List<Player> endTurn() {
        sixBonusPending = false;
        surpriseRollPending = false;
        consecutiveSixes = 0;
        lastRoll = null;
        List<Player> skipped = new ArrayList<>();
        if (phase != TurnPhase.GAME_OVER) {
            for (int guard = 0; guard < players.size() * 2; guard++) {
                current = (current + 1) % players.size();
                Player p = players.get(current);
                if (p.rank() != 0) {
                    continue;
                }
                if (p.skipNextTurn()) {
                    p.setSkipNextTurn(false);
                    skipped.add(p);
                    continue;
                }
                break;
            }
            phase = TurnPhase.AWAITING_ROLL;
        }
        fire();
        return skipped;
    }

    public boolean isGameOver() {
        return phase == TurnPhase.GAME_OVER;
    }

    public List<Player> ranking() {
        return List.copyOf(ranking);
    }

    /** Prototype demo: ends the game now and ranks unfinished players by progress. */
    public List<Player> finishNow() {
        completeRanking();
        fire();
        return ranking();
    }

    private void completeRanking() {
        players.stream()
                .filter(p -> p.rank() == 0)
                .sorted(Comparator.comparingInt(Player::finishedCount).reversed()
                        .thenComparing(Comparator.comparingInt(Player::totalProgress).reversed()))
                .forEach(p -> {
                    ranking.add(p);
                    p.setRank(ranking.size());
                });
        endMillis = clock.getAsLong();
        phase = TurnPhase.GAME_OVER;
    }
}
