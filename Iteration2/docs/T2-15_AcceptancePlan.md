# T2-15: acceptance plan and test script for the Iteration 2 stories

Task **T2-15** · Issue [#66](https://github.com/shafeekseef81-svg/Sharks/issues/66) · Owner: **Mohamed Zereny** (Product Owner) · Reviewer: Shafeek Saif · Due 05/12/2026

Stories: [US-2.1 #48](https://github.com/shafeekseef81-svg/Sharks/issues/48), [US-2.2 #49](https://github.com/shafeekseef81-svg/Sharks/issues/49), [US-2.3 #50](https://github.com/shafeekseef81-svg/Sharks/issues/50), [US-2.4 #51](https://github.com/shafeekseef81-svg/Sharks/issues/51).

**Status 21/11/2026.** Part automated and passing, part blocked on unmerged code, part manual by design.
Nothing in this file is recorded as passed unless it was actually run and its numbers are given.

## 1. What this task has to accept, and what the baseline allows today

The baseline for acceptance is `iteration-2`, today at `6e13bcf`. Merged into it: T2-01 (#84), T2-04
(#76, #80), T2-05 (#78), T2-06 (#77). **Not** merged: T2-08 (#89), T2-09 (#90), T2-10 (#87, #92),
T2-11 (#91), T2-13 (#79), T2-14 (#93).

That split decides what can honestly be accepted now:

| # | Acceptance area | Story | Possible on `iteration-2` today? | How it is covered |
|---|---|---|---|---|
| A1 | Medium and Hard board setup | US-2.1 | **Yes** | automated, section 2 |
| A2 | Medium and Hard gameplay to the end | US-2.1, US-2.4 | **Yes** | automated, section 2 |
| A3 | Boost and UltraBoost dice | US-2.1 | **Yes** | automated, section 2 |
| A4 | Double Blocks | US-2.2 | **Yes** (landing) | automated, section 2 |
| A5 | Questions, correct and incorrect answers | US-2.3 | **No** - T2-08 (#89) not merged | manual script M4, section 4 |
| A6 | Surprise outcomes | US-2.3 | **No** - T2-09 (#90) not merged | manual script M5, section 4 |
| A7 | Chained effects | US-2.3 | **No** - T2-10 (#87, #92) not merged | manual script M6, section 4 |
| A8 | Turn State transitions | US-2.4 | **No** - T2-13 (#79) not merged | manual script M7, section 4 |
| A9 | A full Medium and Hard game played by a person | all | partly - the rules run, the dialogs do not exist yet | manual script M1-M3, section 4 |

**On this baseline a question or surprise square is an ordinary square**: nothing happens when a piece
stops on it, because the code that notices it is in T2-10. So the automated complete games of section 2
exercise the boards, the dice, the blocks, the captures and the end of the game, and they exercise the
question and surprise rules **not at all**. That limit is stated inside the test class as well, so nobody
reads it as more than it is.

## 2. What is automated and passing today

`Iteration2/test/Model/MediumHardAcceptanceTest.java` - 7 acceptance tests, written by the Product Owner.
Games are played to their end through the `GameModel` facade with the real dice, the real random boards and
all the real rule classes; there are no stubs and no fixed dice, so a pass means a player really can play
that game. Deterministic seeds are used so a failure can be reproduced exactly.

Measured on 21/11/2026, branch `feature/mohamed/T2-15-acceptance` off `iteration-2` (`6e13bcf`), Java 17
(Temurin 17.0.17), `Iteration2/test.sh`:

* `Model.MediumHardAcceptanceTest` alone: **7 found, 7 successful, 0 failed**
* the whole suite with it: **440 found, 440 successful, 0 failed** (43 containers)
* the suite without it, i.e. the `iteration-2` baseline: 433 tests

| Test | Accepts | Requirements |
|---|---|---|
| a player can start a Medium or a Hard game with 2, 3 or 4 players | every level and player count starts, four pieces each, all in their castles | FR-05 |
| the Medium and Hard board a player gets matches Table 1, over 20 games | grid size, track squares, protection / question / surprise / starting / arrow counts, and the questions per level, on 20 random boards per level | FR-10, FR-11, FR-12 |
| a complete Medium game rolls Boost 1-3 and moves Regular x (Boost - 1) | the Boost die is the Medium die, every value 1-3 really came up, a Boost 1 moves nothing, the longest movement is 12 | FR-16, FR-17 |
| a complete Hard game rolls UltraBoost 1-6 and never moves over 30 | the UltraBoost die is the Hard die, every value 1-6 really came up, an UltraBoost 1 moves nothing, the longest movement is 30 | FR-16, FR-17 |
| over complete Medium and Hard games no piece lands on an opponent's Double Block | every move of 6 complete games (3 seeds x 2 levels) is checked against the blocks standing at that moment | FR-28, FR-29 |
| complete Medium games end with a winner and a full ranking, over 5 seeds | the game ends once, all but one player finished, a finished player has all four pieces home, the ranking follows the finishing order, exactly one rank 1 | FR-43, FR-44, FR-45 |
| complete Hard games end with a winner and a full ranking, over 5 seeds | the same for Hard | FR-43, FR-44, FR-45 |

In addition, every move of every game above is checked for: the movement formula of the level (FR-17), that
no piece ever leaves the board, that only opponents are captured and never on a protected square (FR-26,
FR-27), and that no move ends on an opponent's Double Block (FR-29).

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
| P12 | a complete Medium and a complete Hard game with questions, surprises and chains all active ends with a winner | the whole of US-2.3 end to end | FR-34..FR-45 | all of the above |

**P12 is the real acceptance of US-2.3** and it cannot be written, let alone passed, before the effect
queue is merged. It must not be reported as Completed in the Scrum Report until it has actually run.

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

**Reference numbers for the tester**, read from `Model.LevelConfig` on `iteration-2` (`6e13bcf`) on
21/11/2026, so the board can be checked by counting:

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

### M4 - questions (FR-34..FR-38) - BLOCKED until #89 and #87/#92 are merged

| # | Step | Expected result | Actual | Pass / Fail | Evidence |
|---|---|---|---|---|---|
| 4.1 | Land a piece on a question square | A dialog shows the question and four answers, to all players (FR-34); the question is of the **square's** level | | | screenshot |
| 4.2 | Answer correctly | The dialog says it was correct and names the correct answer (FR-36); the piece moves **forward** 1 / 2 / 3 steps by the question's level (FR-35) | | | screenshot |
| 4.3 | Answer incorrectly | It says so and names the correct answer (FR-36); the piece moves **back** the same number of steps (FR-35) | | | screenshot |
| 4.4 | Close the dialog without answering | Counts as a wrong answer (FR-35) - **see the open Product Owner decision on NO_ANSWER** | | | |
| 4.5 | Keep playing until a level's questions run out | No question of a level repeats before all of that level were asked (FR-37) | | | note the questions seen |
| 4.6 | Land on an unprotected question square holding one opponent piece | The capture happens **first**, then the question (FR-38) | | | screenshot |

### M5 - surprise outcomes (FR-39, FR-40) - BLOCKED until #90 and #87/#92 are merged

| # | Step | Expected result | Actual | Pass / Fail | Evidence |
|---|---|---|---|---|---|
| 5.1 | Land on a surprise square | Exactly one outcome is triggered and shown (FR-39) | | | screenshot |
| 5.2 | Repeat until every outcome has been seen at least once | Every outcome in the SRS occurs and does what it says (FR-39) | | | one screenshot per outcome; list which were seen |
| 5.3 | Get the "move an opponent" outcome | The **active player** chooses which opponent piece moves (FR-40) | | | screenshot |
| 5.4 | Get an extra roll / a skipped turn outcome | The turn order changes as the SRS says - **see the open Product Owner decisions on the recipient** | | | |

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
| M4 Questions | | | blocked 21/11 |
| M5 Surprises | | | blocked 21/11 |
| M6 Chains | | | blocked 21/11 |
| M7 States | | | blocked 21/11 |

**No row of this table may be marked as passed unless the script was actually run to its end on that
computer.** A Medium or Hard playthrough in particular is only a pass when the game really reached its
results screen.

## 5. What this task still needs

1. **The dependencies merge** (#89, #90, #87/#92, #79). Until then A5-A8 cannot be accepted at all.
2. **Write the 12 planned tests of section 3**, in a second class, once they can run.
3. **Run the manual script of section 4** on both release computers with the release JAR (with N2-06).
4. **Open Product Owner decisions** that the script runs into: NO_ANSWER, the recipient of SKIP_TURN and
   EXTRA_ROLL, and the push-back onto a start square. They are listed in
   `Iteration2/docs/T2-15_PO_Decisions.md`; the manual steps that depend on them cannot be judged pass or
   fail until they are decided.
