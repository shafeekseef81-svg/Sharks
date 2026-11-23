# N2-04, section 3(e): testing evidence from Rakan's tasks

Source text for the system report `iteration2_report_Sharks` (N2-04, #70, owner Mohamed Zereny). It follows the
structure of `Iteration2/docs/N2-04_SystemReport.md` (PR #100), so the owner can paste it into section 3(e) or leave
it out. Written by Rakan Ziyan, 22/11/2026. Every number below was measured. A number that changes after the last
merge is marked **re-measure**.

## 1. Independent rule tests (SDP 5.3.4)

Each rule is tested by a member who did not implement it. The expected values are written out by hand from the SRS:
cells from the board's paths, steps, ⌈R/2⌉ and the Table 2 shares. They are never read from the code under test.

| Rules | Test class | Tests | Code author | PR | Status 22/11 |
|---|---|---|---|---|---|
| Dice distribution, movement formula, routes (FR-16, FR-17, FR-25, NFR-13) | `DiceRouteIndependentTest` | 15 | Shafeek (T2-04) | #78 | merged |
| Questions, surprises, chains (FR-32, FR-34..FR-42, Table 2, PO decision D2) | `EffectChainIndependentTest` | 25 | Mohamed (T2-08, T2-09), Shafeek (T2-10) | #95 | approved, merges after #92 |

How they work:
- **NFR-13:** 10,000 draws per level. Each face of the Regular, Boost and UltraBoost dice must fall within 2 percentage
  points of an equal share. The zero-movement share (a special die showing 1) is checked against 1/3 on Medium and
  1/6 on Hard.
- **Routes (FR-25):** checked geometrically on every colour and level:
  - the outer rings run clockwise;
  - each colour turns inward only at its own arrow;
  - a route passes every start square;
  - the home path is private.
- **Special squares:** every scenario is played through the `GameModel` facade with the real board, rules, factories
  and effect queue; only the dice are scripted. The special squares are placed at random (FR-12), so each test finds
  the first seeded board on which its scenario exists. Fixed seeds mean every failure reproduces exactly.

## 2. Proof that the tests catch wrong rules (mutation check)

A test that passes on the first run proves little. So for T2-12 each rule was broken on purpose in a copy of the
code, one at a time, and the full suite was run against each broken copy:

| Rule broken on purpose | Caught by |
|---|---|
| FR-32: backward movement not stopped at the start square | `pushStopsOnTheStartSquare`, `wrongAnswerStopsOnTheStartSquare` |
| FR-41: effects ignore Double Blocks | `rightAnswerStopsBeforeABlock`, `wrongAnswerStopsBeforeABlockBehind`, `zeroStepsDoNotReactivate` |
| FR-42 (Q81): a zero-step effect counts as a movement | `zeroStepPushDoesNotActivateTheSquareUnderThePiece` |
| FR-42: only the first square of a chain is activated | `answerChainsIntoTheNextSquare` |
| FR-40: half the Regular die rounded down instead of up | `pushBackHalfTheDieRoundedUp` |
| Table 2 (Q27): the extra roll uses the level's dice | `extraRollOutsideTheRuleOfSix` |
| Table 2 (Q56): skip-the-next-turn keeps a pending 6 | `skipTheNextTurn` |
| PO decision D2: a turn surprise falls on the owner of the piece | `skipFromAnOpponentsPieceFallsOnTheActivePlayer` (the only test in the suite that catches it) |

All 8 broken copies were caught.

**One test was rewritten because of this check.** The first Q81 test did *not* catch its broken copy, because the
"each square once per chain" guard hid the fault. It was replaced by a scenario in a new chain: an opponent stands on
a question square and is pushed into a Double Block behind it.

The same method was used by Shafeek for the Double Block tests T2-07 (#85): with blocks switched off, 16 of 19 fail.

## 3. Double Block (T2-06, FR-19, FR-28..FR-30)

- **Author's tests:** `DoubleBlockEngineTest`, 20 tests. They cover:
  - forming a block;
  - blocking in both directions (`isPassable` is direction-free);
  - stopping before a block only when no full move exists;
  - a block on the very next cell gives no move;
  - the castle exit onto a blocked start square;
  - the forced break.
- **Independent tests:** `DoubleBlockIndependentTest` (Shafeek, T2-07, #85), 19 tests through the facade.
- **Review:** two Deep reviews (Shafeek, Mohamed).
- **Implemented behaviour:** an opponent Double Block on a player's start square keeps that player's piece in its
  castle, even on a Regular 6 (`StandardMoveEngine.moveFor` checks `isPassable` on the start cell). This is the
  exception written into FR-19 itself: "a Regular 6 shall allow placing a piece on its starting square ... **unless an
  opponent Double Block occupies that square** (Q40)", and it is the blocking rule of FR-29.
- **Open point (PO decision D1):** staff answers Q7/Q35 say that two pieces of one colour on a *start square* do not
  form a Double Block at all. If the Product Owner follows them, the castle exit would no longer be blocked there. The
  code keeps the FR-19/Q40 behaviour until that decision.

## 4. Release checks (N2-06, NFR-10)

The ZIP is built only by `tools/make_iteration2_zip.py` (PR #81). The script:
1. exports the versioned files with `git archive`;
2. builds the JAR with `--release 17`;
3. checks `Main-Class: Controller.Main` and that every class file is Java 17 (major version 61);
4. runs the whole test suite. It **stops if any test failed, or if no test ran at all**: the JUnit launcher exits 0
   when it finds no tests, a gap found in Mohamed's review;
5. writes the ZIP with forward slashes under one top folder, and prints the file count, the test count and the
   SHA-256.

| Run | Code | Tests | ZIP | Started from a path with Hebrew letters |
|---|---|---|---|---|
| Dry run, 16/11 | `iteration-2` 38e2e1d (Iteration 1 code) | 380/380 | 106 files | yes |
| CP1 rehearsal, 21/11: all 16 open PRs merged locally | local merge 3b4039e | 664/664 | 168 files, JAR 261,390 bytes | yes, no error output |
| Script check after the review fix, 22/11 | `iteration-2` d445a38 | 531/531 | 142 files, JAR 222,254 bytes | — |
| CP1, 25/11 | merged `iteration-2` | **re-measure** | **re-measure** | **re-measure**, both computers |

The CP1 rehearsal also showed that every open PR merges without a conflict in the planned order.

**Not automated:** the double-click on `run.bat` and on the JAR, and a full game played by a person. They are rows 5–9
of the checklist in `Iteration2/docs/N2-06_Release.md` and are filled in by hand on both computers.

## 5. A finding the tests produced

The NFR-04 timing test (T2-14, #93) passed when run alone. But when nine test suites ran in parallel on Rakan's
computer, the 30-square Hard move took **2140 ms**, over the 2-second limit. This shows the risk on a busy or slow
computer. Mohamed lowered the animation budget from 1600 to 1200 ms in PR #97 (approved), and the off-screen
measurements after the change are in section 3(e) of the main draft.
