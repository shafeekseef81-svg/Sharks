# T2-11 — Question and surprise dialogs

Task **T2-11** · Issue [#62](https://github.com/shafeekseef81-svg/Sharks/issues/62) · SRS **NFR-05**
Owner: **Mohamed Zereny**. Review level: Light — checklist (reviewer Rakan Ziyan).
Depends on T2-08 ([#59](https://github.com/shafeekseef81-svg/Sharks/issues/59), PR #89) and
T2-09 ([#60](https://github.com/shafeekseef81-svg/Sharks/issues/60), PR #90).

## 1. What this task owns

Two cards in the `View` package, shown inside the existing `ModalOverlay`:

| Class | What it shows |
|---|---|
| `View/QuestionDialog.java` | the level of the square, whose turn it is, the question, its four answers, and then the verdict, the correct answer and the movement (FR-34, FR-35, FR-36) |
| `View/SurpriseDialog.java` | the drawn outcome, its chance, its explanation and, for the push of FR-40, the Regular die and the eligible opponent pieces to choose from |

`View` and `View.components` are mine per `Iteration2/README.md`, so both files are new files of my own. **No
file of Shafeek's or Rakan's is touched.**

## 2. What already existed and was reused

Nothing of this task existed in `Iteration2/`. These existing parts were reused rather than rewritten:

- `View.components.ModalOverlay` — the dimmed in-window modal layer, with Escape handling and focus trapping.
- `View.ConfirmDialog` — the card layout both dialogs follow, and its `IconBadge` (now shared by all three).
- `View.components.CustomPanel` (`Style.CARD`), `CustomButton`, `ToggleChip`, `UiKit` (`label`, `wrap`, `pill`,
  `ltr`, `vgap`), `Theme` (`level(Difficulty)`, `player(PlayerColor)`, `SURPRISE`, `SUCCESS`, `DANGER`,
  `WARNING`) and `Icons` — the icon set already had `QUESTION`, `GIFT`, `FLAG`, `PUSH`, `CASTLE`,
  `STEPS_BACK`, `SKIP`, `ROCKET`, `CHECK` and `CROSS`, which cover all six outcomes.
- The Assignment 3 prototype's `QuestionDialog`/`SurpriseDialog` were used only as a reference for *what* to
  show. Their code was not copied: it mixed the rules into the dialog and painted everything by hand.

## 3. MVC: what the cards never do (NFR-08)

Neither card knows a rule. Both take the rule from the Model as a small interface the caller supplies:

| Card | Interface | What the caller passes |
|---|---|---|
| `QuestionDialog` | `Judge.judge(int chosenIndex)` | `QuestionFactory.judge(question, index)` (T2-08) |
| `SurpriseDialog` | `Resolver.resolve(prompt, pick)` | `SurpriseFactory.resolve(prompt, pick)` (T2-09) |

So the cards do not decide which answer is correct, do not work out how far a piece moves, do not draw an
outcome, do not roll a die, do not decide which pieces are eligible, do not change a turn and move nothing.
They display a `QuestionResult` or hand over a `SurpriseResult`. `MvcDependencyTest` still passes, and a test
checks that the number of steps shown is exactly the number the Model produced, for every level and both
outcomes.

For the surprise push the caller should pass `SurpriseFactory::resolve` rather than
`SurprisePrompt::resolve`, because the factory re-checks the pick against the board **as it is now**: an
effect chain can capture or move a piece between the moment the list is shown and the moment the player picks.
The card is asked for a resolver only when there is a pick to check; every other surprise goes through
`SurprisePrompt.resolve()`, which is Model code too.

## 4. Behaviour that was asked for, and where it is

| Requirement | Where |
|---|---|
| Question with four choices | `QuestionDialog`, buttons `question.answer.0` … `.3` |
| Difficulty of the question shown | pill `question.level`, coloured by `Theme.level(...)` |
| Only the active player answers (FR-34) | the card names the player it was given; the controller passes the active player |
| No duplicate submission | `submit(int)` returns at once once `result != null`; all four answer buttons **and the No Answer button** are disabled on the first click; `Continue` hands the result over exactly once |
| Correctness and the correct answer shown (FR-36) | `question.verdict`, `question.correctAnswer`, `question.movement` |
| Not answering counts as wrong (FR-35) | **D3:** the `question.noAnswer` button, labelled **No Answer**, calls `submit(QuestionResult.NO_ANSWER)`; the Model rule decides the rest. No timer, and closing the card is not an answer |
| Selected surprise and its explanation | `surprise.title`, `surprise.chance`, `surprise.description` |
| Eligible pieces offered to the **active** player (FR-40) | chips `surprise.target.0…`, built from `SurprisePrompt.targets()` |
| Invalid selection prevented | the confirm button starts disabled for a push, `confirm()` refuses without a pick, and a pick the Model refuses is shown as a message with the choice cleared |
| No eligible target supported | `surprise.noTargets` shows `SurprisePrompt.NO_TARGET_NOTE`; one `Continue` button resolves it to `NOTHING` |

## 5. D2 — the SKIP_TURN / EXTRA_ROLL recipient: DECIDED

**Product Owner decision D2, approved 21/11/2026: Option A — the active player.**

When an effect chain sets a surprise square off with **another** player's piece (FR-42), a `SKIP_TURN` or an
`EXTRA_ROLL` falls on the player whose **turn** it is, never on the owner of the piece that landed. Red pushes
Green onto a "skip the next turn" square: **Red** misses the next turn.

*Rationale recorded by the Product Owner:* FR-34 already gives the question in exactly this situation to the
active player, so keeping the turn-level consequence with the active player is the consistent reading, and it
keeps every turn-level consequence with the player who caused the chain.

**What changed in this task (the View side of D2).** The card previously named nobody and warned that the
rule was open. That is all removed, because the rule is now decided:

| Removed | Why |
|---|---|
| `SurpriseDialog.UNDECIDED_NOTE` and the `surprise.undecided` label | the rule is decided, so there is nothing to warn about |
| `recipientUndecided()` | no branch depends on it any more |
| `neutralTitle(...)` and `neutralDescription(...)` | the Model's own wording is written to the player whose turn it is ("Skip **your** next turn"), which under D2 is exactly right |

**What stayed.** The card shows `SurpriseOutcome.title()` and `description()` unchanged in every case, beside
the `surprise.activePlayer` pill that names the active player. `surprise.chainNote` still appears when an
opponent's piece set the square off — it states the fact ("this square was reached by Shafeek's piece during
Mohamed's turn"), names both players, and never claims the opponent is the one affected. That is what keeps
the requirement "displaying SKIP_TURN or EXTRA_ROLL must never identify the opponent as the affected player".

The Model side of D2 is in `T2-09_SurpriseOutcomes.md` section 8: `SurpriseResult.turnAffected()` is new,
`turnTargetUndecided()` is removed, and `affected()` is unchanged in behaviour.

MVC is unchanged: the card still decides nothing. It reads the outcome and the prompt from the Model and sends
the pick back through the `Resolver`.

## 5a. D3 — the No Answer button: DECIDED

**Product Owner decision D3, approved 21/11/2026: an explicit No Answer button.**

FR-35 counts not answering as a wrong answer, but until now nothing in the interface could produce it —
`submit(QuestionResult.NO_ANSWER)` existed for a controller or a future timer only. The decision is that the
player gets an explicit button instead.

| What | Where |
|---|---|
| The button | `QuestionDialog`, name `QuestionDialog.NO_ANSWER_NAME` = `question.noAnswer`, label **No Answer** |
| Variant | `CustomButton.Variant.GHOST` — the existing component, visually the quietest option on the card, so it never competes with the four answers |
| What it does | one line: `submit(QuestionResult.NO_ANSWER)`. **No question logic is duplicated in the View**; the Model's `QuestionFactory.judge` decides that it is wrong and by how many steps |
| Backward movement | whatever the Model returns: −1 Easy, −2 Medium, −3 Hard (FR-35), shown by `question.movement` |
| Feedback | the ordinary `question.verdict`, `question.correctAnswer` and `question.movement`, so FR-36 is met for this route too |
| Continue | unchanged: the player must press `question.continue`, and the callback runs exactly once |
| No second submission | `submit` returns immediately once `result != null`, and the first submission disables the four answer buttons **and** this one |
| Keyboard | it is a `CustomButton extends JButton`, so Tab reaches it and Space or Enter presses it; it also carries an accessible description for a screen reader |
| No timer | none was added, as the decision requires |
| Closing is not answering | the card owns no close control and starts no timer, so nothing can answer on the player's behalf |

## 6. NFR-05 — "the dialog opens within 1 s" — NOT verified

What was measured: **building and laying out each card**, on the event dispatch thread, after a warm-up, with
no window. Both are far under the limit (the tests assert under 500 ms and in practice it is a few
milliseconds).

What NFR-05 actually asks for is the time **from the piece stopping to the dialog being on screen**. That path
does not exist yet (section 7), so **it has not been measured and NFR-05 is not verified.** The acceptance box
in issue #62 must stay unticked. What is still missing from the measurement:

- noticing that the piece stopped on a special square — `EffectQueue.start(...)` / `next()`, T2-10;
- the `RESOLVING` turn phase that decides a dialog is due — T2-13;
- the movement animation that runs before it — `GameController.animate(...)`, Rakan's file;
- `ModalOverlay.show(...)`'s fade, about 170 ms, and the real frame's repaint.

Once T2-10 and T2-13 are merged, the measurement is a stopwatch from the end of `animate` to the overlay being
visible, on the two computers of CP1. That belongs to T2-14/T2-15.

## 7. Blocked: the cards are not wired into a running game

The dialogs are complete and tested, but **nothing opens them yet**, and finishing that is not mine to do:

| What is needed | Whose file |
|---|---|
| noticing that a piece stopped on a question or surprise square, and the chain order | `EffectQueue` — Shafeek, T2-10 (PR #87) |
| the `RESOLVING` phase that refuses rolls while a dialog is open | `StandardTurnManager` / State classes — Rakan, T2-13 (#64) |
| creating the factories for a game and exposing the pending activation | `GameModel` — Rakan's shared contract |
| `nav.overlay(card)` and closing it, after the move animation | `GameController`, `NavigationController` — Rakan |

**I stopped at the boundary and changed none of those.** The exact wiring I propose, for Rakan to place in
`GameController` once T2-10 and T2-13 are in:

```java
// a question square (FR-34): the ACTIVE player answers, whoever owns the piece
QuestionFactory factory = factories.get(activation.square().questionLevel());
Question question = factory.ask();
ModalOverlay[] holder = new ModalOverlay[1];
QuestionDialog card = new QuestionDialog(question, model.currentPlayer().name(),
        index -> factory.judge(question, index),
        result -> { holder[0].close(); effects.run(new MoveStepsEffect(activation.piece(), result.steps())); });
holder[0] = nav.overlay(card);

// a surprise square (FR-39, FR-40)
SurprisePrompt prompt = surprises.draw(activation.piece(), model.currentPlayer().color());
ModalOverlay[] holder = new ModalOverlay[1];
SurpriseDialog card = new SurpriseDialog(prompt,
        color -> model.playerOf(color).name(), surprises::resolve,
        result -> { holder[0].close(); applyToQueue(result); });   // the switch of PR #90
holder[0] = nav.overlay(card);
```

Note `surprises.draw(piece, currentPlayer().color())` — the two-argument form. The one-argument form would be
wrong inside a chain, for the reason PR #90 documents.

## 8. Tests

`Iteration2/test/View/QuestionDialogTest.java` (20 tests) and
`Iteration2/test/View/SurpriseDialogTest.java` (16 tests), 36 in all; full suite **512/512** on this branch,
measured 21/11/2026 with `Iteration2/test.sh` on Java 17 (Temurin 17.0.17) after D2 and D3.

**D3 adds 8 tests to `QuestionDialogTest`** (12 -> 20): the button submits `NO_ANSWER` through the Model and
is judged wrong; it moves backward by the level for Easy, Medium and Hard (−1/−2/−3); the verdict and the
correct answer are still shown and Continue is required; after it every button is disabled and a second click
or a direct `submit` of a real answer changes nothing and never asks the Model twice; answering normally
disables the No Answer button so the two routes cannot both be used; Continue after No Answer calls back
exactly once; the button is focusable and carries an accessible description; and closing or re-laying out the
card answers nothing, with no timer running. Checked by mutation: removing the one line that disables the
button makes exactly 2 of these tests fail.

**D2 changes `SurpriseDialogTest`** (16 tests, count unchanged): the test that asserted the undecided note and
that the card named nobody now asserts the decided rule instead — the card shows the Model's own title and
sentence, the `surprise.undecided` label is gone, the active player is named in the pill, the chain note still
names both players, and the resolved `SurpriseResult` reports the **active** player as affected with the owner
of the landed piece explicitly different. The now-unused `assertNoSecondPerson` helper was removed.

Both wire the cards to the **real** Model factories, as a controller would, and build every component on the
event dispatch thread without a `JFrame`, so they run headless like the other `View` tests.

They cover: the question, its level, its four answers and the player's name; nothing given away before
answering; a correct answer and a wrong answer reported with the correct answer and the direction; the steps
shown being exactly the Model's, for all three levels and both outcomes; a second click not answering again
and the Model being judged exactly once; not answering counted as wrong; the result handed over once however
often `Continue` is clicked; a judge that returns nothing failing loudly; an invalid answer index refused;
the drawn outcome, chance and explanation for all six outcomes; one button for the five that need no choice;
every eligible opponent piece offered and never the active player's own; nothing confirmable before a choice;
the chosen piece being the pushed piece and not the leading one; the no-eligible-target case; a refused pick
reported and retried with the chip selection really cleared; a second confirmation ignored; the resolver
asked only when there is a pick; an opponent-triggered chain naming the turn holder; a turn outcome in such a
chain naming nobody, in neither a name nor a second person; an ordinary surprise showing neither note;
null arguments; and the build-and-lay-out timing of section 6.

`Iteration2/test/Model/PiecePlacement.java` is new test infrastructure: `Piece.setProgress` is
package-private on purpose (NFR-08), so a `View` test cannot place a piece. This one public helper in the test
tree is the single door for that, following the pattern of the existing `FixedDice`. It is never packaged into
the JAR, because `build.sh` compiles `src/` only.

**Not claimed:** no end-to-end question or surprise in a running game, and NFR-05 not verified (sections 6
and 7). A manual smoke test of both cards on screen is still required for the Light review, and can only be
done once the wiring exists.
