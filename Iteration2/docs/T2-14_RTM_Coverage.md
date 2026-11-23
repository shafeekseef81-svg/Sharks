# T2-14: timing tests and RTM coverage check (snapshot 23/11)

Task **T2-14** · Issue [#65](https://github.com/shafeekseef81-svg/Sharks/issues/65) · SRS **NFR-02, NFR-04, NFR-14** · Owner: Shafeek Saif · Reviewer: Rakan Ziyan

**This is a snapshot, not the final check.** The final check runs on `iteration-2` after the last merge (05/12) with the same script. This snapshot was measured on `iteration-2` `e9f6b2e` with every open PR that touches code or tests merged locally: #92, #95, #96, #97, #98, #103, #104 and #105 (#105 carries #99). Result: **700/700** tests, no conflicts. The first snapshot (19/11, `6e13bcf`, 641/641) is in the history of this file.

## 1. Timing (NFR-02, NFR-04): `Iteration2/test/View/TimingTest.java`

`TimingTest` prints a time only when it fails, so the times were read by setting the limit to 1 ms in a scratch copy. Two ways of running it give different times. In the full suite (`test.sh`) the JVM is already warm. On its own (`--select-class View.TimingTest`) the JVM is cold, which is closer to the first move after the program starts.

| Requirement | What is measured | Full suite, 23/11 | Alone (cold JVM), 23/11 | Limit |
|---|---|---|---|---|
| NFR-02 | The game is started (model and random board with protection, question and surprise squares), then the screen is laid out and painted. Hard, 4 players, 21 boards. | worst **173 ms** | worst 243–288 ms | 2000 ms |
| NFR-02 | The same for Easy and Medium, 4 players, 10 boards each. | all 20 pass; the first Easy board, which pays the class loading, 329 ms | first Easy board 329 ms | 2000 ms |
| NFR-04 | The longest move: Regular 6 × (UltraBoost 6 − 1) = 30 squares, animated square by square. | **1294–1319 ms** | **1612–1737 ms** | 2000 ms |
| NFR-04 | The surprise jumps to the castle and to the finish. | 993 / 868–889 ms | 968–1051 / 852–889 ms | 2000 ms |

**The risk of 19/11 is handled by #97, which must merge before the CP1 run.** On 19/11 the 30-square move took 1702 ms in the full suite, with a 1600 ms animation cap. #97 (Mohamed) lowers the cap to 1200 ms (`BoardComponent.MAX_MOVE_MILLIS`). Measured on 23/11, 3 cold runs each:

| Build | 30-square move, alone (cold JVM) |
|---|---|
| `iteration-2` `e9f6b2e`, without #97 | 1992, 2008, 2043 ms (**2 of 3 runs fail**) |
| `iteration-2` + #97 | 1612, 1659, 1668 ms |

The release script (#81) stops on any failed test. So without #97 the CP1 run can fail at random.

**The 30-square move is not Hard-only.** The `EXTRA_ROLL` surprise rolls the Regular and UltraBoost dice on every level (SRS Table 2, Q27), so a Medium move can also reach 30 squares (found in #105). The budget caps by distance, not by level, so the row above covers both levels.

A cold start of the program (the first game after a double-click) can't be measured inside the test run. It is checked by hand on the two release computers (N2-06).

## 2. RTM coverage of the movement and special-square rules (NFR-14)

**Method.** `Iteration2/tools/rtm_coverage.py` reads the FR table of `Assignment3_Prototype/Sharks_SRS.pdf` (as text from `pdftotext -layout`). It then counts every JUnit `@DisplayName` that names an FR, alone or in a range (`FR-28..FR-30` counts for 28, 29 and 30). The counts are therefore an upper bound: a whole-game test whose name covers `FR-34..FR-45` counts once for each of them.

```
python tools/rtm_coverage.py srs.txt test 15 45
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
| FR-35 | Correct answers shall move the piece forward 1 | 12 | EffectChainIndependentTest, EffectResolutionTest, LevelConfigTest, QuestionDialogTest, QuestionFactoryTest, SpecialDiceGameTest, SquareEffectTest |
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


**No FR from FR-15 to FR-45 is without a test.**

**The weak spots of 19/11 now have independent tests.** SDP 5.3.4 asks for every rule to be tested by a member who did not implement it. On 19/11 the special-square rules were tested only by their authors. In this snapshot `EffectChainIndependentTest` (T2-12, Rakan Ziyan, #95, 25 tests) names every FR from FR-32 to FR-42, except FR-33, which has independent board tests. The counts of the weak spots, 19/11 → 23/11:

| FR | Rule | 19/11 | 23/11 | Independent test |
|---|---|---:|---:|---|
| FR-32 | backward movement stops at the start square | 2 | 4 | `EffectChainIndependentTest` |
| FR-36 | feedback after an answer | 3 | 5 | `EffectChainIndependentTest` |
| FR-37 | no repeated question | 4 | 6 | `EffectChainIndependentTest`, `EffectsAcceptanceTest` (#103) |
| FR-38 | a capture on a question square comes first | 2 | 3 | `EffectChainIndependentTest` |
| FR-41 | effects obey the movement and blocking rules | 4 | 8 | `EffectChainIndependentTest` |
| FR-42 | chains, no reactivation | 12 | 16 | `EffectChainIndependentTest` |

This holds only once #95 is merged. On `iteration-2` alone the 19/11 picture still applies.

**One limit that stays.** Natural play never uses up a level's questions, so the FR-37 reshuffle is covered only by `QuestionFactoryTest` (found in #99).

## 3. What is left for the final check (05/12)
- Run the script again on the final `iteration-2` and replace the table above.
- Confirm that #95 is merged, so every FR from FR-32 to FR-42 has an independent test on the branch itself.
- Measure NFR-02 cold on the two release computers (N2-06) and add the result here.
