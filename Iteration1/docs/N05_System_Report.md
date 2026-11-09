# System Report – Iteration 1 (N05)

**Educational Ludo Game – Team Sharks** (Shafeek Saif, Rakan Ziyan, Mohamed Zereny)
University of Haifa, Software Engineering and Software Quality Assurance, Assignment 4

**DRAFT of 09/11/2026.** It describes the repository on that date. Update the status tables as pull requests merge,
and remove this notice before submission. The structure must still be checked against the Assignment 4 brief.

Sources: SRS (baseline), SDP v1.1, Ludo Project-V2 description, staff answers (`ANSWERS.xlsx`), the `iteration-1`
branch and the pull requests in the repository.

## 1. Goal and scope of the iteration

SDP 5.2.1 sets the goal: *a playable basic Easy game, from Main Menu to results* (deliverable D2, stories US-1.1 to
US-1.5). Known limits until Iteration 2: no Double Blocks, Medium/Hard boards or special dice.

Status words in this report:

| Word | Meaning |
|---|---|
| **Merged** | In `iteration-1`, with passing tests |
| **In review** | In an open pull request, not part of the delivered code yet |
| **Not started** | No pull request exists |
| **Planned** | Assigned to a later iteration by the SDP |

## 2. Status of the requirements

| SRS IDs | Requirement | Task | PR | Status |
|---|---|---|---|---|
| NFR-08 (P), NFR-20 (P) | MVC skeleton, shared contracts | T27 | #1 | Merged |
| FR-04, FR-06..FR-09 | Setup validation, immutable settings | T04 | #2 | Merged |
| NFR-19, FR-10 (P), FR-11 (P) | `LevelConfig`, Easy board | T07 | #6 | Merged |
| FR-12 (P), FR-13 | Random protection squares, castles | T08 | #4 | Merged |
| FR-11, FR-12 | Board uses `ProtectionSquarePlacer` | T07 | #12 | Merged |
| FR-01, FR-03 | Menu and navigation | T01 | #15 | Merged |
| FR-01, FR-02 (P), FR-03 | Screen routing tests | T02 | #17 | In review |
| FR-15, FR-16 (P), FR-17 (P) | Turn order, Regular die | T11 | #3 | Merged |
| FR-19 (P), FR-20, FR-25 (P) | Move engine | T16 | #11 | In review |
| FR-18, FR-31, FR-33 | Home path, exact finish | T18 | #7 | In review |
| FR-26 (P), FR-27 (P) | Capture, protected squares | T17 | #9 | In review (2 approvals; waits for merge order) |
| FR-21..FR-23 | Rule of 6, third 6, no-move pass | T12 | #5 | In review |
| FR-43..FR-45 | Finish detection, ranking | T23 | #10 | In review |
| FR-24, FR-47, NFR-08 | `GameController` integration | N02 | #14 | Draft |
| FR-46 | Game timer | T24 | – | Not started (placeholder class) |
| FR-05 (P) | Setup screen, Easy playable only | T03 | – | Not started |
| FR-14 (P), NFR-23 | Board drawing and scaling | T09 | – | Not started (screen reused from the prototype) |
| NFR-04 | Square-by-square animation | T19 | – | Not started as its own task (part of #14) |
| NFR-01..NFR-03, NFR-07, NFR-10, NFR-11 (P), NFR-24 | JAR on two computers, timing and input checks | T28 | – | Not started |
| FR-48, FR-63 | CSV history | – | – | Planned for Iteration 3 by the SDP; **see section 7** |

**The application cannot yet play a game** from `iteration-1`. The rule classes in review replace placeholders, and
`GameController` (N02) and `GameClock` (T24) are needed before a game can start in the real application.

## 3. Architecture

- **MVC (NFR-08).** Packages `Model`, `View`, `Controller`. `MvcDependencyTest` checks that `Model` has no Swing,
  View or Controller imports.
- **Frozen contracts.** `MoveEngine`, `SquareRules`, `HomePathRules`, `TurnManager`, `RankingTracker`, `Dice`,
  `GameModel`, `Move` and `GameListener`/`GameEvent` were frozen at checkpoint CP1. The in-review PRs did not change them.
- **Move flow.** `GameModel` → `TurnManager` → `Dice`, `MoveEngine` → `SquareRules`, `HomePathRules`, `Board`.
  `GameModel` notifies views through `GameEvent`.
- **Design patterns (NFR-09).** Used now: Factory Method (`BoardFactory`) and Observer (`GameListener`).
  Planned: State (Iteration 2) and Singleton (CSV history, Iteration 3).
- **Position model.** A piece is an index along its colour's path: −1 castle, 0 start square, 1–50 shared track,
  51–55 home path, 56 finish.

## 4. Team decisions on the rules

Recorded in GitHub issue #13.

| Topic | Decision | Basis |
|---|---|---|
| A 6 that captures | Keeps the one extra roll of the 6; the capture adds none | FR-21, Q57, Q38. No staff answer names this exact case |
| Several pieces on a plain square | Only one lone opponent is captured. With no Double Blocks in Iteration 1, a piece can land beside two or more opponents | FR-26, Q37; SDP limit on Double Blocks |
| Easy only | Only Easy is playable in Iteration 1 | SDP FR-05 (P), deliverable D2 |

## 5. Verification

Results of local runs on 09/11/2026 with Java 23 and `--release 17`, unless marked otherwise.

| Run | Tests | Passed | Failed | Skipped |
|---|---|---|---|---|
| `iteration-1` at 8af164c plus T05, T06, T10 | 100 | 100 | 0 | 0 |
| PR #11 (T16) at 88166f7 plus T20 | 114 | 114 | 0 | 0 |
| PR #5 (T12) at 683ab67 plus T14 | 48 | 48 | 0 | 0 |
| Local merge of `iteration-1` (8af164c), #11, #7, #9, #5, #10 plus all new tests | 231 | 228 | 1 | 2 |

`test.bat` compiles only `test\Model` and `test\Architecture` until PR #16 merges, so these counts do not include the
tests in `test\View`.

- The one failure is `GameModelSkeletonTest` in #5, which expects a placeholder that #11 replaces. It must be removed
  before #5 merges.
- The two skipped tests are the duration checks, disabled until T24 exists.
- **The local merge is not the delivered code.** These results apply to `iteration-1` only after the PRs merge.

**Independent and acceptance tests by the PO** (draft PRs): T05 10, T06 4, T10 5, T14 7, T15 8, T20 14, T22 13,
T26 6 tests. The T26 run plays 120 complete games with real dice and all the rule classes. T20 and T10 take the
expected squares from a table written by hand from the official diagram. T20 and T14 were checked by planting faults
in a local copy of the code: T20 detected 18 of 19 and T14 7 of 8, and the undetected faults did not change behaviour.

**Reviews.** Deep review needs two reviewers (SDP 5.3.4).

| PR | Reviews on 09/11/2026 |
|---|---|
| #9 T17 | Rakan, Mohamed: approved |
| #7 T18 | Rakan approved; Mohamed outstanding |
| #11 T16 | Mohamed approved; Shafeek reviewed, no blocking defect, approval pending |
| #5 T12 | Shafeek commented, not approved; Mohamed outstanding |
| #10 T23 | None submitted yet |

**Not performed at the date of this draft – no result may be claimed:**

- Manual acceptance on the screen: FR-01..FR-03, field marking (FR-08), FR-14, FR-24, FR-47, NFR-04, NFR-23.
- Any full game in the real Swing application by the PO.
- Timing and input checks (NFR-01..NFR-03), and the JAR on a second computer or a Hebrew path (T28).
- The demonstration rehearsal (T29).
- The final test run on `iteration-1` after integration.

## 6. Known limitations in Iteration 1

- Only Easy is playable. Until T03, the setup screen still offers Medium and Hard, which fail at the first roll.
- No Double Blocks (FR-28..FR-30). `isPassable` always returns true, but each cell of a move already goes through it.
- No question or surprise squares, no saved history and no administrator functions (Iterations 2 and 3).
- A name made only of non-breaking spaces (U+00A0) is accepted (found by T05; low severity).

## 7. Open conflicts and questions for the course staff

| # | Topic | Conflict | State |
|---|---|---|---|
| 1 | CSV history | Rakan reports that the Assignment 4 brief says all teams must use the history CSV file. SDP 5.2.1 schedules FR-48/FR-63 for Iteration 3, and they are not implemented. The team must still read the brief itself | **Open. Clarification needed; the requirement has not been removed** |
| 2 | Q40, Double Block on a start square | The second sheet of `ANSWERS.xlsx` has both "does not block the castle exit" and "blocks it"; FR-19 follows "blocks" | Open; affects Iteration 2 |
| 3 | Two sheets in `ANSWERS.xlsx` | 52 of 105 answers differ in wording, and some in substance (Q7, Q56, Q94). Which is current is not stated | Open; no Iteration 1 rule depends on a difference found so far |

## 8. Remaining work before submission (15/11/2026)

1. Merge in the order of `CONTRIBUTING.md`: T16, T18, T17, T12, T23, then T24 and N02.
2. Finish T03, T24 and T28, and add the required reviews.
3. Run `test.bat` on the integrated `iteration-1` and update section 5.
4. Perform and record the manual acceptance and the JAR check.
5. Resolve conflict 1 with the staff.
6. Check this report against the Assignment 4 brief.
