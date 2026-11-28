# T2-15: Product Owner acceptance record, Iteration 2

Task **T2-15** · Issue [#66](https://github.com/shafeekseef81-svg/Sharks/issues/66) ·
Product Owner: **Mohamed Zereny** · Stories [#48](https://github.com/shafeekseef81-svg/Sharks/issues/48),
[#49](https://github.com/shafeekseef81-svg/Sharks/issues/49),
[#50](https://github.com/shafeekseef81-svg/Sharks/issues/50),
[#51](https://github.com/shafeekseef81-svg/Sharks/issues/51)

Issue #66 asks for **"PO acceptance recorded for each story"**. This file is that record: one block per
story, with the evidence behind it and what is still missing, so the acceptance is a judgement on stated
evidence rather than an impression.

**Baseline for every figure here: `iteration-2` `06e2ab2`, suite 705/705** (0 failed, 0 skipped,
0 aborted), re-run 28/11/2026 on Temurin 17.0.17+10.

> **This file does not accept anything by itself.** Each block ends with a line the Product Owner fills in.
> An unfilled line means the story is **not** accepted, however good the evidence above it looks.

---

## How to read the evidence: four kinds, never mixed

The four are not interchangeable, and a claim is only as strong as its weakest supporting kind.

| Tag | Kind | What it is | What it cannot show |
|---|---|---|---|
| **A** | **Automated model test** | JUnit 5 through the `GameModel` facade with the real dice, boards and rule classes, fixed seeds. Off-screen. | Anything about what is **painted**, or about the application as a user meets it. The No Answer defect passed 23 of these. |
| **G** | **GUI-driven gameplay** | Real Win32 mouse clicks and key presses delivered to the real Swing window. Nothing simulated inside the application. **Moves chosen by a script, not by a person.** | It is not a human play-test. A script does not notice that something looks wrong unless it was told to measure it. |
| **H** | **Human playthrough** | A person playing the real game by hand and reporting what they saw. | Not repeatable, and not measured. |
| **T** | **Targeted harness** | A purpose-built program compiled **against the released JAR**, driving a real production component in a real window to measure one thing (used for NFR-04). | It is one component, not the game being played. |

Where evidence is a teammate's, it says so and is **not** merged into mine.

---

## US-2.1 — Medium and Hard boards with Boost / UltraBoost dice (#48)

**SRS IDs:** FR-05, FR-10–FR-12, FR-14, FR-16, FR-17, FR-25 · Story owner: Mohamed Zereny

| Requirement | Tests | Strongest evidence |
|---|---|---|
| FR-05 all three levels selectable | 22 mentions | **A** `SettingsValidationTest`, `MediumHardAcceptanceTest`; **G** both boards started from the menu |
| FR-10–FR-12 board, special squares, random placement | 20 / 16 / 15 | **A** `BoardGeometryTest`, `DiceRouteIndependentTest` (independent); **G** figures G2, G3 |
| FR-14 legend | 13 | **G** the on-screen legend prints the same counts as `LevelConfig` |
| FR-16 both dice shown | 19 | **A** `DiceStrategyTest`; **G** the dice panel on both boards |
| FR-17 movement = Regular × (special − 1) | 24 | **A** `DiceStrategyTest`, `SpecialDiceGameTest`; **G** the formula printed live, including `6 × (6 − 1) = 30 steps` on Hard |
| FR-25 routes | 23 | **A** `DiceRouteIndependentTest` (Rakan, independent — geometry written out by hand from the SRS) |

**Complete games (A):** 12 complete Medium and Hard games played to a winner and a full ranking by
`MediumHardAcceptanceTest`, four players, random boards, fixed seeds.
**On screen (G):** both boards confirmed, with the track arithmetic, the counts of every special square and
the dice panel; a roll of 1 correctly passing the turn with all pieces in the castle.

**Nothing outstanding for this story.**

> **Product Owner decision:** ______________________  **Date:** __________

---

## US-2.2 — Double Blocks (#49)

**SRS IDs:** FR-19, FR-28–FR-30 · Story owner: Rakan Ziyan

| Requirement | Tests | Strongest evidence |
|---|---|---|
| FR-19 Regular 6 leaves the castle | 25 | **A** `DoubleBlockEngineTest`, `RuleOfSixTest` |
| FR-28 a block forms | 13 | **A** `DoubleBlockEngineTest` (author), `DoubleBlockIndependentTest` (Shafeek, independent) |
| FR-29 cannot land on or pass, in either direction | 22 | **A** both classes above; `isPassable` is direction-free |
| FR-30 the block breaks when a piece leaves | 6 | **A** `DoubleBlockEngineTest` |

**Mutation-checked (A):** with blocking switched off, **16 of the 19 independent tests fail** — so the tests
are not passing by accident.

**Outstanding: decision D1.** An opponent Double Block standing on a player's **start** square keeps that
player's pieces in the castle even on a Regular 6. **This is what FR-19 literally specifies** — it states the
exception itself. What is open is narrower: whether a block forms on a start square *at all* (course answers
Q7/Q35, which the SRS cites by number without reproducing). **D1 is OPEN and is not closable by the Product
Owner alone.**

> This story can be accepted **only** as "accepted with D1 recorded as an open question against the course
> staff", or held until the staff answer. The Product Owner should say which.

> **Product Owner decision:** ______________________  **Date:** __________

---

## US-2.3 — Question and surprise squares, including chained effects (#50)

**SRS IDs:** FR-26, FR-27, FR-32, FR-34–FR-42 · Story owner: Mohamed Zereny

| Requirement | Tests | Strongest evidence |
|---|---|---|
| FR-26 / FR-27 capture and protected squares | 26 / 15 | **A** `SquareEffectTest`, `SpecialDiceGameTest` |
| FR-32 backward movement stops at the start square | 4 | **A** `EffectChainIndependentTest` (independent). **Decision D4, RATIFIED 28/11** |
| FR-34 the question is of the **square's** level | 12 | **A** `QuestionFactoryTest`; **G** an Easy question on a Medium board, and both an Easy and a Hard question on a Hard board |
| FR-35 / FR-36 movement and feedback | 13 / 5 | **A** `QuestionFactoryTest`, `QuestionDialogTest`; **G** right, wrong and No Answer each on screen, the correct answer named every time |
| FR-37 no repeat until the deck is used up | 6 | **A** `QuestionFactoryTest`. **The reshuffle is never reached by natural play** — the most questions of one level in one game is exactly 12, the deck size |
| FR-38 capture before the question | 3 | **A** `SquareEffectTest` and, independently, `EffectChainIndependentTest`. The thinnest row in the story |
| FR-39 exactly one outcome | 21 | **A** `SurpriseFactoryTest` (47 tests), `SurpriseOutcomeTest` |
| FR-40 the active player chooses the opponent piece | 31 | **A** `SurpriseDialogTest`, `SurpriseFactoryTest` |
| FR-41 / FR-42 chains obey the rules; chain guard | 8 / 16 | **A** `EffectChainIndependentTest`, `SquareEffectTest` |

**Complete games with everything live (A):** five Hard and three Medium games with questions, surprises and
chains all active, each ending with a winner. 94 questions asked across the five Hard games, **0 repeats
within any game**, 34 distinct ids of the 36 that exist, 22 surprises, and **all six surprise outcomes
drawn**.
**Decisions:** **D2 DECIDED 21/11** (a turn outcome falls on the active player) and **D3 DECIDED 21/11** (an
explicit No Answer button) are both implemented and merged. **D5 RATIFIED 28/11.**
**On screen (G):** the question card, the level badge, all three answer outcomes, and — after PR #111 — the
No Answer button visible, focusable and clickable.

**Outstanding, and it is the weakest point in the iteration's evidence:**

1. **No surprise square has ever been reached in a GUI run.** Surprises are covered end to end by **A**, and
   by Rakan's **H** full game only to the extent that he reported the game finishing — he did not itemise
   which squares he landed on. **Nobody has recorded seeing a surprise card on screen.**
2. **Chained effects likewise** — 2 chain steps in five Hard games (**A**), never seen on screen.
3. **FR-38 and FR-36 are the thinnest rows** (3 and 5 tests).

> **Product Owner decision:** ______________________  **Date:** __________

---

## US-2.4 — Every rule checked by unit tests; State pattern for turn phases (#51)

**SRS IDs:** NFR-02, NFR-04, NFR-05, NFR-13, NFR-14 · Story owner: Shafeek Saif

| Requirement | Evidence |
|---|---|
| **The suite itself** | **705 tests, 705 passing**, 0 failed, 0 skipped, 0 aborted. Model 542, View 142, Controller 17, Architecture 4 |
| NFR-02 board appears | **A/T** worst **78 ms** over 21 random Hard boards, limit 2000 ms |
| NFR-04 longest movement | **T** 30 squares in a real visible window, **1296–1313 ms** over 5 runs (mean ≈1305 ms); **G** 16 in-game measurements all under 2000 ms, longest 1578 ms for a 10-step move, already at the 1200 ms cap. Limit 2000 ms |
| NFR-05 | **A** `EffectsAcceptanceTest`, `MediumHardAcceptanceTest`, `QuestionDialogTest` |
| NFR-13 dice distribution | **A** `DiceRouteIndependentTest` (independent), `RegularDiceTest`, `SurpriseFactoryTest` — 10,000 draws per level, each value within 2 points of its expected share |
| NFR-14 traceability | Satisfied by the team's own tool, `tools/rtm_coverage.py`, **not** by a test — it has 0 mentions in the suite, which is correct. Result on `06e2ab2`: **`GAPS: none` for FR-15…FR-47** |
| **State pattern** | `TurnState`, `StandardTurnManager`, `ResolvingState`; `TurnStateTest` 9 tests (NFR-09). Documented in `iteration2_dp_Sharks` section 3 |

**Independence (SDP 5.3.4).** Every rule is tested by someone who did not write it:
`DiceRouteIndependentTest` 15, `DoubleBlockIndependentTest` 19, `EffectChainIndependentTest` 25.
**Mutation checking.** All eight seeded faults in the special-square rules were caught; 16 of 19 Double Block
tests fail with blocking off; one test was rewritten because it did **not** catch its mutant.

**Outstanding:** a 30-square movement arising **inside a full game** and timed on screen. The 30-square
figure is **T** (a harness) and the in-game figure is a 10-step move (**G**); the two have not been combined.

> **Product Owner decision:** ______________________  **Date:** __________

---

## The two acceptance criteria of issue #66

| Criterion | State |
|---|---|
| PO acceptance recorded for each story | **This file, once the four decision lines are filled in.** |
| A full game played to the end on Medium and on Hard | **Partly.** **A**: 12 complete Medium and Hard games, plus 8 more with every effect live, all to a winner and a full ranking. **H**: Rakan played one full game to the results screen by hand on computer 1 (N2-06 row 8, #109) — his evidence, on an unstated level. **Not done:** a complete Medium **and** a complete Hard game played to the results screen by a person following scripts M1 and M2. |

---

## Decisions at a glance

| # | State | Where |
|---|---|---|
| D1 | **OPEN** — needs the course staff on Q7/Q35 | blocks nothing in the code; the implementation follows FR-19 |
| D2 | DECIDED 21/11/2026 | implemented, merged, mutation-checked |
| D3 | DECIDED 21/11/2026 | implemented, merged |
| D4 | **RATIFIED 28/11/2026** | no gameplay change; behaviour already merged and independently tested |
| D5 | **RATIFIED 28/11/2026** | no gameplay change; two named tests re-run green for the ratification |

Full register, with the Product Owner's own wording: `Iteration2/docs/T2-15_PO_Decisions.md`.

---

## What still needs a human, or a second physical computer

Nothing in this list can be closed by any automation we have. It is deliberately short: these are the only
gaps left.

### Needs a person at the screen

| # | Check | Why automation cannot close it | Script |
|---|---|---|---|
| H1 | A complete **Medium** game played by hand, menu to results screen | A script does not judge whether the game *looked* right on the way; this is the acceptance of US-2.1 and US-2.4 as a player meets them | M1 |
| H2 | A complete **Hard** game played by hand, menu to results screen | as above | M2 |
| H3 | **Reach a surprise square and read the card**: confirm exactly one outcome, and for "move an opponent" that **you** choose the piece and nothing is auto-selected (FR-39, FR-40) | **No surprise card has ever been seen on screen.** This is the largest single gap in the iteration's evidence | M5 |
| H4 | **See a chained effect**: a question or surprise movement landing on another special square and opening a second card (FR-41, FR-42) | Rare in natural play — 2 chain steps in five full automated games | M6 |
| H5 | **Stopwatch a 30-square Hard move inside a real game** (Regular 6 × UltraBoost 6) and write down the milliseconds | The 30-square figure we have is from a harness; the in-game figure is a 10-step move. Only a person can do both at once | M2.4 |
| H6 | Press **Tab** then **Space** on the question card and confirm the **No Answer** button is visible and shows its focus ring | The regression test proves the pixels; a person confirms it reads as a button | row 3 |
| H7 | Confirm the **results screen**: ranking in finishing order, with the game clock (FR-43–FR-47) | FR-47 is the thinnest requirement in the range (2 tests) | M1.7 |
| H8 | Record the **Product Owner acceptance** of US-2.1 to US-2.4 in this file | A judgement, not a measurement | — |

### Needs a second physical computer

| # | Check | Note |
|---|---|---|
| S1 | N2-06 rows 1–8 on a **second computer** | Must be run on a **byte-identical copy** of the final ZIP, not on a rebuild. Verify with `Get-FileHash -Algorithm SHA256` before testing |
| S2 | Double-click `run.bat` **there** | The row that can disqualify the assignment |
| S3 | Double-click `EducationalLudo.jar` **there** | A separate required row |
| S4 | A game started from a folder whose path contains **Hebrew letters**, there | The Iteration 1 failure mode |

**A second window, a virtual machine, another worktree or another process on this machine is not a second
computer** and must not be recorded as one.

### Needs the course staff

| # | Item | Note |
|---|---|---|
| C1 | **Decision D1** — does a Double Block form on a start square at all (Q7/Q35)? | The SRS cites those answers by number without reproducing them, and the Q&A sheet is not in this repository. **Not closable by the Product Owner.** |
