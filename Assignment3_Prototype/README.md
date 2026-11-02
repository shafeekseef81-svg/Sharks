# Educational Ludo – Interactive Prototype (Team Sharks)

Interactive prototype of the **Educational Ludo Game** for Assignment 3 of *Software Engineering and Software
Quality Assurance* (University of Haifa). It shows how the game described in our SRS (`Sharks_SRS.pdf`) will look
and how players and the administrator will use it. It is presented in class on **02/11/2026**.

This is a **throwaway prototype**, not the final game. All screens and the main user flows work; the data is
sample data kept in memory, and the game rules are a simplified subset of the SRS rules (see
[What is simulated](#what-is-simulated) and [`PROTOTYPE_TRACEABILITY.md`](PROTOTYPE_TRACEABILITY.md)).

Team: Shafeek Saif, Rakan Ziyan, Mohamed Zereny.

---

## Quick start

| What you need | Java **17 or later**. A JRE is enough to run the JAR; a JDK is needed only to rebuild it. |
|---|---|
| **Run on Windows** | Double-click **`run.bat`**. It starts `EducationalLudo.jar` (and builds it first if it is missing). |
| Run on macOS / Linux | `./run.sh`, or `java -jar EducationalLudo.jar` from this folder. |
| Rebuild | `build.bat` (Windows) or `./build.sh`. |

Manual build, the same commands the scripts use:

```bat
javac --release 17 -encoding UTF-8 -d build\classes src\main\*.java src\model\*.java src\data\*.java src\components\*.java src\view\*.java src\controller\*.java src\devtools\*.java
jar --create --file EducationalLudo.jar --main-class main.Main -C build\classes .
java -jar EducationalLudo.jar
```

If Java is missing, the scripts say so and stop. Nothing is downloaded automatically. Install a JDK 17+ (for example
Eclipse Temurin) and run the script again.

## Offline operation

The prototype runs **completely offline**:

- Only the standard Java library is used (Swing + Java2D). There are no dependencies, no Maven/Gradle and no downloads.
- No network code: the source contains no `java.net`, URL, socket or HTTP usage (checked with a search over `src/`).
- No external fonts or images. Fonts fall back from Segoe UI to standard fonts, and every icon, die, pawn and
  board is drawn with Java2D.
- The application reads and writes **no files**. All data lives in memory for the current session.

## Screens and flows

The seven main flows, all interactive:

| # | Flow | Screens |
|---|------|---------|
| 1 | Start a new game | Main Menu → Game Setup (validation) → Game Board (Easy / Medium / Hard) |
| 2 | Play a turn | Roll Dice (animation, formula) → glowing legal pieces (hover shows the path) → click → piece moves → next player |
| 3 | Educational question | Question dialog (4 answers) → Submit → correct/incorrect + correct answer + movement → Continue |
| 4 | Surprise event | Surprise dialog (reveal, outcome, chance, effect) → Continue → effect on the board |
| 5 | Game results | Podium (1st–3rd), 4th place row, difficulty, duration → Play Again / Main Menu |
| 6 | Game history | Table of completed games, newest first, filter by difficulty |
| 7 | Administrator | Admin Login (generic error, 3-try lockout) → Question Management (add / edit / delete / filter) → Log Out |

Screen list: Main Menu, Game Setup, Game Board, Question dialog, Surprise dialog, Leave-game confirmation, Game
Results, Game History, Administrator Login, Question Management, Add/Edit Question form, Delete confirmation.

Useful keys: **Space** rolls the dice, **A–D / 1–4** choose an answer, **Enter** confirms dialogs and forms,
**Esc** closes confirmations and the question form.

### Prototype demo panel

The game board has a small dashed **PROTOTYPE DEMO** box (bottom right). It lets testers reach the important
features without playing a full match. It uses the same dialogs and screens as normal play:

| Button | What it does |
|---|---|
| Question | Opens a question for the current player. Levels cycle Easy → Medium → Hard; the leading piece moves ±1/2/3. Medium and Hard games only: an Easy board has no question squares (SRS Table 1), so the button is off there and its tooltip says why. |
| Surprise | Triggers a surprise for the current player. The six outcomes come in turn, so each one can be shown. Medium and Hard games only: an Easy board has no surprise squares (SRS Table 1), so the button is off there and its tooltip says why. |
| Next roll: 6 | The next roll of the regular die shows a 6, so a piece can leave its castle right away. |
| Show Results | Ends the game now and opens the results screen. The ranking is simulated from board progress and labelled. |

## Demo administrator account

| Username | Password |
|---|---|
| `admin` | `Sharks2026` |

Authentication is **simulated**: the credentials are compared in memory. There is no PBKDF2 hash and no encrypted
file yet. The lockout is real: after 3 wrong attempts, login is disabled for 30 seconds with a countdown. The demo
account is printed on the login card, so students can continue without asking the team. The final product will not
show it.

## What is simulated

| Area | In this prototype |
|---|---|
| Game rules | A light rule set: a 6 to leave the castle, the movement formula, home path by the regular die, exact finish, extra roll on a 6 (third 6 cancelled), "no legal move" handling, capture of a lone opponent on an unprotected square, skipped turns, finishing order and game end. |
| Double Blocks | **Not modelled** (FR-28–FR-30): pieces may share squares and pass each other. |
| Questions | 36 sample course questions (12 per level) in memory. Each question square has its own level colour (green / yellow / red), as in the SRS. |
| Surprises | Drawn with the SRS Table 2 probabilities. "Push an opponent back" picks the leading opponent piece automatically (in the SRS the player chooses). A piece moved by a question or surprise does not trigger the next special square (FR-42). |
| Game history | 14 sample games plus the games finished in this session. They are kept in memory, not in a CSV file. |
| Question bank | Add/edit/delete changes stay in memory for the session and apply from the next game. They are lost when the program closes. |
| Login | Simulated credential check (see above). Changing the password (FR-55) is not included. |
| Show Results (demo) | Ranks unfinished players by pieces finished, then by board progress, and adds the game to the history. |

## Board fidelity notes

The boards were drawn from the **official diagrams** in `Ludo Project-V2 - 2026.pdf` (page 2). The diagrams were
extracted at full resolution and compared cell by cell with the drawn boards.

- **Easy**: the classic 15×15 board. It has 52 track squares, 6-square home paths (5 coloured squares plus the
  centre), 4 `#` start squares, 4 arrows and 4 protection stars.
- **Medium**: the Easy board plus one outer ring, 17×17 in total (52 + 64 squares). **Hard**: two outer rings,
  19×19 in total (52 + 64 + 72 squares).
- On Medium and Hard, each `#` start square is the outer corner next to its castle. A piece first laps the outer
  ring, then turns inward at its colour's arrow. The arrows of one colour form a "staircase", for example red on
  Hard at rows/columns (7,0) → (8,1) → (9,2). Medium has 8 arrows and Hard has 12. The classic start square stays
  coloured, as in the diagrams, but has no special function there.
- Each board keeps the colour placement of its own diagram. Easy has green at the top-left; Medium and Hard have
  red at the top-left. The clockwise colour order is the same on all boards.
- The Hard diagram shows only one corner. The other three corners mirror it, as stated in SRS assumption 4.
- Protection, question and surprise squares are **placed at random in every game** (FR-12). Their positions
  therefore differ from the examples in the diagrams; their counts match Table 1. They never overlap and never sit
  on start squares, arrows or home paths.
- Visual simplifications: protection squares use a star outline, surprise squares use a gift icon, and
  question-square colours follow the question level. The board scales with the window and stays fully visible at
  1280×720 (NFR-23).

## Where the documents differ, and what we did

1. **Question level.** The task brief says "questions appropriate to the selected difficulty". The SRS (FR-34,
   FR-35, Table 1) and the project description give each question square its own level (Medium: 2 per level; Hard:
   3 per level). The SRS was followed. An Easy game has no question squares (Table 1), so no questions appear on
   Easy, and the demo *Question* button is disabled there. Questions are shown in Medium and Hard games.
2. **Board orientation.** The Easy diagram has green at the top-left; the Medium and Hard diagrams have red there.
   Each board follows its own diagram.
3. **Admin credentials.** FR-52 says the credentials go to the course staff. Because testers get no instructions,
   the prototype shows the demo account on the login card.
4. **36 instead of 30 questions.** This lets "Delete" be demonstrated before the 30-question minimum (FR-60) blocks it.
5. **Assignment scope.** The assignment allows inactive buttons and no game logic. Here every button works, and a
   light rule set makes the board feel real.

## Known limitations

- Double Blocks, chained special squares and the player's choice in "push an opponent back" are not implemented.
- There is no file persistence (history CSV, question file, admin account), no PBKDF2 hashing and no AES encryption.
- Tested on **one Windows 11 computer** only (the SRS asks for two, NFR-10). The program was compiled for Java 17
  (`--release 17`, class version 61) and run on JDK 25; it has not been run on a Java 17 runtime or on macOS.
- **Folder names with Hebrew letters:** on Windows, the Java launcher cannot open a JAR through an absolute path that
  contains characters outside the system code page. `java -jar "C:\...\עבודה 3\EducationalLudo.jar"` fails with
  *Unable to access jarfile*, and double-clicking the JAR fails the same way (confirmed on the team's computer). **Use `run.bat`**: it
  changes to the program folder and starts the JAR by its relative name, which works. Copying the folder to a path
  without Hebrew letters also avoids the problem.

## Verification performed

| Check | Result |
|---|---|
| Build with `build.bat` (`javac --release 17`) | No errors or warnings |
| `java -jar EducationalLudo.jar --selftest` | **121 checks passed, 0 failed.** Covers: board counts and paths for all levels (25 random boards each); dice formula and 10,000-draw distributions; light rules; setup validation; question bank; history; login lockout; every menu button; 2/3/4 players × Easy/Medium/Hard; roll → highlight → click → move; question correct and wrong; all six surprises; results; history filter; admin add/edit/delete/minimum/logout; performance (Hard board 220 ms; history with 1,014 games 60 ms). |
| Launch through `run.bat` | Window "Educational Ludo – Team Sharks" shown 1.3 s after start (NFR-01 ≤ 5 s) |
| Screenshots at 1280×720 and at 1264×641 (a 1280×720 screen minus window borders) | All screens inspected: no clipped text, no overlaps, whole board visible |
| Offline | No network APIs in `src/`; the app reads/writes no files |

**Not verified automatically; check by hand before the presentation:** hover effects and animation smoothness;
playing with the real mouse (clicks were tested through the hit-test code, not real OS mouse events); keyboard
shortcuts; resizing the window; the lockout countdown; double-clicking the JAR from a folder without Hebrew
letters; other computers; macOS.

## Developer tools

```bat
java -jar EducationalLudo.jar --selftest                          :: automatic checks, exit code 0 = all passed
java -jar EducationalLudo.jar --screenshots screenshots           :: renders every screen (1280x720) into screenshots\
java -jar EducationalLudo.jar --submission-pdf prototype_Sharks.pdf screenshots   :: builds the submission PDF
```

`prototype_Sharks.pdf` is the Assignment 3 submission file. It has a cover, the 18 screenshots in flow order with
short captions, and the tools list.

## Project structure

```
src/
  main/        Main.java – starts the app (and the tools above)
  model/       Game data and rules, no GUI code: Difficulty (level settings), BoardFactory/Board/Square/Cell,
               GameState (turns, dice, light rules, ranking), Player, Piece, DiceRoll, TurnPhase, SurpriseOutcome,
               Question/QuestionBank, GameRecord/GameHistory (Singleton), AdminAuthenticator, GameSettings
  data/        DemoData – sample questions, sample history, demo admin account
  controller/  NavigationController (screens, dialogs), PrototypeGameController (game flow), AdminController
  view/        One class per screen/dialog: MainMenuView, GameSetupView, GameBoardView, QuestionDialog,
               SurpriseDialog, GameResultsView, GameHistoryView, AdminLoginView, QuestionManagementView,
               QuestionFormDialog, ConfirmDialog
  components/  Reusable widgets: BoardComponent (Java2D board), DiceComponent, CustomButton, CustomPanel,
               ModalOverlay, StyledTextField, StyledTable, ToggleChip, ColorSwatch, Icons, Theme, Confetti
  devtools/    SelfTest, ScreenshotTool, SubmissionPdf (not used by the game itself)
build.bat / build.sh, run.bat / run.sh, EducationalLudo.jar, screenshots/, prototype_Sharks.pdf
```

Design patterns from NFR-09, in simplified form: **Factory** – `BoardFactory.create(level)`; **Observer** –
`GameState.Listener` refreshes the board screen; **State** – the `TurnPhase` turn states (an enum-based state
machine); **Singleton** – `GameHistory.getInstance()`.

## Tools used and why

| Tool | Why |
|---|---|
| Java 17 (Eclipse Temurin JDK) | Required by the course and the SRS (NFR-07). Compiled with `--release 17`. |
| Java Swing + Java2D | Standard-library GUI and the custom-drawn boards, pieces, dice and icons; works offline with nothing to install. |
| javac, jar, build/run scripts | A single runnable JAR and a double-click start on Windows. |
| Claude Code (Anthropic AI assistant) | Used to analyse the SRS and the project description, plan the prototype, write the Java code, the self-test, the screenshot and PDF tools and this documentation. The team reviews and tests the result. |
| Visual Studio Code | Editing, reviewing and running the project. |
| Python with PyMuPDF and Pillow | Used once to extract the official board diagrams at full resolution for the comparison (not part of the prototype). |
