package Model;

/**
 * <b>Command</b> design pattern (GoF): one effect of a question or surprise square, as an object that the
 * {@link EffectQueue} runs (FR-35, FR-39..FR-42). Task T2-10, owner: Shafeek Saif.
 *
 * <p><b>Problem.</b> A move that lands on a question or surprise square starts an effect, and the effect can move
 * a piece onto another special square, which starts the next effect (FR-42). The effects must run one at a time,
 * in the order the squares are reached, each with the movement and blocking rules (FR-41), and the chain must
 * end (SDP risk R2). If every effect were a method call inside the turn logic, the order, the rules and the
 * chain guard would be repeated in each effect. As commands, the effects only say <i>what</i> to do; one
 * invoker runs them all in the same way.
 *
 * <p><b>Roles.</b>
 * <ul>
 *   <li><i>Command</i>: this interface.</li>
 *   <li><i>Concrete commands</i>: {@link MoveStepsEffect} (an answer moves the piece forward or back, FR-35; three
 *       steps back; an opponent pushed back, FR-40), {@link ReturnToCastleEffect} and {@link JumpToFinishEffect}
 *       (SRS Table 2).</li>
 *   <li><i>Receiver</i>: {@link EffectMover}. It carries out the movement with the rules of the game.</li>
 *   <li><i>Invoker</i>: {@link EffectQueue}. It keeps the special squares waiting in the order they were
 *       reached, runs each command, and queues the next special square a command reaches (chain guard).</li>
 *   <li><i>Client</i>: the code that turns a player's answer or a surprise outcome into a command: the
 *       questions of T2-08, the surprise outcomes of T2-09 and the turn while it resolves effects (T2-13).</li>
 * </ul>
 *
 * <p>A command is immutable and holds everything it needs (which piece, how many steps), so it can be created
 * when the answer or the outcome is known and run later, and it can be logged or shown to the players.
 */
public interface SquareEffect {

    /**
     * Carries out the effect.
     *
     * @param mover the receiver that moves pieces with the rules of the game
     * @return what happened to the piece, for the view and for the chain check of the {@link EffectQueue}
     */
    MoveResult execute(EffectMover mover);
}
