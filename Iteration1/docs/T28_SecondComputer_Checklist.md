# T28 — The release JAR on a second computer

Task **T28** · Owner: Mohamed Zereny · SRS **NFR-01–NFR-03, NFR-07, NFR-10, NFR-11 (P), NFR-24**

The Assignment 4 brief requires the executable to work on **more than one computer**, and states that a
submission that does not compile scores 0. This file is the evidence record for that check.

**Artefact under test (computer 1):** `iteration1_Sharks.zip`, built from `main` commit `af0886e`.
SHA-256 of the ZIP: `f3344400c608fc71d171eb332362e6feb39036294c2f18720034258e1d2dfade`
`EducationalLudo.jar` inside it: 196,702 bytes, `Main-Class: Controller.Main`, class file major version 61 (Java 17).

**Artefact under test (computer 2, 14 Nov 2026):** `iteration1_Sharks.zip` rebuilt by the N07 procedure
from `main` commit `f3471fa`. Between `af0886e` and `f3471fa` only `Iteration1/docs` changed: `src/`,
`test/`, `lib/` and the scripts are identical, so it is the same program. Its `EducationalLudo.jar`
is 194,338 bytes (built with `--release 17`, class file major version 61, `Main-Class: Controller.Main`);
the size differs from computer 1 only because a newer JDK compiled it.

**Final submission ZIP (computer 2, 14 Nov 2026):** after the run above, four independent Task 07 tests were
added (`LevelConfigIndependentTest`, no product code changed) and the ZIP was rebuilt from the final code.
That final ZIP was checked again on computer 2 from a Hebrew folder: `test.bat` 380/380 and `run.bat`
starts the game.

Use the same ZIP on both computers. If the code changes, rebuild the ZIP and redo the check.

---

## Computer 1 — PASS (verified 11 Nov 2026)

| # | Check | Result |
|---|---|---|
| 1.1 | Environment | Windows 11 Pro 10.0.26200, Temurin OpenJDK 17.0.17+10 |
| 1.2 | `test.bat` from the unzipped copy | **PASS** — 376 found, 376 successful, 0 failed, 0 skipped |
| 1.3 | `build.bat` produces `EducationalLudo.jar` | **PASS** — 196,702 bytes, class major 61 |
| 1.4 | `run.bat` starts the shipped JAR | **PASS** — window "Educational Ludo - Team Sharks", process responding |
| 1.5 | Main menu renders (board, title, buttons) | **PASS** — checked on screen |
| 1.6 | Path containing Hebrew letters | **PASS** — extracted to `...\בדיקה עברית\iteration1_Sharks`, `run.bat` started normally |

`build.bat`, `run.bat` and `test.bat` all `cd /d "%~dp0"` and use relative paths, which is why the
Hebrew path works. Keep that property if the scripts are ever changed (owner: Rakan Ziyan).

---

## Computer 2 — PASS (11 and 14 Nov 2026)

**Status: VERIFIED — T28 is complete.**

Computer 2 is **Rakan Ziyan's separate physical computer** (Windows 11 Home 10.0.26300; computer 1 runs
Windows 11 Pro 10.0.26200). Mohamed Zereny made a first run on 11 Nov 2026; Rakan Ziyan completed the
remaining rows on 14 Nov 2026 with the rebuilt ZIP described above.

What Mohamed Zereny observed on computer 2 on 11 Nov 2026:

| Observation | Verdict |
|---|---|
| The application opened | **PASS** |
| Rolling the die worked | **PASS** |
| Game pieces moved | **PASS** |

The rows that run left open were completed on 14 Nov 2026 (table below), following the steps here.

### Steps

1. Copy `iteration1_Sharks.zip` to the second computer (USB stick, Drive, email to yourself).
   Do **not** rebuild it there — the point is to test the file that will be submitted.
2. Check Java: open a terminal and run `java -version`. It must report **17 or later**.
   If it is missing, install Eclipse Temurin 17 and record which version you installed.
3. Extract the ZIP. You get one folder, `iteration1_Sharks`.
4. **Hebrew path check:** put that folder inside a directory whose name has Hebrew letters,
   for example `C:\בדיקה\iteration1_Sharks`.
5. Double-click `run.bat`. The main menu must appear.
6. Start a 2-player game on **Easy**: New Game → type two names → pick Easy → Start.
7. Play: roll the die, watch a piece move square by square, confirm the turn passes to the other
   player, and confirm the clock on the board counts upwards.
8. If you have time, finish the game and check the results screen shows the ranking and the
   total time as HH:MM:SS.
9. Take 2–3 photos or screenshots as evidence.

### Evidence table — computer 2

| # | Check | SRS | Expected | Result | Note |
|---|---|---|---|---|---|
| 2.0 | Computer 2 identity | NFR-10 | different physical machine | **PASS** | Rakan Ziyan's computer, Windows 11 Home 10.0.26300, screen 1707x1067 (scaled) |
| 2.1 | `java -version` | NFR-10 | 17 or later | **PASS** | Temurin OpenJDK 25.0.1 LTS (14 Nov) |
| 2.2 | ZIP extracts, one folder `iteration1_Sharks` | — | JAR present inside | **PASS** | one top folder, 110 entries, all with forward slashes, `EducationalLudo.jar` present, no `build/` or `.class` (14 Nov) |
| 2.3 | Folder path contains Hebrew letters | NFR-07 | path accepted | **PASS** | extracted to `...\בדיקה עברית\iteration1_Sharks`; `test.bat` there: 376 found, 376 successful, 0 failed, 0 skipped (final ZIP: 380/380); `run.bat` started the game (14 Nov) |
| 2.4 | The application opens | NFR-10 | main menu appears | **PASS** | 11 Nov (Mohamed Zereny) and 14 Nov: window "Educational Ludo - Team Sharks", process responding |
| 2.5 | Main menu readable, no clipped text | NFR-24 | text ≥ 12 pt, nothing cut | **PASS** | title, board picture and the four buttons fully visible; "Soon" tags on Game History and Question Management (14 Nov) |
| 2.6 | Setup screen: two names + Easy | FR-04–FR-09 | Start becomes enabled | **PASS** | 2 players, names "Rakan" and "Tester" (counters 5/12, 6/12), Red and Yellow, Easy; Start Game opened the board (14 Nov) |
| 2.7 | Medium and Hard shown but disabled | FR-05 (P) | "Iteration 2" tag visible | **PASS** | both tagged "Iteration 2"; clicking Medium left Easy selected (14 Nov) |
| 2.8 | Easy board drawn correctly | FR-10–FR-14 | 4 castles, tracks, protection squares | **PASS** | 15 x 15 board, 52 track squares, 4 castles (unused colours shown as "Not playing"), 4 start squares, 4 protection stars, 4 arrows, home paths (14 Nov) |
| 2.9 | Rolling the die works | FR-15–FR-17 | the die can be rolled | **PASS** | 11 Nov, and 14 Nov: values 2, 4 and 6 seen; with a 6 the movable piece glowed and the message said "Click a glowing piece" (FR-24) |
| 2.10 | A game piece moves | FR-19, FR-20 | the piece moves on the board | **PASS** | 11 Nov, and 14 Nov: on a 6 the clicked yellow piece left the castle onto its start square and the same player rolled again ("Roll again!", FR-21) |
| 2.11 | Turn passes to the other player | FR-15, FR-23 | active player changes | **PASS** | "Rakan has no legal move with a 4; the turn passes. Next: Tester." Current-turn card and TURN tag moved to Tester (14 Nov) |
| 2.12 | Game clock on the board | FR-46 | counts up, HH:MM:SS | **PASS** | 00:00:02 → 00:00:17 → 00:01:58 → 00:02:13 during the game (14 Nov) |
| 2.13 | Response to a click | NFR-01–NFR-03 | no noticeable delay | **PASS** | every roll and click was shown at the next screenshot (under about 2 s, including the roll animation); the window stayed responding; no stopwatch measurement (14 Nov) |
| 2.14 | Finish a game | FR-43–FR-47 | results screen, ranking, total time | **PASS** | on computer 2 (his own computer) by Rakan Ziyan on 10 Nov (a full game by hand on the JAR built from `main`, to the results screen; recorded in the Scrum Report), and by the automated full-game tests run on computer 2 on 14 Nov (GameController complete game to the results screen; T26: 120 complete games) |
| 2.15 | Leave game confirmation | FR-03 | asks before leaving | **PASS** | Main Menu during a game showed "Leave this game?" with Keep Playing / Leave Game; Leave Game returned to the menu (14 Nov) |

**Overall T28 verdict: PASS** — 16 of 16 rows passed on computer 2.

**All three team computers:** in addition to the rows above, the team played the game by hand on the
computers of all three members — Rakan Ziyan, Shafeek Saif and Mohamed Zereny (confirmed by Rakan Ziyan,
14 Nov 2026).
Computer-2 runs: 11 Nov 2026 by Mohamed Zereny (2.0, 2.4, 2.9, 2.10) and 14 Nov 2026 by Rakan Ziyan
(all rows) · machine: Rakan Ziyan's computer. Screenshots of the 14 Nov run are kept by Rakan Ziyan.

T28 is **Completed** in the Scrum Report.

### If something fails

Record the exact message and the step number, and do not change the submitted ZIP without telling the
team: the Scrum Report, the system report (N05, Shafeek Saif) and N07 all refer to this same build.

---

## Where the result goes

1. **System report (N05), section 6.2:** the release was checked on two physical computers
   (done 14 Nov 2026).
2. **Scrum Report (N03):** T28 *Completed*, with the evidence above (done 14 Nov 2026).
