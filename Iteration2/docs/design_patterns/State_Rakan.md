# State: turn phases (Rakan Ziyan)

Section for `iteration2_dp_Sharks` (task N2-05, #71). Pattern implemented in task T2-13 (#64, PR #79); `ResolvingState` added by T2-10 part 2 (#61, PR #92, Shafeek Saif) on the hook T2-13 left for it.

## 1. The pattern
**State** (GoF, behavioural): an object changes its behaviour when its internal state changes. Each state is its own class, and the object (the *context*) passes every request to the class of its current state.

## 2. The problem it solves
A turn goes through phases: waiting for a roll, waiting for a move, a question or surprise square waiting for its effect, the move done, and game over. What the player may do depends on the phase:

| Request | Waiting for roll | Waiting for move | Resolving a square | Moved | Game over |
|---|---|---|---|---|---|
| Roll | yes | only after a 6 or an extra roll, with no legal move (FR-21, Q27) | no | no | no |
| Move a piece | no | yes, a legal piece (FR-20) | no | no | no |
| Answer / carry out a surprise | no | no | yes (FR-34, FR-39..FR-42) | no | no |
| End the turn | no | only without a legal move or extra roll; shows the FR-22/FR-23 message | no | yes | no |

In Iteration 1, `StandardTurnManager` kept the phase as an enum value, and every method began with `if (phase == ...)` checks. Iteration 2 adds a new phase for question and surprise effects (`RESOLVING`, T2-10). With checks on a phase value, each new phase would change every method and repeat the same conditions in several places.

**Why State:** the rules above are a table of *phase × request*. State turns each column into one class, so a new phase is one new class and the existing ones don't change. The rules of a phase are read in one place, and a request that a phase doesn't allow is refused by that phase's class alone.

## 3. Structure

```
           «interface»
            TurnState                      StandardTurnManager  (context)
   ─────────────────────────              ──────────────────────────────
   phase(): TurnPhase                      - state: TurnState
   canRoll(turn): boolean        ◄──────── + roll()      → state.roll(this)
   roll(turn): DiceRoll                    + legalMoves()→ state.legalMoves(this)
   legalMoves(turn): Map                   + move(p)     → state.move(this, p)
   move(turn, piece): MoveResult           + endTurn()   → state.endTurn(this)
   endTurn(turn): String                   + runEffect(e)→ state.runEffect(this, e)
   runEffect, skipEffect,                  shared steps: rollDice, applyMove,
   skipNextTurn, grantSurpriseRoll         stateAfterMove, requireTurnCanEnd,
     (default: refuse)                     passTurn
        ▲      ▲        ▲        ▲      ▲
        │      │        │        │      │
 AwaitingRoll AwaitingMove Resolving Moved GameOver   (concrete states)
```

Transitions:
`AwaitingRoll` → roll → `AwaitingMove` → move → `AwaitingRoll` (after a 6, FR-21) or `Moved` → endTurn → `AwaitingRoll` of the next player. A move that stops on a question or surprise square goes to `Resolving` instead; it stays there for the whole chain of effects (FR-42) and then goes on exactly as after a move, through the one method `stateAfterMove()`. `GameOver` starts when all but one player have finished (FR-44), also in the middle of a chain. From `AwaitingMove` without a legal move, endTurn passes the turn with a message (FR-23); a cancelled third 6 does the same (FR-22).

## 4. Where it is in the code (`Iteration2/src/Model/`)

| Role | File |
|---|---|
| State | `TurnState.java` (interface; its Javadoc names the pattern and the roles) |
| Concrete states | `AwaitingRollState.java`, `AwaitingMoveState.java`, `ResolvingState.java`, `MovedState.java`, `GameOverState.java` |
| Context | `StandardTurnManager.java`: field `state`; the public methods delegate (`return state.roll(this);` etc.); the shared steps `rollDice()`, `applyMove()`, `stateAfterMove()` and `passTurn()` set the next state |
| Phase reported to the outside | `TurnPhase.java` (unchanged): `StandardTurnManager.phase()` returns `state.phase()` |

The refactoring of T2-13 changed neither `TurnManager`, `GameModel`, the Controller nor the View; the pattern is internal to the model (MVC). T2-10 part 2 then added the effect requests to `TurnManager` and `GameModel`, which the Controller calls after its question and surprise dialogs.

## 5. How it was tested
- All Iteration 1 turn tests (Tasks 11, 12, 14, 15 and the full-game acceptance tests) pass without changes, so the behaviour is the same as before the refactoring.
- `TurnStateTest` (9 tests) checks the pattern itself: each state's phase, the full cycle of transitions, rolling from `AwaitingMove` only after an unusable 6, and that a request a state refuses throws and changes nothing (in `AwaitingRoll`, `AwaitingMove`, `Moved` and `GameOver`).

- `EffectResolutionTest` (T2-10 part 2) and the independent `EffectChainIndependentTest` (T2-12, PR #95) check `Resolving`: roll, move and end of turn are refused while a square waits, a chain stays in the state until it is over, and afterwards a 6 still gives its extra roll, otherwise the turn passes.

## 6. Trade-offs
**The extension proved the design.** The effects of question and surprise squares (T2-10 part 2) needed a fifth phase. It came as one new class, `ResolvingState`, and four new requests on `TurnState` declared as *default methods that refuse*. The four existing states did not change a line, and only `ResolvingState` overrides the new requests (and refuses roll, move and end of turn in return). With phase checks, every public method of the turn manager would have needed a new branch.

**Costs.**
- Five small classes and one interface instead of one class with `if` checks; a reader has to open several files to see the whole turn. The table in section 2 and the transitions above are the map.
- The states call back into the context (`turn.rollDice()`, `turn.applyMove(...)`), so the shared steps are package-private methods of `StandardTurnManager`, not private ones. The package boundary (`Model`) keeps them away from the View and the Controller.
- The states hold no data, so the context keeps one instance of each and all turn data stays in one place. This keeps them simple, but a state cannot be tested without a context; `TurnStateTest` therefore tests their behaviour through `StandardTurnManager` (only `phase()` is checked on a state alone).

**Alternative considered.** An `enum` with a method per constant (enum-based State) would be shorter, but a new phase would still edit the enum, and the transitions would need the context's data passed in anyway. The separate classes keep each phase's rules in its own file, which is what made the Iteration 2 extension a pure addition.

*Screenshots for the Word version: `TurnState.java` (interface with Javadoc and the default methods that refuse), `ResolvingState.java`, the delegating methods of `StandardTurnManager` (lines with `state.`) and `stateAfterMove()`.*
