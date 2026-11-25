# Command: question and surprise effects (Shafeek Saif)

Section for `iteration2_dp_Sharks` (task N2-05, #71). Pattern implemented in task T2-10 (#61): part 1 built the commands, the receiver and the queue; part 2 connected the queue to the turn (`RESOLVING`, T2-13), the questions (T2-08), the surprise outcomes (T2-09) and the cards of T2-11.

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

Flow of one chain: a dice move lands on a question square → `start` queues it and the turn enters `RESOLVING` → the active player answers on the card → the client creates `new MoveStepsEffect(piece, ±steps)` → `run` executes it → the piece reaches a surprise square → it is queued → … → `isEmpty()` ends the chain and the turn goes on (another roll after a 6, or the next player).

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
| Client | `GameModel.applyAnswer` (a `QuestionResult` of T2-08 becomes `MoveStepsEffect`) and `GameModel.applySurprise` (a `SurpriseResult` of T2-09 becomes one of the three commands, or a turn change) |
| Invoker's caller | `StandardTurnManager` in `ResolvingState` (T2-13): a dice move that reaches a special square starts the chain; roll, move and end of turn are refused until it is over; a piece an effect brings home counts for its owner (FR-43) |

Turn surprises are not commands: "skip the next turn" and "extra roll" change the turn, not a piece, so they go to the turn (`skipNextTurn`, `grantSurpriseRoll`) and the square is resolved with `skip()`. The receiver of the commands stays the movement of pieces only.

## 5. How it was tested
`SquareEffectTest` has 21 tests on a Medium board with the real board, the real rules and scripted dice.
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
  - misuse is refused, and a failing command leaves the square waiting;
  - a piece leaving its castle starts nothing;
  - while a square waits (`RESOLVING`), end of turn, roll and move are refused and change nothing.
- **Part 2, `EffectResolutionTest` (13 tests):** the turn waits in `RESOLVING` and refuses roll and move; the question has the square's level and a right or wrong answer moves ±1/2/3; a wrong answer chains into a surprise; a 6 still rolls again after the chain; an opponent pushed onto a surprise square is resolved by the active player; a jump that brings an opponent's last piece home ranks that opponent and ends the game (FR-43, FR-44); a push with nobody to push does nothing; only the drawn surprise is carried out; "skip the next turn" cancels the 6 and passes over the player once; the extra roll uses the UltraBoost die, does not count for the rule of 6 and comes before the roll of a 6; an extra roll that reaches "skip the next turn" cancels the 6, and one turn is missed (D5); stale answers are refused.
- **Whole games:** `SpecialDiceGameTest` plays 48 complete Medium and Hard games with random answers and picks, and `GameControllerTest` plays a Medium and a Hard game through the real question and surprise cards to the results screen.
- **Mutation check:** switching off the chain guard, the blocks, the stop at the start square or the captures each makes at least one test fail; so does switching off the ranking of an effect finish, the cancelled 6, the missed turn, or the UltraBoost dice of the extra roll.
- The independent rule tests are task T2-12 (Rakan Ziyan).

## 6. Trade-offs
**The extension proved the design.** Part 2 connected the queue to the turn, the questions, the surprises and the cards. It did not change a line of the three commands or of the receiver, and `EffectQueue` changed by one Javadoc line. Everything part 2 added sits around the pattern: the clients (`GameModel.applyAnswer`, `GameModel.applySurprise`) and the invoker's caller (`ResolvingState`). The effect is known only after the player answers, and that needed nothing special: the reached square waits in the queue as an `Activation`, the turn waits in `RESOLVING`, and the client creates the command once the answer or the outcome is known.

**What it costs**
- Six small types (the interface, three commands, the receiver and the invoker) for three kinds of movement. One method per effect in the turn manager would be shorter, but each method would repeat the block, start-square and capture checks and the chain guard.
- No undo. Command is often chosen for undo and redo, but here an effect is final; for example, a capture before a question is not reversed by the answer (FR-38). The pattern is used to separate creating an effect from running it, and to queue and guard the runs.
- Surprises end in one of two ways. "Skip the next turn" and "extra roll" change the turn, not a piece, so they are not commands (section 4). A reader has to know both paths. Making them commands would have given the receiver access to the turn and mixed piece movement with turn rules.
- The invoker does not drive itself. Its caller takes `next()`, then `run(...)` or `skip()`, one square at a time, while the player answers. This order is kept by `ResolvingState` and `GameModel`, so a mistake there is caught by the refusals (a stale answer; roll, move or end of turn in `RESOLVING`) and by the tests, not by the types.

**Alternatives considered**

| Alternative | Why not |
|---|---|
| A `switch` on the outcome in the turn manager | Every new outcome edits the turn code, the rule checks and the chain guard are repeated in each branch, and an effect that waits for the player needs extra flags. |
| Events only (Observer, like `GameListener`) | A listener is told *that* something happened. It gives no order, no single place for the chain guard, and no result to check. |
| Strategy | A strategy is one interchangeable algorithm chosen in advance, like the dice of T2-04. Here each effect is a separate request: it is created when the answer is known, queued, and run later. That is what Command is for. |

*Screenshots for the Word version: `SquareEffect.java` (interface with Javadoc), `EffectQueue.run` and `reach` (chain guard), `EffectMover.moveSteps`, and `GameModel.applySurprise` (the client turning a surprise into a command).*
