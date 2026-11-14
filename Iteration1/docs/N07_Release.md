# N07 — Iteration 1 release and submission ZIP

Task **N07** · Owner: Mohamed Zereny · SRS **NFR-10**

## Release notes — Iteration 1

**Release:** Iteration 1, built from `main` commit `af0886e` (11 Nov 2026).
**Tests:** 376 found, 376 successful, 0 failed, 0 skipped (37 containers).
**Build:** Temurin OpenJDK 17.0.17, `--release 17`, class file major version 61.

### What a player can do in this release

- Main menu, with Game History and Question Management shown but marked as not yet available (I3).
- Set up a game for 2–4 players with name validation; Easy is playable, Medium and Hard are shown
  but disabled with an "Iteration 2" tag.
- Play a full game on the Easy board: roll the Regular die, see the movable pieces highlighted,
  move a piece square by square, capture, pass protected squares, enter the home path and finish
  on an exact count.
- The Rule of Six: a 6 grants another roll; the third 6 in a row is cancelled.
- Game end: a player finishes when all four pieces are home, the game ends when all but one have
  finished, and the results screen shows the ranking and the total time as HH:MM:SS.

### Not in this release (planned)

Iteration 2: questions, surprise squares, Double Block, Boost/UltraBoost dice, Medium and Hard boards.
Iteration 3: game history CSV, admin login, question management.

## How the submission ZIP is built

The JAR is deliberately **not** versioned (see `Iteration1/.gitignore`), so the ZIP is produced at
release time from a clean export of `main`. Rebuild it after any later merge.

```bash
# 1. clean export of the project from main, as the folder the brief asks for
git archive --format=tar origin/main Iteration1 | tar -x -C <stage>
mv <stage>/Iteration1 <stage>/iteration1_Sharks

# 2. build the JAR inside that folder, then drop the intermediate classes
cd <stage>/iteration1_Sharks && ./build.bat && rm -rf build

# 3. zip the folder (forward slashes, so it also extracts on Linux and macOS)
python -c "import os,zipfile; ..."   # see the loop below
```

The ZIP is written with Python's `zipfile` rather than Windows Explorer, because
`ZipFile.CreateFromDirectory` on Windows writes **backslash** path separators, which is outside the
ZIP specification and can extract wrongly on Linux and macOS.

### What the ZIP contains

`iteration1_Sharks/` — 108 files, the project exactly as on `main` plus the runnable JAR:

| Entry | Purpose |
|---|---|
| `EducationalLudo.jar` | the runnable game, 196,702 bytes, `Main-Class: Controller.Main` |
| `src/` | 58 Java files in `Model`, `View`, `View/components`, `Controller` |
| `test/` | 35 JUnit 5 test files |
| `lib/` | `junit-platform-console-standalone-1.10.2.jar`, so the tests run offline |
| `build.bat` / `build.sh`, `run.bat` / `run.sh`, `test.bat` / `test.sh` | build, run and test |
| `README.md`, `docs/` | how to run, the class diagram source and these notes |

No `build/` directory and no `.class` files are included.

### Verification done on this build (11 Nov 2026)

| Check | Result |
|---|---|
| ZIP extracts to a single folder `iteration1_Sharks` | PASS |
| All entries use forward slashes | PASS — 108/108 |
| `test.bat` from the unzipped copy | PASS — 376/376, 0 skipped |
| `build.bat` from the unzipped copy | PASS — JAR rebuilt, 196,702 bytes |
| `run.bat` starts the shipped JAR | PASS — window "Educational Ludo - Team Sharks" |
| Path containing Hebrew letters | PASS |
| Second physical computer | **PENDING** — see `T28_SecondComputer_Checklist.md` |

## The five files to submit

| File | Where it comes from |
|---|---|
| `iteration1_Sharks.zip` | built as above (not versioned) |
| `iteration1_scrumReport_Sharks.xlsx` | repository root (N03, Rakan Ziyan) |
| `iteration1_report_Sharks.pdf` / `.docx` | repository root (N05, Shafeek Saif) |
| `classDiagram_Sharks.pdf` | repository root (N04, Mohamed Zereny) |
| `questions_Sharks.xlsx` | repository root (N06, prepared by Rakan Ziyan) |

Deadline: **15 November 2026, 23:59**.
