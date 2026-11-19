# T2-09 — Surprise outcomes, and the opponent chosen by the player

Task **T2-09** · Issue [#60](https://github.com/shafeekseef81-svg/Sharks/issues/60) · SRS **FR-39, FR-40**
Owner: **Mohamed Zereny**. Depends on T2-08 ([#59](https://github.com/shafeekseef81-svg/Sharks/issues/59), PR #89).

> Task documentation. T2-09 is **not** a graded design-pattern task: the graded patterns of this iteration are
> Factory Method (T2-08, Mohamed), Strategy (T2-04, Shafeek), Command (T2-10, Shafeek) and State (T2-13, Rakan).
> Section 6 explains why no new pattern was added here.

## 1. What this task owns, and what it does not

A surprise square has to do three things that are *rules about surprises*:

1. draw one of six outcomes with the probabilities of SRS Table 2 (FR-39);
2. for the outcome "push an opponent back", roll a Regular die and work out ⌈R/2⌉ (FR-40);
3. decide which opponent pieces the active player is allowed to choose from, and accept the one the player
   picked (FR-40).

Everything *after* that is somebody else's rule, and T2-09 deliberately does none of it:

| Who | What |
|---|---|
| **T2-10** (Shafeek, Command, [#61](https://github.com/shafeekseef81-svg/Sharks/issues/61), PR #87) | carrying the outcome out: moving pieces, Double Blocks (FR-29, FR-41), captures and protection (FR-26, FR-27), the backward stop at the start square (FR-32), the order of a chain and the chain guard (FR-42), the ranking of a piece that finishes (FR-43) |
| **T2-11** (Mohamed, [#62](https://github.com/shafeekseef81-svg/Sharks/issues/62)) | the dialog: showing the outcome and letting the player pick the opponent piece |
| **T2-13** (Rakan, [#64](https://github.com/shafeekseef81-svg/Sharks/issues/64)) | the turn: skipping the next turn, granting the extra roll, and the `RESOLVING` phase |

Nothing in this task moves a piece, changes a player's state or ends a turn. Creating a surprise is a pure
calculation, which is why all of it can be unit-tested without a running game.

## 2. The six outcomes and their probabilities (FR-39)

| Outcome | Chance | Draw range | What it asks for |
|---|---|---|---|
| `JUMP_TO_FINISH` | 10% | 0–9 | the piece that landed goes straight to its finishing square |
| `OPPONENT_BACK` | 10% | 10–19 | an opponent piece **chosen by the player** goes back ⌈R/2⌉ |
| `BACK_TO_CASTLE` | 10% | 20–29 | the piece that landed returns to its castle |
| `BACK_THREE` | 30% | 30–59 | the piece that landed moves 3 steps backward |
| `SKIP_TURN` | 30% | 60–89 | the active player misses the next turn |
| `EXTRA_ROLL` | 10% | 90–99 | the active player rolls again with Regular + UltraBoost |

The draw is one value from 0 to 99 read against that table, so the shares are exact whole percentages and add
up to exactly 100. `SurpriseOutcome` refuses to load if a later edit of the table breaks that sum, so a gap in
which no outcome could be drawn cannot slip through unnoticed.

### Where the numbers come from — open point for the PO

These six values are **carried over from the Assignment 3 prototype**
(`Assignment3_Prototype/src/model/SurpriseOutcome.java`), which `PROTOTYPE_TRACEABILITY.md` records as "the
Table 2 probabilities". **They could not be verified against the SRS itself in this repository:** the
committed `SRS_Sharks.pdf` and `SRS_Sharks.docx` are version 1.0 and stop before the requirement tables, so
neither the text of FR-39/FR-40 nor Table 2 is available here. The values are therefore **provisional**, and
changing one is a one-line change in the enum plus the matching expected value in `SurpriseOutcomeTest`.

## 3. The push of FR-40, step by step

FR-40 is the requirement the prototype only partly met: it picked the most advanced opponent piece by itself
(`GameState.leadingOpponentPiece`) and said so in the dialog. **This task never picks a piece for the player.**

1. The factory draws `OPPONENT_BACK` and rolls one Regular die, R.
2. ⌈R/2⌉ is computed as `(R + 1) / 2` in whole numbers — no fractions, no rounding mode to argue about:

   | R | 1 | 2 | 3 | 4 | 5 | 6 |
   |---|---|---|---|---|---|---|
   | ⌈R/2⌉ | 1 | 1 | 2 | 2 | 3 | 3 |

3. The factory lists **every** eligible opponent piece, in player order and then piece order. A piece is
   eligible when it belongs to another player, is on the board (not in a castle, not finished) and is still on
   the shared track, that is `progress < board.homeStartIndex()`. A piece standing on its own start square
   stays eligible; stopping a backward move there is FR-32 and belongs to T2-10.
4. The dialog of T2-11 shows that list; the player picks one.
5. `SurprisePrompt.resolve(piece)` checks the pick against the list and returns the instruction. A piece of
   the active player, a piece in a castle, a finished piece, a piece on its home path and `null` are all
   rejected with an exception rather than silently replaced.

**If no opponent piece is on the shared track**, the surprise does nothing: the result's action is `NOTHING`
with a note explaining why, which T2-10 turns into `EffectQueue.skip()`. That is what the prototype did and
what PR #87 proposes, but it is **not** confirmed against the SRS text — see section 7.

## 4. The classes

All five are new files in `Iteration2/src/Model/`, owned by Mohamed Zereny.

| Class | What it is |
|---|---|
| `SurpriseOutcome` | the enum of six outcomes with the probability table; `at(int)` reads one point of 0–99, `draw(Random)` draws one |
| `OpponentTargets` | the FR-40 rules: `backwardSteps(R)` = ⌈R/2⌉, `eligible(...)`, `isEligible(...)`, `requireEligible(...)` |
| `SurpriseFactory` | the single creation point: draws the outcome, rolls the die of a push, collects the eligible pieces |
| `SurprisePrompt` | a drawn surprise before it is carried out — what the dialog shows; `resolve()` / `resolve(Piece)` |
| `SurpriseResult` | what must happen, as data: an action, a piece, signed steps, the die that was rolled |

MVC is kept: everything is in `Model`, nothing imports `View` or `Controller`, and the architecture test
(`MvcDependencyTest`) still passes.

## 5. How T2-10 consumes the result

`SurpriseResult` carries a `SurpriseAction`, deliberately **not** a `SquareEffect` command. T2-09 has to
compile and be tested on its own branch, which does not contain Shafeek's classes, and T2-10 stays the only
owner of how an effect is carried out. One `switch` connects the two:

```java
switch (result.action()) {
    case JUMP_TO_FINISH   -> queue.run(new JumpToFinishEffect(result.piece()));
    case RETURN_TO_CASTLE -> queue.run(new ReturnToCastleEffect(result.piece()));
    case MOVE_STEPS       -> queue.run(new MoveStepsEffect(result.piece(), result.steps()));
    case SKIP_NEXT_TURN   -> turn.skipNextTurn(result.affected());
    case EXTRA_ROLL       -> turn.grantSurpriseRoll();
    case NOTHING          -> queue.skip();
}
```

`result.piece()` is already the right piece for every action — the piece that landed, or the opponent piece the
player chose — and `result.steps()` is already signed and never zero for `MOVE_STEPS`, so it fits
`MoveStepsEffect` unchanged. This matches the "Surprises" part of the part-2 proposal in PR #87 and needs **no
change to any file of Shafeek's or Rakan's**.

## 6. Why no new design pattern here

Issue #60 asks for surprise outcomes "created by the factory", and `SurpriseFactory` is that single creation
point: nothing else in the game draws an outcome or rolls the die of a push. But it is **a creation point, not
a second GoF Factory Method**, and it is deliberately not a subclass of `QuestionFactory`.

Factory Method earns its place in T2-08 because a question square carries a level of its own and each level
produces its own kind of product from its own pool, so the creator genuinely varies by subclass. A surprise
square has no levels and no family of products: there is one fixed table of six outcomes, drawn at random.
Giving that an abstract creator and six subclasses would add a class hierarchy with nothing to vary — a
pattern used for its name rather than for the problem it solves, which is exactly what the course warns
against and what would cost marks on the design-pattern grade. The graded Factory Method of this member
remains the question factory of T2-08.

## 7. Open points that need the team or the PO

1. **The probabilities of Table 2** — provisional, taken from the prototype (section 2). PO to confirm against
   the approved SRS.
2. **A push with no eligible opponent piece** — implemented as "nothing happens". PO to confirm.
3. **Who answers a question caused by a chain reaction** — if a pushed opponent piece lands on a question
   square, FR-34 says the *active* player answers, but the piece belongs to the opponent. This is open point 2
   of PR #87 and is **not decided by this task**: `SurpriseResult` only names the piece and its owner
   (`affected()`), so either answer can be implemented without changing T2-09. It must be agreed between
   T2-09, T2-10 (chain) and T2-11 (dialog) before T2-11 starts.
4. **Where the factory is created** — `SurpriseFactory`, like the question factories of T2-08, is not yet
   wired into `GameModel`. `GameModel` is shared, and the wiring belongs with the effect queue and the
   `RESOLVING` phase, so it is left to T2-10 and T2-13 rather than done unilaterally here.

## 8. Tests

`Iteration2/test/Model/SurpriseOutcomeTest.java` (14 tests) and
`Iteration2/test/Model/SurpriseFactoryTest.java` (33 tests), 47 in all; full suite **462/462** on this branch.

They cover: the six outcomes and their texts; the probability sum of exactly 100%; every point of 0–99 mapped
once and each outcome getting its own share; the exact boundary of every share (0/9, 10/19, 20/29, 30/59,
60/89, 90/99); points outside 0–99 rejected; the draw reading exactly one 0–99 value; a frequency check over
60 000 seeded draws within one percentage point; the same seed repeating a whole sequence of draws and dice;
the instruction produced by each of the six outcomes; ⌈R/2⌉ for R = 1…6 and invalid die values rejected; the
push rolling exactly one Regular die; own pieces, castle pieces, finished pieces and home-path pieces never
offered as targets; all eligible pieces offered in a fixed order; the leading piece **not** chosen
automatically and a trailing piece successfully pushed; wrong, unreachable and missing targets rejected; the
no-eligible-opponent case; read-only target lists; and every contradiction a `SurprisePrompt` or
`SurpriseResult` can be built with.

The independent rule tests of this feature are **T2-12**
([#63](https://github.com/shafeekseef81-svg/Sharks/issues/63), Rakan Ziyan), who did not write this code
(SDP 5.3.4). The tests above are developer tests and do not replace them.

**Not claimed:** surprises are not yet triggered in a running game. A piece landing on a surprise square does
not open anything until the effect queue (T2-10) and the turn phase (T2-13) are connected, and there is no
dialog until T2-11. Nothing here has been verified end to end in actual play.

## 9. Reuse gate (SDP 6.1)

Reused from `Assignment3_Prototype/src/model/SurpriseOutcome.java`: the six outcomes, their player-facing
texts and their probabilities. Changed: the prototype's enum was drawn and then applied by the prototype
controller, which moved pieces itself. Here the enum only names the outcome, the factory produces a
description, and all movement belongs to T2-10 — so the rules are testable and the MVC split holds. The
prototype's automatic choice of the leading opponent piece was **dropped**, because it is the part of FR-40
the prototype itself recorded as only partly met.
