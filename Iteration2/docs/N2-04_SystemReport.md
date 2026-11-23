# N2-04: Iteration 2 system report (source for `iteration2_report_Sharks`)

Task **N2-04** · Issue [#70](https://github.com/shafeekseef81-svg/Sharks/issues/70) · Owner: **Mohamed Zereny** · Reviewer: Shafeek Saif · Due 05/12/2026

Deliverable 3 of Assignment 5: a Word or PDF file named `iteration2_report_Sharks`, **continuing the
Iteration 1 report** (`iteration1_report_Sharks.docx` / `.pdf` in the repository root). The sections below
follow the brief's required structure 3(a)–3(e).

**Status 23/11/2026: structure complete, content partly verified.** Every statement here is either measured
and marked as such, or marked **TODO** with what is still needed. Nothing is assumed, and no screenshot,
measurement or playthrough is recorded that was not actually taken. The report cannot be finished before the
Iteration 2 code is merged (see section 6).

Baseline for everything measured here: `iteration-2` at **`1d47f2d`** (23/11/2026, second revision; it was
`e9f6b2e` earlier the same day, `d445a38` on 22/11 and `6e13bcf` on 21/11). Baseline suite: **656 tests, all
passing**.

**The whole feature scope of Iteration 2 is now merged.** T2-01 to T2-07, N2-03 (#86, #101), T2-10 part 1
(#87, `e2bbdaa`), T2-08 (#89, `5540f53`), T2-13 the turn State pattern (#79, `c689a85`), T2-14 (#93),
N2-05 (#88, #94), and — **new on 23/11** — **T2-09 surprise outcomes (#90, `36661fc`)**, **T2-11 the
question and surprise dialogs (#91, `12a01b6`)** and **T2-10 part 2, the effect wiring (#92, `1d47f2d`)**.

**Still not merged:** the NFR-04 animation budget (**#97**, approved) and the independent effect tests
(**#95**, approved).

**What that changes for this report.** Until #92 merged, a question or surprise square was an ordinary
square in any build made from `iteration-2` — nothing opened a dialog, so no amount of manual testing could
accept US-2.3 from the release branch. That is no longer true: **a player can now reach every Iteration 2
rule in a build made from `iteration-2`.** Sections 3(b)–3(d) are therefore no longer TODO for the
player-visible behaviour; what remains genuinely unverified is that **nobody has sat down and played it**
(see "What has not been verified yet").

**#97 is a release blocker for CP1**, and that is the one open item with a deadline. See the NFR-04
subsection of 3(e).

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

All figures Java 17 (Temurin 17.0.17), `Iteration2/test.sh`, measured **23/11/2026** unless a row says
otherwise.

| What | Tests | Result |
|---|---|---|
| `iteration-2` baseline `1d47f2d` | 656 | **656 passed, 0 failed** |
| with the T2-15 acceptance tests (#99, `df396b6`) | 663 | **663 passed, 0 failed** |
| with the US-2.3 end-to-end acceptance (#103, `ee465e4`) | 664 | **664 passed, 0 failed** |
| **#99 and #103 together** | 671 | **671 passed, 0 failed** |
| **the CP1 candidate: `iteration-2` + #97 + #95 + #99 + #103 + #98** | **700** | **700 passed, 0 failed** |

The last row is the number that matters for CP1. **All five branches merged with no conflict**, and the
figure is independently corroborated: Shafeek measured the same **700/700** on his own locally-merged
candidate (#106, T2-14 snapshot of 23/11), built from a different starting commit and a slightly different
set of PRs.

**#95 is not really conflicting.** GitHub shows it that way because it is compared against plain
`iteration-2`; merged in its intended order it is clean.

The unmerged rows are integration checks done in throwaway worktrees; **nothing was pushed to anyone else's
branch.** Earlier figures, kept so the growth stays traceable: the baseline was 540 on `e9f6b2e` (earlier on
23/11), 531 on `d445a38` (22/11) and 433 on `6e13bcf` (21/11). The suite grew because tasks merged, not
because tests changed.

### An integration failure the reviews caught, and how it was fixed

This is the clearest evidence this iteration that reviewing against the *integrated* system, and not only
against a branch's own baseline, is worth the effort — so it is recorded in full.

**What was found.** Shafeek reviewed #99 (T2-15 acceptance) on 23/11. The seven acceptance tests passed on
`iteration-2`, and the whole suite was 547/547. But he also ran them against **`iteration-2` + #92**, and
**five of the seven failed**:

```
java.lang.IllegalStateException: Resolve the special square first (phase RESOLVING)
    at Model.ResolvingState.roll(ResolvingState.java:32)
    at Model.MediumHardAcceptanceTest.playToEnd(MediumHardAcceptanceTest.java:96)
```

**Why.** Since #92, a move onto a question or surprise square leaves the turn in `RESOLVING`, and the next
`roll()` is refused until the square is resolved. The acceptance test's play loop rolled again regardless.
Every test that plays a complete game failed.

**Why it mattered more than a failing test.** There is no CI on `iteration-2`. Whichever of #92 and #99
merged second would have broken the branch, days before CP1, and nobody would have seen it until someone
happened to run the suite.

**How it was fixed, in two stages.** It was reproduced exactly in an isolated worktree, and fixed — but at
first **not on #99**. The fix needs `pendingEffect()`, `judgeAnswer()` and `resolveSurprise()`, and none of
those existed on `iteration-2`, which was #99's own target; putting the fix there would have stopped #99
compiling against the branch it merges into. So the fix was proved green on a Draft follow-up, **#105**,
built on `iteration-2` + #92 + #99, at 663/663, while #99 itself stayed buildable on its target at 547/547.

**Once #92 merged later the same day, the fix moved into #99 itself** (`df396b6`), which is where it
belongs, and **#105 became redundant — it is to be closed without merging, not merged**, because its diff
also contains all of #92 and #99. `playToEnd` now resolves every waiting square, and any chain it sets off,
before taking the next turn.

**Why the two-stage route was the right one, and not over-caution.** Had the loop gone straight onto #99, the
PR would have stopped compiling against its own target branch — the exact failure mode the review was
warning about, only pointed the other way. Keeping the two apart meant that at every moment one branch was
green against `iteration-2` and the other was green against the integrated tree, and neither claim depended
on a merge that had not happened.

**A second finding came out of that fix, and it is a rule rather than a bug.** The first run of the fixed
loop failed with `MEDIUM: the special die of the level is rolled ==> expected: <BOOST> but was:
<ULTRA_BOOST>`. The `EXTRA_ROLL` surprise rolls the **Regular + UltraBoost** dice *whatever level is being
played* (SRS Table 2, Q27; `StandardTurnManager.SURPRISE_ROLL_DICE`). So the assumption that a level only
ever rolls its own dice is false on the integrated game, and it was written into an acceptance test.

Player-visible consequence, worth the team's attention: **a Medium game can produce a movement of more than
12 squares.** Measured over nine Medium games, the longest ordinary movement was 12 as specified, but the
longest surprise-roll movement was **25**, and the bound that actually holds on Medium is the UltraBoost
one, 6 × (6 − 1) = **30**. The NFR-04 animation budget caps by distance and not by level, so **it still
covers this** — but the 30-square case is no longer Hard-only, and anywhere we have written "the longest
Medium movement is 12" needs the qualifier "from the level's own dice". Shafeek's Strategy section of
`iteration2_dp_Sharks` already records the underlying rule.

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

> **Corrected 23/11/2026.** An earlier draft of this table said "**94**, all distinct ids". That was not
> supportable and has been withdrawn. The course bank holds **36** questions, 12 of each level
> (`Model/CourseQuestions`), and each game builds its own decks, so 94 distinct ids cannot exist. Three
> different things were being conflated, and they are now separate columns: **94** is the number of
> questions *asked* over five games; **34** distinct ids appear among them, of the 36 that exist; and **0**
> questions repeated within any single game — which is the FR-37 property actually worth claiming.

All six surprise outcomes are drawn across these games, so the outcome table of FR-39 is exercised end to
end rather than in part.

**Three limits of this evidence**, recorded because they decide what still needs covering:

* **The cross-player case of PO decision D2 never occurs naturally.** Measured, 26 turn outcomes across nine
  games and **0** of them opponent-triggered. The D2 check in the full-game tests is a standing guard over
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

#### #97 is a release blocker for CP1, and this is the evidence

This is the one open item with a deadline, so it is stated precisely rather than as a recommendation.

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

**Recommendation: merge #97 before building the CP1 artifact.** It is approved by both reviewers
(@ziyanrakan33 22/11, @shafeekseef81-svg 23/11), mergeable, and touches only `BoardComponent` and its own
tests. With it in the CP1 candidate, `TimingTest` passed 4/4 on three cold runs and
`MovementAnimationTest` 15/15 here.

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

  **Rakan sharpened this in #102 on 22/11, and I checked his correction against the code (23/11).** He now
  states the implemented behaviour as FR-19's *own* exception rather than as FR-29 simply overriding FR-19:
  FR-19 reads "a Regular 6 shall allow placing a piece on its starting square … **unless an opponent Double
  Block occupies that square** (Q40)". That is a better reading than mine, because it means the code follows
  FR-19 rather than contradicting it, and it leaves D1 as a genuine open question about whether two pieces
  on a *start* square form a Double Block at all (staff answers Q7/Q35) — not about which requirement wins.
  I verified the behaviour he describes is what `StandardMoveEngine` does: `moveFor` checks `isPassable` on
  the start cell before a castle exit. **D1 remains OPEN.**

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
| **Full rehearsal, 23/11**, into an isolated folder | integration candidate (`13bc5b3`) | **700/700** | **171 files, JAR 263,868 bytes**, SHA-256 `05b3e113…3544b9` | — |
| **CP1 candidate rehearsal, 23/11** (second revision), isolated folder | `iteration-2` `1d47f2d` + #97 + #95 + #99 + #103 + #98 (`a312868`) | **700/700** | **172 files, JAR 263,868 bytes**, SHA-256 `462c774a…4ac386` | — |
| CP1, 25/11 | merged `iteration-2` | **TODO** | **TODO** | **TODO**, both computers |

The CP1 rehearsal also showed that every open PR merges without a conflict in the planned order.

**Not automated:** the double-click on `run.bat` and on the JAR, and a full game played by a person. Those are
rows 5–9 of the checklist in `Iteration2/docs/N2-06_Release.md` and are filled in by hand on both computers.

### What has not been verified yet

1. **A human playthrough** of a complete Medium and Hard game (scripts M1, M2). The short, run-it-now
   version is `Iteration2/docs/T2-15_ManualChecklist_CP1.md`.
2. ~~**Questions, surprises and chains as player-visible behaviour**~~ - **no longer outstanding.** #90, #91
   and #92 merged on 23/11, so a player can now reach every Iteration 2 rule in a build made from
   `iteration-2`, and sections 3(b)-3(d) are written as fact rather than as TODO. What is left of this item
   is item 1: **the code is all there, and nobody has played it.** The independent effect tests (#95) are
   still unmerged, which is a coverage gap rather than a functionality gap - see the D4 caveat in the
   decision register.
3. **The double-click start on both computers** with the final ZIP (N2-06). **Partly done, and the limit is
   stated:** on 23/11 I verified on a real Windows display that `EducationalLudo.jar` started by
   `java -jar`, and that `run.bat` started the game through `javaw`, each opening a visible top-level window
   titled "Educational Ludo - Team Sharks" and leaving no console window behind. **Both were started from a
   command line, not by a double-click in File Explorer**, which also exercises the `.jar` file association
   and the working directory Explorer hands the process. The double-click itself, on both computers, is
   still TODO.
4. **The real on-screen animation timing** (NFR-04), as above. Every automated timing figure in this report
   is **off-screen**; none of them paints a real window, which is exactly the gap that produced the 2140 ms
   measurement. A stopwatch on a real window is the only thing that closes it.
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
| 1 | ~~Merge the remaining Iteration 2 feature PRs, then rewrite 3(b)-3(d)~~ **done 23/11** - #90, #91 and #92 merged; 3(b)-3(d) now state the behaviour as fact | — |
| 1b | **Merge #97 before building the CP1 artifact.** It is a release blocker, not a nicety: without it the 30-square timing test fails on at least one of our machines, and the ZIP script stops on a failed test | the Product Owner's merge |
| 2 | Run the manual script M1-M7 and paste the filled-in tables in as the 3(e) evidence | T2-15, N2-06 |
| 3 | Re-run the whole suite on the final `iteration-2` and replace every test count | after the last merge |
| 4 | Take the screenshots from the final build | after the last merge |
| 5 | Settle **D1** so 3(c) states a rule and not an open question, and ratify **D4** and **D5** (both implemented and tested; see the register) | staff clarification on Q7/Q35, then the Product Owner |
| 5b | Add the US-2.3 end-to-end acceptance evidence of **#103** to 3(e) once it merges, and the independent-test evidence of #95 | #103 is now retargeted at `iteration-2` and ready for review; #95 still unmerged |
| 5c | ~~Move the fix on **#105** into #99~~ **done 23/11** (`df396b6`). #105 is now redundant and is to be **closed without merging** | — |
| 5d | Fill in `T2-15_ManualChecklist_CP1.md` on both computers, including the stopwatch row, and paste the filled tables into 3(e) | a human, before CP1 |
| 6 | Continue the Iteration 1 report: keep its structure and numbering, add Iteration 2 as the new iteration rather than replacing it, and export to Word or PDF as `iteration2_report_Sharks` | all of the above |

**This file is the source text, not the deliverable.** The deliverable is the Word or PDF
`iteration2_report_Sharks`, continuing `iteration1_report_Sharks`.
