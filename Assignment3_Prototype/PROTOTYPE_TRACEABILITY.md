# Prototype Traceability – Educational Ludo (Team Sharks)

How the Assignment 3 prototype relates to the requirements in `Sharks_SRS.pdf`.
A requirement marked **Implemented** works in the prototype as written. Every other mark is a partial,
simulated or missing part, so the table does not overstate what the prototype does.

**Status legend**

| Status | Meaning |
|---|---|
| **Implemented** | Works in the prototype as the SRS describes, for the prototype's sample data. |
| **Partial** | Works, but with the stated simplification. |
| **Simulated** | The screen and the interaction are real, but the data or mechanism behind them is a stand-in (e.g. in memory instead of a file). |
| **Not in prototype** | Planned for the final product only. |
| **To verify** | Can only be checked outside the prototype (e.g. the classroom usability test). |

"Interactive" means the user can operate the feature in the running prototype. "Simulated" behaviour is labelled in
the user interface where testers can meet it (e.g. *PROTOTYPE DEMO* tags).

---

## 1. Screens and features

| Screen / feature | SRS requirements | Status | Interactive or simulated | Limitations |
|---|---|---|---|---|
| Main Menu | FR-01, FR-02, NFR-01 | Implemented | Interactive | None. All four buttons work; Exit asks for confirmation. Main menu visible 1.3 s after start (measured). |
| Navigation and Back | FR-02, FR-03 | Implemented | Interactive | Question Management is left through *Log Out* (ends the session, FR-54). Leaving a running game asks for confirmation and the game is not saved. |
| Game Setup | FR-04 – FR-09, NFR-11 | Implemented | Interactive | Names are unique without regard to case and limited to 12 characters while typing. Colours taken by another player are disabled. |
| Board generation and display (Easy / Medium / Hard) | FR-10 – FR-14, FR-33, NFR-02, NFR-23 | Implemented | Interactive; new random board each game | Geometry follows the official diagrams; the unshown Hard corners mirror the shown one (SRS assumption 4). Special squares are random per game (FR-12), so their positions differ from the diagram examples. |
| Turn indicator, player list, timer | FR-15, FR-46, NFR-24 | Implemented | Interactive | None. |
| Dice and movement value | FR-16, FR-17, FR-21 – FR-23, NFR-03, NFR-13 | Implemented | Interactive | The *Next roll: 6* demo button can fix the next regular die (labelled prototype aid). |
| Piece selection and movement | FR-18 – FR-20, FR-24 – FR-27, FR-31, FR-32, NFR-04 | Implemented (FR-19 partial) | Interactive | Legal pieces glow and hovering previews the path. Moves are animated square by square. The Double-Block part of FR-19 does not apply (no blocks). |
| Double Blocks | FR-28 – FR-30 | Not in prototype | – | Pieces may share squares and pass each other. |
| Educational question | FR-34 – FR-38, NFR-05 | Implemented | Interactive; sample questions | The level comes from the question square's colour. "Only the active player answers" is stated in the dialog; it cannot be enforced on one shared screen. |
| Surprise event | FR-39 – FR-42, NFR-05 | Partial | Interactive; some details simulated | Outcomes use the Table 2 probabilities. FR-40: the opponent piece is chosen automatically (the most advanced one) and the die for it is rolled automatically. FR-41: blocking is not modelled. FR-42: a piece moved onto another special square does not trigger it. |
| Game end and results | FR-43 – FR-47 | Implemented; demo shortcut simulated | Interactive | A real game ends when all but one player have finished. *Show Results* (demo) ends the game early and ranks unfinished players by progress; the screen labels this. |
| Saving completed games | FR-48, FR-63, NFR-06, NFR-12 | Simulated | Simulated (in memory) | A record with the CSV fields in template order is added to an in-memory list. No CSV file is written. Unfinished games are not saved. |
| Game History | FR-49, FR-50, NFR-06 | Implemented (sample data) | Interactive | 14 sample games plus the games of this session; newest first; filter by difficulty. |
| Administrator login | FR-51 – FR-54, NFR-15 – NFR-17, FR-65 | Partial | Interactive; authentication simulated | General error and 3-try / 30 s lockout work. Credentials are compared in memory (no salt, no PBKDF2, FR-65 / NFR-16). The demo account is shown on the login card for testers. |
| Change administrator password | FR-55 | Not in prototype | – | Optional (priority 1). |
| Question Management and form | FR-56 – FR-62, FR-64 | Implemented (in memory) | Interactive | 36 sample questions. Add/edit with validation; delete with confirmation and the 30-question minimum; filter. Changes apply from the next game but are not written to a file (FR-61); no AES (NFR-18). |
| Prototype demo panel | supports FR-34, FR-39, FR-44 – FR-47 | Prototype only | Simulated shortcuts | Small dashed box, labelled *PROTOTYPE DEMO*, enabled only while the game waits for a roll. *Question* and *Surprise* are off on Easy (no question or surprise squares, Table 1). Uses the normal dialogs and screens. |
| Architecture and code | NFR-07 – NFR-09, NFR-19, NFR-20 | Partial | – | See section 2. |
| Delivery and portability | NFR-10, NFR-21, NFR-22 | Partial | – | Runnable JAR; tested on one Windows 11 computer. |

## 2. Requirement by requirement

### Functional requirements

| ID | Requirement (short) | Status | Notes / evidence |
|---|---|---|---|
| FR-01 | Main menu: New Game, Game History, Question Management, Exit | Implemented | Self-test: every button |
| FR-02 | Options open screens; Question Management needs login first | Implemented | Self-test |
| FR-03 | Back on every screen; leaving a game needs confirmation, not saved | Implemented | Self-test |
| FR-04 | 2, 3 or 4 players | Implemented | Self-test: 2/3/4 × all levels |
| FR-05 | Easy, Medium, Hard | Implemented | |
| FR-06 | Unique name 1–12 characters | Implemented | Self-test: empty, 13 chars, duplicate |
| FR-07 | Unique colour | Implemented | |
| FR-08 | Block invalid setup and show the field | Implemented | Messages under each field |
| FR-09 | Settings fixed after start | Implemented | Settings object is immutable |
| FR-10 | Board per Table 1 and official diagrams | Implemented | See README "Board fidelity notes" |
| FR-11 | Special square counts per level | Implemented | Self-test on 25 random boards per level |
| FR-12 | Random, non-overlapping placement on shared tracks | Implemented | Self-test |
| FR-13 | Four pieces in each castle | Implemented | |
| FR-14 | Distinct symbols for square types | Implemented | Plus an on-screen legend |
| FR-15 | Setup turn order; only the active player rolls | Implemented | |
| FR-16 | Uniform Regular / Boost / UltraBoost dice | Implemented | Self-test: 10,000 draws each |
| FR-17 | Regular, or Regular × (special − 1); 0 moves nothing | Implemented | Formula shown on screen |
| FR-18 | Home path moves by the regular die | Implemented | Self-test |
| FR-19 | A regular 6 brings a piece out, ignoring the special die | Partial | Double-Block exception not modelled |
| FR-20 | Exactly one piece per roll; a legal move cannot be skipped | Implemented | The player must click a highlighted piece |
| FR-21 | Extra roll on a 6; none for capture or finish | Implemented | Self-test |
| FR-22 | Third 6 in a row cancelled | Implemented | Self-test |
| FR-23 | No legal move: message and pass | Implemented | |
| FR-24 | Highlight movable pieces | Implemented | Glow plus hover path preview |
| FR-25 | Clockwise; only own arrows | Implemented | Each colour follows its own path |
| FR-26 | Capture a lone opponent on an unprotected square | Implemented | Self-test |
| FR-27 | Protected squares prevent capture | Implemented | Protection, start, surprise and finish squares |
| FR-28 | Double Block | Not in prototype | |
| FR-29 | Double Block stops opponents | Not in prototype | |
| FR-30 | Double Block persistence | Not in prototype | |
| FR-31 | Exact finish | Implemented | Self-test |
| FR-32 | Backward movement stops at the start square | Implemented | Self-test |
| FR-33 | Home path: no special squares, own colour only | Implemented | By construction |
| FR-34 | Question of the matching level with four options | Implemented | 36 sample questions |
| FR-35 | ±1 / 2 / 3 steps by level | Implemented | Self-test: correct and wrong |
| FR-36 | Show correctness and the correct answer | Implemented | |
| FR-37 | No repeat until the level's questions are used | Implemented | Self-test |
| FR-38 | Capture happens before the question | Implemented | Capture is part of the move |
| FR-39 | One surprise outcome per Table 2 | Implemented | Self-test: odds within 2 points |
| FR-40 | Push an opponent back ⌈R/2⌉ (player chooses) | Partial | Opponent chosen automatically |
| FR-41 | Question/surprise moves obey the rules | Partial | Blocking not modelled |
| FR-42 | Chained special squares | Not in prototype | |
| FR-43 | A player finishes with four pieces and stops playing | Implemented | Self-test: no extra roll after finishing; skipped in turn order |
| FR-44 | Game ends when all but one have finished | Implemented | |
| FR-45 | Ranking by finishing order | Implemented | Demo shortcut: ranking simulated (labelled) |
| FR-46 | Elapsed time HH:MM:SS | Implemented | |
| FR-47 | Results: rankings, level, duration | Implemented | |
| FR-48 | Save completed games as CSV | Simulated | In memory only |
| FR-49 | History, newest first | Implemented | Sample data |
| FR-50 | History filter by level | Implemented | |
| FR-51 | Question Management needs a login | Implemented | Simulated credential check |
| FR-52 | One predefined administrator account | Simulated | `admin` / `Sharks2026`, shown on the login card |
| FR-53 | General error without revealing which field was wrong | Implemented | Self-test |
| FR-54 | Leaving ends the admin session | Implemented | Self-test |
| FR-55 | Change password (optional) | Not in prototype | |
| FR-56 | At least 30 questions on three levels | Implemented | 36 (12 per level), in memory |
| FR-57 | List with text, level, correct answer | Implemented | Also shows the ID |
| FR-58 | Add with validation | Implemented | Self-test |
| FR-59 | Edit with the same validation | Implemented | Self-test |
| FR-60 | Delete after confirmation; keep 30 | Implemented | Self-test |
| FR-61 | Save immediately; apply from the next game | Partial | Applies from the next game; not written to a file |
| FR-62 | Question filter by level | Implemented | |
| FR-63 | Append one record in CSV field order | Simulated | In memory; empty places stay blank |
| FR-64 | Question: ID, text, four answers, correct answer, level | Implemented | In memory |
| FR-65 | Store only username, salt and hash | Not in prototype | |

### Non-functional requirements

| ID | Requirement (short) | Status | Notes / evidence |
|---|---|---|---|
| NFR-01 | Main menu within 5 s | Implemented | Measured 1.3 s from launch to visible window |
| NFR-02 | Board within 2 s | Implemented | Self-test: Hard, 4 players, generated and drawn in 220 ms |
| NFR-03 | Dice result within 1 s, incl. animation | Implemented | 0.62 s animation by design (not timed by a test) |
| NFR-04 | Square-by-square animation within 2 s | Implemented | ≤ 1.6 s by design (not timed by a test) |
| NFR-05 | Question/surprise screen within 1 s after the piece stops | Implemented | Opens 0.25 s after the piece stops, by design |
| NFR-06 | Save ≤ 1 s; history ≤ 2 s for 1,000 games | Partial | History with 1,014 games: 60 ms (self-test). Saving is in memory only |
| NFR-07 | Java 17 and Swing | Implemented | `--release 17`, class version 61, standard library only |
| NFR-08 | MVC: rules in the model, none in views | Partial | The model has no GUI code (verified). The question dialog compares the chosen answer itself |
| NFR-09 | Factory Method, Observer, State, Singleton | Partial | Simplified: `BoardFactory`, `GameState.Listener`, `TurnPhase` enum, `GameHistory` |
| NFR-10 | Runnable JAR, tested on two computers | Partial | JAR delivered; tested on one computer |
| NFR-11 | No crash on invalid input; explanatory messages | Implemented | For the prototype flows; self-test ran without exceptions |
| NFR-12 | Recreate a missing history file; skip damaged records | Not in prototype | No files in the prototype |
| NFR-13 | 10,000 draws within 2 percentage points | Implemented | Self-test |
| NFR-14 | Unit tests for every movement and special-square rule | Partial | Built-in self-test (121 checks), not a JUnit suite |
| NFR-15 | Question Management only after login | Implemented | Simulated authentication |
| NFR-16 | Password policy, salted PBKDF2 | Partial | Demo password meets the policy; it is not hashed |
| NFR-17 | 3 failures → 30 s lockout | Implemented | Self-test; countdown on screen |
| NFR-18 | AES-encrypted question file | Not in prototype | |
| NFR-19 | Level settings and surprise odds in one configuration class | Partial | Two enums: `Difficulty` and `SurpriseOutcome` |
| NFR-20 | Separate MVC packages; Javadoc | Partial | Separate packages; Javadoc on public classes and most public methods |
| NFR-21 | Same JAR on Windows 10/11 and macOS 13+ | Partial | Windows 11 tested; macOS not tested |
| NFR-22 | Data files relative to the program folder | Not in prototype | No data files; the scripts use relative paths |
| NFR-23 | Whole board visible at 1280×720; scales | Implemented | Screenshots checked at 1280×720 and 1264×641 |
| NFR-24 | Text ≥ 12 pt; colour also shown by name | Implemented | Smallest UI text 12 pt; names on castles, turn card and player list |
| NFR-25 | 3 of 4 first-time users start a game within 60 s | To verify | Classroom usability test, 02/11/2026 |

## 3. Decisions where the documents differ

| Topic | Documents | Decision |
|---|---|---|
| Question level | Task brief: "appropriate to the selected difficulty". SRS FR-34/FR-35 and Table 1: question squares of every level (Medium 2 each, Hard 3 each). | SRS followed: the square's colour sets the question level. Easy games have no question squares (Table 1), so no questions appear there and the demo *Question* button is disabled on Easy. |
| Board orientation | Easy diagram: green top-left. Medium/Hard diagrams: red top-left. | Each board follows its own diagram; the clockwise colour order is the same. |
| Admin credentials | FR-52: delivered to the course staff. | Shown on the login card, because testers get no instructions. To be removed in the final product. |
| Number of sample questions | FR-56: at least 30. | 36, so Delete can be shown before the FR-60 minimum blocks it. |
| Prototype scope | Assignment 3: buttons outside the flows may be inactive; no game logic required. | All buttons work, and a light rule set makes play realistic. Simplifications are listed above. |
