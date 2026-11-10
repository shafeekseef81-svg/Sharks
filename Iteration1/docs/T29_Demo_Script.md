# Iteration 1 demonstration script (T29)

Owner: Shafeek Saif (PO). Audience: the team, for rehearsal, and the presenter on the day.
Status: **not rehearsed yet.** See "Readiness".

The demonstration covers US-1.1 to US-1.5 on the Easy level. It has a live part (plan A) and a repeatable
fallback (plan B).

## Readiness

| Needed for plan A (live application) | State on 10/11/2026 |
|---|---|
| Rules: T16 #11, T18 #7, T17 #9, T12 #5, T23 #10 | Merged into `iteration-1` |
| Menu and protection squares: T01 #15, T07 #12 | Merged |
| `GameController` connected to the model: N02 #14 | Ready for review; passes on `iteration-1` with this PR (376/376) |
| `GameClock`: T24 #31 | Merged |
| Only Easy selectable: T03 #32 | Merged; Medium and Hard are shown as "Iteration 2" and cannot be chosen |
| Runnable JAR on two computers, including a Hebrew path: T28 | Not done. On one computer the JAR built from `iteration-1` + N02 starts with `run.bat`; `java -jar` with a full Hebrew path fails (documented in the system report) |

Plan A can run once N02 (#14) is merged. Plan B runs on `iteration-1` today. Neither has been rehearsed yet.

## Plan A: live application (about 6 minutes)

Before starting: run `test.bat`, then `build.bat`, and open the game with `run.bat` (not `java -jar`, because of
Hebrew folder names). Have plan B ready in a second window.

| # | Show | Action | Expected | SRS |
|---|---|---|---|---|
| 1 | Start | `run.bat` | Main menu with New Game, Game History, Question Management, Exit | FR-01 |
| 2 | Setup | New Game; choose 2, then 3 players | The player rows follow the count | FR-04 |
| 3 | Validation | Leave a name empty; repeat a colour; Start | Start is refused and the wrong field is marked | FR-06..FR-08 |
| 4 | Level | Show that only Easy can be played | Medium and Hard are shown as coming later | FR-05 (P) |
| 5 | Board | Start a valid game | 15x15 board, 4 protection squares, all pieces in the castles | FR-10..FR-14 |
| 6 | Dice and turn | Roll a 1-5 with nothing on the board | A message, and the turn passes | FR-15, FR-23 |
| 7 | Castle exit | Keep rolling until a 6; click a highlighted piece | The piece stands on its own start square; the same player rolls again | FR-19, FR-21, FR-24 |
| 8 | Move | Roll; click a piece | It moves square by square, by exactly the die value | FR-20, NFR-04 |
| 9 | Capture | Land on a lone opponent on a plain square, when it happens | The opponent goes back to its castle | FR-26 |
| 10 | Protected | Land on a star with an opponent on it, when it happens | No capture; both stay | FR-27 |
| 11 | Timer | Point at the timer | It runs in HH:MM:SS | FR-46 |

Random dice will not reliably reach capture, protection, home path and finish within the time. **Show steps 1 to 8
live, and use plan B for the rest** unless they happen by chance. Do not claim steps that were not shown.

## Plan B: repeatable fallback (about 2 minutes)

`DemoScenarioTest` plays one fixed game through the real `GameModel` and the real rules and prints a transcript.
Part 1 is played from the start of the game: passes, castle exit, moves, capture, and the extra roll of the capturing
6. In part 2 the test places pieces near the end, and each placement is printed as `Placed:`. It then shows the
protection square, entering the home path, a refused overshoot, the exact finish and the final ranking.

After `test.bat` has built the tests, run this in `Iteration1`:

```bat
java -jar lib\junit-platform-console-standalone-1.10.2.jar execute --class-path "build\classes;build\test-classes" --select-class Model.DemoScenarioTest --disable-banner --details=none
```

Tell the audience plainly that part 2 places the pieces. The product itself has no demo hooks (SDP 5.3.1), so the
live application cannot be given fixed dice.

## What this demonstration does not show

Medium and Hard, Double Blocks, questions, surprises, saved game history and administrator functions are planned for
Iterations 2 and 3. Plan B has no duration or results screen; those are shown in plan A (FR-46, FR-47).
