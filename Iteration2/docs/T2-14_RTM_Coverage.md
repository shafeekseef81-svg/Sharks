# T2-14: timing tests and RTM coverage check (snapshot 19/11)

Task **T2-14** · Issue [#65](https://github.com/shafeekseef81-svg/Sharks/issues/65) · SRS **NFR-02, NFR-04, NFR-14** · Owner: Shafeek Saif · Reviewer: Rakan Ziyan

**This is a snapshot, not the final check.** The final check runs on `iteration-2` after the last merge (05/12) with the same script. The snapshot was measured on `iteration-2` 6e13bcf with every open code PR merged locally: #79, #82, #83, #85, #87, #89, #90 and #91, plus T2-10 part 2 and this task. Result: **641/641** tests, no conflicts.

## 1. Timing (NFR-02, NFR-04): `Iteration2/test/View/TimingTest.java`

| Requirement | What is measured | Measured 19/11 | Limit |
|---|---|---|---|
| NFR-02 | The game is started (model and random board with protection, question and surprise squares), then the screen is laid out and painted. Hard, 4 players, 21 boards. | worst **78 ms** | 2000 ms |
| NFR-02 | The same for Easy and Medium, 4 players, 10 boards each. | under 0.1 s | 2000 ms |
| NFR-04 | The longest Hard move: Regular 6 × (UltraBoost 6 − 1) = 30 squares, animated square by square. | **1702 ms** | 2000 ms |
| NFR-04 | The surprise jumps to the castle and to the finish. | 956 / 830 ms | 2000 ms |

**Risk (for the team):** the 30-square move passes, but with only about 300 ms to spare. `BoardComponent.animateAlong` allows at most 1600 ms per move (`min(1600, steps × 120)`), and a slow computer adds timer delay on top. Lowering the cap to about 1200 ms (40 ms per square at 30 squares) would give a safe margin. This is the animation code of Task 19 (Mohamed), so it is a proposal, not a change made here.

A cold start (the first game after starting the program) can't be measured inside the test run, because other tests have already loaded the classes. It is checked by hand on the two release computers (N2-06).

## 2. RTM coverage of the movement and special-square rules (NFR-14)

**Method.** `Iteration2/tools/rtm_coverage.py` reads the FR table of `Assignment3_Prototype/Sharks_SRS.pdf` (as text from `pdftotext -layout`). It then counts every JUnit `@DisplayName` that names an FR, alone or in a range (`FR-28..FR-30` counts for 28, 29 and 30). The counts are therefore an upper bound: a whole-game test whose name covers `FR-34..FR-45` counts once for each of them.

```
python tools/rtm_coverage.py srs.txt test 15 45
```

| FR | Requirement (SRS, first line) | Tests | Test classes |
|---|---|---:|---|
| FR-15 | The system shall enforce the setup turn order and | 10 | GameControllerTest, TurnAcceptanceTest, TurnOrderTest, TurnSequenceIndependentTest |
| FR-16 | Dice results shall be uniformly random: Regular | 17 | DiceRouteIndependentTest, DiceStrategyTest, GameSetupViewTest, RegularDiceTest, SpecialDiceGameTest, TurnAcceptanceTest, TurnOrderTest, TurnSequenceIndependentTest |
| FR-17 | Movement shall equal the Regular die in Easy, or | 22 | DiceRouteIndependentTest, DiceStrategyTest, RegularDiceTest, SpecialDiceGameTest, StandardHomePathRulesTest, TurnAcceptanceTest, TurnSequenceIndependentTest |
| FR-18 | A piece on its home path shall move by the | 7 | CaptureProtectionFinishTest, MovementAcceptanceTest, SpecialDiceGameTest, StandardHomePathRulesTest |
| FR-19 | A Regular 6 shall allow placing a piece on its | 25 | DiceRouteIndependentTest, DiceStrategyTest, DoubleBlockEngineTest, DoubleBlockIndependentTest, EasyBoardTest, GameModelSkeletonTest, LevelConfigTest, MoveEngineIndependentTest, MovementAcceptanceTest, SpecialDiceGameTest, StandardHomePathRulesTest, StandardMoveEngineTest, TurnStateTest |
| FR-20 | Each roll shall move exactly one piece; steps shall | 21 | DoubleBlockEngineTest, GameControllerTest, MoveEngineIndependentTest, MovementAcceptanceTest, MovementAnimationTest, MvcDependencyTest, RuleOfSixTest, StandardMoveEngineTest, TurnSequenceIndependentTest, TurnStateTest |
| FR-21 | A Regular 6 shall grant an extra roll even if | 19 | DoubleBlockIndependentTest, EffectResolutionTest, FullGameAcceptanceTest, GameControllerTest, GameModelSkeletonTest, MovementAcceptanceTest, RuleOfSixTest, SpecialDiceGameTest, TurnAcceptanceTest, TurnSequenceIndependentTest, TurnStateTest |
| FR-22 | Three consecutive Regular 6s shall cancel the | 16 | CaptureProtectionFinishTest, GameModelSkeletonTest, MoveEngineIndependentTest, RuleOfSixTest, StandardHomePathRulesTest, StandardMoveEngineTest, TurnAcceptanceTest, TurnSequenceIndependentTest, TurnStateTest |
| FR-23 | If no legal move is available and no additional roll | 23 | BoardComponentRenderTest, CaptureProtectionFinishTest, DoubleBlockIndependentTest, GameControllerTest, GameModelSkeletonTest, MainMenuViewTest, MediumHardBoardRenderTest, RuleOfSixTest, StandardMoveEngineTest, TurnAcceptanceTest, TurnSequenceIndependentTest, TurnStateTest |
| FR-24 | After a roll, the system shall highlight the pieces | 15 | GameClockTest, GameControllerTest, LegalMoveHighlightTest, MainMenuViewTest, TurnAcceptanceTest |
| FR-25 | Pieces shall move clockwise, follow only arrows of | 23 | DiceRouteIndependentTest, EasyBoardTest, MediumHardBoardIndependentTest, MoveEngineIndependentTest, MovementAcceptanceTest, SpecialDiceGameTest, StandardMoveEngineTest, StandardSquareRulesTest |
| FR-26 | Landing on an unprotected square with one | 26 | CaptureProtectionFinishTest, DoubleBlockEngineTest, MoveEngineIndependentTest, MovementAcceptanceTest, SquareEffectTest, StandardMoveEngineTest, StandardSquareRulesTest |
| FR-27 | Protection, starting, surprise, and finishing squares | 15 | CaptureProtectionFinishTest, EasyBoardTest, MovementAcceptanceTest, SquareEffectTest, StandardMoveEngineTest, StandardSquareRulesTest |
| FR-28 | Two or more same-player pieces on one square | 12 | DoubleBlockEngineTest, DoubleBlockIndependentTest, SquareEffectTest |
| FR-29 | An opponent's Double Block shall prevent landing | 21 | DoubleBlockEngineTest, DoubleBlockIndependentTest, MovementAcceptanceTest, SquareEffectTest |
| FR-30 | A Double Block shall remain while two pieces | 6 | DoubleBlockEngineTest, DoubleBlockIndependentTest |
| FR-31 | Finishing shall require exact steps; if a move | 20 | CaptureProtectionFinishTest, DoubleBlockEngineTest, MoveEngineIndependentTest, MovementAcceptanceTest, SquareEffectTest, StandardHomePathRulesTest, StandardMoveEngineTest |
| FR-32 | Backward movement caused by questions or | 2 | EffectResolutionTest, SquareEffectTest |
| FR-33 | A home path shall contain no special squares and | 15 | BoardProtectionIntegrationTest, CaptureProtectionFinishTest, DiceRouteIndependentTest, EasyBoardAcceptanceTest, EasyBoardTest, MediumHardBoardIndependentTest, MoveEngineIndependentTest, MovementAcceptanceTest, StandardHomePathRulesTest, StandardMoveEngineTest, StandardSquareRulesTest |
| FR-34 | Landing on a question square shall display to all | 11 | EffectResolutionTest, GameControllerTest, QuestionDialogTest, QuestionFactoryTest, SpecialDiceGameTest, SquareEffectTest |
| FR-35 | Correct answers shall move the piece forward 1 | 10 | EffectResolutionTest, LevelConfigTest, QuestionDialogTest, QuestionFactoryTest, SpecialDiceGameTest, SquareEffectTest |
| FR-36 | After an answer, the system shall show whether it | 3 | QuestionDialogTest, SpecialDiceGameTest |
| FR-37 | A question shall not repeat in the same game until | 4 | QuestionFactoryTest, SpecialDiceGameTest |
| FR-38 | Capturing on an unprotected question square shall | 2 | SpecialDiceGameTest, SquareEffectTest |
| FR-39 | Landing on a surprise square shall trigger exactly | 19 | EffectResolutionTest, GameControllerTest, SpecialDiceGameTest, SurpriseDialogTest, SurpriseFactoryTest, SurpriseOutcomeTest |
| FR-40 | For "move an opponent", the active player shall | 28 | EffectResolutionTest, SpecialDiceGameTest, SurpriseDialogTest, SurpriseFactoryTest |
| FR-41 | Question and surprise movements shall obey | 4 | SpecialDiceGameTest, SquareEffectTest |
| FR-42 | Question or surprise movement shall activate any | 12 | EffectResolutionTest, SpecialDiceGameTest, SquareEffectTest, SurpriseDialogTest, SurpriseFactoryTest |
| FR-43 | A player shall finish when all four pieces reach the | 15 | EffectResolutionTest, FullGameAcceptanceTest, GameControllerTest, GameEndRankingTest, MoveEngineIndependentTest, RuleOfSixTest, SpecialDiceGameTest, StandardHomePathRulesTest, StandardMoveEngineTest, StandardRankingTrackerTest, TurnOrderTest |
| FR-44 | The game shall end when all but one player have | 18 | EffectResolutionTest, FullGameAcceptanceTest, GameClockTest, GameControllerTest, GameEndRankingTest, MoveEngineIndependentTest, RuleOfSixTest, SpecialDiceGameTest, StandardMoveEngineTest, StandardRankingTrackerTest, TurnOrderTest, TurnStateTest |
| FR-45 | Rankings shall follow the order in which players | 9 | FullGameAcceptanceTest, GameControllerTest, GameEndRankingTest, SpecialDiceGameTest, StandardRankingTrackerTest |

**No FR from FR-15 to FR-45 is without a test.**

**Weak spots for T2-12 (independent tests, Rakan Ziyan, due 02/12).** SDP 5.3.4 asks for every rule to be tested by a member who did not implement it. Today the special-square rules are tested only by their authors: T2-08 and T2-09 by Mohamed, T2-10 by Shafeek. Priorities:
1. **FR-32** (backward movement stops at the start square): 2 tests.
2. **FR-38** (a capture on a question square comes first and stays): 2 tests.
3. **FR-41** (effects obey the movement and blocking rules) and **FR-42** (chains, no reactivation with zero steps): 4 and 12 tests, all by the authors.
4. **FR-36, FR-37** (feedback after an answer, no repeated question): 3 and 4 tests.

## 3. What is left for the final check (05/12)
- Run the script again on the final `iteration-2` and replace the table above.
- Add the independent tests of T2-12 to the list, and confirm that every FR from FR-32 to FR-42 has one.
- Measure NFR-02 cold on the two release computers (N2-06) and add the result here.
