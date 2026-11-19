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
| `OPPONENT_BACK` | 10% | 10–19 | an opponent piece **chosen by the active player** goes back ⌈R/2⌉ |
| `BACK_TO_CASTLE` | 10% | 20–29 | the piece that landed returns to its castle |
| `BACK_THREE` | 30% | 30–59 | the piece that landed moves 3 steps backward |
| `SKIP_TURN` | 30% | 60–89 | the active player misses the next turn |
| `EXTRA_ROLL` | 10% | 90–99 | the active player rolls again with Regular + UltraBoost |

The draw is one value from 0 to 99 read against that table, so the shares are exact whole percentages and add
up to exactly 100. `SurpriseOutcome` refuses to load if a later edit of the table breaks that sum, so a gap in
which no outcome could be drawn cannot slip through unnoticed.

### Where the numbers come from

These six values match the Assignment 3 prototype
(`Assignment3_Prototype/src/model/SurpriseOutcome.java`), which `PROTOTYPE_TRACEABILITY.md` records as "the
Table 2 probabilities". The **Product Owner has since checked them against the full submitted Sharks SRS and
the original project specification and confirmed them**, together with FR-40 (the active player chooses an
eligible opponent piece on the shared track, and the push is cancelled when there is none), FR-34 (only the
active player may answer a question) and FR-42 (effect movement may activate another special square).

**Those documents are not committed to this repository.** The `SRS_Sharks.pdf` and `SRS_Sharks.docx` that are
here are version 1.0 and stop before the requirement tables, so nothing in a clone of this repository lets a
reader verify the table. The confirmation rests on the Product Owner's reading of documents held outside it.
Reviewers who want to check the numbers themselves need those documents, not this repository.

## 3. The push of FR-40, step by step

FR-40 is the requirement the prototype only partly met: it picked the most advanced opponent piece by itself
(`GameState.leadingOpponentPiece`) and said so in the dialog. **This task never picks a piece for the player.**

1. The factory draws `OPPONENT_BACK` and rolls one Regular die, R.
2. ⌈R/2⌉ is computed as `(R + 1) / 2` in whole numbers — no fractions, no rounding mode to argue about:

   | R | 1 | 2 | 3 | 4 | 5 | 6 |
   |---|---|---|---|---|---|---|
   | ⌈R/2⌉ | 1 | 1 | 2 | 2 | 3 | 3 |

3. The factory lists **every** piece belonging to an opponent **of the active player**, in player order and
   then piece order. A piece is eligible when it belongs to another player than the active one, is on the
   board (not in a castle, not finished) and is still on the shared track, that is
   `progress < board.homeStartIndex()`. A piece standing on its own start square stays eligible; stopping a
   backward move there is FR-32 and belongs to T2-10.
4. The dialog of T2-11 shows that list; the player picks one.
5. `SurpriseFactory.resolve(prompt, piece)` checks the pick and returns the instruction. A piece of the active
   player, a piece in a castle, a finished piece, a piece on its home path, a piece of no player of this game,
   a piece that was never offered and `null` are all rejected with an exception rather than silently replaced.

**If no eligible opponent piece is on the shared track**, the surprise does nothing: the result's action is
`NOTHING` with a note explaining why, which T2-10 turns into `EffectQueue.skip()`. The Product Owner has
confirmed this against the recovered specification, and it is what PR #87 already expects.

## 4. Whose turn it is, and whose piece landed (FR-42)

This is the part that changed after the integration review of PR #87, and it matters.

`EffectQueue.reach(...)` queues `new Activation(move.piece(), square)` — **the piece that moved**, whoever owns
it. So after a push, an *opponent's* piece can land on a question or surprise square, and that square is then
activated while the original player is still taking their turn. A surprise therefore involves two players, and
they must not be collapsed into one:

| | What it is |
|---|---|
| `active()` | the player whose **turn** it is. FR-40 gives the choice to this player; FR-34 says this player answers a question |
| `subjectOwner()` / `triggerOwner()` | the owner of the piece that **landed** on the square |
| `opponentTriggered()` | true when the two differ, that is when the chain was set off by an opponent's piece |

In an ordinary chain the two are the same. **An earlier version of this task derived the active player from
`subject.color()`, which was wrong in a chain**: with Red taking the turn and Green's pushed piece landing on a
surprise square, a second push would have offered Red *its own* pieces as targets and hidden Green's — a direct
FR-40 violation. The active player is now an explicit argument.

For T2-10: call `draw(activation.piece(), colourOfThePlayerTakingTheTurn)`. The shorter `draw(piece)` still
exists and still means "this piece belongs to the active player", which is right for the first square of a
chain but **not** after a push.

One consequence worth knowing: the piece that set the square off is itself an opponent of the active player on
the shared track, so **it may be chosen as the target of the new push**. That follows FR-40 as written and is
covered by a test.

## 5. The classes

All five are new files in `Iteration2/src/Model/`, owned by Mohamed Zereny.

| Class | What it is |
|---|---|
| `SurpriseOutcome` | the enum of six outcomes with the probability table; `at(int)` reads one point of 0–99, `draw(Random)` draws one |
| `OpponentTargets` | the FR-40 rules: `backwardSteps(R)` = ⌈R/2⌉, `eligible(...)`, `isEligible(...)`, `requireEligible(...)` |
| `SurpriseFactory` | the single creation point: `draw(piece, active)`, `promptFor(outcome, piece, active)`, `resolve(prompt, pick)`, `eligibleTargets(active)` |
| `SurprisePrompt` | a drawn surprise before it is carried out — what the dialog shows; `targets()`, `needsChoice()`, `resolve()` / `resolve(Piece)`, `opponentTriggered()` |
| `SurpriseResult` | what must happen, as data: `action()`, `piece()`, signed `steps()`, `active()`, `trigger()`, `affected()`, `turnTargetUndecided()` |

MVC is kept: everything is in `Model`, nothing imports `View` or `Controller`, and the architecture test
(`MvcDependencyTest`) still passes.

## 6. How T2-10 consumes the result

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

## 7. Why no new design pattern here

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

## 8. Open points that need the team or the PO

1. **Who gets `SKIP_TURN` or `EXTRA_ROLL` in a chain set off by an opponent's piece.** Red is taking the turn,
   Red's push moved Green's piece onto a surprise square, and that square draws "skip the next turn". Does Red
   skip, or Green? The confirmed requirements cover FR-39, FR-40, FR-34 and FR-42 but **do not answer this**,
   so T2-09 does not decide it. The code returns the **player whose turn it is** as a *provisional* answer and
   sets `SurpriseResult.turnTargetUndecided()` to `true` in exactly that case, so the open question is visible
   in code rather than hidden. Changing the answer is a one-line change in `SurpriseResult.affected()`.
   **Needs: a PO ruling, then one line plus one test.**
2. **Who answers a question in such a chain.** FR-34 is confirmed: **only the active player may answer**. So
   when a pushed opponent piece lands on a question square, the *active* player answers and the *opponent's*
   piece moves by the result. That is now unambiguous, but it still has to be implemented in T2-10 (which
   piece the answer moves) and T2-11 (which player the dialog addresses). T2-09 gives both of them what they
   need: `active()` and `triggerOwner()` are separate. **Needs: Shafeek and I to confirm we read it the same
   way before T2-11 starts.**
3. **Where the factory is created.** `SurpriseFactory`, like the question factories of T2-08, is not yet wired
   into `GameModel`. `GameModel` is shared, and the wiring belongs with the effect queue and the `RESOLVING`
   phase, so it is left to T2-10 and T2-13 rather than done unilaterally here.
4. **The specification is not in the repository.** The confirmed FR texts and Table 2 live in documents outside
   this repository (section 2). Committing them, or an extract, would let any reviewer check the numbers.

## 9. Tests

`Iteration2/test/Model/SurpriseOutcomeTest.java` (14 tests) and
`Iteration2/test/Model/SurpriseFactoryTest.java` (45 tests), 59 in all; full suite **474/474** on this branch.

They cover: the six outcomes and their texts; the probability sum of exactly 100%; every point of 0–99 mapped
once and each outcome getting its own share; the exact boundary of every share (0/9, 10/19, 20/29, 30/59,
60/89, 90/99); points outside 0–99 rejected; the draw reading exactly one 0–99 value; a frequency check over
60 000 seeded draws within one percentage point; the same seed repeating a whole sequence of draws and dice;
the instruction produced by each of the six outcomes; ⌈R/2⌉ for R = 1…6 and invalid die values rejected; the
push rolling exactly one Regular die; own pieces, castle pieces, finished pieces and home-path pieces never
offered as targets; all eligible pieces offered in a fixed order; the leading piece **not** chosen
automatically and a trailing piece successfully pushed; wrong, unreachable, stale, foreign and missing targets
rejected; the no-eligible-opponent case; read-only target lists; and every contradiction a `SurprisePrompt` or
`SurpriseResult` can be built with.

The FR-42 chain cases added by the integration review: the turn holder kept apart from the owner of the piece
that landed; **a push in an opponent-triggered chain never offering the active player's own pieces** and still
offering the pieces of the player who set it off; the trigger piece itself being choosable; own-piece outcomes
moving the trigger piece and falling on *its* owner; turn outcomes in such a chain flagged as undecided;
ordinary chains flagging nothing; the short calls still meaning "the active player's piece"; an active colour
no player of the game plays rejected; a pick captured back to its castle after it was offered rejected; a pick
that turned into its home path after it was offered rejected by the board-aware `resolve`.

The independent rule tests of this feature are **T2-12**
([#63](https://github.com/shafeekseef81-svg/Sharks/issues/63), Rakan Ziyan), who did not write this code
(SDP 5.3.4). The tests above are developer tests and do not replace them.

**Not claimed:** surprises are not yet triggered in a running game. A piece landing on a surprise square does
not open anything until the effect queue (T2-10) and the turn phase (T2-13) are connected, and there is no
dialog until T2-11. Nothing here has been verified end to end in actual play.

## 10. Reuse gate (SDP 6.1)

Reused from `Assignment3_Prototype/src/model/SurpriseOutcome.java`: the six outcomes, their player-facing
texts and their probabilities. Changed: the prototype's enum was drawn and then applied by the prototype
controller, which moved pieces itself. Here the enum only names the outcome, the factory produces a
description, and all movement belongs to T2-10 — so the rules are testable and the MVC split holds. The
prototype's automatic choice of the leading opponent piece was **dropped**, because it is the part of FR-40
the prototype itself recorded as only partly met.
