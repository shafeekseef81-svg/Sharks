# Command: question and surprise effects (Shafeek Saif)

Section for `iteration2_dp_Sharks` (task N2-05, #71). Pattern implemented in task T2-10 (#61). **Draft:** part 1 (commands, receiver, queue) is done. Part 2 connects the queue to the turn (`RESOLVING`, T2-13), the questions (T2-08) and the surprise outcomes (T2-09). This section will be updated when part 2 is done.

## 1. The pattern
**Command** (GoF, behavioural): a request becomes an object. The code that decides *what* to do (the client) creates the command. Another object (the invoker) decides *when* to run it, and the command carries the request out on the object that does the work (the receiver).

## 2. The problem it solves
When a piece lands on a question or surprise square, an effect starts: a right or wrong answer moves the piece 1-3 steps (FR-35), or a surprise moves it back, home, to the finish, or moves an opponent (SRS Table 2). Three rules make this harder than a single method call:

- **Rules apply to effects as well (FR-41).** An effect obeys Double Blocks in both directions, stops at the starting square when it moves back (FR-32), and captures or is protected like any move (FR-26, FR-27). The jump to the finish is the only exception.
- **Chains (FR-42).** An effect can move a piece onto another special square, which starts the next effect. A move of zero steps must not start the same square again (Q81).
- **Order and an end (SDP risk R2).** Effects must run one at a time, in the order the squares are reached, and a chain must never go on forever.

If every effect were written inside the turn logic, each one would repeat these checks. Some effects also wait for the player (an answer, or the choice of an opponent), so the effect is known only *after* the square was reached.

**Why Command:** each effect becomes a small immutable object that only says *what* to do (which piece, how many steps). One invoker runs every command in the same way. It checks the result, and it queues the next special square under one chain guard. The movement rules sit in one receiver, so all effects share them. A command can be created when the answer is known and run then. It can also be shown to the players or logged.

## 3. Structure

```
   Client                         Invoker                              Receiver
 (answer, surprise          EffectQueue                           EffectMover
  outcome, turn)       ───────────────────────────            ───────────────────────────
      │  creates        start(diceMove)                        moveSteps(piece, ±steps)
      │                 next(): Activation  (piece, square)    returnToCastle(piece)
      ▼                 run(effect) ──► effect.execute(mover)  jumpToFinish(piece)
  «interface»           skip()                                 rules: Double Block both ways,
  SquareEffect          chain guard: one square at a time,     stop at start (FR-32), exact
  execute(mover):       zero steps start nothing,              finish (FR-31), capture and
    MoveResult          each square once per chain             protection (FR-26, FR-27)
      ▲      ▲      ▲
      │      │      │
 MoveStepsEffect  ReturnToCastleEffect  JumpToFinishEffect      (concrete commands)
```

Flow of one chain: a dice move lands on a question square → `start` queues it → the player answers → the client creates `new MoveStepsEffect(piece, ±steps)` → `run` executes it → the piece reaches a surprise square → it is queued → … → `isEmpty()` ends the chain.

**Chain guard** (in `EffectQueue`):
- squares are resolved first come, first served;
- only a real movement starts a square, so zero steps start nothing (Q81);
- each special square starts at most once in a chain.

A chain therefore has at most as many effects as the board has special squares: 8 on Medium, 12 on Hard. The next dice move starts a new chain.

## 4. Where it is in the code (`Iteration2/src/Model/`)

| Role | File |
|---|---|
| Command | `SquareEffect.java` (interface; its Javadoc names the pattern and the roles) |
| Concrete commands | `MoveStepsEffect.java`, `ReturnToCastleEffect.java`, `JumpToFinishEffect.java` |
| Receiver | `EffectMover.java`: the movement rules for effects. It uses `MoveEngine.isPassable` (T2-06), so a Double Block means the same for effects as for dice moves. |
| Invoker | `EffectQueue.java`: the queue of reached squares (`Activation`), `run` / `skip`, and the chain guard |
| Client | Part 2: the answer to a question (T2-08), the surprise outcomes (T2-09) and the turn in its `RESOLVING` state (T2-13) |

## 5. How it was tested
`SquareEffectTest` has 19 tests on a Medium board with the real board, the real rules and scripted dice.
- **Receiver:**
  - forward and backward steps, with the cells in the order they are passed;
  - stop at the starting square;
  - an opponent block stops the effect from either side, and a block right next to the piece gives no move;
  - the piece crosses or joins its own block;
  - captures forward and backward, and protection on protection and surprise squares;
  - home path and exact finish;
  - the jump to the finish ignores blocks;
  - return to the castle;
  - invalid input.
- **Invoker:**
  - a dice move onto a question square queues it, and its capture comes first and stays (FR-38);
  - a plain square starts nothing;
  - a chain of three squares in order;
  - zero steps start nothing (Q81);
  - each square once per chain, and a new dice move resets the guard;
  - an opponent pushed onto a special square;
  - jumps and `skip` end the chain;
  - misuse is refused, and a failing command leaves the square waiting.
- **Mutation check:** switching off the chain guard, the blocks, the stop at the start square or the captures each makes at least one test fail.
- The independent rule tests are task T2-12 (Rakan Ziyan).

*Screenshots for the Word version: `SquareEffect.java` (interface with Javadoc), `EffectQueue.run` and `reach` (chain guard), and `EffectMover.moveSteps`.*
