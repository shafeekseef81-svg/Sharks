# How Team Sharks works in this repository

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
