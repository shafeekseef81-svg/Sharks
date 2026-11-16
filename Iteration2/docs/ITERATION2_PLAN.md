# Iteration 2 plan (Assignment 5)

Team Sharks. Prepared 16/11/2026 by Rakan Ziyan for the Iteration 2 planning (task N2-01, [#67](https://github.com/shafeekseef81-svg/Sharks/issues/67)).
Shafeek Saif, Scrum Master in Iteration 2, confirms or adjusts it at the planning meeting.
Sources: the Assignment 5 brief (`Assignment5_Iteration2_Brief.pdf`), SDP 5.2.1, 5.2.2, 5.3.4 and 5.4, and the Iteration 1 retrospective (`Iteration1/docs/N08_Retrospective.md`).

**Submission: 06/12/2026 23:59.**

## 1. What Assignment 5 requires

| # | Deliverable | Required name |
|---|---|---|
| 1 | Source that compiles and runs from a run file. The ZIP holds the code and the run file. **A run file that does not start with a double-click disqualifies the whole assignment.** | folder `iteration2_Sharks` |
| 2 | Scrum report: the features built in this iteration and who built each one. **Continue the Iteration 1 file.** | `iteration2_scrumReport_Sharks` |
| 3 | System report (Word or PDF): a. how to run the system, with libraries and versions; b. inputs; c. expected results in different scenarios; d. what the player can do; e. how we tested it. **Continue the Iteration 1 report.** | `iteration2_report_Sharks` |
| 4 | Design patterns (Word or PDF): which patterns, which problem each solves, where it is in the code (screenshots allowed) | `iteration2_dp_Sharks` |

- Each student implements one design pattern, four in total. Singleton and MVC do not count. A pattern not taught in the course needs the TA's approval several days before the deadline; otherwise it is not graded.
- The code contains the Iteration 2 tasks from our task table (SDP 5.2.1).
- Group grade: working code for the tasks reported as Completed, the reports and their consistency. Personal grade: own contribution to the code, active participation in meetings and teamwork, and the quality of the design-pattern implementation.

## 2. Roles (SDP table 4, retrospective section 5)

| Member | Iteration 2 role |
|---|---|
| Mohamed Zereny | Product Owner + Developer: acceptance criteria and tests, system report |
| Shafeek Saif | Scrum Master + Developer: planning, board, Scrum Report, retrospective |
| Rakan Ziyan | Developer: critical-path rules, release |

## 3. Scope: SDP Iteration 2 compared with Iteration 1

| Story | SRS IDs | State after Iteration 1 |
|---|---|---|
| [US-2.1 #48](https://github.com/shafeekseef81-svg/Sharks/issues/48) Medium/Hard boards, Boost/UltraBoost dice | FR-05, FR-10–FR-12, FR-14, FR-16, FR-17, FR-25 | `BoardFactory` builds the Medium/Hard geometry and `LevelConfig` holds the counts; `RandomDice` throws for Medium/Hard; setup allows Easy only (issue #13) |
| [US-2.2 #49](https://github.com/shafeekseef81-svg/Sharks/issues/49) Double Block | FR-19, FR-28–FR-30 | not implemented (risk R1) |
| [US-2.3 #50](https://github.com/shafeekseef81-svg/Sharks/issues/50) Questions, surprises, chains | FR-26, FR-27, FR-32, FR-34–FR-42; FR-56, FR-64 partial | not implemented; the prototype's `Question`, `QuestionBank` and `SurpriseOutcome` are reused only through the reuse gate (risk R2) |
| [US-2.4 #51](https://github.com/shafeekseef81-svg/Sharks/issues/51) Unit tests for every rule; State pattern (D3) | NFR-02, NFR-04, NFR-05, NFR-13, NFR-14 | 380 JUnit tests on Easy; `TurnPhase` is an enum, with no State pattern yet |

## 4. Design patterns (one per student, four in total)

| Pattern | Problem it solves | Task | Owner |
|---|---|---|---|
| State | Turn phases (roll, choose piece, move, special-square effects, next turn) | T2-13 [#64](https://github.com/shafeekseef81-svg/Sharks/issues/64) | Rakan |
| Strategy | A different die and movement formula per level (Regular / Boost / UltraBoost) | T2-04 [#55](https://github.com/shafeekseef81-svg/Sharks/issues/55) | Shafeek |
| Factory Method | Creating questions by level and surprise outcomes | T2-08 [#59](https://github.com/shafeekseef81-svg/Sharks/issues/59) | Mohamed |
| Command | Square effects queued and run in order, with a chain guard | T2-10 [#61](https://github.com/shafeekseef81-svg/Sharks/issues/61) | Shafeek |

Observer (`GameListener`) and the board factory already exist from Iteration 1 and are not counted as the Iteration 2 patterns.
Each owner writes their own section of `iteration2_dp_Sharks` (N2-05).

## 5. Tasks

Rule tests are written by a member other than the code author (SDP 5.3.4). Deep review = two reviewers.

| ID | Issue | Task | Owner | Reviewer(s) | Depends on | Due |
|---|---|---|---|---|---|---|
| T2-01 | [#52](https://github.com/shafeekseef81-svg/Sharks/issues/52) | Enable Medium/Hard in setup (FR-05) | Mohamed | Shafeek | T2-04 | 25/11 |
| T2-02 | [#53](https://github.com/shafeekseef81-svg/Sharks/issues/53) | Medium/Hard routes, arrows, counts, placement (FR-10–FR-12) | Mohamed | Rakan | – | 25/11 |
| T2-03 | [#54](https://github.com/shafeekseef81-svg/Sharks/issues/54) | Medium/Hard drawing, square symbols (FR-14, NFR-23) | Mohamed | Shafeek | T2-02 | 25/11 |
| T2-04 | [#55](https://github.com/shafeekseef81-svg/Sharks/issues/55) | **Strategy**: dice and movement formula (FR-16, FR-17, FR-25) | Shafeek | Rakan, Mohamed | – | 25/11 |
| T2-05 | [#56](https://github.com/shafeekseef81-svg/Sharks/issues/56) | Tests: 10,000 draws, formula, routes (NFR-13) | Rakan | Shafeek | T2-04 | 25/11 |
| T2-06 | [#57](https://github.com/shafeekseef81-svg/Sharks/issues/57) | Double Block (FR-19, FR-28–FR-30) | Rakan | Shafeek, Mohamed | – | 25/11 |
| T2-07 | [#58](https://github.com/shafeekseef81-svg/Sharks/issues/58) | Tests: Double Block | Shafeek | Rakan | T2-06 | 25/11 |
| T2-08 | [#59](https://github.com/shafeekseef81-svg/Sharks/issues/59) | **Factory Method**: questions (FR-34–FR-38) | Mohamed | Rakan, Shafeek | T2-04 | 02/12 |
| T2-09 | [#60](https://github.com/shafeekseef81-svg/Sharks/issues/60) | Surprise outcomes (FR-39, FR-40) | Mohamed | Rakan, Shafeek | T2-08 | 02/12 |
| T2-10 | [#61](https://github.com/shafeekseef81-svg/Sharks/issues/61) | **Command**: effect queue and chains (FR-26, FR-27, FR-32, FR-41, FR-42) | Shafeek | Rakan, Mohamed | T2-06, T2-09 | 02/12 |
| T2-11 | [#62](https://github.com/shafeekseef81-svg/Sharks/issues/62) | Question and surprise dialogs (NFR-05) | Mohamed | Rakan | T2-08 | 02/12 |
| T2-12 | [#63](https://github.com/shafeekseef81-svg/Sharks/issues/63) | Tests: questions, surprises, chains | Rakan | Shafeek | T2-10 | 02/12 |
| T2-13 | [#64](https://github.com/shafeekseef81-svg/Sharks/issues/64) | **State**: turn phases (D3) | Rakan | Shafeek, Mohamed | T2-06, T2-10 | 02/12 |
| T2-14 | [#65](https://github.com/shafeekseef81-svg/Sharks/issues/65) | Timing tests, RTM coverage (NFR-02, NFR-04, NFR-14) | Shafeek | Rakan | T2-13 | 05/12 |
| T2-15 | [#66](https://github.com/shafeekseef81-svg/Sharks/issues/66) | Acceptance per story; full games on Medium and Hard | Mohamed | Shafeek | T2-13 | 05/12 |
| N2-01 | [#67](https://github.com/shafeekseef81-svg/Sharks/issues/67) | Sprint planning; close #13 | Shafeek | Rakan | – | 25/11 |
| N2-02 | [#68](https://github.com/shafeekseef81-svg/Sharks/issues/68) | `iteration-2` branch, `Iteration2/` folder, branch protection | Rakan | Shafeek | – | 25/11 |
| N2-03 | [#69](https://github.com/shafeekseef81-svg/Sharks/issues/69) | Scrum Report, updated at every merge | Shafeek | Rakan | – | 05/12 |
| N2-04 | [#70](https://github.com/shafeekseef81-svg/Sharks/issues/70) | System report | Mohamed | Shafeek | T2-15 | 05/12 |
| N2-05 | [#71](https://github.com/shafeekseef81-svg/Sharks/issues/71) | Design patterns document | Shafeek (assembles) | All | pattern tasks | 05/12 |
| N2-06 | [#72](https://github.com/shafeekseef81-svg/Sharks/issues/72) | Release: double-click JAR, ZIP, two computers (also at CP1) | Rakan | Mohamed | – | 25/11 + 05/12 |
| N2-07 | [#73](https://github.com/shafeekseef81-svg/Sharks/issues/73) | Retrospective, hand-over to Iteration 3 | Shafeek | All | – | 05/12 |

**Critical path (SDP 5.2.2):** move engine → T2-06 Double Block → T2-10 chains → T2-13 State.

## 6. Milestones

| Milestone | Date | Content |
|---|---|---|
| I2-CP1 | 25/11/2026 | Dice (Strategy), Medium/Hard boards, Double Block merged into `iteration-2`; the JAR runs on two computers (retrospective action 4) |
| I2-CP2 | 02/12/2026 | Questions, surprises, chains (Command), State merged |
| I2-Release D3 | 06/12/2026 | 03–05/12 integration only (SDP 5.2.2); reports, ZIP, submission |

## 7. Way of working

- Branches: `feature/<name>/<task-id>-<short-name>` from `iteration-2`; PRs into `iteration-2`. Merge into `main` only with the release PR, approved by the team.
- The issue body names the reviewers and the deadline. Review within 24 hours (retrospective action 3).
- A task is Completed in the Scrum Report only after review and passing tests (SDP 5.3.4).
