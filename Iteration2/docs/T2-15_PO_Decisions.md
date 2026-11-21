# Product Owner decision register — Iteration 2 open rules

Owner: **Mohamed Zereny** (Product Owner). Opened 21/11/2026. Related: issue
[#75](https://github.com/shafeekseef81-svg/Sharks/issues/75), PRs #87, #89, #90, #91, #92, and the acceptance
plan `Iteration2/docs/T2-15_AcceptancePlan.md`.

**Status 21/11/2026: D2 and D3 are DECIDED and implemented. D1, D4 and D5 are still OPEN.**

An entry marked OPEN ends in a **proposal**, which is not a decision: it becomes one only when the Product
Owner records it as decided, with a date, and the team is told. Until then the code keeps behaving as it does
today and the acceptance steps that depend on it stay unjudged.

Why this register exists: five rules in Iteration 2 are not answered by the confirmed requirements. Each one
was settled *somewhere in the code* by whoever wrote that code first. That is not wrong - it is how the work
got done - but it meant the rule was decided by an implementation detail rather than by the Product Owner, and
two of the five are visible to the player. The point of this file is to make each one explicit, say where the
evidence is, and keep the proposal separate from the code.

**Where the decision differed from the proposal, both are kept below.** For D2 and D3 the Product Owner chose
differently from what this register proposed; the proposals are left in place, marked as superseded, so the
record of what was considered stays honest.

**How to read the decision blocks.** The sentence under *The rule* is the approved decision. Anything labelled
*Engineering explanation* is the developer's technical reading of why the rule fits the specification — it was
**not** supplied by the Product Owner and is not a quotation. The Product Owner approved the rules; the
justifications written here are the developer's own and can be corrected without reopening the decision.

| # | Rule | Status | Visible to a player? | Decided by | Blocks |
|---|---|---|---|---|---|
| D1 | A Double Block on a start square | **OPEN** | **Yes** | `StandardMoveEngine.moveFor` (merged, Rakan) | M3 in the acceptance script |
| D2 | Who gets SKIP_TURN / EXTRA_ROLL after an opponent's piece set off the chain | **DECIDED 21/11/2026 - Option A, the active player** | **Yes** | Product Owner; implemented in T2-09 and T2-11 | nothing - M5.4 can now be judged |
| D3 | What "did not answer" means, and whether it can happen at all | **DECIDED 21/11/2026 - an explicit No Answer button** | **Yes** | Product Owner; implemented in T2-11 | nothing - M4.4 can now be judged |
| D4 | A push-back that would go past the pushed piece's start square | **OPEN** | **Yes** | not implemented yet (T2-10, Shafeek) | M6.2, P10 |
| D5 | EXTRA_ROLL and SKIP_TURN arriving in the same effect chain | **OPEN** | Rarely | not implemented yet (T2-10, T2-13) | P9 |

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

> ## ✅ DECIDED — 21/11/2026 — **Option A: the active player**
>
> **The rule.** A `SKIP_TURN` or `EXTRA_ROLL` falls on the player whose **turn** it is, never on the owner of
> the piece that landed on the surprise square, even when an effect chain set that square off with an
> opponent's piece (FR-42). Red is the active player, Red pushes Green onto a surprise square, the square
> draws "skip the next turn": **Red** misses the next turn. An `EXTRA_ROLL` likewise goes to **Red**.
>
> **Engineering explanation — written by the developer, not supplied by the Product Owner.** The rule is
> consistent with FR-34, which already gives the question in exactly this situation to the active player, so
> keeping the turn-level consequence with the active player avoids two different answers to the same
> question. **What the Product Owner approved is the rule itself**, quoted above; this paragraph is only the
> technical reading of why it fits the rest of the specification, and it should not be read as the Product
> Owner's words or reasoning.
>
> **This is Option A below, not the Option B that this register proposed.** The proposal is left in place,
> marked superseded.
>
> **Requirements affected:** FR-39, FR-40, FR-42 (FR-34 as the consistency argument). The meaning of the other
> four outcomes is unchanged, and the push is still chosen by the active player.
>
> **Implemented 21/11/2026:**
>
> | Where | What changed | Commit |
> |---|---|---|
> | `Iteration2/src/Model/SurpriseResult.java` | `affected()` keeps returning the active player for a turn outcome - behaviour unchanged, since a turn outcome carries no piece - and its javadoc now records D2 as an approved rule. New `turnAffected()` returns the active player and refuses when the outcome does not change the turn, so D2 is named at the call site. `turnTargetUndecided()` **removed**. | `016d3c1` (#90) |
> | `Iteration2/test/Model/SurpriseFactoryTest.java` | 45 -> 47 tests: a turn outcome in an opponent-triggered chain falls on the active player and explicitly **not** on the owner of the landed piece; the same in an ordinary chain; `turnAffected()` refused for every piece-moving outcome. | `016d3c1` (#90) |
> | `Iteration2/src/View/SurpriseDialog.java` | `UNDECIDED_NOTE`, the `surprise.undecided` label, `recipientUndecided()`, `neutralTitle(...)` and `neutralDescription(...)` **removed**. The card now always shows the Model's own title and sentence, which are addressed to the player whose turn it is - correct under D2 - beside the pill naming that player. `surprise.chainNote` stays and never claims the opponent is affected. | `ae6e52a` (#91) |
> | `Iteration2/test/View/SurpriseDialogTest.java` | the undecided test is rewritten as the D2 rule: the resolved result reports the **active** player as affected, with the owner of the landed piece explicitly different. | `ae6e52a` (#91) |
>
> **Before removing `turnTargetUndecided()`, every caller on every branch was checked.** Only my own four
> files referred to it (`SurpriseResult`, `SurpriseDialog`, `SurpriseFactoryTest`, `SurpriseDialogTest`). No
> file owned by Shafeek or Rakan calls it on any branch, including #92, #95 and #96. `GameModel`,
> `EffectQueue`, `ResolvingState` and `StandardTurnManager` do not reference it at all.
>
> **For T2-10 (#92):** `turn.skipNextTurn(result.turnAffected())` is now the intended call. `affected()`
> returns the same value for a turn outcome, so existing code keeps working unchanged.
>
> **Test evidence, Java 17 (Temurin 17.0.17), measured 21/11/2026:** 476/476 on the T2-09 branch (from
> 474/474) and 512/512 on the T2-11 branch. `SurpriseFactoryTest` 47/47, `SurpriseDialogTest` 16/16,
> `MvcDependencyTest` 4/4.
>
> **Not verified:** no manual gameplay was performed. A surprise is still not triggered in a running game,
> because T2-10 (#92) is not merged, so M5.4 has an approved rule to judge against but has **not** been run.

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

**~~Proposal~~ — SUPERSEDED by the decision above.** This register proposed **B**, the owner of the piece that
landed, arguing that it reads the way a board game reads and makes FR-40's push meaningful. **The Product Owner
chose Option A instead**, on the grounds of consistency with FR-34, which gives the question in the same
situation to the active player. Option A is the rule; this paragraph is kept only as a record of what was
considered.

---

## D3 — What "did not answer" means, and whether a player can refuse to answer

> ## ✅ DECIDED — 21/11/2026 — **an explicit "No Answer" button**
>
> **The rule.** The question card carries an explicit button labelled **No Answer**. Pressing it submits
> `QuestionResult.NO_ANSWER` through the existing Model judging rule, which counts it as an incorrect answer
> under FR-35 and produces the backward movement of the question's level (−1 Easy, −2 Medium, −3 Hard). The
> normal feedback follows, including the correct answer (FR-36), and the player must press **Continue** to go
> on. **No timer is introduced**, and **accidentally closing the card is not an answer.** The four answer
> buttons behave exactly as before.
>
> **This is neither Option A nor Option B below.** The register proposed A (make the card non-closable and
> leave `NO_ANSWER` unreachable from the interface); the Product Owner chose to give the player an explicit,
> deliberate way to decline instead, which implements FR-35 fully without inventing a timer and without
> punishing a misclick. The proposal is left in place, marked superseded.
>
> **Requirements affected:** FR-35, FR-36, NFR-05 (presentation only).
>
> **Implemented 21/11/2026, commit `ae6e52a` (#91):**
>
> | Where | What |
> |---|---|
> | `Iteration2/src/View/QuestionDialog.java` | new button `question.noAnswer` (`QuestionDialog.NO_ANSWER_NAME`), label **No Answer**, built from the existing `CustomButton` with `Variant.GHOST` so it is the quietest option on the card. Its only action is `submit(QuestionResult.NO_ANSWER)`. The first submission now disables the four answer buttons **and** this one. Keyboard-accessible as a `JButton` (Tab, Space, Enter) with an accessible description. |
> | `Iteration2/test/View/QuestionDialogTest.java` | 12 -> 20 tests (see below). |
>
> **No question logic was duplicated in the View.** The button submits an index; `QuestionFactory.judge`
> decides that it is wrong and by how many steps. `QuestionResult` and `QuestionFactory` were **not changed** -
> `NO_ANSWER` and its judging already existed and were already tested in `QuestionFactoryTest`.
>
> **Test evidence, Java 17 (Temurin 17.0.17), measured 21/11/2026 — 8 new tests, all passing:**
>
> 1. clicking it submits `NO_ANSWER` through the Model and is judged incorrect, asked exactly once;
> 2. the backward movement is −1 / −2 / −3 for Easy / Medium / Hard, straight from the Model (FR-35);
> 3. the verdict and the correct answer are still shown, and Continue is required (FR-36);
> 4. afterwards every button is disabled, and a second click or a direct `submit` of a real answer changes
>    nothing and never asks the Model twice;
> 5. answering normally also disables the No Answer button, so the two routes cannot both be used;
> 6. Continue after No Answer calls the completion callback exactly once, however often it is clicked;
> 7. re-laying out the card answers nothing and no timer runs, so closing it cannot answer for the player;
> 8. **SPACE** is really bound on the button (`InputMap(WHEN_FOCUSED)` → `ActionMap`), and firing those
>    bindings submits `NO_ANSWER` — this replaces an earlier test that only called `doClick()`, which proves
>    nothing about the keyboard;
> 9. **ENTER is pinned down as *not* bound on the button** — a `JButton` binds only SPACE, and ENTER
>    activates the screen's default button instead;
> 10. the conditions a Tab traversal needs are asserted (in the tree, focusable, enabled, does not swallow
>    Tab) for this button and the four answers;
> 11. the button is focusable and carries an accessible description.
>
> **Tab order itself is NOT verified automatically** and is manual step **M4.4d**. The card is shown inside a
> `ModalOverlay`, which is a focus cycle root, but `LayoutFocusTraversalPolicy` only returns components that
> are *showing*, and nothing is showing without a real window — tried, and the policy returned no components
> at all. No `JFrame` is created in the suite, so this cannot be closed off-screen.
>
> `QuestionDialogTest` 23/23, full suite on the T2-11 branch **515/515**, `MvcDependencyTest` 4/4.
> **Checked by mutation:** removing the single line that disables the button on submission makes exactly 2 of
> these tests fail, so they are not vacuous.
>
> **Not verified:** no manual gameplay was performed. The question card is still not opened by a running game,
> because T2-10 (#92) is not merged, so M4.4 has an approved rule to judge against but has **not** been run.
> The Tab-order part of the keyboard requirement (M4.4d) also needs a real window and has **not** been run.

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

**~~Proposal~~ — SUPERSEDED by the decision above.** This register proposed **A**: make the card non-closable,
keep `NO_ANSWER` in the model as tested-but-unreachable code, and say so plainly in the system report. **The
Product Owner chose a third option** - an explicit button - which implements FR-35 fully, needs no timer, and
cannot punish a misclick because declining is a deliberate press rather than a closed window. This paragraph
is kept only as a record of what was considered.

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

**Done on 21/11/2026:**

1. ~~D2 and D3 must be closed before #90 and #91 merge.~~ **Both decided and implemented.** D2 is in
   `016d3c1` (#90) and `ae6e52a` (#91); D3 is in `ae6e52a` (#91). The "undecided" note no longer ships to the
   player. Both PRs had approvals before these commits, so **those approvals are now stale and the PRs need a
   fresh review** - that is expected, not hidden.

**Still to do:**

2. **D1** needs an explicit Product Owner acceptance of the start-square lock-out, or a change in Rakan's
   merged code - the first is cheap, the second is not. Blocks M3.
3. **D4** and **D5** are confirmations to request from Shafeek (T2-10) and Rakan (T2-13) rather than new
   rules. D4 is already settled by FR-32 if T2-10 implements it; D5 is partly settled inside #92, where
   `TurnState.skipNextTurn` is documented as also cancelling a pending roll from a 6 - that should be a
   recorded rule rather than a javadoc line. Block P9 and P10.
4. M3 in `T2-15_AcceptancePlan.md` stays unjudged until D1 is decided. **M4.4 and M5.4 now have an approved
   rule to judge against**, but neither has been run: the dialogs are still not opened by a running game
   until T2-10 (#92) merges.
5. Each decision above is also to be posted as a comment on the PR that implements it, so the implementer
   sees it without reading this file.
