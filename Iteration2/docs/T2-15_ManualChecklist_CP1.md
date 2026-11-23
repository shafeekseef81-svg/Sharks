# T2-15: manual acceptance checklist for CP1 (Windows PC)

Task **T2-15** · Issue [#66](https://github.com/shafeekseef81-svg/Sharks/issues/66) · Owner: **Mohamed Zereny** (Product Owner) · Prepared 23/11/2026, CP1 due 25/11/2026.

This is the short, run-it-now version of the scripts M1–M7 in `T2-15_AcceptancePlan.md`. It exists because the acceptance plan is long and CP1 is close. **Fill the Result column in while running. A row that was not run stays empty — an empty row is not a pass.**

## 0. Which build to test, and why it matters

**Updated 23/11/2026 (second revision): #90, #91 and #92 have merged.** The earlier version of this file split the work across two builds, because questions and surprises could not be reached at all from `iteration-2`. **That split is gone — one build now covers every row.**

| Build | Contains | Use it for |
|---|---|---|
| **`iteration-2` `1d47f2d`** | everything: boards, dice, Double Blocks, rankings, **and** the question and surprise dialogs, chained effects and the turn phases | **all rows** |

```bash
cd /c/Ludo/Sharks
git worktree add /c/Ludo/Sharks-CP1 origin/iteration-2 --detach
cd /c/Ludo/Sharks-CP1/Iteration2 && ./build.bat
```

**One thing is still missing from that build: #97**, the NFR-04 animation budget. It is approved and mergeable but not merged, and **row 10 is the row it affects.** Either merge #97 first, or add it to your test build:

```bash
git merge --no-edit origin/feature/mohamed/NFR-04-animation-budget
```

Timing row 10 **without** #97 measures the old 1600 ms budget — the one Rakan measured at 2140 ms under load. If you time it without #97 and it is over 2 seconds, that is expected: it is the reason #97 exists.

## 1. What I already verified on this PC (23/11/2026) — do not re-run unless you want to

| # | Check | Result | How it was verified |
|---|---|---|---|
| 0.1 | The JAR starts and opens a real window | **PASS** | `java -jar EducationalLudo.jar` on build B created a visible top-level window titled **"Educational Ludo - Team Sharks"**, confirmed in `tasklist /V` as *Running* on a real 1920×1200 display |
| 0.2 | `run.bat` starts the game | **PASS** | `run.bat` on build B started **javaw.exe** with the same window title, *Running*. The script uses `javaw`, so no console window stays behind |
| 0.3 | The suite passes on the integration candidate | **PASS** | 700 found, 700 successful, 0 failed (Java 17, Temurin 17.0.17) |
| 0.4 | The release script produces a ZIP from that candidate | **PASS** | 171 files, JAR 263,868 bytes, Java 17 class files, `Main-Class: Controller.Main`, 700/700, SHA-256 `05b3e113…3544b9`, into an isolated folder — the team's official artifact was not touched |

**What 0.1 and 0.2 do *not* prove.** They were started from a command line, not by a **double-click in File Explorer**. That is still row 1 and row 2 below: a double-click also exercises the `.jar` file association and the working directory Explorer hands the process, and only you can do it.

**No gameplay screenshots were taken.** A full-screen capture on this PC caught your own browser rather than the game, so I deleted it and did not retry. Screenshots are rows 11 below and are yours to take.

## 2. The checklist

Build under test: ________________  ·  Date: ________  ·  Tester: ________

| # | Step | Expected | Result | Evidence |
|---|---|---|---|---|
| 1 | In File Explorer, **double-click `run.bat`** | The window opens within a few seconds. No console window is left behind. No Java error | | |
| 2 | In File Explorer, **double-click `EducationalLudo.jar`** | The same window opens. If Windows asks which app to use, pick Java — and **note that**, because a grader will hit it too | | |
| 3 | Start a **Medium** game, 4 players, with names | The setup accepts the names; the Medium board is drawn; play begins | | |
| 4 | Start a **Hard** game, 4 players | The Hard board is drawn and is clearly larger | | |
| 5 | play until a piece stops on a **question square** | The question dialog appears. The question's level matches the **square's colour**, not the board's. Four options | | |
| 6 | in that dialog press **Tab** repeatedly, then **Space** | Tab reaches every button in a sensible order and the focused button is visibly focused. Space activates the focused button | | |
| 7 | answer one question **right** and one **wrong** | Right: the piece moves **forward** 1/2/3 by the question's level. Wrong: it moves **back** the same number. Both show the correct answer afterwards | | |
| 8 | press **No Answer**, then **Continue** | No Answer is accepted as an explicit choice and counts as **wrong** (PO decision D3). Continue closes the dialog and the turn goes on | | |
| 9 | play until a **surprise square** fires, and watch for a **chain** | The surprise dialog names the outcome. For "Push an opponent back" **you** choose which opponent piece — nothing is auto-selected (FR-40). If the effect lands the piece on another special square, a second dialog follows (FR-42) | | |
| 10 | **Stopwatch the longest movement.** In a **Hard** game, time a Regular 6 with an UltraBoost 6 — a 30-square move — from the moment the piece starts to the moment it stops | **Under 2 seconds** (NFR-04). Record the actual number, not "fine" | | |
| 11 | Take screenshots: main menu, Medium board, Hard board, question dialog, surprise dialog, results screen | Saved as PNG with the date in the filename | | |
| 12 | Finish one complete game | The results screen shows the ranking in finishing order and the game clock | | |

### Row 10 is the one most likely to fail, and there is a new wrinkle

NFR-04 gives every movement 2 seconds. The automated timing tests are **off-screen** — they never paint a real window — and that is exactly the gap that bit us before: Rakan measured **2140 ms** for the 30-square move under parallel load at the old 1600 ms budget. #97 lowers the budget to **1200 ms** and the off-screen figure is now **1278 ms**, but **only a stopwatch on a real window closes this**.

**New, found today:** a **Medium** game can also produce a movement of more than 12 squares. The `EXTRA_ROLL` surprise rolls the **Regular + UltraBoost** dice whatever the level (SRS Table 2, Q27), so a Medium surprise roll can reach the same 30-square bound — measured at 25 over nine Medium games. The animation budget caps by distance, not by level, so it still covers this. But **if you get a long move on Medium after an "Extra roll!", time that too** and note it as Medium.

## 3. What must be true before this is signed off

* **All rows** on **both release computers** (N2-06 asks for two).
* Row 10 needs **#97** in the build, or it measures the old budget.
* **D1 stays OPEN.** If an opponent's Double Block sits on your start square and a Regular 6 will not let you out of the castle, that is the known open decision, **not a bug**. Note it and move on.

## 4. Sign-off

| Computer | Rows 1–4 | Rows 5–9 | Row 10 (actual ms) | Rows 11–12 | Tester | Date |
|---|---|---|---|---|---|---|
| PC 1 | | | | | | |
| PC 2 | | | | | | |

**Do not tick a row that was not run.** The whole point of this task is that the report says only what somebody actually saw.
