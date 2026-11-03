# Educational Ludo – Iteration 1 (Team Sharks)

Iteration 1 of the Educational Ludo Game, for Assignment 4 of *Software Engineering and Software Quality
Assurance* at the University of Haifa. The goal of this iteration is a basic Easy game that can be played
from the Main Menu to the results (SDP 5.2.1, US-1.1 to US-1.5).

**Current state: Phase A (foundation) only.** The MVC structure, the shared interfaces, the build and the
tests are in place. The game rules are **not implemented yet**. Rolling and moving show a
"Phase B" message, and the placeholder classes throw `UnsupportedOperationException` with the task number
that implements them.

## Build, test, run

| | Windows | macOS / Linux / Git Bash |
|---|---|---|
| Build `EducationalLudo.jar` | `build.bat` | `./build.sh` |
| Build + run all JUnit tests | `test.bat` | `./test.sh` |
| Start the game | `run.bat` | `./run.sh` |

- Requires a JDK 17 or later. The code is compiled with `--release 17` and needs no internet connection.
- **Folders with Hebrew letters:** on Windows, `java -jar "C:\...\עבודה 4\...\EducationalLudo.jar"` fails
  (*Unable to access jarfile*). The scripts change to their own folder and use relative paths, so they work.
  Always start the game with `run.bat`.
- `EducationalLudo.jar` and `build/` are not stored in Git. Build them with `build.bat`.

## Tests

JUnit 5 runs through `lib/junit-platform-console-standalone-1.10.2.jar`, taken from Maven Central with
SHA-1 `552303cce67dc3485ded393d744c0f6b5cae355b`. This JAR is used only for the tests and is not packed
into the game JAR. Tests live in `test/`, in the same packages as the code they test.

Each test's name carries the SRS ID it verifies. Rule tests are written by a member who did not implement
the rule (SDP 5.3.4, Definition of Done).

## Packages and ownership

The package names follow the assignment: `Model`, `View`, `Controller`.

| Package | Content | Owner |
|---|---|---|
| `Model` | Game data and rules, no GUI code (NFR-08) | By file, see below |
| `View`, `View.components` | Swing screens and widgets, no game rules | Mohamed Zereny |
| `Controller` | `Main`, `NavigationController`, `GameController` | Rakan Ziyan |

| `Model` file(s) | Owner | Task |
|---|---|---|
| `GameModel`, `GameEvent`, `GameListener`, `Move` (shared contracts) | Rakan | T27 |
| `TurnManager`, `StandardTurnManager`, `Dice`, `RandomDice` | Rakan | T11, T12 |
| `MoveEngine`, `StandardMoveEngine` | Rakan | T16 |
| `GameSettings`, `Piece`, `Player` | Rakan | T04 |
| `ProtectionSquarePlacer` (to be created) | Rakan | T08 |
| `Difficulty` / `LevelConfig`, `Board`, `BoardFactory`, `Square`, `SquareType`, `Cell`, `Direction` | Shafeek | T07 |
| `SquareRules`, `StandardSquareRules` | Shafeek | T17 |
| `HomePathRules`, `StandardHomePathRules` | Shafeek | T18 |
| `RankingTracker`, `StandardRankingTracker` | Shafeek | T23 |
| `GameClock` | Mohamed | T24 |

The interfaces (`Dice`, `MoveEngine`, `SquareRules`, `HomePathRules`, `RankingTracker`, `TurnManager`,
`GameListener`, `GameEvent`, `Move`, `GameModel`) are the **shared contracts**. They are frozen at
checkpoint CP1. Any change to them needs a change request and the agreement of all three members.

### How the parts talk to each other

```
View --(Actions: roll, select piece, leave)--> GameController --> GameModel (facade)
GameModel --> TurnManager --> Dice, MoveEngine --> SquareRules, HomePathRules, Board
                         \--> RankingTracker
GameModel --(GameEvent, Observer)--> View refreshes;  GameController runs the animations
```

## What came from the Assignment 3 prototype

Reused after review, with only the package changed:
- `Board`, `BoardFactory`, `Square`, `SquareType`, `Cell`, `Direction`, `Difficulty`, `PlayerColor`
- `DiceRoll`, `DieType`, `MoveResult`, `TurnPhase`, `GameSettings`
- all `View.components`
- the screens `MainMenuView`, `GameSetupView`, `GameBoardView`, `GameResultsView`, `ConfirmDialog`, `ScreenView`

Changes made in Phase A:
- `Piece.setProgress` is now package-private.
- The demo-only ranking `Player.totalProgress` was removed.
- The prototype demo panel was removed.
- The results screen no longer refers to the history record (that is Iteration 3).
- `GameState` and `PrototypeGameController` were **not** reused; they are replaced by the new contracts (SDP 6.1).

Not part of Iteration 1, and kept out of this tree:
- Questions, surprises and their dialogs (Iteration 2).
- History, administrator and question management (Iteration 3).
- `DemoData` and the prototype `devtools`.

The prototype self-test does **not** count as verification of this product.
