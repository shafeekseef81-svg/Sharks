# Iteration 1 retrospective and role hand-over (N08)

Team Sharks. Facilitator: Rakan Ziyan (Scrum Master, Iteration 1).
Status: **draft prepared from the GitHub record. To be confirmed by the team in the retrospective meeting (14-15/11)**; edit the sections below with what is agreed.

## 1. What was delivered

| Item | Result |
|---|---|
| User stories | US-1.1 to US-1.5 on Easy: menu and setup, Easy board, turns and dice, legal movement, game end and ranking |
| Code | 42 pull requests, all reviewed and merged; Iteration 1 released to `main` in PR #42 (`56ad104`) |
| Tests | 376 automated tests, 376 pass, 0 skipped. Every rule has author tests, independent tests by another member and PO acceptance tests |
| Deliverables | `iteration1_Sharks` ZIP (source + JAR), `iteration1_scrumReport_Sharks`, `iteration1_report_Sharks`, `classDiagram_Sharks`, `questions_Sharks` |

## 2. What went well (keep doing)

- **One PR per task with required reviews.** Deep reviews by two members for rule classes (T12, T16, T17, T18, T23) and one reviewer for the rest. Every merge was tested before and after.
- **Independent tests and PO acceptance found real defects before release:**
  - FR-06: invisible characters in player names, found by Shafeek in T05, fixed in #33.
  - Integer overflow in the home-path check, found by Mohamed in the T18 review.
  - Three minor findings in the move engine review (#11), fixed in #34 with tests that fail without the fix.
- **Interfaces frozen at checkpoint CP1** let the three members work in parallel (model rules, View, acceptance tests) without blocking each other's files.
- **Full-integration runs** (all open PRs merged together on a scratch branch) caught conflicts early; no PR needed rework because of a conflict.
- **Fault seeding** in the engine and turn tests checked that the tests actually catch mistakes.

## 3. What did not go well (change)

- **A long dependency chain at the end.** T12 had to merge before T23, T24, N02 and five test PRs. Many PRs waited as drafts, and most merges happened on 09-10/11.
- **Reviews arrived late.** Several PRs waited more than a day for a reviewer.
- **Some checks started late:** the JAR on a second computer (T28) and the ZIP (N07) were left to the last days. The Hebrew-folder problem with `java -jar` was found late; `run.bat` avoids it.
- **`main` was not protected** during the iteration; we relied on the team following the process.
- **Medium and Hard could be chosen** in setup until T03 (#32) and crashed at the first roll.
- **Status rows lagged** behind GitHub until the Scrum Report was kept in the repository (#30).

## 4. Actions for Iteration 2

| # | Action | Owner |
|---|---|---|
| 1 | Protect `main` and the iteration branch: changes only through a PR with at least one approval | Shafeek Saif (repository owner) |
| 2 | Split rule tasks so that no single PR blocks more than two others; merge small PRs as soon as they are approved | Shafeek Saif (SM, Iteration 2) |
| 3 | Review within 24 hours of a request; the reviewer is named when the task is assigned | All |
| 4 | Build the JAR and run it on two computers (including a Hebrew path) at the middle checkpoint, not at the end | Developer of the release task |
| 5 | Keep the Scrum Report in the repository and update it at every merge | SM, Iteration 2 |
| 6 | Write independent tests together with the code, not after it, so the DoD does not wait | All |

## 5. Role hand-over to Iteration 2

The course requires the roles to rotate every iteration.

| Member | Iteration 1 | Iteration 2 |
|---|---|---|
| Rakan Ziyan | Scrum Master + Developer | Developer |
| Shafeek Saif | Product Owner + Developer | Scrum Master + Developer |
| Mohamed Zereny | Developer | Product Owner + Developer |

Handed over:
- **To the new SM (Shafeek):** `CONTRIBUTING.md` (branches, review levels, merge order), the Scrum Report workbook and how it is kept in the repository, and the open items below.
- **To the new PO (Mohamed):** the SRS, the PO decisions in issue #13, and the Iteration 2 scope from the SDP: Medium and Hard boards, special dice, Double Blocks, question and surprise squares.

## 6. Open items carried over

- T28: run the submitted JAR on a second computer (Mohamed), and add the result to section 6.2 of the system report.
- A full game played by hand on the final JAR, and a rehearsal of the demo (T29).
- Issue #8 (BoardFactory and ProtectionSquarePlacer) and issue #13 (PO decisions): close them or carry them into Iteration 2.

## 7. Meeting record

To be filled in at the meeting: date, attendees, agreed changes to sections 2-6.
