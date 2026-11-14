# T28 — The release JAR on a second computer

Task **T28** · Owner: Mohamed Zereny · SRS **NFR-01–NFR-03, NFR-07, NFR-10, NFR-11 (P), NFR-24**

The Assignment 4 brief requires the executable to work on **more than one computer**, and states that a
submission that does not compile scores 0. This file is the evidence record for that check.

**Artefact under test:** `iteration1_Sharks.zip`, built from `main` commit `af0886e`.
SHA-256 of the ZIP: `f3344400c608fc71d171eb332362e6feb39036294c2f18720034258e1d2dfade`
`EducationalLudo.jar` inside it: 196,702 bytes, `Main-Class: Controller.Main`, class file major version 61 (Java 17).

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

## Computer 2 — PARTIALLY VERIFIED (11 Nov 2026)

**Status: PARTIALLY VERIFIED — T28 is not complete.**

The release was run on **Rakan Ziyan's separate physical computer**, by Mohamed Zereny. Three checks
passed there. Everything else in the table below was **not** carried out and stays pending.

What was observed on computer 2:

| Observation | Verdict |
|---|---|
| The application opened | **PASS** |
| Rolling the die worked | **PASS** |
| Game pieces moved | **PASS** |

That is the whole of the evidence from computer 2. The operating system, the Java version, the screen
resolution, the Hebrew-path check, the setup screen, the turn change, the game clock and the results
screen were **not** recorded, and nothing about them may be claimed.

This does establish the part the brief cares about most — the submitted executable starts and is
playable on a second, different physical machine — but the check is not finished until the remaining
rows are filled in.

### Steps (for completing the remaining rows)

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
| 2.0 | Computer 2 identity | NFR-10 | different physical machine | **PASS** | Rakan Ziyan's computer; OS and version not recorded |
| 2.1 | `java -version` | NFR-10 | 17 or later | PENDING | not checked; the version reported was not recorded |
| 2.2 | ZIP extracts, one folder `iteration1_Sharks` | — | JAR present inside | PENDING | not recorded |
| 2.3 | Folder path contains Hebrew letters | NFR-07 | path accepted | PENDING | not tested on this computer |
| 2.4 | The application opens | NFR-10 | main menu appears | **PASS** | observed 11 Nov 2026 |
| 2.5 | Main menu readable, no clipped text | NFR-24 | text ≥ 12 pt, nothing cut | PENDING | not inspected; screen resolution not recorded |
| 2.6 | Setup screen: two names + Easy | FR-04–FR-09 | Start becomes enabled | PENDING | not recorded |
| 2.7 | Medium and Hard shown but disabled | FR-05 (P) | "Iteration 2" tag visible | PENDING | not recorded |
| 2.8 | Easy board drawn correctly | FR-10–FR-14 | 4 castles, tracks, protection squares | PENDING | not inspected in detail |
| 2.9 | Rolling the die works | FR-15–FR-17 | the die can be rolled | **PASS** | the roll itself worked; whether the movable pieces were highlighted was not recorded |
| 2.10 | A game piece moves | FR-19, FR-20 | the piece moves on the board | **PASS** | movement worked; square-by-square animation (NFR-04) and one-move-per-roll (FR-20) were not separately checked |
| 2.11 | Turn passes to the other player | FR-15, FR-23 | active player changes | PENDING | not recorded |
| 2.12 | Game clock on the board | FR-46 | counts up, HH:MM:SS | PENDING | not recorded |
| 2.13 | Response to a click | NFR-01–NFR-03 | no noticeable delay | PENDING | not measured |
| 2.14 | Finish a game | FR-43–FR-47 | results screen, ranking, total time | PENDING | the game was not played to the end |
| 2.15 | Leave game confirmation | FR-03 | asks before leaving | PENDING | not recorded |

**Overall T28 verdict: PARTIALLY VERIFIED** — 4 of 16 rows passed on computer 2 (2.0 the machine
itself, plus the three observations 2.4, 2.9 and 2.10); the other 12 rows are pending.
Date of the computer-2 run: 11 Nov 2026 · tester: Mohamed Zereny · machine: Rakan Ziyan's computer.

T28 stays **In progress** in the Scrum Report. It becomes *Completed* only when the pending rows above
are filled in on a second computer.

### If something fails

Record the exact message and the step number, and do not change the submitted ZIP without telling the
team: the Scrum Report, the system report (N05, Shafeek Saif) and N07 all refer to this same build.

---

## Where the result goes

**Now, with T28 partially verified:**

1. **Shafeek Saif** (system report N05, section 6.2): the report may say that the submitted JAR was
   started on a second physical computer and that rolling the die and moving a piece worked there.
   It may **not** yet say the system was fully checked on two computers, because it was not.
2. **Rakan Ziyan** (Scrum Report N03): the T28 row stays **In progress**, with the note that the
   application opened, the die rolled and pieces moved on a second computer on 11 Nov 2026.

**Later, once the pending rows are filled in:**

3. Complete the table above, commit it, and only then does T28 move to *Completed*.
