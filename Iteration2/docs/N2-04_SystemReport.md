# N2-04: Iteration 2 system report (source for `iteration2_report_Sharks`)

Task **N2-04** · Issue [#70](https://github.com/shafeekseef81-svg/Sharks/issues/70) · Owner: **Mohamed Zereny** · Reviewer: Shafeek Saif · Due 05/12/2026

Deliverable 3 of Assignment 5: a Word or PDF file named `iteration2_report_Sharks`, **continuing the
Iteration 1 report** (`iteration1_report_Sharks.docx` / `.pdf` in the repository root). The sections below
follow the brief's required structure 3(a)–3(e).

**Status 21/11/2026: structure complete, content partly verified.** Every statement here is either measured
and marked as such, or marked **TODO** with what is still needed. Nothing is assumed, and no screenshot,
measurement or playthrough is recorded that was not actually taken. The report cannot be finished before the
Iteration 2 code is merged (see section 6).

Baseline for everything measured here: `iteration-2` at **`d445a38`** (22/11/2026). Merged into it since the
first draft: T2-02 (#82), T2-03 (#83), T2-07 (#85), N2-03 (#86), **T2-10 part 1 (#87)**, **T2-08 (#89)**,
T2-14 (#93) and N2-05 (#94), on top of T2-01, T2-04, T2-05 and T2-06. Baseline suite: **531 tests, all
passing**.

**Still not merged:** the surprise outcomes (#90), the dialogs (#91), T2-10 part 2 (#92), the turn State
pattern (#79) and the independent effect tests (#95) — all approved and queued. So sections 3(b)–3(d) still
describe questions, surprises, chains and turn phases as **TODO for the player-visible behaviour**, even
though their code exists and is tested on branches. Section 3(e) now carries that branch evidence, clearly
marked as not-yet-merged.

---

## 3(a) How to run the system, with libraries and versions

### Requirements

| | Version | Verified |
|---|---|---|
| Java (JDK or JRE) to run | **17 or later** | yes - built and run on Temurin **17.0.17+10** on 21/11/2026 |
| JDK to build | **17 or later**; compiled with `javac --release 17` | yes - `javac 17.0.17` |
| Operating system | Windows 10/11, macOS or Linux | Windows 11 Pro 10.0.26200 verified 21/11; **TODO** confirm the second computer at the release (N2-06) |
| Internet connection | **not needed**, for building or running | yes - the build uses no network |

### Libraries

| Library | Version | Used for | Shipped in the game JAR? |
|---|---|---|---|
| Java Standard Library, **Swing** (`javax.swing`) and **AWT 2D** | part of the JDK (17) | the whole user interface and all drawing | n/a - part of Java |
| `junit-platform-console-standalone` | **1.10.2**, SHA-1 `552303cce67dc3485ded393d744c0f6b5cae355b`, from Maven Central, stored in `Iteration2/lib/` | running the JUnit 5 tests only | **no** |

**There is no other third-party library.** No build tool (no Maven or Gradle), no external service, no
database. The game is plain Java plus Swing, built by a shell script and a batch file.

### Running it

The submitted folder is `iteration2_Sharks`. From inside it:

| Action | Windows | macOS / Linux / Git Bash |
|---|---|---|
| **Start the game** | double-click **`run.bat`** | `./run.sh` |
| Build `EducationalLudo.jar` | `build.bat` | `./build.sh` |
| Build and run every test | `test.bat` | `./test.sh` |

- **Start the game with `run.bat`, by double-clicking it.** The assignment requires a run file that works on
  a double-click; a run file that does not start that way disqualifies the assignment.
- **Why not double-click the JAR itself:** on Windows, a path containing Hebrew letters makes
  `java -jar "C:\...\עבודה 4\...\EducationalLudo.jar"` fail with *Unable to access jarfile*. The scripts
  change into their own folder and use a relative path, so they work regardless of the folder name. This was
  found in Iteration 1 and is the reason the run file, not the JAR, is the supported way in.
- `EducationalLudo.jar` and `build/` are **not** stored in Git; `build.bat` recreates them. Verified
  21/11/2026: the JAR builds from a clean tree, `Main-Class: Controller.Main`, 120 classes.

**TODO (needs N2-06, the release task):** confirm the double-click start of `run.bat` on **both** release
computers with the final ZIP, and record who did it and when. Iteration 1's equivalent evidence is
`Iteration1/docs/T28_SecondComputer_Checklist.md`; for Iteration 2 the partial run recorded on branch
`feature/mohamed/T28-second-computer` is **not** a completed verification and must not be reported as one.

---

## 3(b) Inputs and game scenarios

All input is by mouse and keyboard through the Swing interface. There is no file, command-line or network
input.

### Inputs

| Screen | Input | Values accepted | Verified |
|---|---|---|---|
| Main menu | click | New game, (Iteration 3: history, administrator), exit | yes - `MainMenuViewTest`, `ScreenNavigationTest` |
| Game setup | **level** | **Easy, Medium, Hard** - all three selectable since T2-01 (FR-05) | yes - `GameSetupViewTest`, acceptance test of T2-15 |
| Game setup | **number of players** | 2, 3 or 4 | yes - `SetupAcceptanceTest`, `SettingsValidationTest` |
| Game setup | player names | non-blank text, one per player | yes - `SetupInputIndependentTest` |
| Game board | **roll** | a click on the dice | yes - `GameControllerTest` |
| Game board | **choose a piece** | a click on one of the highlighted pieces | yes - `LegalMoveHighlightTest`, `GameControllerTest` |
| Game board | leave the game | a click, then a confirmation | yes - `ScreenNavigationTest` |
| Question dialog | **one of four answers** | 0-3, or no answer | **TODO** - T2-11 (#91) not merged |
| Surprise dialog | **which opponent piece to move** | one of the offered pieces (FR-40) | **TODO** - T2-11 (#91) not merged |

### Game scenarios

| # | Scenario | State |
|---|---|---|
| 1 | A complete **Easy** game, 2-4 players | verified in Iteration 1 (380 tests) and still passing |
| 2 | A complete **Medium** game: 17x17 board, 1 outer ring, 116 track squares, Boost die | **verified automatically** - section 3(e) |
| 3 | A complete **Hard** game: 19x19 board, 2 outer rings, 188 track squares, UltraBoost die | **verified automatically** - section 3(e) |
| 4 | **Double Blocks**: forming one, being stopped by an opponent's, breaking it | **verified automatically** - section 3(e) |
| 5 | **Questions**: a question of the square's level, a right and a wrong answer | **TODO** - T2-08 (#89) and T2-10 (#87/#92) not merged |
| 6 | **Surprises**: each of the six outcomes, and moving an opponent's piece | **TODO** - T2-09 (#90) and T2-10 not merged |
| 7 | **Chained effects**: an effect landing on another special square | **TODO** - T2-10 not merged |
| 8 | **Turn phases** refusing an illegal action | **TODO** - T2-13 (#79) not merged |

---

## 3(c) Expected results in the different scenarios

Verified board and dice values, read from `Model.LevelConfig` on 21/11/2026:

| Level | Grid | Outer rings | Track squares | Home path | Start | Protection | Arrow | Question (per level) | Surprise | Dice | Movement | Question steps |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Easy | 15x15 | 0 | 52 | 6 | 4 | 4 | 4 | 0 (0) | 0 | Regular (1-6) | the Regular value | 1 |
| Medium | 17x17 | 1 | 116 | 6 | 4 | 8 | 8 | 6 (2) | 2 | Regular + **Boost** (1-3) | Regular x (Boost - 1) | 2 |
| Hard | 19x19 | 2 | 188 | 6 | 4 | 12 | 12 | 9 (3) | 3 | Regular + **UltraBoost** (1-6) | Regular x (UltraBoost - 1) | 3 |

### Expected results, by scenario

**Scenario 2-3, Medium and Hard play (verified).**
- The chosen level's board appears with exactly the counts in the table above, the special squares placed at
  random, and a legend (FR-10–FR-12, FR-14).
- Each roll shows both dice. The movement is Regular x (special - 1) (FR-17). A special **1 therefore gives a
  movement of 0 and moves nothing** - on Medium this happens on roughly a third of all rolls and on Hard
  roughly a sixth (measured, section 3(e)). This is the formula the SRS specifies; it is noted here because a
  player will notice it.
- The longest movement is **12** on Medium and **30** on Hard (Regular 6 with a special 6).
- A Regular 6 brings a piece out of its castle onto its start square and grants an extra roll; three Regular
  6s in a row cancel the third roll and end the turn (FR-19, FR-21, FR-22).
- A piece landing on an unprotected square holding a single opponent piece captures it, and the captured
  piece returns to its castle. Start, protection and surprise squares are protected; question squares are
  **not** (FR-26, FR-27).
- The game ends when all but one player have all four pieces home. The results screen shows the ranking in
  finishing order and the duration (FR-43–FR-45, FR-47).

**Scenario 4, Double Blocks (verified).** Two or more pieces of one colour on one track square form a Double
Block. An opponent cannot land on it or pass it; if that is a player's only possible move, the piece stops on
the square before it. The block ends as soon as one of the two pieces moves away (FR-28–FR-30).
**Known limitation, player-visible:** an opponent Double Block standing on a player's **start square** keeps
that player's pieces in their castle even on a Regular 6. This is the current reading of FR-29 over FR-19 and
is decision **D1** in `T2-15_PO_Decisions.md`; it is **not yet accepted by the Product Owner** and must be
settled before this report is final.

**Scenario 5, questions - TODO.** Expected, per the requirements, once merged: a piece stopping on a question
square shows all players a question **of that square's level** with four answers; a correct answer moves the
piece forward 1, 2 or 3 steps by that level and a wrong one moves it back the same number; the dialog then
says whether it was right and names the correct answer; no question repeats until its level is used up
(FR-34–FR-37). A capture on an unprotected question square happens before the question (FR-38).
Open decision **D3**: whether a player can decline to answer at all.

**Scenario 6, surprises - TODO.** Expected once merged: landing on a surprise square triggers exactly one
outcome (FR-39); for "move an opponent" the **active player** chooses which opponent piece moves (FR-40).
Open decision **D2**: who skips a turn or rolls again when an opponent's piece set the chain off.

**Scenario 7, chains - TODO.** Expected once merged: a question or surprise movement activates any special
square it lands on, obeying the ordinary movement and blocking rules, with a guard against an endless chain
(FR-41, FR-42); a backward movement stops at the start square (FR-32, decision **D4**).

**Scenario 8, turn phases - TODO.** Expected once merged: moving before rolling, or rolling twice without
moving, is refused (NFR-09).

---

## 3(d) What the player can do

| The player can | State |
|---|---|
| Start a new game and choose **Easy, Medium or Hard** | **works** (FR-05) |
| Choose 2, 3 or 4 players and name them | **works** |
| Roll the dice, and see both dice on Medium and Hard | **works** (FR-16) |
| See which pieces may legally move, highlighted after a roll | **works** (FR-24) |
| Choose which piece to move, and watch it move square by square | **works** (FR-20, NFR-04) |
| Bring a piece out of its castle with a Regular 6, and roll again | **works** (FR-19, FR-21) |
| Capture an opponent's piece on an unprotected square | **works** (FR-26) |
| Be stopped by, and form, a Double Block | **works** (FR-28–FR-30) |
| Bring pieces up the home path and finish them exactly | **works** (FR-18, FR-31) |
| See the game clock and the elapsed time | **works** (FR-46, FR-47) |
| Finish a game and see the ranking and duration | **works** (FR-43–FR-45, FR-47) |
| Leave a game, with a confirmation | **works** |
| **Answer a question on a question square** | **TODO** - #89, #91, #87/#92 |
| **Receive a surprise, and choose which opponent piece to move** | **TODO** - #90, #91, #87/#92 |
| See the history, or manage questions as an administrator | not in Iteration 2 - Iteration 3 (FR-56–FR-64) |

---

## 3(e) How we tested it, and the actual evidence

### Method

- **JUnit 5**, run by `Iteration2/test.sh` or `test.bat` through the standalone console JAR. Every test's
  `@DisplayName` names the SRS requirement it verifies, so the test output is itself the traceability
  evidence.
- **Rule tests are written by a member who did not implement the rule** (SDP 5.3.4). Tests whose names begin
  "independent" are that second pair of eyes.
- **Acceptance tests** play complete games through the `GameModel` facade with the real dice, the real random
  boards and all the real rule classes - no stubs - with fixed seeds so any failure reproduces exactly.
- **View tests** run off-screen: a component is laid out and painted into a `BufferedImage`, so the drawing
  and the animation are tested without a visible window.
- **Manual tests** cover what automation cannot: the double-click start, the real on-screen animation timing,
  and the look of the board and dialogs. The script is `Iteration2/docs/T2-15_AcceptancePlan.md`, section 4.

### Measured results

All figures Java 17 (Temurin 17.0.17), `Iteration2/test.sh`. Measured **22/11/2026** against `iteration-2`
`d445a38` unless a row says otherwise. The 21/11 figures against the older `6e13bcf` baseline (433, 437, 440,
415) are superseded and listed only in the last column so the two are not confused.

| What | Tests | Result | was on 21/11 |
|---|---|---|---|
| `iteration-2` baseline `d445a38` | 531 | **531 passed, 0 failed** | 433 |
| with the NFR-04 animation budget change (#97, `fd27f53`) | 535 | **535 passed, 0 failed** | 437 |
| with the T2-15 acceptance tests (#99) | 538 | **538 passed, 0 failed** | 440 |
| T2-09 surprise outcomes (#90, `d118253`) merged with the baseline | 592 | **592 passed, 0 failed** | — |
| T2-11 dialogs (#91, `cf96b77`) merged with the baseline | 631 | **631 passed, 0 failed** | — |
| T2-13 State (#79, `05bd1c0`) merged with the baseline | 540 | **540 passed, 0 failed** | — |
| T2-10 part 2 (#92, `a90eacc`) merged with the baseline | 656 | **656 passed, 0 failed** | — |
| T2-15 end-to-end acceptance of US-2.3 (#103) on #92 | 602 | **602 passed, 0 failed** | — |

The not-yet-merged rows are integration checks done in throwaway worktrees; nothing was pushed to anyone
else's branch. Every one of them merged **without a conflict**.

**Complete games actually played by the acceptance tests** (four players, random board, random legal choice,
measured 21/11/2026):

| Level | Seeds | Rolls per game | Moves per game | Captures | Rolls that moved nothing | Longest movement | Players finishing |
|---|---|---|---|---|---|---|---|
| Medium | 1-5, 2026 | 767-1153 | 516-788 | 19-41 | 258-402 | 12 | 3 of 4 |
| Hard | 1-5, 2026 | 485-644 | 371-488 | 4-17 | 72-105 | 30 | 3 of 4 |

So 12 complete Medium and Hard games ended with a winner and a full ranking.

**Complete games with questions, surprises and chained effects all live** (#103, on the #92 branch, measured
22/11/2026). These are the games that exercise US-2.3, which the games above cannot, because on plain
`iteration-2` a question or surprise square does nothing:

| Level | Seeds | Questions asked | Surprises drawn | Chained effects | Players finishing |
|---|---|---|---|---|---|
| Hard | 1–5 | **94**, all distinct ids | **22** | 2 | 3 of 4, every game |
| Medium | 1–3 | **81** | **22** | 3 | 3 of 4, every game |

Two findings from those runs, recorded because they affect what still needs testing:

* **The cross-player case of PO decision D2 never occurred naturally** in any of the eight games. A turn
  outcome landing in a chain that an *opponent's* piece set off is too rare to appear by chance, so what
  actually covers D2 is the scripted independent test described below — not the full-game runs.
* **Chained effects are rare in natural play** — 2 in five Hard games. The full-game runs show chains happen
  and terminate; the scripted chain tests are what cover FR-42 systematically.

**All of the above are automated games.** A Medium or Hard game played by a person from the menu to the
results screen is **TODO** and is script M1 and M2 of the acceptance plan; it must not be reported as done
until somebody has run it.

**Timing (NFR-02, NFR-04).**

* **NFR-02, the board appearing:** worst **78 ms** over 21 random Hard boards, against a 2000 ms limit
  (T2-14, #93, now merged).
* **NFR-04, the longest Hard movement — 30 squares.** This is the one requirement where a test found a real
  problem, and it is worth stating in full because it shows the method working.

  The T2-14 timing test passed when run on its own. But Rakan ran **nine test suites in parallel** on his
  computer, and the same 30-square move then took **2140 ms** — over the 2-second limit. That is the finding:
  not that the game is too slow normally, but that on a busy or slower computer the Iteration 1 animation
  budget of 1600 ms left too little headroom, because NFR-04 measures the budget *plus* the Swing timer's
  latency and the repaints of the final frame.

  I lowered the budget to **1200 ms** in **#97** (approved by Rakan). Measured off-screen, 10 runs per
  length, on an otherwise idle machine:

  | Movement | Before (1600 ms budget) | After (1200 ms budget) | Limit |
  |---|---|---|---|
  | 30 squares (longest Hard, FR-17) | 1649 ms | **1278 ms** | 2000 ms |
  | 6 squares (longest Easy) | 758 ms | 779 ms | 2000 ms |

  Under a deliberate 20-thread CPU load the 30-square move measured 1700 ms before and 1387–1526 ms after.
  Only moves longer than ten squares are capped at all, so short moves are unchanged.

  Shafeek's own T2-14 timing tests **pass with the new budget** (4/4), so the change fixes the risk his tests
  reported rather than merely coexisting with them.

  **Honest limit:** all of these runs are off-screen and therefore exclude the cost of painting a real
  window, which is what produced the 2140 ms. The fix lowers the one term the code controls by 400 ms; it is
  not a guarantee on every machine. **TODO:** time the 30-square movement on a real window with a stopwatch,
  on both release computers (script M2.4).

  The 30-square movement is **not** a corner case: it is the longest movement in every Hard game the
  acceptance tests play.

### Independent rule tests (SDP 5.3.4)

Every rule is tested by a member who did not implement it. Expected values are written out by hand from the
SRS — cells from the board's paths, step counts, ⌈R/2⌉, the Table 2 shares — and never read from the code
under test. Source: `Iteration2/docs/N2-04_Testing_Rakan.md` (PR #102, Rakan Ziyan), integrated here.

| Rules | Test class | Tests | Code author | PR | Status 22/11 |
|---|---|---|---|---|---|
| Dice distribution, movement formula, routes (FR-16, FR-17, FR-25, NFR-13) | `DiceRouteIndependentTest` | 15 | Shafeek (T2-04) | #78 | merged |
| Double Block (FR-19, FR-28–FR-30) | `DoubleBlockIndependentTest` | 19 | Rakan (T2-06) | #85 | merged |
| Questions, surprises, chains (FR-32, FR-34–FR-42, Table 2, decision D2) | `EffectChainIndependentTest` | 25 | Mohamed (T2-08, T2-09), Shafeek (T2-10) | #95 | approved, merges after #92 |

How they work:

* **NFR-13:** 10,000 draws per level; each face of the Regular, Boost and UltraBoost dice must fall within 2
  percentage points of an equal share. The zero-movement share — a special die showing 1 — is checked against
  1/3 on Medium and 1/6 on Hard.
* **Routes (FR-25):** checked geometrically on every colour and level: the outer rings run clockwise; each
  colour turns inward only at its own arrow; a route passes every start square; the home path is private.
* **Special squares:** every scenario is played through the `GameModel` facade with the real board, rules,
  factories and effect queue — only the dice are scripted. Because the special squares are placed at random
  (FR-12), each test finds the first seeded board on which its scenario exists. Fixed seeds mean every failure
  reproduces exactly.

### Proof that the tests catch wrong rules (mutation checking)

A test that passes on its first run proves little. So each rule was broken on purpose in a copy of the code,
one at a time, and the suite run against each broken copy. **All eight broken copies were caught:**

| Rule broken on purpose | Caught by |
|---|---|
| FR-32: backward movement not stopped at the start square | `pushStopsOnTheStartSquare`, `wrongAnswerStopsOnTheStartSquare` |
| FR-41: effects ignore Double Blocks | `rightAnswerStopsBeforeABlock`, `wrongAnswerStopsBeforeABlockBehind`, `zeroStepsDoNotReactivate` |
| FR-42 (Q81): a zero-step effect counts as a movement | `zeroStepPushDoesNotActivateTheSquareUnderThePiece` |
| FR-42: only the first square of a chain is activated | `answerChainsIntoTheNextSquare` |
| FR-40: half the Regular die rounded down instead of up | `pushBackHalfTheDieRoundedUp` |
| Table 2 (Q27): the extra roll uses the level's dice | `extraRollOutsideTheRuleOfSix` |
| Table 2 (Q56): skip-the-next-turn keeps a pending 6 | `skipTheNextTurn` |
| PO decision D2: a turn surprise falls on the owner of the piece instead of the active player | `skipFromAnOpponentsPieceFallsOnTheActivePlayer` — **the only test in the suite that catches it** |

**One test was rewritten because of this check**, which is the clearest evidence it was worth doing: the first
Q81 test did *not* catch its broken copy, because the "each square once per chain" guard hid the fault. It was
replaced by a scenario in a fresh chain — an opponent standing on a question square, pushed into a Double
Block behind it.

The same method was applied to the Double Block tests (T2-07, #85): with blocking switched off, **16 of 19
fail**.

Two mutation checks of my own, in the same spirit:

* **D3:** removing the one line that disables the No Answer button on submission makes exactly 2 of the new
  `QuestionDialogTest` tests fail (#91).
* **NFR-04:** putting the animation budget back to 1600 ms makes the headroom test fail (#97).

### Double Block (T2-06, FR-19, FR-28–FR-30)

* **Author's tests:** `DoubleBlockEngineTest`, 20 tests — forming a block; blocking in both directions
  (`isPassable` is direction-free); stopping before a block only when no full move exists; a block on the very
  next cell giving no move; the castle exit onto a blocked start square; the forced break.
* **Independent tests:** `DoubleBlockIndependentTest` (Shafeek, T2-07, #85), 19 tests through the facade.
* **Reviews:** two Deep reviews (Shafeek, Mohamed).
* **Player-visible open point — decision D1, still OPEN.** An opponent's Double Block standing on a player's
  **start square** keeps that player's pieces in the castle even on a Regular 6. I verified the behaviour in
  the merged code on `iteration-2` `d445a38`: `StandardMoveEngine.moveFor` requires
  `isPassable(board.startCell(owner.color()), owner)` before a castle exit, so **FR-29 wins over FR-19**.
  FR-19 and FR-29 genuinely conflict in this one case, and course-staff answers Q7/Q35 are cited in the
  discussion. **This is not decided**: it waits on staff clarification and then a Product Owner ruling, and
  the game rule is unchanged in the meantime. Register: `Iteration2/docs/T2-15_PO_Decisions.md`.

### Release checks (N2-06, NFR-10)

The submission ZIP is built only by `tools/make_iteration2_zip.py` (#81). The script exports the versioned
files with `git archive`; builds the JAR with `--release 17`; checks `Main-Class: Controller.Main` and that
every class file is Java 17 (major version 61); runs the whole suite; and writes the ZIP with forward slashes
under one top folder, printing the file count, the test count and the SHA-256.

**The test gate was strengthened after my review of #81.** It previously accepted a run in which **no test was
discovered at all**, because the JUnit launcher exits 0 in that case and the summary still prints
`[ 0 tests failed ]` — so both the exit code and the text check passed on an untested build. The script now
parses the JUnit summary and stops if any test failed or was aborted, **or if no test ran**; tests also run by
default instead of only with a flag. I verified the new gate against eight synthetic summaries: 0 failures
passes; 1, 10, 20 and 100 failures are all blocked; an aborted test is blocked; zero tests ran is blocked; and
an unreadable summary is blocked.

| Run | Code | Tests | ZIP | Started from a path with Hebrew letters |
|---|---|---|---|---|
| Dry run, 16/11 | `iteration-2` `38e2e1d` (Iteration 1 code) | 380/380 | 106 files | yes |
| CP1 rehearsal, 21/11, all 16 open PRs merged locally | local merge `3b4039e` | 664/664 | 168 files, JAR 261,390 bytes | yes, no error output |
| Script check after the review fix, 22/11 | `iteration-2` `d445a38` | 531/531 | 142 files, JAR 222,254 bytes | — |
| CP1, 25/11 | merged `iteration-2` | **TODO** | **TODO** | **TODO**, both computers |

The CP1 rehearsal also showed that every open PR merges without a conflict in the planned order.

**Not automated:** the double-click on `run.bat` and on the JAR, and a full game played by a person. Those are
rows 5–9 of the checklist in `Iteration2/docs/N2-06_Release.md` and are filled in by hand on both computers.

### What has not been verified yet

1. **A human playthrough** of a complete Medium and Hard game (scripts M1, M2).
2. **Questions, surprises, chains and turn States as player-visible behaviour** - the code exists, is tested
   and is approved, but #90, #91, #92, #79 and #95 are **not merged yet**, so a player cannot reach any of it
   in a build made from `iteration-2`. Section 3(e) carries the branch evidence; sections 3(b)-3(d) keep these
   as TODO until the merges land.
3. **The double-click start on both computers** with the final ZIP (N2-06).
4. **The real on-screen animation timing** (NFR-04), as above.
5. ~~**Independent tests of the question and surprise rules**~~ - **done**, pending merge. T2-12 (#95, Rakan)
   adds `EffectChainIndependentTest`, 25 tests, with the eight mutation checks above. It merges after #92. The
   gap the T2-14 coverage snapshot listed is closed in code, not yet in `iteration-2`.
6. **Screenshots.** None are in this file. The list for the design-pattern document is in
   `docs/design_patterns/FactoryMethod_Mohamed.md`; the report needs its own, and they can only be taken from
   the final build.

---

## 6. What finishing this report needs

| # | Needed | Depends on |
|---|---|---|
| 1 | Merge the remaining Iteration 2 feature PRs, then rewrite 3(b)-3(d) with questions, surprises, chains and States as **works** rather than TODO | #79, #90, #91, #92, #95 (#87 and #89 are merged) |
| 2 | Run the manual script M1-M7 and paste the filled-in tables in as the 3(e) evidence | T2-15, N2-06 |
| 3 | Re-run the whole suite on the final `iteration-2` and replace every test count | after the last merge |
| 4 | Take the screenshots from the final build | after the last merge |
| 5 | Settle **D1** so 3(c) states a rule and not an open question, and ratify **D4** and **D5** (both implemented and tested; see the register) | staff clarification on Q7/Q35, then the Product Owner |
| 5b | Add the US-2.3 end-to-end acceptance evidence of **#103** to 3(e) once it merges, and the independent-test evidence of #95 | after #92 |
| 6 | Continue the Iteration 1 report: keep its structure and numbering, add Iteration 2 as the new iteration rather than replacing it, and export to Word or PDF as `iteration2_report_Sharks` | all of the above |

**This file is the source text, not the deliverable.** The deliverable is the Word or PDF
`iteration2_report_Sharks`, continuing `iteration1_report_Sharks`.
