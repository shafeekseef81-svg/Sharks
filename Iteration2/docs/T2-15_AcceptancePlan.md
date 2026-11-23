# T2-15: acceptance plan and test script for the Iteration 2 stories

Task **T2-15** · Issue [#66](https://github.com/shafeekseef81-svg/Sharks/issues/66) · Owner: **Mohamed Zereny** (Product Owner) · Reviewer: Shafeek Saif · Due 05/12/2026

Stories: [US-2.1 #48](https://github.com/shafeekseef81-svg/Sharks/issues/48), [US-2.2 #49](https://github.com/shafeekseef81-svg/Sharks/issues/49), [US-2.3 #50](https://github.com/shafeekseef81-svg/Sharks/issues/50), [US-2.4 #51](https://github.com/shafeekseef81-svg/Sharks/issues/51).

**Status 23/11/2026, second revision — the dependency is gone.** T2-09 (#90), T2-11 (#91) and T2-10 part 2 (#92) are **merged**, so questions, surprises and chained effects now run in a build made from `iteration-2`. The automated acceptance below is therefore of the **real** game, not of a baseline in which a third of the board did nothing. What stays manual stays manual by design.
Nothing in this file is recorded as passed unless it was actually run and its numbers are given.

## 1. What this task has to accept, and what the baseline allows today

The baseline for acceptance is `iteration-2`, today at **`1d47f2d`** (`e9f6b2e` earlier on 23/11,
`d445a38` on 22/11, `6e13bcf` on 21/11).

**Merged into it:** T2-01 (#84), T2-02 (#82), T2-03 (#83), T2-04 (#76, #80), T2-05 (#78), T2-06 (#77),
T2-07 (#85), T2-08 (#89, `5540f53`), T2-10 part 1 (#87, `e2bbdaa`), T2-13 (#79, `c689a85`), T2-14 (#93),
and — **new on 23/11** — **T2-09 (#90, `36661fc`)**, **T2-11 (#91, `12a01b6`)** and **T2-10 part 2
(#92, `1d47f2d`)**.

**Not merged:** NFR-04 (#97, approved), T2-12 (#95, approved).

> **History of this paragraph, kept so the corrections are traceable.** On 21/11 it listed T2-08, T2-10,
> T2-13 and T2-14 as not merged; all four merged on 22/11. It then said the one remaining dependency for the
> US-2.3 acceptance was **#92** — and that was right, until #92 merged later on 23/11. **There is now no
> unmerged dependency for the automated acceptance of US-2.3.**

That split decides what can honestly be accepted now:

| # | Acceptance area | Story | Possible on `iteration-2` today? | How it is covered |
|---|---|---|---|---|
| A1 | Medium and Hard board setup | US-2.1 | **Yes** | automated, section 2 |
| A2 | Medium and Hard gameplay to the end | US-2.1, US-2.4 | **Yes** | automated, section 2 |
| A3 | Boost and UltraBoost dice | US-2.1 | **Yes** | automated, section 2 |
| A4 | Double Blocks | US-2.2 | **Yes** - landing **and passing** (FR-29 in full, since 23/11) | automated, section 2 |
| A5 | Questions, correct and incorrect answers | US-2.3 | **Yes**, since #92 merged | automated here and in P12 (#103), manual script M4, section 4 |
| A6 | Surprise outcomes | US-2.3 | **Yes**, since #90 and #92 merged | automated here and in P12 (#103), manual script M5, section 4 |
| A7 | Chained effects | US-2.3 | **Yes**, since #92 merged - but **rare in natural play**, see section 2 | automated in P12 (#103); the scripted coverage is T2-12 (#95), manual script M6 |
| A8 | Turn State transitions | US-2.4 | **Yes** - #79 gave the states, #92 is what ever enters `RESOLVING` | automated here and in P12 (#103), manual script M7, section 4 |
| A9 | A full Medium and Hard game played by a person | all | **the code is all there now**; nobody has played it | manual script M1-M3, section 4 — **still the real gap** |

**What changed on 23/11, and what it means for this file.** Until #92 merged, a question or surprise square
was an ordinary square: nothing happened when a piece stopped on it, and the complete games of section 2
exercised the question and surprise rules *not at all*. That is no longer true. `playToEnd` now resolves
every waiting square, and any chain it sets off, before the next roll — so the games below are played on the
whole board.

**This is also why five of these seven tests failed when #92 first met this branch.** `playToEnd` called
`model.roll()` while the turn was still in `RESOLVING`, and the turn refused it:
`IllegalStateException: Resolve the special square first (phase RESOLVING)`. Shafeek found this by running
the branch against `iteration-2` + #92 rather than against its own baseline, which is the only reason it was
caught before a merge. The resolution loop is the fix.

**What this file still does not assert about the effects.** It resolves them *neutrally* — it answers
questions and carries out surprises the way a player would, and checks only that the turn leaves
`RESOLVING` and that the chain guard holds (FR-42). It makes no assertion about what a question or a
surprise *does*. That is **P12** (`EffectsAcceptanceTest`, #103) end to end, and Rakan's
`EffectChainIndependentTest` (T2-12, #95) per rule. Duplicating their assertions here would add no coverage
and two more places to maintain.

## 2. What is automated and passing today

`Iteration2/test/Model/MediumHardAcceptanceTest.java` - 7 acceptance tests, written by the Product Owner.
Games are played to their end through the `GameModel` facade with the real dice, the real random boards and
all the real rule classes; there are no stubs and no fixed dice, so a pass means a player really can play
that game. Deterministic seeds are used so a failure can be reproduced exactly.

Measured on **23/11/2026**, branch `feature/mohamed/T2-15-acceptance` merged with `iteration-2`
(**`1d47f2d`**, i.e. with #90, #91 and #92 in), Java 17 (Temurin 17.0.17), `Iteration2/test.sh`:

* `Model.MediumHardAcceptanceTest` alone: **7 found, 7 successful, 0 failed**
* the whole suite with it: **663 found, 663 successful, 0 failed**
* the suite without it, i.e. the `iteration-2` baseline: **656** tests
* `EducationalLudo.jar` builds

Earlier figures for the same seven tests, kept so the growth is traceable: 7/7 with **547/547** on
`e9f6b2e` (earlier on 23/11, before #92), 7/7 with **538/538** on `d445a38` (22/11), and 7/7 with
**440/440** on `6e13bcf` (21/11). The suite grew because other tasks merged, not because this task changed.

| Test | Accepts | Requirements |
|---|---|---|
| a player can start a Medium or a Hard game with 2, 3 or 4 players | every level and player count starts, four pieces each, all in their castles | FR-05 |
| the Medium and Hard board a player gets matches Table 1, over 20 games | grid size, track squares, protection / question / surprise / starting / arrow counts, and the questions per level, on 20 random boards per level | FR-10, FR-11, FR-12 |
| a complete Medium game rolls Boost 1-3 and moves Regular x (Boost - 1) | the Boost die is the Medium die, every value 1-3 really came up, a Boost 1 moves nothing, the longest movement is 12 | FR-16, FR-17 |
| a complete Hard game rolls UltraBoost 1-6 and never moves over 30 | the UltraBoost die is the Hard die, every value 1-6 really came up, an UltraBoost 1 moves nothing, the longest movement is 30 | FR-16, FR-17 |
| over complete Medium and Hard games no route lands on or passes an opponent's Double Block | every square entered by every move of 6 complete games (3 seeds x 2 levels) is checked against the blocks standing before that move - 19,698 squares in all - and the test also proves the check was not empty (see below) | FR-28, FR-29 |
| complete Medium games end with a winner and a full ranking, over 5 seeds | the game ends once, all but one player finished, a finished player has all four pieces home, the ranking follows the finishing order, exactly one rank 1 | FR-43, FR-44, FR-45 |
| complete Hard games end with a winner and a full ranking, over 5 seeds | the same for Hard | FR-43, FR-44, FR-45 |

In addition, every move of every game above is checked for: the movement formula of the level (FR-17), that
no piece ever leaves the board, that only opponents are captured and never on a protected square (FR-26,
FR-27), and that no move **ends on or passes through** an opponent's Double Block (FR-29).

### The Double Block check is not vacuous, and FR-29 now covers passing

Two weaknesses in the 22/11 version of this test were found in Shafeek's review of #99 and fixed on 23/11.

**1. FR-29 covers "landing or passing in either direction", and only landing was checked.** The test now
walks `MoveResult.cells()`, the whole route of the move, and checks every square the move actually entered -
every cell after the first, or from the first on a castle exit, where the one cell in the route is the start
square the piece entered (FR-19).

*Measured evidence that this is a real gain.* With `StandardMoveEngine.firstBlocked` mutated so that only
the **landing** square is blocked and passing straight through a block is allowed:

| Version of the test | Result against that mutant |
|---|---|
| 22/11, landing only | **7/7 passed** - the defect was not detected |
| 23/11, whole route | **4 of 7 failed**, e.g. *"HARD seed 2: FR-29 a YELLOW piece entered a Double Block of GREEN at step 7 of 20 (STEPS from 83 to 103)"* |

The failures name intermediate steps of the route, which is what makes them evidence about passing rather
than landing. The engine was restored immediately afterwards and the suite re-run at 547/547.

**2. The Double Block test counted moves, not blocks.** It added `p.moves` into a variable called
`blocksSeen` and then asserted that it was positive, which is true of any game in which anything moved at
all. It never showed that an opponent's block had ever stood. The test now measures the real condition and
prints it per game:

Measured on `1d47f2d`, i.e. with the effects live:

| Level | Seed | Moves | Squares entered and checked | Rolls with a block standing | Moves under an opponent's block | Questions | Surprises |
|---|---|---|---|---|---|---|---|
| Medium | 11 | 543 | 2,583 | 138 | 67 | 22 | 7 |
| Hard | 11 | 461 | 3,876 | 34 | 14 | 12 | 9 |
| Medium | 12 | 578 | 2,737 | 62 | 30 | 22 | 4 |
| Hard | 12 | 486 | 4,392 | 24 | 10 | 24 | 5 |
| Medium | 13 | 569 | 2,730 | 232 | 110 | 27 | 10 |
| Hard | 13 | 381 | 3,380 | 36 | 14 | 22 | 7 |

19,698 squares checked, and 129 questions and 42 surprises resolved along the way.

**The figures moved when #92 merged, and that is worth understanding rather than glossing over.** Resolving
the effects changes where the pieces go, so these are different games from the ones measured before #92 —
same seeds, different trajectories. One consequence is a happy one: **Hard seed 12 used to produce no
opponent Double Block at all** (0 rolls with a block standing), which was the clearest evidence that the old
"count the moves" assertion was empty. With the effects live it produces 24, so all six games now exercise
the check.

The assertion is still made over the three seeds of a level **together** rather than per game. That is
deliberate and stays: the per-game figure depends on the trajectory, and a trajectory can change again — as
it just did — leaving a per-game assertion to fail for a reason that is not a defect. The per-game numbers
are printed in the failure message, so a reader does not have to take the coverage on trust.

That an opponent's block cannot be landed on or passed **when one does stand in the way** is pinned down by
the scripted scenarios of T2-06/T2-07 (`DoubleBlockEngineTest`, `DoubleBlockIndependentTest`, both merged).
This acceptance test deliberately does not repeat them; what it adds is that over six complete games with
the real dice, no route ever crossed one.

### The games really were played to the end

These are the games the tests above play, measured separately on 21/11/2026 so the numbers are real and not
assumed. Four players, random board, random legal choice per turn:

| Level | Seed | Rolls | Moves | Captures | Rolls that moved nothing | Longest movement | Players finished |
|---|---|---|---|---|---|---|---|
| Medium | 1 | 767 | 516 | 22 | 258 | 12 | 3 of 4 |
| Medium | 2 | 845 | 570 | 21 | 275 | 12 | 3 of 4 |
| Medium | 3 | 802 | 526 | 19 | 261 | 12 | 3 of 4 |
| Medium | 4 | 1153 | 788 | 41 | 402 | 12 | 3 of 4 |
| Medium | 5 | 805 | 551 | 19 | 270 | 12 | 3 of 4 |
| Medium | 2026 | 823 | 573 | 21 | 265 | 12 | 3 of 4 |
| Hard | 1 | 623 | 482 | 17 | 103 | 30 | 3 of 4 |
| Hard | 2 | 506 | 371 | 4 | 77 | 30 | 3 of 4 |
| Hard | 3 | 485 | 375 | 11 | 72 | 30 | 3 of 4 |
| Hard | 4 | 583 | 451 | 13 | 92 | 30 | 3 of 4 |
| Hard | 5 | 535 | 448 | 14 | 80 | 30 | 3 of 4 |
| Hard | 2026 | 644 | 488 | 13 | 105 | 30 | 3 of 4 |

Two things worth passing on to the team:

1. **A movement of 0 is common.** On Medium about a third of all rolls move nothing (a Boost 1), on Hard
   about a sixth. That is FR-17 working as written, but it is a lot of empty turns for a player, and it is
   worth a word in the retrospective (N2-07) rather than a code change, because the formula is the SRS's.
2. **The 30-square Hard movement really occurs** in ordinary play - it is the longest movement in every Hard
   game above. That is the movement NFR-04 has to animate inside two seconds, so it is not a corner case.
   See the animation budget change on branch `feature/mohamed/NFR-04-animation-budget`.

## 3. What is still to automate, once the dependencies merge

These tests are designed but **not written**, because the code they would drive is not on `iteration-2`.
They go into a second class, `QuestionSurpriseAcceptanceTest`, when T2-08, T2-09, T2-10 and T2-13 are merged.
Each one uses a fixed die (`Model.FixedDice`) or a scripted random (`Model.ScriptedRandom`) so the piece can
be steered onto a chosen square.

| # | Planned test | Accepts | Requirements | Needs |
|---|---|---|---|---|
| P1 | a piece stopping on a question square is asked a question of that square's level | the level of the question is the square's level, not the board's | FR-34 | #89, #87/#92 |
| P2 | a correct answer moves the piece forward 1, 2 or 3 steps by the question's level | FR-35 forward | FR-35 | #89, #87/#92 |
| P3 | a wrong answer moves it back the same number of steps | FR-35 backward | FR-35 | #89, #87/#92 |
| P4 | no answer counts as a wrong answer | the NO_ANSWER rule | FR-35 | #89, #87/#92, PO decision |
| P5 | a question is not asked again before its level is used up, over a complete game | FR-37 in real play | FR-37 | #89, #87/#92 |
| P6 | a capture on an unprotected question square happens before the question | the ordering of FR-38 | FR-38 | #87/#92 |
| P7 | each surprise outcome occurs and does what it says, over many seeded games | FR-39 outcomes | FR-39 | #90, #87/#92 |
| P8 | "move an opponent" is chosen by the active player and moves an opponent's piece | FR-40 | FR-40 | #90, #91 |
| P9 | an effect that lands on another special square runs the next effect, and a chain cannot loop | FR-41, FR-42 and the chain guard | FR-41, FR-42 | #87/#92 |
| P10 | backward movement never goes past the start square | FR-32 | FR-32 | #87/#92 |
| P11 | the turn passes through its States in the legal order, and an illegal action is refused in each | the State pattern of D3 | NFR-09 | #79 |
| ~~P12~~ | **DONE 22/11/2026** - see below. A complete Medium and a complete Hard game with questions, surprises and chains all active ends with a winner | the whole of US-2.3 end to end | FR-34..FR-45 | written on a branch stacked on #92 |

### P12 is written and passing - `Iteration2/test/Model/EffectsAcceptanceTest.java` (PR #103)

**P12 is the real acceptance of US-2.3**, and it is now implemented. It needs the effect queue, so it could not
go on this PR: it lives on `feature/mohamed/T2-15-effects-acceptance`, **stacked on T2-10 part 2 (#92)** as
Draft PR **#103**, and merges after #92. This PR is deliberately unchanged so it keeps working on plain
`iteration-2`.

Seven acceptance tests, all playing complete games through the `GameModel` facade with the real dice, boards,
factories and effect queue - nothing scripted, nothing stubbed:

| Test | Accepts |
|---|---|
| five complete **Hard** games with questions, surprises and chains live | US-2.3 end to end; one winner, all but one finished |
| three complete **Medium** games, same | US-2.3 on Medium |
| a game completes when **every answer is wrong** | FR-35 backward movement cannot deadlock a game |
| a game completes when the player **always declines** | **D3** through real play |
| within one game, no question of a level repeats before that level's deck is used up | FR-37 in real play, as far as natural play can reach it |
| the D2 turn-outcome check is reached in real play, and its cross-player case is not | **D2**, with its limit stated rather than implied |
| phases always declared; `RESOLVING` only while a square waits | **US-2.4** / NFR-09 invariant |
| rolling refused and no move offered while a square waits | NFR-09 reached by playing |

**Measured on 23/11/2026** (Java 17, Temurin 17.0.17) on `feature/mohamed/T2-15-effects-acceptance`
(`50420fd`) over #92 (`9a22814`): `EffectsAcceptanceTest` **8/8**, full suite **664/664**, JAR builds.
What the games really exercised:

| Level | Seeds | Questions asked | Distinct ids within each game | Distinct ids across the games | Surprises | Chain steps | Finished |
|---|---|---|---|---|---|---|---|
| Hard | 1-5 | **94** | **every question of a game was different** (0 repeats in any game) | **34** | **22** | 2 | 3 of 4 every game |
| Medium | 1-3 | **81** | the same, 0 repeats in any game | **36** | **22** | 3 | 3 of 4 every game |

> **Corrected 23/11/2026 — "94 questions, all distinct ids" was not supportable.** The course bank holds
> **36** questions, 12 of each level (`Model/CourseQuestions`), and each game builds its own decks, so 94
> *distinct* ids cannot exist: 94 is the number of questions **asked** over five games, and only **34**
> distinct ids appear among them. What is true, and is the FR-37 property worth claiming, is that **within a
> single game no question was ever asked twice** — measured, 0 repeats in any of the eight games. The three
> things were being conflated, and the table above now separates them.

**Three limits worth knowing, because they change what still needs covering:**

1. **The cross-player D2 case never occurred** in any of the eight games - the counter stayed at 0. A turn
   outcome landing in a chain an *opponent's* piece set off is too rare to appear naturally. The D2 assertion
   in P12 is a standing guard, **not evidence**. What covers D2 is Rakan's scripted
   `skipFromAnOpponentsPieceFallsOnTheActivePlayer` in T2-12 (#95) - his mutation check shows it is the only
   test in the suite that catches a broken D2.
2. **Chained effects are rare in natural play** - 2 chain steps in five Hard games, longest chain 2 effects.
   P12 shows chains happen and terminate; it does not systematically exercise them, and it never approaches
   the FR-42 chain guard. The scripted chain tests of T2-12 and #92 remain the real FR-42 coverage.
3. **No level's deck is ever used up**, so **FR-37's reshuffle is not reached by natural play.** The most
   questions of one level in one game is exactly **12**, which is the deck size, and no game passes it. P12
   therefore covers the "no repeat until the level is exhausted" half of FR-37 and not the "then shuffle
   again and keep dealing" half; that half is covered in isolation by `QuestionFactoryTest`, which can
   exhaust a deck on purpose.

None of these is a reason to weaken P12; they are the reason the independent scripted tests of T2-12 are not
redundant with it. All three are now stated inside `EffectsAcceptanceTest` itself, so a reader of the test
does not have to find this document to learn them.

## 4. Manual test script

To be run by hand on the double-click JAR of the release (N2-06), on both release computers. Fill the
evidence columns in while running; **do not** fill them in in advance. A step that is not run stays empty -
an empty row is not a pass.

Build under test: `Iteration2/EducationalLudo.jar`, built by `Iteration2/build.bat`.

| Field | Value |
|---|---|
| Tester | |
| Date and time | |
| Computer (1 or 2) | |
| OS and Java version | |
| Commit of the build | |

**Reference numbers for the tester**, read from `Model.LevelConfig` (unchanged on `iteration-2` `d445a38`,
re-checked 22/11/2026), so the board can be checked by counting:

| Level | Grid | Outer rings | Track squares | Home path | Start | Protection | Arrow | Question (per level) | Surprise | Special die | Question steps |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Easy | 15x15 | 0 | 52 | 6 | 4 | 4 | 4 | 0 (0) | 0 | none | 1 |
| Medium | 17x17 | 1 | 116 | 6 | 4 | 8 | 8 | 6 (2) | 2 | Boost (1-3) | 2 |
| Hard | 19x19 | 2 | 188 | 6 | 4 | 12 | 12 | 9 (3) | 3 | UltraBoost (1-6) | 3 |

### M1 - a Medium game, start to finish

| # | Step | Expected result | Actual | Pass / Fail | Evidence |
|---|---|---|---|---|---|
| 1.1 | Double-click `EducationalLudo.jar` | The main menu opens, no console window needed | | | screenshot |
| 1.2 | Start a new game, choose **Medium**, 4 players, enter names | Medium can be chosen (FR-05); the names are accepted | | | screenshot |
| 1.3 | Look at the board | A **17x17** Medium board with **1** outer ring and 116 track squares; **6** question squares (2 per level), **2** surprise, 8 protection, 4 start and 8 arrow squares, and the legend (FR-10..FR-12, FR-14) | | | screenshot |
| 1.4 | Roll | Both dice are shown: Regular and **Boost** (FR-16); the movement shown is Regular x (Boost - 1) (FR-17) | | | screenshot |
| 1.5 | Roll until a Boost 1 comes up | The movement is 0 and no piece can move; the turn passes (FR-17, FR-23) | | | |
| 1.6 | Play the game to its end | The game ends when 3 of the 4 players have all four pieces home (FR-44); the results screen shows the ranking in finishing order and the duration (FR-45, FR-47) | | | screenshot of the results screen |
| 1.7 | Note the wall-clock time the game took | - | | | |

### M2 - a Hard game, start to finish

| # | Step | Expected result | Actual | Pass / Fail | Evidence |
|---|---|---|---|---|---|
| 2.1 | Start a new game, choose **Hard**, 4 players | Hard can be chosen (FR-05) | | | screenshot |
| 2.2 | Look at the board | A **19x19** Hard board with **2** outer rings and 188 track squares; **9** question squares (3 per level), **3** surprise, 12 protection, 4 start and 12 arrow squares (FR-10..FR-12) | | | screenshot |
| 2.3 | Roll | Regular and **UltraBoost** are shown (FR-16); movement = Regular x (UltraBoost - 1) (FR-17) | | | screenshot |
| 2.4 | Get a Regular 6 with an UltraBoost 6 and move | The piece moves **30** squares, one square at a time, and the movement finishes within 2 seconds (FR-17, NFR-04) | | | time it with a stopwatch; record the seconds |
| 2.5 | Play the game to its end | As 1.6 | | | screenshot of the results screen |

> **M2.4 is the NFR-04 check on a real window.** The automated timing tests run off-screen and so do not
> include the cost of painting the real window. Record the measured seconds, not just pass or fail.

### M3 - Double Blocks (FR-28..FR-30)

| # | Step | Expected result | Actual | Pass / Fail | Evidence |
|---|---|---|---|---|---|
| 3.1 | Get two pieces of one player onto the same track square | They are drawn as a Double Block (FR-28) | | | screenshot |
| 3.2 | With another player, try to land on that square | The move is not offered; the piece cannot land there (FR-29) | | | screenshot |
| 3.3 | With another player, try to pass over that square | The move is not offered (FR-29) | | | |
| 3.4 | Move one of the two pieces away | The block is gone and the square can be used again (FR-30) | | | |
| 3.5 | Bring the owner into a position where the only legal move is a block piece | The owner is forced to break the block (FR-30) | | | |

### M4 - questions (FR-34..FR-38) - BLOCKED until #89, #91 and #87/#92 are merged

> D3 (the **No Answer** button) is decided and implemented in #91, so rows 4.4-4.4c have an approved rule.
> They cannot be run until the card is opened by a running game.

| # | Step | Expected result | Actual | Pass / Fail | Evidence |
|---|---|---|---|---|---|
| 4.1 | Land a piece on a question square | A dialog shows the question and four answers, to all players (FR-34); the question is of the **square's** level | | | screenshot |
| 4.2 | Answer correctly | The dialog says it was correct and names the correct answer (FR-36); the piece moves **forward** 1 / 2 / 3 steps by the question's level (FR-35) | | | screenshot |
| 4.3 | Answer incorrectly | It says so and names the correct answer (FR-36); the piece moves **back** the same number of steps (FR-35) | | | screenshot |
| 4.4 | Press the **No Answer** button | **D3, decided 21/11/2026:** it counts as a wrong answer - the verdict says "Not correct", the correct answer is named (FR-36), and the piece moves **backward** by the level (−1 Easy / −2 Medium / −3 Hard, FR-35). Continue is required | | | screenshot |
| 4.4b | Try to answer again after pressing No Answer | Every button is disabled; nothing is judged a second time (D3) | | | |
| 4.4c | Close or move the card without pressing any button | **Nothing is answered.** There is no timer, and closing is not an answer (D3) | | | |
| 4.4d | **Keyboard only:** press **Tab** repeatedly from the question card, then **Space** on the No Answer button | Tab reaches all four answers and the **No Answer** button, the focused button is visibly highlighted, and **Space** submits it. **Note:** Space is already verified automatically; **Tab order needs this manual check**, because a focus traversal policy only returns components that are showing and the tests create no window. **Enter does NOT press this button** - it activates the screen default button - so do not record Enter as a pass | | | screenshot of the focused button |
| 4.5 | Keep playing until a level's questions run out | No question of a level repeats before all of that level were asked (FR-37) | | | note the questions seen |
| 4.6 | Land on an unprotected question square holding one opponent piece | The capture happens **first**, then the question (FR-38) | | | screenshot |

### M5 - surprise outcomes (FR-39, FR-40) - BLOCKED until #90, #91 and #87/#92 are merged

> D2 (a turn outcome falls on the active player) is decided and implemented in #90 and #91, so rows 5.4 and
> 5.4b have an approved rule. They cannot be run until a surprise is triggered by a running game.

| # | Step | Expected result | Actual | Pass / Fail | Evidence |
|---|---|---|---|---|---|
| 5.1 | Land on a surprise square | Exactly one outcome is triggered and shown (FR-39) | | | screenshot |
| 5.2 | Repeat until every outcome has been seen at least once | Every outcome in the SRS occurs and does what it says (FR-39) | | | one screenshot per outcome; list which were seen |
| 5.3 | Get the "move an opponent" outcome | The **active player** chooses which opponent piece moves (FR-40) | | | screenshot |
| 5.4 | Get a skipped-turn or extra-roll outcome in an **ordinary** chain | The active player misses the next turn, or rolls again (FR-39) | | | |
| 5.4b | Push an opponent onto a surprise square that draws **skip the next turn** or **extra roll** | **D2, decided 21/11/2026 - Option A:** the outcome falls on the **active** player (the one who pushed), never on the owner of the pushed piece. Red pushes Green, the square says "skip a turn": **Red** misses the next turn. The card shows no "undecided" note and names the active player | | | screenshot |

### M6 - chained effects (FR-41, FR-42) - BLOCKED until #87/#92 are merged

| # | Step | Expected result | Actual | Pass / Fail | Evidence |
|---|---|---|---|---|---|
| 6.1 | Make a question or surprise movement end on another special square | The next effect runs too (FR-42) | | | screenshot |
| 6.2 | Make a backward movement reach the start square | It stops at the start square and does not go past it (FR-32) | | | |
| 6.3 | Make an effect movement be blocked by a Double Block | The effect obeys the movement and blocking rules (FR-41) | | | |
| 6.4 | Make a chain that would loop | The chain guard stops it; the game continues | | | |

### M7 - turn States (NFR-09) - BLOCKED until #79 is merged

| # | Step | Expected result | Actual | Pass / Fail | Evidence |
|---|---|---|---|---|---|
| 7.1 | Try to move a piece before rolling | Refused; the game stays in the rolling phase | | | screenshot |
| 7.2 | Try to roll twice without moving | Refused, except where FR-21 grants an extra roll | | | |
| 7.3 | Roll three Regular 6s in a row | The third is cancelled and the turn ends (FR-22) | | | screenshot |
| 7.4 | Watch a turn with a question on it | The phases run in order: roll, choose piece, move, effects, next turn | | | |

### Sign-off

| Script | Run on computer 1 | Run on computer 2 | Result |
|---|---|---|---|
| M1 Medium full game | | | |
| M2 Hard full game | | | |
| M3 Double Blocks | | | |
| M4 Questions | | | blocked 21/11 - rule decided (D3), waiting on #91 and #92 |
| M5 Surprises | | | blocked 21/11 - rule decided (D2), waiting on #90, #91 and #92 |
| M6 Chains | | | blocked 21/11 |
| M7 States | | | blocked 21/11 |

**No row of this table may be marked as passed unless the script was actually run to its end on that
computer.** A Medium or Hard playthrough in particular is only a pass when the game really reached its
results screen.

## 5. What this task still needs

1. **The dependencies merge** (#89, #90, #87/#92, #79). Until then A5-A8 cannot be accepted at all.
2. **Write the 12 planned tests of section 3**, in a second class, once they can run.
3. **Run the manual script of section 4** on both release computers with the release JAR (with N2-06).
4. **Product Owner decisions.** `Iteration2/docs/T2-15_PO_Decisions.md` is the register.
   - **D2** (the recipient of SKIP_TURN and EXTRA_ROLL) and **D3** (what "did not answer" means) were
     **decided on 21/11/2026 and implemented**, so M4.4, M4.4b, M4.4c, M5.4 and M5.4b now have an approved
     rule to judge against. **They have still not been run**: the dialogs are not opened by a running game
     until T2-10 (#92) merges, so those rows stay empty.
   - **D1** (a Double Block on a start square), **D4** (a push-back past the start square) and **D5**
     (EXTRA_ROLL with SKIP_TURN in one chain) are **still open**, so **M3**, **M6.2** and the planned tests
     P9 and P10 cannot be judged pass or fail yet.
