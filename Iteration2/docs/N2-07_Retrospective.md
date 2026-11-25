# Iteration 2 retrospective and role hand-over (N2-07)

Team Sharks. Facilitator: Shafeek Saif (Scrum Master, Iteration 2). Task N2-07, [#73](https://github.com/shafeekseef81-svg/Sharks/issues/73).
Status: **draft, 23/11/2026.** Written by the SM from the GitHub record (pull requests, reviews, issue #75 and the Scrum Report). The figures are as of 23/11. Sections 1 and 6 get their final figures after the last merge (05/12). The team confirms or corrects the text in the review of the pull request that finalises this file (see section 7).

## 1. What was delivered (state on 23/11)

| Item | Result |
|---|---|
| User stories | US-2.1 to US-2.4: Medium and Hard boards with the special dice, Double Blocks, question and surprise squares with chains, unit tests for every rule and the State pattern |
| Pull requests | 17 merged into `iteration-2` (#74 to #101); 15 open, all code PRs approved |
| Tests | `iteration-2` `e9f6b2e`: 540/540. All open PRs merged together locally (#92, #95, #96, #97, #98, #103, #104, #105): **700/700**, no conflicts |
| Design patterns | Strategy (T2-04), State (T2-13), Factory Method (T2-08) and Command part 1 (T2-10) merged; Command part 2 (#92) approved, Deep 2/2 |
| PO decisions | Register `Iteration2/docs/T2-15_PO_Decisions.md` (#99): D2 and D3 decided on 21/11, D4 and D5 answered on 22/11, D1 open |
| Deliverables (06/12) | `iteration2_Sharks` ZIP, `iteration2_scrumReport_Sharks`, `iteration2_report_Sharks` (N2-04), `iteration2_dp_Sharks` (N2-05): *to be filled in at the release* |

## 2. The Iteration 1 actions: what happened to them

| # | Action from Iteration 1 | Result in Iteration 2 |
|---|---|---|
| 1 | Protect the iteration branch, one approval per PR | **Done.** `iteration-2` is protected and needs one approval (N2-02, #74). Two gaps remain: stale approvals are not dismissed, and the rule does not apply to admins. |
| 2 | No single PR blocks more than two others | **Not met.** T2-10 was split into two parts (#87, #92), which helped. Still, #79 held #92, #95 and #96 until 22/11, and #92 then held six PRs (#95, #96, #103, #104, #105 and the fix for #99). After #79 merged (22/11), #92 merged the next morning, so the chain cleared before CP1. |
| 3 | Review within 24 hours | **Partly.** 14 of the 25 PRs reviewed so far got their first review within 24 hours. From 17/11 to 21/11 eight PRs waited for the same reviewer; the queue was cleared in one day (21/11): all 12 open PRs were reviewed, each with a test run. |
| 4 | JAR on two computers at the middle checkpoint | **Partly.** The release script (#81) was opened on 16/11 and merged only on 23/11 (9fea81d), two days before CP1; the fix for its review finding went in on the day it was asked for (22/11). CP1 run (#109): checks 1–8 PASS on computer 1, 685/685; the second computer is still to be filled in. |
| 5 | Scrum Report in the repository, updated at every merge | **Done**, once per merge day rather than per merge (#86, #101). |
| 6 | Independent tests together with the code | **Done.** T2-05 (#78), T2-07 (#85) and T2-12 (#95) were written from the SRS while the code PRs were still open. |

## 3. What went well (keep doing)

- **Full-integration runs before merging.** All open PRs were merged together on a scratch branch three times: 664/664 (Rakan, 21/11), 683/683 (Shafeek, 22/11) and 700/700 (Mohamed and Shafeek, 23/11). There were no conflicts.
- **Cross-PR test runs found a real break before it reached the branch.** #99's acceptance tests pass on `iteration-2`, but 5 of 7 fail once #92 is merged (`RESOLVING` refuses the roll). It was found in review, and the fix was proved on #105 before anything merged.
- **Timing tests found a risk early.** T2-14 measured the 30-square Hard move at 1.7 s of the 2 s limit (2.1 s on a cold JVM). #97 lowered the animation budget to 1200 ms before CP1.
- **Decisions were written down with their evidence.** The PO register (D1–D5) records the options, the SRS text and the reason for each decision, so the code and the tests can point to it.
- **Tests were checked against broken code.** Mutation checks were done for the special-square rules (8 of 8 caught, T2-12 #95/#102; the Q81 mutant was missed at first and the test was rewritten), for Double Blocks (16 of 19 tests fail with blocks off, T2-07 #85), for FR-29 passing through a block (#99) and for T2-10 part 2.

## 4. What did not go well (change)

- **The long chain at the end came back.** #79 → #90 → #91 → #92, and then six more PRs on top of #92. Several PRs are stacked on other feature branches (#91, #103, #104, #105), so each merge has to happen in a fixed order.
- **Approved PRs waited to be merged.** #90 has had two Deep approvals since 22/11 14:09, and #91 an approval since 22/11 14:10; on 23/11 neither was merged, and nothing else could move. We had no rule for who merges an approved PR, and by when.
- **Approvals went stale without notice.** New commits after an approval (#90 and #91 on 21/11) left the green tick in place, because the repository does not dismiss stale approvals. We relied on the author pointing it out.
- **No CI.** Tests ran only when someone ran them. Whichever of #92 and #99 merged second would have broken `iteration-2` with nobody notified.
- **The release script came late again.** #81 was opened on 16/11 and merged only on 23/11, two days before CP1.
- **Our texts grew long.** PR descriptions, reviews and status comments of more than 100 lines became common, so reading them took time that reviewing needed.

## 5. Actions for Iteration 3

| # | Action | Owner |
|---|---|---|
| 1 | Turn on "Dismiss stale pull request approvals when new commits are pushed" for the iteration branch | Shafeek Saif (repository owner) |
| 2 | Add CI: a GitHub Actions workflow runs `test.sh` on every PR into the iteration branch, required before merge | Proposal: Shafeek Saif, in the Iteration 3 setup |
| 3 | The author merges within 24 hours of the last required approval; after that the SM may merge | SM, Iteration 3 |
| 4 | Stack at most one level: a PR may build on one unmerged PR, not on a chain | All |
| 5 | Merge the release script and checklist in the first days of the iteration, before feature code | Developer of the release task |
| 6 | Keep the PO decision register and the full-integration runs from day one | PO and SM, Iteration 3 |
| 7 | Keep PR descriptions and comments short: what changed, how it was tested, what is needed from the reader | All |

## 6. Role hand-over to Iteration 3

The course requires the roles to rotate every iteration (SDP table 4).

| Member | Iteration 2 | Iteration 3 |
|---|---|---|
| Rakan Ziyan | Developer | Product Owner + Developer |
| Shafeek Saif | Scrum Master + Developer | Developer |
| Mohamed Zereny | Product Owner + Developer | Scrum Master + Developer |

Handed over:
- **To the new SM (Mohamed):** `CONTRIBUTING.md`, `Iteration2/docs/ITERATION2_PLAN.md` as the model for the Iteration 3 plan, the Scrum Report workbook and how its rows are updated, and the open items below.
- **To the new PO (Rakan):** the SRS (`Assignment3_Prototype/Sharks_SRS.pdf` has the FR table), the decision register `Iteration2/docs/T2-15_PO_Decisions.md`, and the Iteration 3 scope from the SDP (administrator question management, FR-56..FR-61).

Open items carried over (state on 23/11; to be updated at the release):
- **D1** and the Double Block on a start square (Q7/Q35 against FR-19): PO decisions.
- **FR-37 reshuffle:** natural play never uses up a level's questions, so only `QuestionFactoryTest` covers the reshuffle.
- **EXTRA_ROLL on Medium:** the surprise roll uses Regular + UltraBoost on every level (SRS Table 2, Q27), so a Medium move can reach 30 squares (found in #105). The NFR-04 budget caps by distance, so it is covered, but texts that say "the longest Medium move is 12" need "from the level's own dice".
- **NFR-02 cold start:** measured by hand on the two release computers (T2-14, section 3).

## 7. Meeting record

*To be completed.* Draft written on 23/11/2026 by Shafeek Saif (SM) from the GitHub record. The final version is written after the last merge, and Rakan Ziyan and Mohamed Zereny confirm or correct it in the review of that pull request.
