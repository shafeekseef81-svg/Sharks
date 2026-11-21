# N2-06: Iteration 2 release and submission ZIP

Task **N2-06** (#72) · Owner: Rakan Ziyan · second-computer run: Mohamed Zereny · SRS **NFR-10**

Assignment 5 §1: the ZIP holds the source and a run file. **A run file that does not start with a double-click
disqualifies the whole assignment.** The release is therefore checked twice: at checkpoint CP1 (25/11) and
before the submission (06/12), each time on two computers, including a path with Hebrew letters (retrospective
action 4).

## How the ZIP is built

From the repository root, in Git Bash:

```bash
git fetch origin
python tools/make_iteration2_zip.py --ref origin/iteration-2 --test
```

The script (`tools/make_iteration2_zip.py`, outside `Iteration2/` so it is not shipped) follows the Iteration 1
procedure (`Iteration1/docs/N07_Release.md`):
1. `git archive` of `Iteration2/` from the given ref, renamed to `iteration2_Sharks/`, so only versioned files go in;
2. `build.sh` builds `EducationalLudo.jar` with `--release 17`; `build/` is removed afterwards;
3. checks the JAR: `Main-Class: Controller.Main`, and every class file is version 61 (Java 17);
4. `--test` runs `test.sh` in the staged copy and stops if a test fails;
5. writes `iteration2_Sharks.zip` with Python `zipfile` (forward slashes, one top folder) and prints the file count
   and the SHA-256.

## Checklist (fill in for each release)

| # | Check | Dry run 16/11 (`iteration-2` 38e2e1d, before Iteration 2 code) | CP1 25/11 | Final 05–06/12 |
|---|---|---|---|---|
| 1 | Script finishes; ZIP has one folder `iteration2_Sharks/` with forward slashes | PASS, 106 files | | |
| 2 | JAR: Main-Class `Controller.Main`, Java 17 class files | PASS, 194,338 bytes | | |
| 3 | `test.sh` in the staged copy: 0 failed | PASS, 380/380 | | |
| 4 | Unzipped into a path with Hebrew letters, `java -jar EducationalLudo.jar` starts without errors | PASS (computer 1) | | |
| 5 | **Double-click** `run.bat` (Windows) opens the game window | | | |
| 6 | Double-click `EducationalLudo.jar` opens the game window | | | |
| 7 | Easy, Medium and Hard each start from the setup screen and accept a roll | n/a (Medium/Hard not in setup yet, T2-01) | | |
| 8 | A full game reaches the results screen | | | |
| 9 | Checks 1–8 on a **second computer** (Mohamed) | | | |
| 10 | ZIP SHA-256 recorded | `4155440d…` (dry run only) | | |

Rebuild the ZIP after every merge into `iteration-2`. The ZIP and the JAR are not versioned.
