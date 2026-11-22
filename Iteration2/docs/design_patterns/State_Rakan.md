# State: turn phases (Rakan Ziyan)

Section for `iteration2_dp_Sharks` (task N2-05, #71). Pattern implemented in task T2-13 (#64, PR #79).

## 1. The pattern
**State** (GoF, behavioural): an object changes its behaviour when its internal state changes. Each state is its own class, and the object (the *context*) passes every request to the class of its current state.

## 2. The problem it solves
A turn goes through phases: waiting for a roll, waiting for a move, the move done, and game over. What the player may do depends on the phase:

| Request | Waiting for roll | Waiting for move | Moved | Game over |
|---|---|---|---|---|
| Roll | yes | only after a 6 with no legal move (FR-21) | no | no |
| Move a piece | no | yes, a legal piece (FR-20) | no | no |
| End the turn | no | only without a legal move or extra roll; shows the FR-22/FR-23 message | yes | no |

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
   endTurn(turn): String                   shared steps: rollDice, applyMove,
        ▲      ▲      ▲      ▲             requireTurnCanEnd, passTurn
        │      │      │      │
 AwaitingRoll AwaitingMove Moved GameOver   (concrete states)
```

Transitions:
`AwaitingRoll` → roll → `AwaitingMove` → move → `AwaitingRoll` (after a 6, FR-21) or `Moved` → endTurn → `AwaitingRoll` of the next player. `GameOver` starts when all but one player have finished (FR-44). From `AwaitingMove` without a legal move, endTurn passes the turn with a message (FR-23); a cancelled third 6 does the same (FR-22).

## 4. Where it is in the code (`Iteration2/src/Model/`)

| Role | File |
|---|---|
| State | `TurnState.java` (interface; its Javadoc names the pattern and the roles) |
| Concrete states | `AwaitingRollState.java`, `AwaitingMoveState.java`, `MovedState.java`, `GameOverState.java` |
| Context | `StandardTurnManager.java`: field `state`; the public methods delegate (`return state.roll(this);` etc.); the shared steps `rollDice()`, `applyMove()` and `passTurn()` set the next state |
| Phase reported to the outside | `TurnPhase.java` (unchanged): `StandardTurnManager.phase()` returns `state.phase()` |

The `TurnManager` interface, `GameModel`, the Controller and the View didn't change. The pattern is internal to the model (MVC).

## 5. How it was tested
- All Iteration 1 turn tests (Tasks 11, 12, 14, 15 and the full-game acceptance tests) pass without changes, so the behaviour is the same as before the refactoring.
- `TurnStateTest` (9 tests) checks the pattern itself: each state's phase, the full cycle of transitions, rolling from `AwaitingMove` only after an unusable 6, and that a request a state refuses throws and changes nothing (in `AwaitingRoll`, `AwaitingMove`, `Moved` and `GameOver`).

## 6. Extension in Iteration 2
The question and surprise effects (T2-10, #61) add a `ResolvingState` for `TurnPhase.RESOLVING`. It is entered after a move that lands on such a square and left when the effect queue is empty. The other states don't change.

*Screenshots for the Word version: `TurnState.java` (interface with Javadoc) and the delegating methods of `StandardTurnManager` (lines with `state.`).*
