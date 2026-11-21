# N2-04: Iteration 2 system report (source for `iteration2_report_Sharks`)

Task **N2-04** · Issue [#70](https://github.com/shafeekseef81-svg/Sharks/issues/70) · Owner: **Mohamed Zereny** · Reviewer: Shafeek Saif · Due 05/12/2026

Deliverable 3 of Assignment 5: a Word or PDF file named `iteration2_report_Sharks`, **continuing the
Iteration 1 report** (`iteration1_report_Sharks.docx` / `.pdf` in the repository root). The sections below
follow the brief's required structure 3(a)–3(e).

**Status 21/11/2026: structure complete, content partly verified.** Every statement here is either measured
and marked as such, or marked **TODO** with what is still needed. Nothing is assumed, and no screenshot,
measurement or playthrough is recorded that was not actually taken. The report cannot be finished before the
Iteration 2 code is merged (see section 6).

Baseline for everything measured here: `iteration-2` at `6e13bcf` (21/11/2026), four PRs of Iteration 2
merged (T2-01, T2-04, T2-05, T2-06). **Questions, surprises, chained effects, the turn State pattern and
their dialogs are not merged yet**, so sections 3(b)–3(e) are incomplete by necessity, not by omission.

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

Measured 21/11/2026, Java 17 (Temurin 17.0.17), on `iteration-2` `6e13bcf` and the branches named:

| What | Tests | Result |
|---|---|---|
| `iteration-2` baseline | 433 | **433 passed, 0 failed** |
| with the T2-15 acceptance tests (`feature/mohamed/T2-15-acceptance`) | 440 | **440 passed, 0 failed** |
| with the NFR-04 animation budget change (`feature/mohamed/NFR-04-animation-budget`) | 437 | **437 passed, 0 failed** |
| T2-08 question Factory Method branch (`feature/mohamed/T2-08-question-factory`) | 415 | **415 passed, 0 failed** |

**Complete games actually played by the acceptance tests** (four players, random board, random legal choice,
measured 21/11/2026):

| Level | Seeds | Rolls per game | Moves per game | Captures | Rolls that moved nothing | Longest movement | Players finishing |
|---|---|---|---|---|---|---|---|
| Medium | 1-5, 2026 | 767-1153 | 516-788 | 19-41 | 258-402 | 12 | 3 of 4 |
| Hard | 1-5, 2026 | 485-644 | 371-488 | 4-17 | 72-105 | 30 | 3 of 4 |

So 12 complete Medium and Hard games ended with a winner and a full ranking. **These are automated games.**
A Medium or Hard game played by a person from the menu to the results screen is **TODO** and is script M1 and
M2 of the acceptance plan; it must not be reported as done until somebody has run it.

**Timing (NFR-02, NFR-04).** Measured by Shafeek in T2-14 (#93) and re-measured by me on 21/11:
- NFR-02, the board appearing: worst **78 ms** over 21 Hard boards, against a 2000 ms limit.
- NFR-04, the longest Hard movement (30 squares): **1649 ms idle / 1700 ms under load** at the Iteration 1
  animation budget of 1600 ms, against a 2000 ms limit - and **2140 ms** in Shafeek's run under load, over
  the limit. With the budget lowered to 1200 ms (branch `feature/mohamed/NFR-04-animation-budget`):
  **1312 ms idle / 1387 ms under load.**
- **These runs are off-screen** and so do not include the cost of painting a real window. **TODO:** time the
  30-square movement on a real window with a stopwatch, on both release computers (script M2.4).

### What has not been verified yet

1. **A human playthrough** of a complete Medium and Hard game (scripts M1, M2).
2. **Questions, surprises, chains and turn States** - the code is not merged (#89, #90, #87/#92, #79, #91),
   so none of it is in this report's scope yet.
3. **The double-click start on both computers** with the final ZIP (N2-06).
4. **The real on-screen animation timing** (NFR-04), as above.
5. **Independent tests of the question and surprise rules** - T2-12 (#63, Rakan). Today those rules are
   tested only by their authors, which the T2-14 coverage snapshot lists as the main gap.
6. **Screenshots.** None are in this file. The list for the design-pattern document is in
   `docs/design_patterns/FactoryMethod_Mohamed.md`; the report needs its own, and they can only be taken from
   the final build.

---

## 6. What finishing this report needs

| # | Needed | Depends on |
|---|---|---|
| 1 | Merge the Iteration 2 feature PRs, then rewrite 3(b)-3(d) with questions, surprises, chains and States as **works** | #89, #90, #87/#92, #79, #91 |
| 2 | Run the manual script M1-M7 and paste the filled-in tables in as the 3(e) evidence | T2-15, N2-06 |
| 3 | Re-run the whole suite on the final `iteration-2` and replace every test count | after the last merge |
| 4 | Take the screenshots from the final build | after the last merge |
| 5 | Settle decisions D1-D5 so 3(c) states rules and not open questions | Product Owner |
| 6 | Continue the Iteration 1 report: keep its structure and numbering, add Iteration 2 as the new iteration rather than replacing it, and export to Word or PDF as `iteration2_report_Sharks` | all of the above |

**This file is the source text, not the deliverable.** The deliverable is the Word or PDF
`iteration2_report_Sharks`, continuing `iteration1_report_Sharks`.
