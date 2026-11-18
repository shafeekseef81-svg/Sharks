# Strategy: the dice of each level (Shafeek Saif)

Section for `iteration2_dp_Sharks` (task N2-05, #71). Pattern implemented in task T2-04 (#55, PR #76, merged 17/11/2026).

## 1. The pattern
**Strategy** (GoF, behavioural): a family of algorithms, each in its own class behind one interface. The object that uses an algorithm (the *context*) holds it through the interface, so the algorithm can be chosen from outside and swapped without changing the context.

## 2. The problem it solves
Each game level rolls different dice and turns them into a movement in a different way (FR-16, FR-17):

| Level | Dice rolled | Movement on the shared track |
|---|---|---|
| Easy | Regular (1-6) | the Regular value |
| Medium | Regular + Boost (1-3) | Regular × (Boost − 1): 0, 1 or 2 times the Regular value |
| Hard | Regular + UltraBoost (1-6) | Regular × (UltraBoost − 1): 0 to 5 times the Regular value |

A surprise square can also give an extra Regular + UltraBoost roll on *any* level (SRS Table 2), so the dice don't always follow the level of the game.

In Iteration 1 only Easy was playable: `RandomDice.roll` rolled the Regular die and refused Medium and Hard with an exception. The direct way to add the two levels would have been a `switch` on the level inside `RandomDice`. Then every new die, and the surprise roll, would change that class and its tests, and the formula would be mixed with the random generator.

**Why Strategy:** what changes from level to level is an *algorithm*: which dice are rolled and how their values become steps. The level chooses it from outside, and the surprise roll needs to swap it at run time. Strategy puts each set of dice in its own small class, the context has no level-specific code, and a new die is a new class with no change to the existing ones.

## 3. Structure

```
             «interface»
             DiceStrategy                          RandomDice  (context)
   ──────────────────────────────           ───────────────────────────────────
   specialDie(): DieType                     - random: Random
   movement(regular, special): int  ◄─────── + roll(level)  → rollWith(level.config().diceStrategy())
   roll(random): DiceRoll                    + rollWith(strategy) → strategy.roll(random)
        ▲                    ▲
        │                    │                     LevelConfig  (selects the strategy)
 RegularDiceStrategy   SpecialDiceStrategy         EASY   → new RegularDiceStrategy()
      (Easy)           «abstract»                  MEDIUM → new BoostDiceStrategy()
                       movement = R × (S − 1)      HARD   → new UltraBoostDiceStrategy()
                         ▲              ▲
                         │              │
               BoostDiceStrategy  UltraBoostDiceStrategy
                   (Medium)            (Hard)
```

- The two special dice share one formula, so `SpecialDiceStrategy` holds it once (`movement` and `roll` are `final`), and its subclasses only name their die.
- The strategies hold no state. `LevelConfig` keeps one shared instance per level, and the context passes in the game's random generator. A fixed seed therefore still makes a whole game repeatable, which the tests rely on.

## 4. Where it is in the code (`Iteration2/src/Model/`)

| Role | File |
|---|---|
| Strategy | `DiceStrategy.java` (interface; its Javadoc names the pattern and the roles) |
| Concrete strategies | `RegularDiceStrategy.java` (Easy), `BoostDiceStrategy.java` (Medium), `UltraBoostDiceStrategy.java` (Hard), with their common part in `SpecialDiceStrategy.java` |
| Context | `RandomDice.java`: `roll(level)` takes the strategy of the level and calls `rollWith(strategy)`, which lets the strategy roll with the game's generator. The class has no `if` or `switch` on the level. |
| Selection | `LevelConfig.java`: the static table of the three levels gives each level its strategy (`new RegularDiceStrategy()`, `new BoostDiceStrategy()`, `new UltraBoostDiceStrategy()`), and `diceStrategy()` returns it. `specialDie()` of a level is read from its strategy, so the dice of a level are defined in one place (NFR-19). |

The `Dice` interface, `DiceRoll`, the move engine, the Controller and the View didn't change. The pattern is internal to the model (MVC).

## 5. How it was tested
- `DiceStrategyTest` (10 tests) checks the pattern itself:
  - each level has its own strategy;
  - hand-computed formula examples, and the formula for every face of every die (a special 1 gives 0 steps);
  - values that are not on a die are refused;
  - the Regular die is drawn first, then the special die;
  - over 3,000 rolls per level, every face appears;
  - a seeded Easy game rolls exactly the Iteration 1 values;
  - the context uses the strategy of the level, and `rollWith` swaps the strategy at run time;
  - null arguments are refused.
- `SpecialDiceGameTest` (7 tests) plays through `GameModel` with the real dice:
  - a movement of 0;
  - Boost and UltraBoost moves along the colour's own route (FR-25);
  - an UltraBoost 6 keeps the extra roll (FR-21);
  - leaving the castle on a 6 with a special 1;
  - the Regular die on the home path (FR-18);
  - 48 complete Medium and Hard games with 2-4 players.
- Independent tests by Rakan Ziyan (T2-05, PR #78): die sizes, the formula and the routes, plus 10,000 draws per level for NFR-13. They were written from the SRS, not from this code.
- All Iteration 1 tests still pass (397/397 at the merge).

## 6. Use in the rest of Iteration 2
The surprise "extra Regular + UltraBoost roll" (T2-09, #60) doesn't need a new die or a change to the context. It can call `rollWith(new UltraBoostDiceStrategy())` on the game's `RandomDice`, whatever the level of the game.

*Screenshots for the Word version: `DiceStrategy.java` (interface with Javadoc), `SpecialDiceStrategy.movement`, the two methods of `RandomDice`, and the static level table in `LevelConfig`.*
