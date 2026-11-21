# Product Owner decision register — Iteration 2 open rules

Owner: **Mohamed Zereny** (Product Owner). Opened 21/11/2026. Related: issue
[#75](https://github.com/shafeekseef81-svg/Sharks/issues/75), PRs #87, #89, #90, #91, #92, and the acceptance
plan `Iteration2/docs/T2-15_AcceptancePlan.md`.

**Nothing in this file is a decision.** Each entry ends in a **proposal**, which becomes a decision only when
the Product Owner records it as decided, with a date, and the team is told. Until then the code must keep
behaving as it does today, and the acceptance steps that depend on it stay unjudged.

Why this register exists: five rules in Iteration 2 are not answered by the confirmed requirements. Each one
is currently settled *somewhere in the code* by whoever wrote that code first. That is not wrong - it is how
the work got done - but it means the rule is decided by an implementation detail rather than by the Product
Owner, and two of the five are visible to the player. The point of this file is to make each one explicit,
say where the evidence is, and keep the proposal separate from the code.

| # | Open rule | Visible to a player? | Decided today by | Blocks |
|---|---|---|---|---|
| D1 | A Double Block on a start square | **Yes** | `StandardMoveEngine.moveFor` | M3 in the acceptance script |
| D2 | Who gets SKIP_TURN / EXTRA_ROLL after an opponent's piece set off the chain | **Yes** | `SurpriseResult.affected`, flagged undecided | M5.4, T2-09, T2-11, T2-13 |
| D3 | What "did not answer" means, and whether it can happen at all | **Yes** | `QuestionFactory.judge` + no timer in T2-11 | M4.4 |
| D4 | A push-back that would go past the pushed piece's start square | **Yes** | not implemented yet (T2-10) | M6.2, P10 |
| D5 | EXTRA_ROLL and SKIP_TURN arriving in the same effect chain | Rarely | not implemented yet (T2-10, T2-13) | P9 |

---

## D1 — A Double Block standing on a start square

**The exact rule in question.** A Regular 6 lets a player put a piece from its castle onto its own start
square (FR-19). An opponent's Double Block prevents landing on, or passing, the square it stands on (FR-29).
What happens when an opponent's Double Block is standing **on your start square** and you roll a Regular 6?

**Conflicting evidence.**
- FR-19: "A Regular 6 shall allow placing a piece on its starting square." Read alone, the piece comes out.
- FR-29: "An opponent's Double Block shall prevent landing" on that square. Read alone, the piece stays in.
- Neither requirement names the other, so the texts genuinely conflict in this one case.
- A start square **is** a protected square (`SquareType.isProtected()` returns true for `START`), so no
  capture can happen there in either reading. The conflict is only about whether the piece may come out.

**What the code does today.** FR-29 wins. `Iteration2/src/Model/StandardMoveEngine.java`, in `moveFor`:

```java
if (piece.inCastle()) {
    // FR-19: a Regular 6 places the piece on its start square; the special die is ignored. An opponent
    // Double Block on the start square keeps the piece in its castle (FR-29).
    boolean canExit = roll.regular() == EXIT_ROLL && isPassable(board.startCell(owner.color()), owner);
    return canExit ? new Move(piece, 0) : null;
}
```

So a piece can be kept in its castle by two opponent pieces parked on its start square. This is merged into
`iteration-2` (T2-06, #77, Rakan).

**Alternatives.**

| | Rule | Consequence |
|---|---|---|
| A (today) | The block wins; the piece stays in its castle | Simple and consistent with FR-29 everywhere. But two opponents can park on a start square and shut a player out of the game for as long as they like. With four players that is a real griefing move, and a player who rolls three 6s and still cannot move will think the game is broken. |
| B | FR-19 wins; the piece comes out onto its start square anyway | The player is never locked out. Needs a rule for what happens to the two pieces already there - they are on a protected square, so they cannot be captured; the square would then hold three pieces of two colours, which no other rule allows. |
| C | A Double Block cannot form on a start square at all | Removes the conflict at its source and keeps both FR-19 and FR-29 intact everywhere else. Costs a new rule that is in neither requirement, and a change in code owned by Rakan. |

**Impact.** A is merged and tested; B and C are changes to another developer's merged code four days before
CP2. B also needs a brand-new rule about three pieces on one square, so it is the most expensive. C is the
cleanest rule but the biggest change in scope.

**Proposal (not decided).** Keep **A** for Iteration 2, because it is merged, tested and consistent, and
record the lock-out as a known limitation in the system report and the retrospective. Revisit C in
Iteration 3 if play-testing shows the lock-out actually happens. **This needs the Product Owner to accept the
lock-out explicitly**, because it is player-visible and it is the kind of thing a grader will try.

---

## D2 — Who gets SKIP_TURN or EXTRA_ROLL when an opponent's piece set off the chain

**The exact rule in question.** Red is taking the turn. A surprise moves **Green's** piece (FR-40), and
Green's piece lands on a surprise square, which draws "skip the next turn" or "roll again" (FR-39, FR-42).
Does **Red** (whose turn it is) skip or roll again, or does **Green** (whose piece landed)?

**Conflicting evidence.**
- FR-39 and FR-40 describe the outcomes and who chooses the opponent piece, but neither says who a *turn*
  outcome falls on when the two players differ.
- FR-42 says a question or surprise movement activates any square it lands on, which is what creates the case,
  but it does not resolve it.
- FR-34 **is** clear for questions: only the active player answers. By analogy the active player would also
  take the turn outcome - but that is an analogy, not a requirement.

**What the code does today.** T2-09 returns the **active player** as a *provisional* answer and marks the case
as unresolved rather than hiding it: `SurpriseResult.turnTargetUndecided()` is `true` in exactly this
situation, and the T2-11 dialog shows `SurpriseDialog.UNDECIDED_NOTE` saying the team is still agreeing it.
Changing the answer is described in the T2-09 notes as a one-line change in `SurpriseResult.affected()` plus
one test. Neither PR is merged (#90, #91).

**Alternatives.**

| | Rule | Consequence |
|---|---|---|
| A (provisional today) | The **active** player skips or rolls again | Consistent with FR-34's "the active player answers". But it is strange to play: Red pushes Green, Green lands on "skip a turn", and **Red** is punished for it. It also makes "push an opponent" self-harming, which no requirement suggests. |
| B | The **owner of the piece that landed** skips or rolls again | Reads naturally: the square acts on whoever stepped on it. Makes the push a weapon, which fits "move an opponent back". Risk: Red could hand *itself* an extra roll by pushing a piece onto an EXTRA_ROLL square - the benefit would go to Green, so actually it cannot be abused that way. |
| C | The outcome is skipped entirely in a cross-player chain | Safest, never surprising, but silently drops an outcome the player can see being drawn, which looks like a bug. |

**Impact.** The code already separates `active()` from `triggerOwner()`, so **all three are a small change**
in unmerged code owned by me. This is the cheapest of the five to change and the one with the clearest
gameplay consequence. It must be decided before #90 and #91 merge, otherwise the `UNDECIDED_NOTE` ships to a
player.

**Proposal (not decided).** **B** - the owner of the piece that landed. It reads the way a board game reads,
it makes FR-40's push meaningful, and it cannot be abused to benefit the active player. Cost: one line in
`SurpriseResult.affected()`, one test, and removing `UNDECIDED_NOTE` from the dialog.

---

## D3 — What "did not answer" means, and whether a player can refuse to answer

**The exact rule in question.** FR-35 is confirmed: not answering counts as a wrong answer, so the piece moves
back 1, 2 or 3 steps. But *how* does a player not answer? There is no time limit anywhere in the SRS, and
T2-11 deliberately implements no timer.

**Evidence.**
- `QuestionResult.NO_ANSWER = -1`, and `QuestionFactory.judge` accepts it and returns a backward result. That
  part matches FR-35 and is tested (`QuestionFactoryTest`, "not answering counts as a wrong answer").
- `Iteration2/src/View/QuestionDialog.java` documents `NO_ANSWER` as what is reported "when the player does
  not answer", and the T2-11 notes record that "a controller or a future timer can call it; there is no timer
  in this task."
- So today the only way to reach NO_ANSWER through the interface is to **close the dialog**. Whether the
  dialog *can* be closed is a window-decoration detail, not a decided rule.

**Alternatives.**

| | Rule | Consequence |
|---|---|---|
| A | The dialog cannot be closed; the player must pick one of the four answers | NO_ANSWER then never happens in play, and FR-35's clause is dead code that is only unit-tested. Simple, and no player ever loses steps to a misclick on the X. |
| B | The dialog can be closed, and closing it is a wrong answer | Implements FR-35 fully. Risk: a player closes the dialog by accident and is punished, with no warning and no undo. Needs a confirmation prompt to be fair. |
| C | Add a time limit, and running out of time is a wrong answer | The most faithful reading of "did not answer", and it makes the educational game tenser. But **no requirement gives a duration**, so the number would be invented, and NFR-05 says nothing about it. Out of scope for Iteration 2. |

**Impact.** A and B are both small changes in my own unmerged T2-11 code. C is new scope and a number nobody
has specified - it should not be done in Iteration 2.

**Proposal (not decided).** **A for the interface, keep the model as it is.** Make the question dialog
non-closable so a player cannot lose steps by accident, and keep `NO_ANSWER` in the model, tested, for the
timer that Iteration 3 may add. The system report should then say plainly that FR-35's "did not answer" clause
is implemented in the model but cannot be triggered from the interface in Iteration 2 - which is honest, and
better than inventing a timer.

---

## D4 — A push-back that would go past the pushed piece's start square

**The exact rule in question.** FR-32 says backward movement caused by questions or surprises stops at the
start square and does not go past it. When a surprise pushes an **opponent's** piece back ⌈R/2⌉ squares and
that would take it past **its own** start square, does it stop there, or is the push cancelled, or does the
piece go back into its castle?

**Evidence.**
- FR-32 is confirmed and says movement stops at the start square. It does not say whose start square, but the
  only sensible reading is the moving piece's own, since that is the square its route begins at.
- The T2-09 notes state the position explicitly: "A piece standing on its own start square stays eligible;
  stopping a backward move there is FR-32 and belongs to T2-10." So T2-09 offers such a piece as a target and
  leaves the stopping to the effect queue.
- `StandardMoveEngine.isPassable` is already direction-independent and documents that "backward moves
  (questions, surprises) can use it as well", so the blocking half is ready.
- **The stopping itself is not implemented anywhere on `iteration-2` yet**, and the RTM snapshot
  (`T2-14_RTM_Coverage.md`) lists FR-32 as having only 2 tests, both by the authors.

**Alternatives.**

| | Rule | Consequence |
|---|---|---|
| A | The piece stops on its start square and stays there | The plain reading of FR-32. The piece keeps its place in the game and the push still hurts. |
| B | The push is cancelled if it cannot be made in full | Treats the push like an exact-finish move (FR-31). But FR-32 already says movement *stops*, which is the opposite of cancelling, so this contradicts a confirmed requirement. |
| C | The piece goes back into its castle | Much harsher than any requirement suggests, and FR-26 reserves the castle for captures. |

**Impact.** This is in Shafeek's T2-10 (#87, #92), unmerged. It is a requirement that is already confirmed, so
this is less a decision than a **check that T2-10 implements A** - but it is player-visible and currently
untested, so it belongs in this register until somebody has run it.

**Proposal (not decided).** **A**, because FR-32 says so. Action: ask Shafeek to confirm T2-10 stops the piece
at its own start square, and add the acceptance test P10 and the manual step M6.2 as the evidence.

---

## D5 — EXTRA_ROLL and SKIP_TURN in the same effect chain

**The exact rule in question.** A chain (FR-42) can set off more than one surprise. If one draws EXTRA_ROLL
and another draws SKIP_TURN, and both fall on the same player, what happens? Do they cancel out, does the last
one win, or do both apply (roll again now, and miss the next turn)?

**Evidence.**
- FR-39 says a surprise square triggers exactly one outcome - per square, not per chain. FR-42 allows a chain
  to touch several squares, so several outcomes in one chain are possible.
- Nothing in the confirmed requirements says how two turn-level outcomes combine.
- `Player.skipNextTurn()` exists on `iteration-2`, and the T2-09 notes show the intended wiring
  (`turn.skipNextTurn(result.affected())` and `turn.grantSurpriseRoll()`), but the queue that would run both is
  in T2-10/T2-13 and is not merged.
- The chain guard mentioned in the plan limits chain length, so this is rare, not impossible.

**Alternatives.**

| | Rule | Consequence |
|---|---|---|
| A | Both apply: the player rolls again now, then misses the next turn | Each outcome does what it says, and no outcome is silently dropped. Needs the turn code to hold both flags at once. |
| B | They cancel each other out | Tidy and easy to explain, but the player watches two outcomes be drawn and nothing happens, which looks broken. |
| C | The last outcome in the chain wins | Simplest to implement, but the result then depends on the order the queue happens to run in, which is an implementation detail, not a rule. |

**Impact.** Unmerged code owned by Shafeek (T2-10) and Rakan (T2-13). Rare in play, so low risk either way -
but it should not be left to whichever line of code runs last.

**Proposal (not decided).** **A**, both apply, because it is the only option where every outcome the player
saw drawn actually happens. It also needs no new rule - just that the extra roll and the skip flag are
independent, which they already are. Action: confirm with Rakan that `skipNextTurn` and the surprise roll can
both be set in one turn, and cover it in the planned test P9.

---

## How these get closed

1. Product Owner (me) reviews each proposal and records a decision with a date in this file.
2. The decision goes into the relevant issue or PR as a comment, so the implementer sees it.
3. D2 and D3 must be closed **before #90 and #91 merge**, because they change my own unmerged code and D2
   currently ships an "undecided" note to the player.
4. D1 needs an explicit acceptance of the lock-out, or a change in Rakan's merged code - the first is cheap,
   the second is not.
5. D4 and D5 are confirmations to request from Shafeek and Rakan rather than new rules.
6. Once decided, the matching rows of `T2-15_AcceptancePlan.md` (M3, M4.4, M5.4, M6.2, P9, P10) can be judged
   pass or fail instead of staying unjudged.
