package Model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The square effects of one move and its chain: the <i>invoker</i> of the {@link SquareEffect} Command pattern
 * (FR-34, FR-38, FR-39, FR-41, FR-42; SDP risk R2). Task T2-10, owner: Shafeek Saif.
 *
 * <p>How it is used:
 * <ol>
 *   <li>After a dice move, {@link #start} begins a new chain. If the piece landed on a question or surprise
 *       square, that square waits in the queue. A capture on a question square has already happened in the move
 *       itself, so it comes before the question and no answer can undo it (FR-38).</li>
 *   <li>{@link #next} tells which square waits and which piece is on it, so the turn can ask the question or
 *       draw the surprise.</li>
 *   <li>The answer or outcome becomes a command, and {@link #run} executes it through the {@link EffectMover}.
 *       When the command moves a piece onto another special square, that square joins the queue (FR-42).
 *       {@link #skip} resolves a square whose outcome moves no piece.</li>
 *   <li>The chain is over when {@link #isEmpty} is {@code true}.</li>
 * </ol>
 *
 * <p><b>Chain guard</b> (R2: no wrong order and no endless chain):
 * <ul>
 *   <li>Squares are resolved one at a time, first reached, first resolved.</li>
 *   <li>Only a real movement activates a square. A command that moves zero steps leaves the piece where it was
 *       and does not activate that square again (FR-42, staff answer Q81). Jumps to the castle or the finish
 *       never reach a special square.</li>
 *   <li>Each special square is activated at most once in a chain, whichever piece reaches it. A chain therefore
 *       has at most as many effects as the board has special squares (8 on Medium, 12 on Hard), so it always
 *       ends. The next dice move starts a new chain.</li>
 * </ul>
 */
public final class EffectQueue {

    /**
     * A special square that was reached and waits for its effect.
     *
     * @param piece  the piece that reached the square
     * @param square the question or surprise square
     */
    public record Activation(Piece piece, Square square) {

        /** @throws NullPointerException if an argument is null */
        public Activation {
            Objects.requireNonNull(piece, "piece");
            Objects.requireNonNull(square, "square");
        }

        /** @return {@link SquareType#QUESTION} or {@link SquareType#SURPRISE} */
        public SquareType type() {
            return square.type();
        }
    }

    private final EffectMover mover;
    private final Deque<Activation> pending = new ArrayDeque<>();
    /** Special squares activated in the current chain (the chain guard). */
    private final Set<Cell> activated = new HashSet<>();
    /** Results of the commands run in the current chain, in order. */
    private final List<MoveResult> results = new ArrayList<>();

    /**
     * @param mover the receiver that the commands use
     * @throws NullPointerException if {@code mover} is null
     */
    public EffectQueue(EffectMover mover) {
        this.mover = Objects.requireNonNull(mover, "mover");
    }

    /**
     * Starts the chain of a dice move: forgets the previous chain and queues the square the move landed on if it
     * is a question or surprise square (FR-34, FR-39).
     *
     * @param move the result of the dice move
     * @throws NullPointerException if {@code move} is null
     * @throws IllegalStateException if squares of the previous chain still wait; nothing changes
     */
    public void start(MoveResult move) {
        Objects.requireNonNull(move, "move");
        if (!pending.isEmpty()) {
            throw new IllegalStateException("The effects of the previous move are not resolved yet: " + pending);
        }
        activated.clear();
        results.clear();
        reach(move);
    }

    /** @return {@code true} when no square waits for its effect, so the chain is over */
    public boolean isEmpty() {
        return pending.isEmpty();
    }

    /** @return the square whose effect comes next, or {@code null} when the chain is over */
    public Activation next() {
        return pending.peek();
    }

    /**
     * Runs the command for the square returned by {@link #next}, then queues the next special square that the
     * command moved a piece onto (FR-42).
     *
     * @param effect the command made from the answer or the surprise outcome
     * @return the result of the command
     * @throws NullPointerException if {@code effect} is null
     * @throws IllegalStateException if no square waits; if the command fails, the square still waits
     */
    public MoveResult run(SquareEffect effect) {
        Objects.requireNonNull(effect, "effect");
        if (pending.isEmpty()) {
            throw new IllegalStateException("No square is waiting for an effect");
        }
        MoveResult result = Objects.requireNonNull(effect.execute(mover), "the effect returned no result");
        pending.remove();
        results.add(result);
        reach(result);
        return result;
    }

    /**
     * Resolves the square returned by {@link #next} without moving a piece: a surprise outcome that moves none, or
     * an opponent push-back with no opponent on a shared track (FR-40).
     *
     * @return the square that was resolved
     * @throws IllegalStateException if no square waits
     */
    public Activation skip() {
        if (pending.isEmpty()) {
            throw new IllegalStateException("No square is waiting for an effect");
        }
        return pending.remove();
    }

    /** @return the results of the commands run in the current chain, in order; cannot be modified */
    public List<MoveResult> results() {
        return Collections.unmodifiableList(results);
    }

    /** Queues the square a movement reached, if it is a special square that this chain has not activated. */
    private void reach(MoveResult move) {
        Square square = move.landedOn();
        boolean moved = move.kind() == MoveResult.Kind.STEPS && move.fromProgress() != move.toProgress();
        if (!moved || square == null || !isSpecial(square)) {
            return; // zero steps do not activate a square (Q81); jumps reach no special square
        }
        if (activated.add(square.cell())) {
            pending.add(new Activation(move.piece(), square));
        }
    }

    private static boolean isSpecial(Square square) {
        return square.type() == SquareType.QUESTION || square.type() == SquareType.SURPRISE;
    }
}
