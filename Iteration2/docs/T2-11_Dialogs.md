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
| No duplicate submission | `submit(int)` returns at once once `result != null`; all answer buttons are disabled on the first click; `Continue` hands the result over exactly once |
| Correctness and the correct answer shown (FR-36) | `question.verdict`, `question.correctAnswer`, `question.movement` |
| Not answering counts as wrong (FR-35) | `submit(QuestionResult.NO_ANSWER)` — a controller or a future timer can call it; there is no timer in this task |
| Selected surprise and its explanation | `surprise.title`, `surprise.chance`, `surprise.description` |
| Eligible pieces offered to the **active** player (FR-40) | chips `surprise.target.0…`, built from `SurprisePrompt.targets()` |
| Invalid selection prevented | the confirm button starts disabled for a push, `confirm()` refuses without a pick, and a pick the Model refuses is shown as a message with the choice cleared |
| No eligible target supported | `surprise.noTargets` shows `SurprisePrompt.NO_TARGET_NOTE`; one `Continue` button resolves it to `NOTHING` |

## 5. The undecided SKIP_TURN / EXTRA_ROLL recipient

Open point 1 of PR #90: when an effect chain set the square off with **another** player's piece (FR-42), the
approved requirements do not say who misses the next turn or who gets the extra roll.

**This task does not hardcode an answer.** In that case the card:

- replaces the Model's own title and sentence, which are written to one player ("Skip **your** next turn",
  "**You** will miss your next turn…"), with wording that names no recipient: "A turn will be skipped" /
  "One player misses their next turn, and any extra roll from a 6 is cancelled with it." Beside the pill that
  says whose turn it is, the second person would read as a decision the team has not taken. This is
  presentation only; nothing about what the Model does changes;
- adds `surprise.chainNote`, which states the fact that is certain: whose piece reached the square, during
  whose turn;
- adds `surprise.undecided` with `SurpriseDialog.UNDECIDED_NOTE`, saying the team is still agreeing it.

Tests assert that, for every outcome that only affects the turn, neither a player's name nor a second person
("you", "your") appears in the title or the sentence, and that the Model still reports
`SurpriseResult.turnTargetUndecided()`. Outside a chain the Model's own wording is shown unchanged. When the
team decides, the note and that one branch come out; nothing else changes.

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

`Iteration2/test/View/QuestionDialogTest.java` (12 tests) and
`Iteration2/test/View/SurpriseDialogTest.java` (16 tests), 28 in all; full suite **502/502** on this branch.

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
