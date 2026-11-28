# N2-04: Iteration 2 system report (source for `iteration2_report_Sharks`)

Task **N2-04** · Issue [#70](https://github.com/shafeekseef81-svg/Sharks/issues/70) · Owner: **Mohamed Zereny** · Reviewer: Shafeek Saif · Due 05/12/2026

Deliverable 3 of Assignment 5: a Word or PDF file named `iteration2_report_Sharks`, **continuing the
Iteration 1 report** (`iteration1_report_Sharks.docx` / `.pdf` in the repository root). The sections below
follow the brief's required structure 3(a)–3(e).

**Status 28/11/2026: all Iteration 2 code is merged; the measured content below is final except for the
items listed in section 6.** Every statement here is either measured and marked as such, or marked **TODO**
with what is still needed. Nothing is assumed, and no screenshot, measurement or playthrough is recorded
that was not actually taken.

Baseline for everything measured here: `iteration-2` at **`06e2ab2`**. Baseline suite: **705 tests found,
705 passed, 0 failed, 0 skipped, 0 aborted** - re-run for this revision on 28/11/2026 with
`Iteration2/test.sh` on Temurin 17.0.17+10, and independently by Shafeek Saif on 26/11.

**The whole feature scope of Iteration 2 is merged**, in 32 pull requests (#74-#111). The last feature
merges were **#111** (the No Answer fix), **#99** and **#103** (the acceptance evidence) on 25/11. Sections
3(b)-3(d) therefore state player-visible behaviour as fact rather than as a plan: a player can reach every
Iteration 2 rule in a build made from `iteration-2`.

Two earlier release risks named by this report are closed. **#97** lowered the NFR-04 animation budget to
1200 ms; the evidence is kept in the NFR-04 subsection of 3(e), because it is the reason the budget is
1200 ms and a grader will ask. **#95** added the independent effect tests SDP 5.3.4 requires, which
discharged the caveat that the **D4** ratification used to carry.

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
- **Why `run.bat` is the documented way in, even though the JAR is also checked:** on Windows a path containing Hebrew letters made
  `java -jar "C:\...\עבודה 4\...\EducationalLudo.jar"` fail with *Unable to access jarfile*. The scripts
  change into their own folder and use a relative path, so they work regardless of the folder name. This was
  found in Iteration 1 and is the reason the run file is what the README tells a user to start.
- **A double-click on `EducationalLudo.jar` is nevertheless a required release check in its own
  right** — row 6 of the official N2-06 checklist (`Iteration2/docs/N2-06_Release.md`), separate from
  row 5 for `run.bat`. A failure there is a finding to be recorded with its exact message and path,
  not something to be explained away by the Hebrew-path note above: N2-06 row 4 (`java -jar` from a
  Hebrew path) **passed** on computer 1 in the 16/11 dry run.
- `EducationalLudo.jar` and `build/` are **not** stored in Git; `build.bat` recreates them. Verified
  21/11/2026: the JAR builds from a clean tree, `Main-Class: Controller.Main`, 120 classes.

**TODO:** confirm the double-click start of **both** `run.bat` (N2-06 row 5) **and** `EducationalLudo.jar`
(N2-06 row 6) on **both** release computers with the final ZIP, and record who did it and when. The exact
procedure is in 3(e), "The CP1 release and manual-test procedure".

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
| Question dialog | **one of four answers**, or the **No Answer** button (decision D3) | 0-3, or an explicit no-answer which counts as wrong | yes - `QuestionDialogTest` (23 tests), T2-11 (#91) merged as `12a01b6` |
| Surprise dialog | **which opponent piece to move** | one of the offered pieces, chosen by the active player and never auto-selected (FR-40) | yes - `SurpriseDialogTest` (16 tests), T2-11 (#91) merged as `12a01b6` |

### Game scenarios

| # | Scenario | State |
|---|---|---|
| 1 | A complete **Easy** game, 2-4 players | verified in Iteration 1 (380 tests) and still passing |
| 2 | A complete **Medium** game: 17x17 board, 1 outer ring, 116 track squares, Boost die | **verified automatically** - section 3(e) |
| 3 | A complete **Hard** game: 19x19 board, 2 outer rings, 188 track squares, UltraBoost die | **verified automatically** - section 3(e) |
| 4 | **Double Blocks**: forming one, being stopped by an opponent's, breaking it | **verified automatically** - section 3(e) |
| 5 | **Questions**: a question of the square's level, a right and a wrong answer | **verified automatically** on `iteration-2` since #92 merged - section 3(e) |
| 6 | **Surprises**: each of the six outcomes, and moving an opponent's piece | **verified automatically** since #90 and #92 merged; all six outcomes are drawn in the full-game runs - section 3(e) |
| 7 | **Chained effects**: an effect landing on another special square | **verified automatically** since #92 merged, but **rare in natural play** - the systematic coverage is the scripted tests of T2-12 (#95), section 3(e) |
| 8 | **Turn phases** refusing an illegal action | **verified automatically** - T2-13 (#79, `c689a85`) gave the states; #92 is what ever enters `RESOLVING`, and it is merged |

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
- **The longest movement from the level's own dice is 12 on Medium** (Regular 6 x (Boost 3 - 1)) **and 30
  on Hard** (Regular 6 x (UltraBoost 6 - 1)). On Medium that 12 is **not** the overall bound: the
  `EXTRA_ROLL` surprise rolls Regular + UltraBoost on *every* level (SRS Table 2, Q27), so a Medium move can
  reach the same 30. Measured on `06e2ab2`, the longest Medium surprise-roll movement actually seen is
  **24**. The NFR-04 animation budget caps by distance and not by level, so it covers this.
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
that player's pieces in their castle even on a Regular 6. **This is what FR-19 specifies**, which states the
exception itself: a Regular 6 places a piece on its starting square *"unless an opponent Double Block
occupies that square (R-4, Q40)"*. It is therefore not a case of one requirement overriding another, and the
implemented behaviour matches the requirement text.

It remains decision **D1** in `T2-15_PO_Decisions.md`, and **D1 is still OPEN**, for a different reason: the
course-staff answers **Q7/Q35** are reported to say that two pieces on a start square do not form a Double
Block there at all, in which case FR-19's exception could never apply. Those answers are not reproduced
anywhere in this repository, so the point needs authoritative staff resolution and cannot be settled by the
Product Owner alone. **The lock-out must still be accepted explicitly by the Product Owner** for Iteration 2
before this report is final. Full discussion: D1 in the decision register.

**Scenario 5, questions (verified).** A piece stopping on a question square shows all players a question
**of that square's level** — not the board's — with four answers. A correct answer moves the piece forward
1, 2 or 3 steps by that level and a wrong one moves it back the same number; the dialog then says whether it
was right and names the correct answer. No question repeats until its level's deck is used up
(FR-34–FR-37). A capture on an unprotected question square happens before the question (FR-38).
**Decision D3 is DECIDED (21/11): the dialog has an explicit "No Answer" button**, and choosing it counts as
a wrong answer. Evidence: `QuestionDialogTest` (23 tests), `QuestionFactoryTest` (18),
`EffectResolutionTest` (13), `SquareEffectTest` (21).

**Scenario 6, surprises (verified).** Landing on a surprise square triggers exactly **one** outcome (FR-39).
For "move an opponent" the **active player** chooses which opponent piece moves, and nothing is
auto-selected (FR-40). **Decision D2 is DECIDED (21/11): a SKIP_TURN or EXTRA_ROLL falls on the active
player**, never on the owner of the piece that landed on the square, even when an effect chain set it off
with an opponent's piece. Evidence: `SurpriseFactoryTest` (47 tests), `SurpriseDialogTest` (16),
`SurpriseOutcomeTest` (14).

**Scenario 7, chains (verified).** A question or surprise movement activates any special square it lands on,
obeying the ordinary movement and blocking rules, with a guard against an endless chain (FR-41, FR-42). A
backward movement **stops at** the piece's start square rather than wrapping or being cancelled (FR-32).
**Decision D4 is OPEN for ratification only** — the behaviour is implemented and matches FR-32; what is
outstanding is the Product Owner's formal sign-off. The caveat this used to carry is **discharged**: #95
merged on 23/11 (`5decd43`), so FR-32 is no longer tested only by the author of the code under test.
**D5 is likewise OPEN for ratification only**: an EXTRA_ROLL and a SKIP_TURN can both arise in one **turn**,
but never in one **chain**. Evidence: `EffectResolutionTest`, `SquareEffectTest`, and
`EffectChainIndependentTest` (25 tests, merged).

**Scenario 8, turn phases (verified).** Moving before rolling, or rolling twice without moving, is refused,
and a roll is refused while a special square is still waiting to be resolved (phase `RESOLVING`). Evidence:
`TurnStateTest` (9 tests, NFR-09).

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
| **Answer a question on a question square, or decline with "No Answer"** | **works** (FR-34–FR-38, decision D3) - #89, #91, #87/#92 all merged |
| **Receive a surprise, and choose which opponent piece to move** | **works** (FR-39–FR-42, decision D2) - #90, #91, #87/#92 all merged |
| **See one effect set off another, as a chain** | **works** (FR-41, FR-42) |
| See the history, or manage questions as an administrator | not in Iteration 2 - Iteration 3 (FR-56–FR-64) |

---

## 3(e) How we tested it, and the actual evidence

### Method

- **JUnit 5**, run by `Iteration2/test.sh` or `test.bat` through the standalone console JAR. **Most tests'
  `@DisplayName` names the requirement, decision or course answer they verify - 607 of the 705** - so the
  test output is itself a large part of the traceability evidence. The remaining 98 are the ones with no
  single requirement to name: constructor and argument validation, helper classes and pattern-structure
  checks (for example "isPassable refuses null arguments", "Context: RandomDice rolls with the strategy of
  the level it is given"). Counted on `06e2ab2` from the JUnit XML report, which carries the full,
  untruncated display name of each of the 705 tests:

  ```bash
  cd Iteration2 && ./test.sh                      # 705/705
  java -jar lib/junit-platform-console-standalone-1.10.2.jar execute \
       --class-path "build/classes;build/test-classes" --scan-class-path \
       --details=none --reports-dir=/tmp/reports
  # then count display-name lines matching FR-nn, NFR-nn, Dn/decision or Qnn
  ```

  The console's `--details=tree` output **truncates** long display names, so counting from it undercounts;
  the XML report is the reliable source. The traceability claim that does not depend on this count is the
  RTM below: `GAPS: none` for FR-15..FR-47.
- **Rule tests are written by a member who did not implement the rule** (SDP 5.3.4). Tests whose names begin
  "independent" are that second pair of eyes.
- **Acceptance tests** play complete games through the `GameModel` facade with the real dice, the real random
  boards and all the real rule classes - no stubs - with fixed seeds so any failure reproduces exactly.
- **View tests** run off-screen: a component is laid out and painted into a `BufferedImage`, so the drawing
  and the animation are tested without a visible window.
- **Manual tests** cover what automation cannot: the double-click start, the real on-screen animation timing,
  and the look of the board and dialogs. The script is `Iteration2/docs/T2-15_AcceptancePlan.md`, section 4.

### Measured results

All figures Java 17 (Temurin 17.0.17+10), `Iteration2/test.sh`, measured **28/11/2026** unless a row says
otherwise.

| What | Tests | Result |
|---|---|---|
| **`iteration-2` `06e2ab2` - the final Iteration 2 baseline** | **705** | **705 found, 705 passed, 0 failed, 0 skipped, 0 aborted** |
| the same commit, measured independently by Shafeek Saif on 26/11 | 705 | 705 passed, 0 aborted |
| `071cf90` before the last three merges (#111, #99, #103) | 685 | 685 passed, 0 failed |
| `4640416` on 23/11, after #81, #97, #98 and #102 | 660 | 660 passed, 0 failed |
| `1d47f2d` (23/11) | 656 | 656 passed, 0 failed |

The suite grew from **433** on 21/11 (`6e13bcf`) through 531, 540, 656 and 685 to **705**, because tasks
merged and not because tests changed. The three merges of 25/11 account for the last 20: **#111** +5
(`NoAnswerVisibilityTest`), **#99** +7 and **#103** +8.

Each of those three was re-verified against `071cf90` before it merged, on its own and combined, in
throwaway worktrees: #99 alone **692/692**, #103 alone **693/693**, the two together **700/700**, and with
#111 as well **705/705** - which is exactly the figure the merged branch now gives. Nothing was pushed to
anyone else's branch.

### An integration failure the reviews caught (kept as a lesson, not as PR history)

Reviewing against the *integrated* system, and not only against a branch's own baseline, caught a break
that would otherwise have reached `iteration-2` days before CP1.

**What happened.** #99's seven acceptance tests passed on `iteration-2` and the whole suite was green. Run
against **`iteration-2` + #92**, **five of the seven failed**:

```
java.lang.IllegalStateException: Resolve the special square first (phase RESOLVING)
    at Model.ResolvingState.roll(ResolvingState.java:32)
    at Model.MediumHardAcceptanceTest.playToEnd(MediumHardAcceptanceTest.java:96)
```

Since #92, a move onto a question or surprise square leaves the turn in `RESOLVING` and the next `roll()`
is refused until the square is resolved; the acceptance loop rolled again regardless. There is no CI on
`iteration-2`, so whichever of #92 and #99 merged second would have broken the branch with nobody notified.
`playToEnd` now resolves every waiting square, and any chain it sets off, before taking the next turn. Both
PRs are merged and the branch is green at 705/705.

**A rule came out of it, not just a fix.** The first run of the corrected loop failed with
`MEDIUM: the special die of the level is rolled ==> expected: <BOOST> but was: <ULTRA_BOOST>`. The
`EXTRA_ROLL` surprise rolls **Regular + UltraBoost whatever level is being played** (SRS Table 2, Q27;
`StandardTurnManager.SURPRISE_ROLL_DICE`). The assumption that a level only ever rolls its own dice is
false, and it had been written into an acceptance test.

**Player-visible consequence: a Medium game can produce a movement of more than 12.** The bound that holds
on Medium is the UltraBoost one, 6 x (6 - 1) = **30**, not 12. Measured on `06e2ab2`, the longest Medium
movement **from the level's own dice is 12** and the longest **surprise-roll** movement actually seen is
**24**; both are read from `MediumHardAcceptanceTest` by tightening its two bounds until they report the
value. The test's own javadoc records the same 24.

> An earlier revision of this report gave this figure as **25** in one place and **24** in another. The
> measured value on the merged baseline is **24**, and the 25 is withdrawn. The number that matters for
> NFR-04 is neither: the animation budget caps by **distance and not by level**, so the 30-square bound
> covers the Medium case as well.

Anywhere a text says "the longest Medium movement is 12" it needs the qualifier **"from the level's own
dice"**.


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

| Level | Seeds | Questions **asked** | Repeats within a game | Distinct ids **across** the games | Surprises | Chain steps | Players finishing |
|---|---|---|---|---|---|---|---|
| Hard | 1–5 | **94** | **0** | **34** | **22** | 2 | 3 of 4, every game |
| Medium | 1–3 | **81** | **0** | **36** | **22** | 3 | 3 of 4, every game |

The three columns are deliberately separate, because they are easy to conflate. The course bank holds
**36** questions, 12 of each level (`Model/CourseQuestions`), and each game builds its own decks. So **94**
is the number of questions *asked* over the five Hard games; **34** distinct ids appear among them, of the
36 that exist; and **0** questions repeated within any single game — which is the FR-37 property worth
claiming.

All six surprise outcomes are drawn across these games, so the outcome table of FR-39 is exercised end to
end rather than in part.

**Three limits of this evidence**, recorded because they decide what still needs covering:

* **The cross-player case of PO decision D2 never occurs naturally.** On `06e2ab2` the acceptance test
  prints `[T2-15 D2] turn outcomes reached by natural play: 7 (of which opponent-triggered, i.e. D2's
  cross-player half: 0)`. Across the wider set of games measured earlier the count was 26, also with **0**
  opponent-triggered. The figure moves with the seeds and the merged rule set; what does not move is the
  zero. The D2 check in the full-game tests is a standing guard over
  the common half; what covers the rare half is the scripted independent test described below — not the
  full-game runs. The acceptance test now asserts this limit explicitly, so the class cannot look as though
  it covers a case it never reaches.
* **Chained effects are rare in natural play** — 2 chain steps in five Hard games, longest chain 2 effects.
  The full-game runs show chains happen and terminate; they never approach the FR-42 chain guard, which the
  scripted chain tests cover.
* **No level's deck is ever used up**, so **FR-37's reshuffle is not reached by natural play.** The most
  questions of one level in one game is exactly **12**, the deck size. The full-game runs cover "no repeat
  until the level is exhausted"; `QuestionFactoryTest` covers "then shuffle again", because it can exhaust a
  deck on purpose.

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
  acceptance tests play. And since #92 merged it is **not Hard-only** either — the `EXTRA_ROLL` surprise
  rolls the Regular and UltraBoost dice on every level (SRS Table 2, Q27), so a **Medium** game can reach the
  same bound. Measured over nine Medium games the longest surprise-roll movement was 24, against the 12 of
  Medium's own dice. The budget caps by distance and not by level, so it still covers this.

#### #97 was a release blocker for CP1 — the evidence, and how it was closed

> **Resolved on 23/11/2026.** #97 is **merged** into `iteration-2` as `1f099f2`. `MAX_MOVE_MILLIS` is 1200 ms
> in the baseline, confirmed at `src/View/components/BoardComponent.java:265`. On the CP1 candidate built
> from the new baseline, `View.TimingTest` passed **4/4 on three cold JVM runs** and
> `View.MovementAnimationTest` **15/15**. The evidence below is kept because it is why the budget is
> 1200 ms rather than 1600 ms, and because it is the clearest NFR-04 measurement we have.

**What the budget actually is.** For a 30-square movement the animation budget is `max(260, min(BUDGET,
steps * 120))`, which saturates at the cap:

| Build | Cap | Budget for 30 squares | Headroom under NFR-04's 2000 ms |
|---|---|---|---|
| `iteration-2` **without** #97 | 1600 ms | **1600 ms** | 400 ms |
| `iteration-2` **with** #97 | 1200 ms | **1200 ms** | **800 ms** |

The measured time is the budget *plus* the Swing timer's latency and the repaints of the final frame, so
that headroom is what absorbs a busy or slower machine. **#97 doubles it.**

**Shafeek measured the failure on his machine** (#106, T2-14 snapshot of 23/11), `TimingTest` on its own with
a cold JVM, three runs each:

| Build | 30-square move | Verdict |
|---|---|---|
| `iteration-2` without #97 | 1992, 2008, 2043 ms | **2 of 3 fail** |
| `iteration-2` with #97 | 1612, 1659, 1668 ms | pass |

**I could not reproduce the failure on my machine, and that is worth saying plainly rather than omitting.**
On the same plain `iteration-2` `1d47f2d`, without #97, `TimingTest` passed **4/4 on three cold runs** here.
That does not contradict Shafeek — it is the whole point of NFR-04. The limit is wall-clock time on a real
machine, so a faster or less loaded machine has margin that a slower one does not. **Two of our three
computers would ship a build that fails its own timing test**, and which computer runs the submission is not
something we control.

**Why the release script makes this blocking rather than cosmetic.** The ZIP script (#81) runs the whole
suite and **stops on any failed test**. So on a machine like Shafeek's, building the submission ZIP from an
`iteration-2` without #97 does not produce a ZIP with a warning — **it produces no ZIP at all.**

**This recommendation was carried out.** #97 was approved by both reviewers (@ziyanrakan33 22/11,
@shafeekseef81-svg 23/11) and merged on 23/11 as `1f099f2`. On the CP1 candidate built from the new
baseline, `View.TimingTest` passed **4/4 on three cold runs** and `View.MovementAnimationTest` **15/15**.

**This remains an off-screen measurement.** `TimingTest` measures the model and the animation budget, not a
real window being repainted on a real display. **NFR-04 is not closed until the 30-square Hard move is
timed with a stopwatch on screen**, which is row 10 of `docs/T2-15_ManualChecklist_CP1.md` and has not been
done by anyone yet.

### Requirements traceability, verified independently (NFR-14)

This was run with **the team's own tool**, `Iteration2/tools/rtm_coverage.py` (Shafeek, T2-14), not with a
re-implementation. The tool needs an SRS text dump, which is produced from the SRS already in the
repository:

```bash
pdftotext -layout Assignment3_Prototype/Sharks_SRS.pdf srs.txt
cd Iteration2 && python tools/rtm_coverage.py ../srs.txt test 15 47
```

`Assignment3_Prototype/Sharks_SRS.pdf` is the version that carries the numbered requirements FR-01…FR-65.
(`SRS_Sharks.docx` and `SRS_Sharks.pdf` at the repository root are the Assignment 2 version 1.0 and contain
**no FR identifiers at all**, which is worth knowing before anyone else goes looking for the requirement
text.)

**Result on the CP1 candidate: `GAPS: none` for FR-15…FR-47** — every requirement in the Iteration 2 range
is named by at least one test. This is the official tool's own verdict, so it corroborates the T2-14
snapshot (#106) directly rather than by a second method.

The thinnest rows on the candidate, by number of tests: **FR-47 (2)**, FR-38 (3), FR-32 (4), FR-36 (5),
FR-30 (6), FR-37 (6). Every other FR in the range has seven or more. FR-47 — the end-of-game summary screen
— is the thinnest in the whole range and is covered only by `GameControllerTest` and
`NavigationControllerTest`; it is also one of the things the manual checklist looks at (row 12).

**What #95 actually contributes, measured with the same tool.** Running it on the candidate *without* #95
gives `GAPS: none` as well, so **#95 fills no coverage hole.** What it changes is independence:

| FR | Tests without #95 | Whose tests those are | With #95 |
|---|---|---|---|
| **FR-32** backward movement | **2** — `EffectResolutionTest`, `SquareEffectTest` | **both Shafeek's**, who wrote the effect code under test → **no independent test at all** | 4 |
| FR-38 capture on a question square | 2 — `SpecialDiceGameTest`, `SquareEffectTest` | mostly Shafeek's | 3 |
| FR-41 effect movement obeys the rules | 4 — `SpecialDiceGameTest`, `SquareEffectTest` | mostly Shafeek's | 8 |
| FR-36 answer feedback | 4 — `QuestionDialogTest`, `SpecialDiceGameTest` | `QuestionDialogTest` is mine (T2-11) | 5 |

Authorship was taken from `git log --follow` on each file, not assumed. **FR-32 is the clear case: without
#95 it has no test written by anyone other than the author of the code it checks**, which is exactly what
SDP 5.3.4 asks for. That is the argument for merging #95 before CP1, and it is also why the **D4**
ratification in the decision register carries an explicit caveat until #95 lands — D4 is a statement about
FR-32 behaviour.

### Independent rule tests (SDP 5.3.4)

Every rule is tested by a member who did not implement it. Expected values are written out by hand from the
SRS — cells from the board's paths, step counts, ⌈R/2⌉, the Table 2 shares — and never read from the code
under test. Source: `Iteration2/docs/N2-04_Testing_Rakan.md` (PR #102, Rakan Ziyan), integrated here.

| Rules | Test class | Tests | Code author | PR | Status |
|---|---|---|---|---|---|
| Dice distribution, movement formula, routes (FR-16, FR-17, FR-25, NFR-13) | `DiceRouteIndependentTest` | 15 | Shafeek (T2-04) | #78 | merged |
| Double Block (FR-19, FR-28–FR-30) | `DoubleBlockIndependentTest` | 19 | Rakan (T2-06) | #85 | merged |
| Questions, surprises, chains (FR-32, FR-34–FR-42, Table 2, decision D2) | `EffectChainIndependentTest` | 25 | Mohamed (T2-08, T2-09), Shafeek (T2-10) | #95 | **merged** 23/11 (`5decd43`) |

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
  **start square** keeps that player's pieces in the castle even on a Regular 6.

  **This is what FR-19 specifies.** Quoted from `Assignment3_Prototype/Sharks_SRS.pdf`, extracted with
  `pdftotext -layout`:

  > **FR-19** A Regular 6 shall allow placing a piece on its starting square, ignoring the special die, or
  > moving an active piece, **unless an opponent Double Block occupies that square** (R-4, Q40).

  FR-19 states the exception itself and cites Q40 as its source, so the rule agrees with FR-29 and the code
  implements the requirement rather than choosing between two requirements. The reading is @ziyanrakan33's,
  from #102; it is recorded here because it was checked against the requirement text rather than a
  paraphrase. The behaviour is in the merged code: `StandardMoveEngine.moveFor` requires
  `isPassable(board.startCell(owner.color()), owner)` before a castle exit.

  **Why D1 is nevertheless still OPEN.** The open question is narrower: whether two same-player pieces on a
  **start** square form a Double Block *at all*. FR-28 defines a block as "two or more same-player pieces on
  one square" and makes no exception for start squares, which is why FR-19 needs its `unless` clause — but
  the course-staff answers **Q7/Q35** are reported to say that no block forms there, in which case the
  clause could never apply. **Q7/Q35 could not be verified from this repository**: the SRS cites the staff
  answers by number without reproducing their text, and the Q&A sheet is not in the repository. D1 therefore
  needs authoritative staff resolution and **is not closable by the Product Owner alone**. The game rule is
  unchanged meanwhile. Register: `Iteration2/docs/T2-15_PO_Decisions.md`.

### Release checks (N2-06, NFR-10)

The submission ZIP is built only by `tools/make_iteration2_zip.py` (#81). The script exports the versioned
files with `git archive`; builds the JAR with `--release 17`; checks `Main-Class: Controller.Main` and that
every class file is Java 17 (major version 61); runs the whole suite; and writes the ZIP with forward slashes
under one top folder, printing the file count, the test count and the SHA-256.

**The test gate was strengthened after my review of #81.** It previously accepted a run in which **no test
was discovered at all**, because the JUnit launcher exits 0 in that case and the summary still prints
`[ 0 tests failed ]` — so both the exit code and the text check passed on an untested build. The script now
parses the JUnit summary and stops if any test failed or was aborted, **or if no test ran**; tests also run
by default instead of only with a flag.

**Re-verified independently on 23/11 against 17 synthetic JUnit summaries**, driving the gate function
directly so that no build, JAR or ZIP was produced: a normal run and a run with skipped tests are accepted;
zero tests is blocked; 1, 10, 20 and 100 failures are each blocked; an aborted test with no failures is
blocked; and an empty output, a build log with no summary, a half-summary missing either count, a summary in
another language, and container counts without test counts are all blocked as unreadable. **All 17 behaved
correctly.**

The same exercise found **one remaining weakness, low severity**: the summary parser keeps the *last*
occurrence of each word, so a stale `[ 0 tests failed ]` printed *after* a genuine failure block would mask
it — a real summary followed by that line is accepted with `found 664, successful 600, failed 0`. It cannot
happen today, because `test.sh` runs the launcher once and prints one summary; it becomes reachable only if
the suite is ever split across two runs. A four-line consistency check closes it, and I verified the fix
keeps all 17 cases correct. It is raised on #81 as a suggestion, not a blocker.

| Run | Code | Tests | ZIP | Started from a path with Hebrew letters |
|---|---|---|---|---|
| Dry run, 16/11 | `iteration-2` `38e2e1d` (Iteration 1 code) | 380/380 | 106 files | yes |
| CP1 rehearsal, 21/11, all 16 open PRs merged locally | local merge `3b4039e` | 664/664 | 168 files, JAR 261,390 bytes | yes, no error output |
| Script check after the review fix, 22/11 | `iteration-2` `d445a38` | 531/531 | 142 files, JAR 222,254 bytes | — |
| **Integration rehearsals, 23/11**, four runs in isolated folders on rising baselines | `13bc5b3`, `a312868`, `0928117`, `1228597` | **700/700** each | 171–174 files, JAR 263,868 bytes in all four | — |
| **The release procedure run on plain `iteration-2`, 23/11** — i.e. exactly what CP1 would ship today, with nothing merged by hand | **`4640416`** | **660/660** | **168 files**, JAR 263,868 bytes, SHA-256 `a5d4bb3c…442d21` | — |
| **THE CP1 RELEASE RECORD — Rakan Ziyan's artifact, PR #109** | **`1880c58`** | **685/685** | **169 files**, JAR **261,533 bytes**, **SHA-256 `750414c8…2b7878248`** | — |
| My own build of the same commit (a **second build**, not the CP1 record) | `1880c58` | 685/685 | 169 files, JAR 263,868 bytes, SHA-256 `1b6a4999…7620918e`, 3,019,109 bytes | — |
| My own build after #104/#106/#108 (a **second build**) | `071cf90` | 685/685 | 169 files, JAR 263,868 bytes, SHA-256 `bb0a393d…1a6eda3d` | — |
| **Verification candidate** (NOT a submission artifact) — `071cf90` + #111 + #99 + #103 | `d434dba` | **705/705** | 175 files, JAR 263,841 bytes, SHA-256 `f3677b4b…c64ba8` | — |
| **THE FINAL SUBMISSION ARTIFACT** — to be built **once** from the final `iteration-2` after the last merge | **TODO** | **TODO** | **TODO** — one file, copied to the second computer, never rebuilt there | — |

**Which row is the CP1 record.** It is the first of the artifact rows: **Rakan Ziyan's ZIP from `1880c58`,
PR #109, SHA-256 `750414c8…`**, with checks 1–8 passed by hand on computer 1. That is the artifact the team
approved as the CP1 release record, and it is his work, not mine. The rows below it are **my own separate
builds** of the same and later commits; they corroborate that the script reproduces the structure and the
test count, but **they are not the CP1 artifact and must not be quoted as it**. The rehearsal rows above
are integration checks in throwaway folders.

**The final submission ZIP does not exist yet** and is the last row. It is built once, from the final
`iteration-2`, after the last merge — see "The release artifact must be one file, not two rebuilds".

Both ZIPs were opened and checked entry by entry: **all entries under the single top folder
`iteration2_Sharks/`, no backslash in any entry**, and `EducationalLudo.jar`, `run.bat`, `build.bat`,
`test.bat`, `run.sh`, `build.sh`, `test.sh` and `README.md` all present, with the `build/` directory
correctly excluded.

The candidate's JAR was also started once to confirm it runs: it opened a real top-level window titled
**"Educational Ludo - Team Sharks"** on a 1920×1200 display and printed nothing to stderr. That is a
**launch** check and nothing more — it is not a playthrough, and no screenshot was taken.

The CP1 rehearsal also showed that every open PR merges without a conflict in the planned order.

**Not automated:** the double-click on `run.bat` and on the JAR, and a full game played by a person. Those are
rows 5–9 of the checklist in `Iteration2/docs/N2-06_Release.md` and are filled in by hand on both computers.

### The CP1 release and manual-test procedure, exactly as it is to be run

**Verified on 23/11:** steps 1–3 below were run on this computer and produced a complete ZIP from plain
`iteration-2`. Steps 4 onwards are the part **no one has done**, and they need a person at a real screen on
**both** release computers.

**Step 1 — get a clean copy of the merged branch.** In Git Bash:

```bash
cd /c/Ludo/Sharks
git fetch origin
git worktree add /c/Ludo/Sharks-CP1 origin/iteration-2 --detach
```

**Step 2 — build and test.** In File Explorer open `C:\Ludo\Sharks-CP1\Iteration2`, then run `build.bat`
and `test.bat`. Expect **705 found, 705 successful, 0 failed, 0 skipped, 0 aborted** on `06e2ab2`. If
`test.bat` reports any failure, stop: the release script will refuse to build a ZIP anyway.

**Step 3 — build the submission ZIP.** From `C:\Ludo\Sharks-CP1` in Git Bash:

```bash
python tools/make_iteration2_zip.py --ref origin/iteration-2 --out /c/Ludo/CP1-out
```

It prints the file count, the test count and the SHA-256. On `06e2ab2` expect **705/705**; the file count
is 169 plus the evidence images added in this revision. **Record the SHA-256 it prints: that one value is
what the second computer must reproduce on the copied file.** **Do not build the ZIP any other way** — not with Windows Explorer's "Send to
compressed folder", which writes backslash separators.

**Step 4 — the double-click tests, which are the ones that can disqualify the assignment.** Unzip the ZIP to
a fresh folder and run these **in File Explorer, not from a terminal**. The official N2-06 checklist
(`Iteration2/docs/N2-06_Release.md`) lists them as **two separate required rows**, and both must be recorded:

| N2-06 row | Check | Required |
|---|---|---|
| 4 | Unzip into a path **containing Hebrew letters**, then `java -jar EducationalLudo.jar` starts without errors | yes (passed on computer 1 in the 16/11 dry run) |
| **5** | **Double-click `run.bat`** opens the game window | **yes** |
| **6** | **Double-click `EducationalLudo.jar`** opens the game window | **yes** |
| 9 | Rows 1–8 repeated on a **second computer** | **yes** |

Assignment 5 §1 is the reason row 5 is decisive: *"a run file that does not start with a double-click
disqualifies the whole assignment."* No console window may stay behind afterwards.

> **Row 6 is a required check in its own right — do not wave a JAR double-click failure away.** If it fails,
> record it as a finding with the exact error text and the folder path. There is a known Iteration 1 cause to
> look at first: on Windows a path containing Hebrew letters made
> `java -jar "C:\...\עבודה 4\...\EducationalLudo.jar"` fail with *Unable to access jarfile*, which is why
> the scripts change into their own folder and use a relative path. **But N2-06 row 4 passed on computer 1 in
> the dry run**, so a Hebrew path is not by itself an accepted excuse: if row 6 fails, say on which computer,
> in which path, and with what message, and let the Product Owner decide. Note also that a double-click on a
> `.jar` depends on the Windows file association for `.jar`; if Windows asks which program to use, that is
> itself worth recording, because a grader will meet the same prompt.

**Step 5 — play the game, and record what you see.** The full row-by-row script is
`Iteration2/docs/T2-15_ManualChecklist_CP1.md` (12 rows). The checks that no automated test can replace:

| | What to do | Why only a person can do it |
|---|---|---|
| 1 | Start a **Medium** game with 4 named players, then a **Hard** game; confirm the Hard board is visibly larger | layout on a real display |
| 2 | Land on a **question square** and confirm the question's level matches **the square's colour**, not the board's | FR-34; the level mapping is only visible on screen |
| 3 | In the question dialog, press **Tab** repeatedly then **Space** | keyboard traversal and focus visibility. No SRS requirement mandates a focus indicator; this supports decision **D3** |
| 4 | Answer one question **right** and one **wrong**; then use **No Answer** followed by **Continue** | FR-35–FR-37 and decision D3 as the player meets them |
| 5 | Trigger a **surprise**; for "push an opponent back" confirm **you** choose the piece and nothing is auto-selected; watch for a **second** dialog if it lands on another special square | FR-40, FR-42 |
| 6 | **Stopwatch the longest Hard move** (Regular 6 with UltraBoost 6 = 30 squares) and **write down the milliseconds** | **the only check that closes NFR-04.** Every automated figure in this report is off-screen |
| 7 | Finish a complete game and confirm the ranking is in finishing order with the game clock | FR-43–FR-47 |
| 8 | Take screenshots: main menu, Medium board, Hard board, question dialog, surprise dialog, results screen | the report has none |

Time the long **Medium** move as well if an "Extra roll!" appears: `EXTRA_ROLL` uses the UltraBoost die on
every level (Table 2, Q27), so a Medium move can exceed 12 squares.

**If an opponent's Double Block on your start square stops a Regular 6, that is decision D1 — a known open
point, not a bug.** Note it and carry on.

**Step 6 — the second computer.** Repeat steps 1–5 on the other release computer from **the same ZIP**, not
from a fresh clone, and record **who ran it and when**. Iteration 1's equivalent evidence is
`Iteration1/docs/T28_SecondComputer_Checklist.md`. The partial run on branch
`feature/mohamed/T28-second-computer` is **not** a completed verification and must not be reported as one.

### Real-window GUI acceptance (23/11 and 25/11)

**This is the first evidence in this report that comes from the application actually running on a screen.**
Everything else in 3(e) is off-screen.

**The screenshots are in the repository**, under `Iteration2/docs/evidence/`, so they travel with the
submission ZIP and a grader can open them without any machine of ours. They are referenced by figure number
below. The fuller working log of the run stays outside the repository, on my machine, at
`C:\Ludo\CP1-GUI-Acceptance\` — nothing in this report depends on reading it.

| Figure | File | What it shows |
|---|---|---|
| G1 | [`evidence/01_launch_main_menu.png`](evidence/01_launch_main_menu.png) | the main menu of the game started from the CP1 ZIP |
| G2 | [`evidence/02_medium_board.png`](evidence/02_medium_board.png) | the Medium 17x17 board, legend and dice panel |
| G3 | [`evidence/03_hard_board.png`](evidence/03_hard_board.png) | the Hard 19x19 board; the legend counts match 3(c) exactly |
| G4 | [`evidence/04_question_card_level.png`](evidence/04_question_card_level.png) | a question card carrying the **square's** level (FR-34) |
| G5 | [`evidence/05_answer_feedback.png`](evidence/05_answer_feedback.png) | the answer feedback naming the correct answer (FR-35, FR-36) |
| G6 | [`evidence/06_noanswer_visible_after_fix.png`](evidence/06_noanswer_visible_after_fix.png) | the No Answer button, visible, after #111 |
| G7 | [`evidence/07_noanswer_focus_ring.png`](evidence/07_noanswer_focus_ring.png) | its keyboard focus ring |
| G8 | [`evidence/08_noanswer_mouse_result.png`](evidence/08_noanswer_mouse_result.png) | the result of a **mouse** click on it |
| G9 | [`evidence/09_nfr04_harness.png`](evidence/09_nfr04_harness.png) | the NFR-04 visible-window harness mid-move |

**One honest limit on G1.** The four launch routes below (run.bat, the JAR, and each from a Hebrew-letter
path) were each performed separately, and each opened the game — the automation log records four distinct
process ids. But the main menu is a static screen, so the four captures came out **byte-identical**, and a
single image is kept. **The pixels therefore do not distinguish the four routes; the logs do.** G1 shows
what every route opened, not which one.

Method: real Win32 mouse clicks and key presses delivered to the real Swing window on a real 1920x1200
display, with every screenshot taken by `PrintWindow` against the verified game window only. Where moves
were chosen by a script rather than by a person, the result is labelled **automation-driven gameplay** - it
is genuine GUI interaction, but it is not a human play-test.

**Launch checks (N2-06 rows 4, 5, 6), all by genuine double-clicks inside real File Explorer windows:**

| N2-06 row | Check | Result on the CP1 ZIP (`1880c58`) |
|---|---|---|
| 5 | Double-click `run.bat` | **PASS** - window opens, responsive, **no console left behind** |
| **6** | **Double-click `EducationalLudo.jar`** | **PASS** - no association prompt, no error |
| 4/5 | `run.bat` from a path with Hebrew letters | **PASS** |
| **4/6** | **JAR from a path with Hebrew letters** | **PASS** - the Iteration 1 *Unable to access jarfile* failure did **not** reproduce |

**Confirmed on screen:** Medium 17x17 (track 52+64=116, 6 questions, 2 surprises, **figure G2**) and
Hard 19x19 (52+64+72=188, 9 questions, 3 surprises, **figure G3**) — in both, the board legend on screen
prints the same counts as the `LevelConfig` table in 3(c), which is the table checked against the code; the dice panels and the `Regular x (special - 1)` formula; the
turn order and the game clock; legal-move highlighting; a roll of 1 correctly passing the turn with all
pieces in the castle (FR-19); name validation rejecting empty fields; and the leave-game confirmation.

**FR-34 confirmed in the strongest possible way (figure G4):** on the **Medium** board a question appeared
with the **Easy** badge and "1 steps at stake", and on the **Hard** board both an **Easy** and a **Hard**
question appeared. **The level follows the square, not the board.**

**FR-35 and FR-36 confirmed on screen in both directions (figure G5):** a correct answer gave *"Correct!"*, named the
correct answer and moved the piece **1 square forward** (Easy); a wrong answer and a No Answer each gave
*"Not correct"*, named the correct answer, and moved the piece **1 square backward** (Easy) and **3 squares
backward** (Hard). In every case the reported correct answer matched the question's correct option.

### The No Answer defect, found on screen and fixed (decision D3, FR-35)

**This is the one defect the GUI run found, and the existing tests could not have found it.**

On `iteration-2` the **No Answer** button of the question card was **completely invisible**. It was laid
out, it held keyboard focus as the fourth Tab stop, and SPACE on it submitted `QuestionResult.NO_ANSWER`
correctly - but nothing was painted. Measured on the running application: the region below the options was
**a single uniform colour, pure white, across 80,400 sampled pixels**. So **a mouse user could never reach
decision D3's "answer nothing" route at all**, and a keyboard user landed on it blind.

Root cause: the button used `CustomButton.Variant.GHOST`, which is built for dark backgrounds, on the white
question card (`Theme.SURFACE`). GHOST paints no fill, a white border, a `Theme.TEXT_ON_DARK` (white) label
**and a white focus ring**, so setting only a foreground colour would have made the label readable while
leaving the keyboard focus indicator invisible.

**Which requirement this defect is against.** **No SRS requirement mandates a visible keyboard focus
indicator.** NFR-13 is **dice distribution** - *"in 10,000 simulated draws, each die value and surprise
outcome shall be within 2 percentage points of its expected probability"* - which is how the rest of the
code uses it (`DiceStrategy`, `RegularDiceTest`, `SurpriseFactoryTest`), and NFR-24 covers text size and
colour identification, not focus. The basis is therefore **decision D3** - an invisible button does not
deliver the approved "answer nothing" route to a player using the mouse - together with **FR-35**. The
focus-ring work is recorded as supporting D3's usability, not as satisfying a numbered requirement.

Fix (**PR #111**, mine, **merged 25/11**): use `Variant.SECONDARY` for that one button - the variant the four
answer buttons on the same card already use. `CustomButton` is unchanged, so the dark-background Exit button
keeps its styling. No gameplay rule, Model or Controller code is touched.

**Why `QuestionDialogTest`'s 23 tests missed it:** they assert behaviour - `getText`, `isVisible`,
`doClick`, the key binding, the submitted result - and every one of those passed while the button was
invisible, because none looks at a painted pixel.

**Regression test** (`test/View/NoAnswerVisibilityTest.java`, **5 tests**) therefore renders the real card
and measures the painted pixels. The label check is **WCAG 2.1 contrast**; the two focus checks count pixels
close to **`Theme.ACCENT`**, the colour the ring is drawn in, so a dark label cannot satisfy them.

**A first version of the focus test was itself wrong, and is worth recording.** It measured peak contrast
across the whole button - which the dark label satisfies on its own, so it would have passed with the ring
removed entirely - and it tried to establish focus by dispatching a `FocusEvent`. Probed on the real class,
`isFocusOwner()` stays **false** after such a dispatch, and `CustomButton` paints its ring only when that is
true, so no ring was ever rendered in that test. It asserted nothing. The replacement establishes focus
either through a test-only subclass overriding `isFocusOwner`, or - in a second test - by putting the real
card in a real window and **waiting until `isFocusOwner()` actually becomes true**.

Evidence that the tests catch what they claim:

| Mutation | Result |
|---|---|
| the original `GHOST` implementation | **3 of 5 fail** - contrast 1.00 for the label, 0 accent pixels for each focus check |
| **only** the focus ring disabled in `CustomButton`, nothing else | **2 of 5 fail**, both focus tests, "only 0 accent pixels"; the label test correctly still passes |
| the fix | **5 of 5 pass** |

**Verified in the running application after the fix:** the formerly blank region now holds **250 distinct
colours** (**figure G6**); Tab shows **696 `Theme.ACCENT` pixels** forming a visible blue focus ring
(**figure G7**); and a **mouse click** on the button - previously impossible - produced (**figure G8**) *"Not correct"*, *"The correct answer is: Burndown Chart"*
and *"Your piece moves 3 squares backward."* The disabled state is now visible grey as well.

A stale source comment claiming *"Space or **Enter** presses it"* was also corrected: probed on the real
class, the only binding is SPACE (pressed and released, `WHEN_FOCUSED`), ENTER maps to null, and the button
is not the default button. No Enter binding was added, because no requirement asks for one.

### NFR-04 measured on screen - two separate kinds of evidence

These must not be conflated, so they are listed apart.

**(a) Visible-window harness, 30 squares.** An isolated harness compiled **against the released JAR** puts
the **real production `BoardComponent`**, bound to a real `GameModel`, into a **real visible window**, and
runs the real `animateAlong` for the longest Hard move (Regular 6 x (UltraBoost 6 - 1) = 30 squares). The
measurement ends **after the final frame is actually painted**, not when the callback fired.

| Run | Frames repainted | ms to callback | **ms on screen** |
|---|---|---|---|
| 1 | 53 | 1222 | **1306** |
| 2 | 60 | 1230 | **1313** |
| 3 | 61 | 1216 | **1301** |
| 4 | 61 | 1215 | **1296** |
| 5 | 63 | - | **1311** |

The harness window mid-move is **figure G9**.

Mean about **1305 ms**, headroom about **695 ms** under NFR-04's 2000 ms. 53-63 repaints for 30 squares is
about two painted frames per square, so the movement really is animated square by square. **This is a real
component in a real window - but it is not the full game being played.**

**(b) Inside a complete running game, measured from outside the process.** The board region is completely
static at rest (0 of 29 frame pairs changed over 2.5 s), so a move can be bounded externally: t0 at the
mouse click on the piece, t1 at the last capture where the board still changed. Resolution about 90 ms.

**16 valid in-game measurements on a Hard game, every one under 2000 ms**, the longest being **1578 ms for a
10-step move** (`5 x (3 - 1) = 10 steps`).

> **Read the evidence filename carefully.** That measurement is saved as `ingame_move_30_1577ms.png`, and
> the **30 is the attempt number, not a number of squares** — the capture loop names its files
> `ingame_move_<attempt>_<ms>ms.png`. The move it shows is **10 steps in 1577 ms**. No 30-square move was
> produced by natural play in this run.

That 10-step figure is **not** a weaker version of the 30-square case. The budget is
`max(260, min(1200, steps x 120))`, so **any move of 10 squares or more is already at the 1200 ms cap** - a
10-step and a 30-step move are given exactly the same budget. The 30-step move paints more intermediate
frames, so the two are not identical, but the in-game figure is at the cap and under the limit.

**A 30-square move was not produced by natural play** in the bounded run, so case (b) covers up to 10 steps.
13 further attempts could not be timed: when a legal piece is highlighted it **glows**, so the board never
goes quiet and the external timer hits its limit. Those are **measurement timeouts, not slow moves**, and
are excluded rather than reported as failures.

### Manual release checks recorded by Rakan on computer 1 (N2-06, #109)

**These are Rakan's results, run by hand on his machine. They are not mine and are not automation.** They
are recorded here because they cover exactly the scenarios my automated GUI run could not reach.

| N2-06 row | Check | Result |
|---|---|---|
| 1 | ZIP structure | PASS, 169 files, one top folder, no backslash, no `build/` |
| 2 | JAR manifest and Java 17 class files | PASS, 261,533 bytes |
| 3 | Suite in the staged copy | **PASS, 685/685** |
| 4 | `java -jar` from a Hebrew-letter path | PASS (computer 1) |
| 5 | **Double-click `run.bat`** | **PASS** (computer 1, Rakan) |
| 6 | **Double-click `EducationalLudo.jar`** | **PASS** (computer 1, Rakan) |
| 7 | **Easy, Medium and Hard each start and accept a roll** | **PASS** (computer 1, Rakan) |
| 8 | **A full game reaches the results screen** | **PASS** (computer 1, Rakan) |
| 9 | Checks 1-8 on a **second computer** | **still open - mine to run** |

Row 7 and row 8 are the two things this report could not otherwise claim: **an Easy game, and a complete
game played through to the results screen.** They are Rakan's manual evidence, and nothing in my automated
run is presented as covering them.

### The release artifact must be one file, not two rebuilds

**Rakan's ZIP and mine, both built from `1880c58`, do not match:**

| | Rakan (computer 1) | Mohamed (computer 2) |
|---|---|---|
| Files | 169 | 169 |
| Tests | 685/685 | 685/685 |
| **JAR size** | **261,533 bytes** | **263,868 bytes** |
| **SHA-256** | `750414c8d76d140a...2b7878248` | `1b6a49997d3a079f...7620918e` |

There are **two independent reasons** a rebuild cannot reproduce a checksum, so "the SHA-256 should match
while `iteration-2` stays at 1880c58" does not hold:

1. **The ZIP embeds build timestamps.** Measured here: the same commit built twice on the same machine gave
   different SHA-256 values, and the entry timestamps span the moment the JAR was rebuilt.
2. **The JARs themselves differ in size** - 2,335 bytes - from identical source. That is a toolchain
   difference between the two machines (this one is Temurin 17.0.17+10), not a content difference.

**So row 9 must be run on a byte-identical copy of one artifact, not on an independently rebuilt one.** The
procedure is: build the final ZIP once, after the approved merges; record its SHA-256; copy that exact file
to the second computer; verify the hash there with `Get-FileHash -Algorithm SHA256` before testing.

### What has not been verified yet

This list is what an examiner should treat as **not** evidenced. Everything else in 3(e) is measured.

1. **A human playthrough** of a complete Medium and Hard game by one of us, from the menu to the results
   screen (scripts M1 and M2; the short version is `Iteration2/docs/T2-15_ManualChecklist_CP1.md`).
   Automation-driven play on 23/11 and 25/11 covered setup, both boards, the dice, highlighting, four
   question cards and about 200 turns without a freeze, **but reached neither the results screen nor a
   surprise square**. Rakan Ziyan did play a full game by hand on computer 1 (N2-06 row 8, #109); that is
   his evidence, recorded above under his name, and it is not a substitute for the scripted M1/M2 run.
2. **The second computer** (N2-06 row 9). Rows 1-8 pass on computer 1, and rows 5 and 6 - the two
   double-click checks - also pass on this computer by genuine Explorer double-clicks on the CP1 ZIP,
   including from a Hebrew-letter path. **No check has been run on a second physical computer for
   Iteration 2.** It must be run on a byte-identical copy of the final ZIP, not on a rebuild.
3. **A 30-square move produced by natural play, timed on screen** (NFR-04). This is **partly** closed: a
   visible-window harness runs the real `BoardComponent` for a real 30-square move at about **1305 ms**,
   and **16 in-game measurements are all under 2000 ms**, the longest **1578 ms** for a 10-step move, which
   is already at the 1200 ms cap. What is missing is the 30-square case arising inside a full game, with a
   human stopwatch beside it.
4. **Surprise outcomes, opponent-piece selection and chained effects as seen on screen.** They are covered
   by automated tests end to end, and all six outcomes are drawn in the full-game runs, but **no surprise
   square was reached in the real-window GUI run**. The on-screen evidence above covers questions, not
   surprises.
5. **Decisions D1, D4 and D5.** D1 is **OPEN** and needs the course staff's answer on Q7/Q35; D4 and D5 are
   implemented and tested and need only the Product Owner's formal ratification.

## 6. What finishing this report needs

| # | Needed | Depends on |
|---|---|---|
| 1 | Run the manual scripts M1 and M2 (a complete Medium and Hard game played by a person) and paste the filled-in tables in as 3(e) evidence | a human, before 05/12 |
| 2 | Run N2-06 row 9 on a **second physical computer**, from a byte-identical copy of the final ZIP | the final ZIP, then a human |
| 3 | Build the **final submission ZIP once** from the final `iteration-2`, record its SHA-256 in the release table, and use that one file everywhere | the last merge |
| 4 | Stopwatch a 30-square move on screen inside a full game (the last open part of NFR-04) | a human; step 5 of the procedure in 3(e) |
| 5 | Settle **D1**, and ratify **D4** and **D5** | staff clarification on Q7/Q35, then the Product Owner |
| 6 | Export this text as `iteration2_report_Sharks` (Word and PDF), continuing `iteration1_report_Sharks`: keep its structure and numbering and add Iteration 2 as the new iteration rather than replacing it | all of the above |

**This file is the source text, not the deliverable.** The deliverable is the Word and PDF
`iteration2_report_Sharks`, continuing `iteration1_report_Sharks`. The export keeps the final numbers and
the measured evidence; it does not carry the pull-request history, which belongs here and in the Scrum
Report.
