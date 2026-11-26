# T2-14: timing tests and RTM coverage check (snapshot 25/11)

Task **T2-14** · Issue [#65](https://github.com/shafeekseef81-svg/Sharks/issues/65) · SRS **NFR-02, NFR-04, NFR-14** · Owner: Shafeek Saif · Reviewer: Rakan Ziyan

**This is a snapshot, not the final check.** The final check runs on `iteration-2` after the last code merge (by 05/12) with the same script.

**What changed since 23/11.** All the Iteration 2 code is now merged, so this snapshot is measured on **`iteration-2` `06e2ab2` itself**, with nothing merged by hand. #95 and #96 merged on 24/11, and #111, #99 and #103 on 25/11. Result: **705/705** tests, 0 failed, 0 aborted. With every PR that is still open (#100, #107, #109, #110, #112, #113, all documentation) merged locally on top, the result is still 705/705, with no conflicts. The snapshots of 19/11 (`6e13bcf`, 641/641) and 23/11 (`4640416`, 700/700 with the open PRs) are in the history of this file.

## 1. Timing (NFR-02, NFR-04): `Iteration2/test/View/TimingTest.java`

`TimingTest` prints a time only when it fails, so the times were read from a scratch copy that prints them (one `System.out.println` before each assertion; nothing else changed). Two ways of running it give different times. In the full suite (`test.sh`) the JVM is already warm. On its own (`--select-class View.TimingTest`) the JVM is cold, and the 30-square test, which runs first, also pays for loading the board and animation classes.

| Requirement | What is measured | Full suite, 25/11 | Alone (cold JVM), 25/11 | Limit |
|---|---|---|---|---|
| NFR-02 | The game is started (model and random board with protection, question and surprise squares), then the screen is laid out and painted. Hard, 4 players, 21 boards. | worst **67 ms** | worst 82–86 ms (3 runs) | 2000 ms |
| NFR-02 | The same for Easy and Medium, 4 players, 10 boards each. | all 20 pass; the first Easy board, which pays the class loading, 201 ms | first Easy board 465–505 ms (3 runs) | 2000 ms |
| NFR-04 | The longest move: Regular 6 × (UltraBoost 6 − 1) = 30 squares, animated square by square. | **1259 ms** | **1751–1830 ms** (5 runs: 1751, 1751, 1759, 1815, 1830) | 2000 ms |
| NFR-04 | The surprise jumps to the castle and to the finish. | 970 / 863 ms | 949–968 / 832–849 ms (3 runs) | 2000 ms |

**Every timing passes, on the merged branch itself.** The release script (#81) runs the whole suite, where the 30-square move takes about 1.26 s.

**The cold 30-square move has less margin than on 23/11** (1612–1668 ms then, 1751–1830 ms now: 170–249 ms under the limit). No code that this test measures changed between the two snapshots: the only source file changed since `4640416` is `QuestionDialog` (#111), which `TimingTest` doesn't use. So the difference comes from the machine (other programs running), not from the game. This is the reason #97 lowered the animation cap to 1200 ms (`BoardComponent.MAX_MOVE_MILLIS`, merged 23/11 as `1f099f2`): with the old cap of 1600 ms, the cold runs of 23/11 already failed 2 times out of 3 (1992, 2008, 2043 ms).

**The 30-square move is not Hard-only.** The `EXTRA_ROLL` surprise rolls the Regular and UltraBoost dice on every level (SRS Table 2, Q27), so a Medium move can also reach 30 squares. The budget caps by distance, not by level, so the row above covers both levels.

A cold start of the program (the first game after a double-click) can't be measured inside the test run. It is checked by hand on the two release computers (N2-06).

## 2. RTM coverage of the movement and special-square rules (NFR-14)

**Method.** `Iteration2/tools/rtm_coverage.py` reads the FR table of `Assignment3_Prototype/Sharks_SRS.pdf` (as text from `pdftotext -layout`). It then counts every JUnit `@DisplayName` that names an FR, alone or in a range (`FR-28..FR-30` counts for 28, 29 and 30). The counts are therefore an upper bound: a whole-game test whose name covers `FR-34..FR-45` counts once for each of them. This snapshot covers FR-15 to FR-47 (the 23/11 snapshot stopped at FR-45).

```
python tools/rtm_coverage.py srs.txt test 15 47
```

| FR | Requirement (SRS, first line) | Tests | Test classes |
|---|---|---:|---|
| FR-15 | The system shall enforce the setup turn order and | 10 | GameControllerTest, TurnAcceptanceTest, TurnOrderTest, TurnSequenceIndependentTest |
| FR-16 | Dice results shall be uniformly random: Regular | 19 | DiceRouteIndependentTest, DiceStrategyTest, GameSetupViewTest, MediumHardAcceptanceTest, RegularDiceTest, SpecialDiceGameTest, TurnAcceptanceTest, TurnOrderTest, TurnSequenceIndependentTest |
| FR-17 | Movement shall equal the Regular die in Easy, or | 24 | DiceRouteIndependentTest, DiceStrategyTest, MediumHardAcceptanceTest, RegularDiceTest, SpecialDiceGameTest, StandardHomePathRulesTest, TurnAcceptanceTest, TurnSequenceIndependentTest |
| FR-18 | A piece on its home path shall move by the | 7 | CaptureProtectionFinishTest, MovementAcceptanceTest, SpecialDiceGameTest, StandardHomePathRulesTest |
| FR-19 | A Regular 6 shall allow placing a piece on its | 25 | DiceRouteIndependentTest, DiceStrategyTest, DoubleBlockEngineTest, DoubleBlockIndependentTest, EasyBoardTest, GameModelSkeletonTest, LevelConfigTest, MoveEngineIndependentTest, MovementAcceptanceTest, SpecialDiceGameTest, StandardHomePathRulesTest, StandardMoveEngineTest, TurnStateTest |
| FR-20 | Each roll shall move exactly one piece; steps shall | 21 | DoubleBlockEngineTest, GameControllerTest, MoveEngineIndependentTest, MovementAcceptanceTest, MovementAnimationTest, MvcDependencyTest, RuleOfSixTest, StandardMoveEngineTest, TurnSequenceIndependentTest, TurnStateTest |
| FR-21 | A Regular 6 shall grant an extra roll even if | 20 | DoubleBlockIndependentTest, EffectChainIndependentTest, EffectResolutionTest, FullGameAcceptanceTest, GameControllerTest, GameModelSkeletonTest, MovementAcceptanceTest, RuleOfSixTest, SpecialDiceGameTest, TurnAcceptanceTest, TurnSequenceIndependentTest, TurnStateTest |
| FR-22 | Three consecutive Regular 6s shall cancel the | 16 | CaptureProtectionFinishTest, GameModelSkeletonTest, MoveEngineIndependentTest, RuleOfSixTest, StandardHomePathRulesTest, StandardMoveEngineTest, TurnAcceptanceTest, TurnSequenceIndependentTest, TurnStateTest |
| FR-23 | If no legal move is available and no additional roll | 23 | BoardComponentRenderTest, CaptureProtectionFinishTest, DoubleBlockIndependentTest, GameControllerTest, GameModelSkeletonTest, MainMenuViewTest, MediumHardBoardRenderTest, RuleOfSixTest, StandardMoveEngineTest, TurnAcceptanceTest, TurnSequenceIndependentTest, TurnStateTest |
| FR-24 | After a roll, the system shall highlight the pieces | 15 | GameClockTest, GameControllerTest, LegalMoveHighlightTest, MainMenuViewTest, TurnAcceptanceTest |
| FR-25 | Pieces shall move clockwise, follow only arrows of | 23 | DiceRouteIndependentTest, EasyBoardTest, MediumHardBoardIndependentTest, MoveEngineIndependentTest, MovementAcceptanceTest, SpecialDiceGameTest, StandardMoveEngineTest, StandardSquareRulesTest |
| FR-26 | Landing on an unprotected square with one | 26 | CaptureProtectionFinishTest, DoubleBlockEngineTest, MoveEngineIndependentTest, MovementAcceptanceTest, SquareEffectTest, StandardMoveEngineTest, StandardSquareRulesTest |
| FR-27 | Protection, starting, surprise, and finishing squares | 15 | CaptureProtectionFinishTest, EasyBoardTest, MovementAcceptanceTest, SquareEffectTest, StandardMoveEngineTest, StandardSquareRulesTest |
| FR-28 | Two or more same-player pieces on one square | 13 | DoubleBlockEngineTest, DoubleBlockIndependentTest, MediumHardAcceptanceTest, SquareEffectTest |
| FR-29 | An opponent's Double Block shall prevent landing | 22 | DoubleBlockEngineTest, DoubleBlockIndependentTest, MediumHardAcceptanceTest, MovementAcceptanceTest, SquareEffectTest |
| FR-30 | A Double Block shall remain while two pieces | 6 | DoubleBlockEngineTest, DoubleBlockIndependentTest |
| FR-31 | Finishing shall require exact steps; if a move | 20 | CaptureProtectionFinishTest, DoubleBlockEngineTest, MoveEngineIndependentTest, MovementAcceptanceTest, SquareEffectTest, StandardHomePathRulesTest, StandardMoveEngineTest |
| FR-32 | Backward movement caused by questions or | 4 | EffectChainIndependentTest, EffectResolutionTest, SquareEffectTest |
| FR-33 | A home path shall contain no special squares and | 15 | BoardProtectionIntegrationTest, CaptureProtectionFinishTest, DiceRouteIndependentTest, EasyBoardAcceptanceTest, EasyBoardTest, MediumHardBoardIndependentTest, MoveEngineIndependentTest, MovementAcceptanceTest, StandardHomePathRulesTest, StandardMoveEngineTest, StandardSquareRulesTest |
| FR-34 | Landing on a question square shall display to all | 12 | EffectChainIndependentTest, EffectResolutionTest, GameControllerTest, QuestionDialogTest, QuestionFactoryTest, SpecialDiceGameTest, SquareEffectTest |
| FR-35 | Correct answers shall move the piece forward 1 | 13 | EffectChainIndependentTest, EffectResolutionTest, LevelConfigTest, NoAnswerVisibilityTest, QuestionDialogTest, QuestionFactoryTest, SpecialDiceGameTest, SquareEffectTest |
| FR-36 | After an answer, the system shall show whether it | 5 | EffectChainIndependentTest, QuestionDialogTest, SpecialDiceGameTest |
| FR-37 | A question shall not repeat in the same game until | 6 | EffectChainIndependentTest, EffectsAcceptanceTest, QuestionFactoryTest, SpecialDiceGameTest |
| FR-38 | Capturing on an unprotected question square shall | 3 | EffectChainIndependentTest, SpecialDiceGameTest, SquareEffectTest |
| FR-39 | Landing on a surprise square shall trigger exactly | 21 | EffectChainIndependentTest, EffectResolutionTest, GameControllerTest, SpecialDiceGameTest, SurpriseDialogTest, SurpriseFactoryTest, SurpriseOutcomeTest |
| FR-40 | For "move an opponent", the active player shall | 31 | EffectChainIndependentTest, EffectResolutionTest, SpecialDiceGameTest, SurpriseDialogTest, SurpriseFactoryTest |
| FR-41 | Question and surprise movements shall obey | 8 | EffectChainIndependentTest, SpecialDiceGameTest, SquareEffectTest |
| FR-42 | Question or surprise movement shall activate any | 16 | EffectChainIndependentTest, EffectResolutionTest, SpecialDiceGameTest, SquareEffectTest, SurpriseDialogTest, SurpriseFactoryTest |
| FR-43 | A player shall finish when all four pieces reach the | 17 | EffectResolutionTest, FullGameAcceptanceTest, GameControllerTest, GameEndRankingTest, MediumHardAcceptanceTest, MoveEngineIndependentTest, RuleOfSixTest, SpecialDiceGameTest, StandardHomePathRulesTest, StandardMoveEngineTest, StandardRankingTrackerTest, TurnOrderTest |
| FR-44 | The game shall end when all but one player have | 20 | EffectResolutionTest, FullGameAcceptanceTest, GameClockTest, GameControllerTest, GameEndRankingTest, MediumHardAcceptanceTest, MoveEngineIndependentTest, RuleOfSixTest, SpecialDiceGameTest, StandardMoveEngineTest, StandardRankingTrackerTest, TurnOrderTest, TurnStateTest |
| FR-45 | Rankings shall follow the order in which players | 11 | FullGameAcceptanceTest, GameControllerTest, GameEndRankingTest, MediumHardAcceptanceTest, SpecialDiceGameTest, StandardRankingTrackerTest |
| FR-46 | The system shall measure and display elapsed | 22 | FullGameAcceptanceTest, GameClockTest, GameControllerTest, GameEndRankingTest |
| FR-47 | At game completion, the system shall display | 2 | GameControllerTest, NavigationControllerTest |

**No FR from FR-15 to FR-47 is without a test.** Compared with 23/11, the only change is FR-35 (12 → 13 tests), from `NoAnswerVisibilityTest` (#111).

**The thinnest row is FR-47** (the end-of-game summary screen), with 2 tests. It is also checked by hand: N2-06 check 8 ("a full game reaches the results screen") passed on computer 1 (Rakan Ziyan, #109).

**The independent tests are now on the branch itself.** SDP 5.3.4 asks for every rule to be tested by a member who did not implement it. `EffectChainIndependentTest` (T2-12, Rakan Ziyan, #95, 25 tests) merged on 24/11, so every FR from FR-32 to FR-42 now has an independent test in `iteration-2`. (FR-33 has independent board tests.) The counts of the weak spots of 19/11:

| FR | Rule | 19/11 | 25/11 | Independent test |
|---|---|---:|---:|---|
| FR-32 | backward movement stops at the start square | 2 | 4 | `EffectChainIndependentTest` |
| FR-36 | feedback after an answer | 3 | 5 | `EffectChainIndependentTest` |
| FR-37 | no repeated question | 4 | 6 | `EffectChainIndependentTest`, `EffectsAcceptanceTest` (#103) |
| FR-38 | a capture on a question square comes first | 2 | 3 | `EffectChainIndependentTest` |
| FR-41 | effects obey the movement and blocking rules | 4 | 8 | `EffectChainIndependentTest` |
| FR-42 | chains, no reactivation | 12 | 16 | `EffectChainIndependentTest` |

**One limit that stays.** Natural play never uses up a level's questions, so the FR-37 reshuffle is covered only by `QuestionFactoryTest` (found in #99).

## 3. What is left for the final check (05/12)
- If code changes after this snapshot, run `TimingTest` and the script again on the final `iteration-2` and replace the tables above. If only documentation merges, this snapshot stands.
- Measure NFR-02 cold on the two release computers (N2-06): the first game after a double-click, with a stopwatch, and add the result here.
