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

## Computer 2 — PENDING (must be done by hand, by Mohamed Zereny)

Do this on a **different physical computer**. Nothing below can be done from the development machine.

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

### Evidence template — fill this in

| # | Check | SRS | Expected | Result (PASS / FAIL) | Note |
|---|---|---|---|---|---|
| 2.0 | Computer 2 identity | NFR-10 | different physical machine | | OS and version: |
| 2.1 | `java -version` | NFR-10 | 17 or later | | version reported: |
| 2.2 | ZIP extracts, one folder `iteration1_Sharks` | — | JAR present inside | | |
| 2.3 | Folder path contains Hebrew letters | NFR-07 | path accepted | | path used: |
| 2.4 | `run.bat` starts the game | NFR-10 | main menu appears | | start-up time: |
| 2.5 | Main menu readable, no clipped text | NFR-24 | text ≥ 12 pt, nothing cut | | screen resolution: |
| 2.6 | Setup screen: two names + Easy | FR-04–FR-09 | Start becomes enabled | | |
| 2.7 | Medium and Hard shown but disabled | FR-05 (P) | "Iteration 2" tag visible | | |
| 2.8 | Easy board drawn correctly | FR-10–FR-14 | 4 castles, tracks, protection squares | | |
| 2.9 | Roll the die | FR-15–FR-17 | value 1–6, movable pieces highlighted | | |
| 2.10 | Move a piece | FR-19, FR-20, FR-24, NFR-04 | moves square by square, one move per roll | | |
| 2.11 | Turn passes to the other player | FR-15, FR-23 | active player changes | | |
| 2.12 | Game clock on the board | FR-46 | counts up, HH:MM:SS | | |
| 2.13 | Response to a click | NFR-01–NFR-03 | no noticeable delay | | |
| 2.14 | Finish a game (optional) | FR-43–FR-47 | results screen, ranking, total time | | |
| 2.15 | Leave game confirmation | FR-03 | asks before leaving | | |

**Overall T28 verdict:** ☐ PASS ☐ FAIL — date: ____________ tester: Mohamed Zereny

### If something fails

Record the exact message and the step number, and do not change the submitted ZIP without telling the
team: the Scrum Report, the system report (N05, Shafeek Saif) and N07 all refer to this same build.

---

## Where the result goes once computer 2 is done

1. Fill in the table above and commit it.
2. Tell **Shafeek Saif** — the system report (N05) has the testing section that should state the JAR was
   checked on two computers.
3. Tell **Rakan Ziyan** — the Scrum Report row for T28 moves from *In progress* to *Completed*.
