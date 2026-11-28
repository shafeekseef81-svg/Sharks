# T2-15: manual acceptance checklist for CP1 (Windows PC)

Task **T2-15** · Issue [#66](https://github.com/shafeekseef81-svg/Sharks/issues/66) · Owner: **Mohamed Zereny** (Product Owner) · Prepared 23/11/2026, CP1 due 25/11/2026.

This is the short, run-it-now version of the scripts M1–M7 in `T2-15_AcceptancePlan.md`. It exists because the acceptance plan is long and CP1 is close. **Fill the Result column in while running. A row that was not run stays empty — an empty row is not a pass.**

## 0. Which build to test, and why it matters

**Updated 28/11/2026. All Iteration 2 code is merged**, so there is one build and no PR to merge by hand.

| Build | Contains | Use it for |
|---|---|---|
| **`iteration-2` `06e2ab2`** | everything: boards, dice, Double Blocks, rankings, the question and surprise cards, chained effects, the turn phases, the 1200 ms animation budget, **and the No Answer fix (#111)** | **all rows** |

```bash
cd /c/Ludo/Sharks
git fetch origin
git worktree add /c/Ludo/Sharks-CP1 origin/iteration-2 --detach
cd /c/Ludo/Sharks-CP1/Iteration2 && ./build.bat && ./test.bat
```

Expect **705 found, 705 successful, 0 failed, 0 skipped, 0 aborted**. If you want to be sure the animation
budget really is in the build before trusting row 10:

```bash
grep -n "MAX_MOVE_MILLIS" src/View/components/BoardComponent.java
```

It should read `1200`.

**Better still, test the release artifact rather than a worktree**, because that is what is submitted:
unzip the ZIP built by `tools/make_iteration2_zip.py` and run the rows inside the unzipped folder. The
build of 28/11 from `06e2ab2` is 175 files, 705/705, SHA-256 `0ac4d6fb…e707543`.

> **One build note.** Row 3 asks you to press **Tab** then **Space** on the question card. Before #111 the
> **No Answer** button was painted white on a white card — invisible, though it worked. It is fixed on this
> baseline, so the button must be **visible** and must show a blue focus ring when Tab reaches it. If it is
> not visible, you are testing an old build.

## 1. What is already verified on this PC — do not re-run unless you want to

Re-verified on **28/11/2026** against the merged baseline `iteration-2` **`06e2ab2`** and the release ZIP
built from it.

| # | Check | Result | How it was verified |
|---|---|---|---|
| 0.1 | The suite passes on the merged baseline | **PASS** | **705 found, 705 successful, 0 failed, 0 skipped, 0 aborted** (Temurin 17.0.17+10) on `06e2ab2`, nothing merged by hand. Corroborated independently by Shafeek on 26/11 |
| 0.2 | The release script produces a ZIP | **PASS** | **175 files**, JAR 263,841 bytes, Java 17 class files, `Main-Class: Controller.Main`, 705/705, SHA-256 `0ac4d6fb…e707543` |
| 0.3 | The ZIP's structure is correct | **PASS** | All entries under the single top folder `iteration2_Sharks/`, no backslash in any entry, `build/` excluded, and the run/build/test scripts and `README.md` all present |
| **0.4** | **Double-click `run.bat` in File Explorer** | **PASS** | A **genuine double-click inside a real File Explorer window**. The game window opened, 1600×950, responsive, and **0 console windows were left behind** |
| **0.5** | **Double-click `EducationalLudo.jar` in File Explorer** | **PASS** | Same method. No file-association prompt and no error |
| 0.6 | `java -jar` from a folder with **Hebrew letters** | **PASS** | Verified on 25/11 on this PC against the CP1 ZIP; the Iteration 1 *Unable to access jarfile* failure did not reproduce |
| 0.7 | Animation timing, **off-screen** | **PASS** | `View.TimingTest` 4/4 on three cold-JVM runs, `View.MovementAnimationTest` 15/15. Off-screen — this does **not** substitute for row 10 |
| 0.8 | Animation timing, **in a real window** | **PASS** | A harness compiled against the released JAR ran the real `BoardComponent` for a real 30-square move in a visible window: **1296–1313 ms** over 5 runs, limit 2000 ms. Still a harness, not a played game |
| 0.9 | Every FR has a test (NFR-14) | **PASS** | `tools/rtm_coverage.py` against the real SRS text: **`GAPS: none` for FR-15…FR-47** |
| 0.10 | The **No Answer** button is painted, focusable and clickable | **PASS** | On screen after #111: the formerly blank region holds 250 distinct colours, Tab shows a blue focus ring, and a **mouse click** on it produced *"Not correct"* with the correct answer named |

**Rows 0.4 and 0.5 are the two that can disqualify the assignment**, and they now pass on this computer by
real double-clicks. **They have not been done on a second computer.**

**Some gameplay screenshots now exist**, taken from the game window only and committed to
`Iteration2/docs/evidence/`. They cover the menu, both boards, the question card and the No Answer button.
**They do not cover a surprise card, a chained effect or the results screen** — those are still yours to
capture in rows 5, 6 and 7 below.

## 2. The checklist

Build under test: ________________  ·  Date: ________  ·  Tester: ________

| # | Step | Expected | Result | Evidence |
|---|---|---|---|---|
| 1 | In File Explorer, **double-click `run.bat`** | The window opens within a few seconds. No console window is left behind. No Java error | | |
| 2 | In File Explorer, **double-click `EducationalLudo.jar`** | The same window opens. If Windows asks which app to use, pick Java — and **note that**, because a grader will hit it too | | |
| 3 | Start a **Medium** game, 4 players, with names | The setup accepts the names; the Medium board is drawn; play begins | | |
| 4 | Start a **Hard** game, 4 players | The Hard board is drawn and is clearly larger | | |
| 5 | Play until a piece stops on a **question square** | The question dialog appears. The question's level matches the **square's colour**, not the board's. Four options | | |
| 6 | In that dialog press **Tab** repeatedly, then **Space** | Tab reaches every button in a sensible order and the focused button is visibly focused. Space activates the focused button | | |
| 7 | Answer one question **right** and one **wrong** | Right: the piece moves **forward** 1/2/3 by the question's level. Wrong: it moves **back** the same number. Both show the correct answer afterwards | | |
| 8 | Press **No Answer**, then **Continue** | No Answer is accepted as an explicit choice and counts as **wrong** (PO decision D3). Continue closes the dialog and the turn goes on | | |
| 9 | Play until a **surprise square** fires, and watch for a **chain** | The surprise dialog names the outcome. For "Push an opponent back" **you** choose which opponent piece — nothing is auto-selected (FR-40). If the effect lands the piece on another special square, a second dialog follows (FR-42) | | |
| 10 | **Stopwatch the longest movement.** In a **Hard** game, time a Regular 6 with an UltraBoost 6 — a 30-square move — from the moment the piece starts to the moment it stops | **Under 2 seconds** (NFR-04). Record the actual number, not "fine" | | |
| 11 | Take screenshots: main menu, Medium board, Hard board, question dialog, surprise dialog, results screen | Saved as PNG with the date in the filename | | |
| 12 | Finish one complete game | The results screen shows the ranking in finishing order and the game clock | | |

### Row 10 is the one most likely to fail, and there is a new wrinkle

NFR-04 gives every movement 2 seconds. The automated timing tests are **off-screen** — they never paint a real window — and that is exactly the gap that bit us before: Rakan measured **2140 ms** for the 30-square move under parallel load at the old 1600 ms budget. #97 lowered the budget to **1200 ms** and is now merged, and the off-screen figure is **1278 ms** — but **only a stopwatch on a real window closes this**. Write down the milliseconds you actually measure, not "looked fine".

**New, found today:** a **Medium** game can also produce a movement of more than 12 squares. The `EXTRA_ROLL` surprise rolls the **Regular + UltraBoost** dice whatever the level (SRS Table 2, Q27), so a Medium surprise roll can reach the same 30-square bound — measured at 25 over nine Medium games. The animation budget caps by distance, not by level, so it still covers this. But **if you get a long move on Medium after an "Extra roll!", time that too** and note it as Medium.

## 3. What must be true before this is signed off

* **All rows** on **both release computers** (N2-06 asks for two).
* Row 10 no longer needs #97 merged by hand — it is in `iteration-2` as of `1f099f2`. Check that
  `MAX_MOVE_MILLIS` reads `1200` if you want to be certain before timing.
* **D1 stays OPEN.** If an opponent's Double Block sits on your start square and a Regular 6 will not let you out of the castle, that is the known open decision, **not a bug**. Note it and move on.

## 4. Sign-off

| Computer | Rows 1–4 | Rows 5–9 | Row 10 (actual ms) | Rows 11–12 | Tester | Date |
|---|---|---|---|---|---|---|
| PC 1 | | | | | | |
| PC 2 | | | | | | |

**Do not tick a row that was not run.** The whole point of this task is that the report says only what somebody actually saw.
