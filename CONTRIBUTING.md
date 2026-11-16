# How Team Sharks works in this repository

> **Iteration 2 (from 16/11/2026):** the rules below still apply, with `iteration-2` in place of `iteration-1`
> and the changes listed in [Iteration 2](#iteration-2) at the end of this file.

## Branches
| Branch | Purpose | Who writes to it |
|---|---|---|
| `main` | Stable. Changes only when an iteration is delivered, tagged `v0.1` / `v0.2` / `v1.0` | Merge from `iteration-1` by the SM after the PO accepts the increment |
| `iteration-1` | Integration branch of Iteration 1. It must always build and pass `test.bat` | Reviewed pull requests only |
| `feature/<name>/<task>-<topic>` | One short branch per task, e.g. `feature/rakan/T16-move-engine`, `feature/shafeek/T17-capture`, `feature/mohamed/T13-highlight` | Only the task owner |

- Create each feature branch from the latest `iteration-1`.
- Update your branch every day with `git pull --rebase origin iteration-1`.
- Keep branches short, at most 2–3 days of work (SDP risk R8).
- Never force-push to `main` or `iteration-1`. Never commit to them directly.

## Commits
Format: `T16: add castle exit on a Regular 6 (FR-19)`. Start with the task ID and put the SRS IDs in brackets
(SDP 5.3.1). Make small commits that each compile.

## Pull requests
1. Open the PR into `iteration-1` and fill in the template.
2. Reviewers, as set in SDP 5.3.4:
   - **Deep**, two reviewers: move engine and Rule of 6 (T16, T12), and capture, home path and finish (T17, T18, T23).
   - **Standard**, one reviewer.
   - **Light**, checklist only: UI tasks.
3. Review within 24 hours. The author merges after approval and green `test.bat`.
4. Mark the task Done in the Scrum Report **only** after the merge and passing tests (Definition of Done).

## File ownership (avoids merge conflicts)
- Every file has one owner; the README of each iteration folder lists them.
- If you need a change in someone else's file, ask the owner in an issue or a PR comment. Do not edit it yourself.
- `build.*`, `test.*`, `run.*`, `Main`, `NavigationController`, `GameController`: Rakan.
- Shared interfaces in `Model` (see `Iteration1/README.md`) are frozen at CP1. Changing one needs a change
  request agreed by all three members.

## Merge order in Iteration 1
1. Skeleton (T27).
2. T07, then T08, T04 and T11.
3. T16.
4. T18, then T17.
5. T12, then T23 and T24.
6. N02 (`GameController` integration).

UI pull requests depend only on the interfaces, so they can be merged at any time.

## Iteration 2
Plan, owners and deadlines: [`Iteration2/docs/ITERATION2_PLAN.md`](Iteration2/docs/ITERATION2_PLAN.md) and the issues labelled `iteration-2`.

- **Roles (SDP table 4):** Mohamed = Product Owner + Developer, Shafeek = Scrum Master + Developer, Rakan = Developer.
- **Code:** all new work goes into `Iteration2/`. `Iteration1/` stays frozen as submitted for Assignment 4.
- **Branches:** create feature branches from `iteration-2` and open PRs into `iteration-2`. `main` changes only through
  the Iteration 2 release PR, approved by the team.
- **Reviews:** the issue of each task names its reviewers. Deep review (two reviewers): T2-04 dice, T2-06 Double Block,
  T2-08 questions, T2-09 surprises, T2-10 chains, T2-13 State, N2-06 release. Review within 24 hours.
- **Tests:** rule tests are written by a member other than the code author. `Iteration2/test.sh` (or `test.bat`) must pass
  before every merge; it started at 380/380.
- **Scrum Report:** `iteration2_scrumReport_Sharks.xlsx` (Iteration 1 rows kept, Iteration02 rows added). The SM updates it
  at every merge.
- **Ownership:** `build.*`, `test.*`, `run.*`, `Main`, `NavigationController`, `GameController`: Rakan, as in Iteration 1.

### Merge order in Iteration 2 (critical path, SDP 5.2.2)
1. T2-04 dice (Strategy) and T2-06 Double Block, in parallel; T2-02 board checks.
2. T2-05 and T2-07 tests; T2-01 and T2-03 UI. Checkpoint CP1 (25/11): JAR on two computers.
3. T2-08 questions (Factory Method), then T2-09 surprises.
4. T2-10 effect queue and chains (Command), then T2-12 tests.
5. T2-13 State, then T2-14 and T2-15. Checkpoint CP2 (02/12).
6. 03–05/12: integration, reports and release only.
