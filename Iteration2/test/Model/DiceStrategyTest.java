package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests of T2-04: the Strategy pattern for the dice of each level and the movement formula
 * (FR-16, FR-17). Owner: Shafeek Saif.
 *
 * <p>The independent tests of T2-05 (10,000 draws per die, formula, routes per colour and level) are written by
 * Rakan Ziyan, who did not write this code (SDP 5.3.4).
 */
class DiceStrategyTest {

    private static final DiceStrategy REGULAR = new RegularDiceStrategy();
    private static final DiceStrategy BOOST = new BoostDiceStrategy();
    private static final DiceStrategy ULTRA_BOOST = new UltraBoostDiceStrategy();

    @Test
    @DisplayName("FR-16, NFR-19: each level is configured with its own strategy; the special die comes from it")
    void eachLevelHasItsStrategy() {
        assertInstanceOf(RegularDiceStrategy.class, LevelConfig.of(Difficulty.EASY).diceStrategy());
        assertInstanceOf(BoostDiceStrategy.class, LevelConfig.of(Difficulty.MEDIUM).diceStrategy());
        assertInstanceOf(UltraBoostDiceStrategy.class, LevelConfig.of(Difficulty.HARD).diceStrategy());

        assertEquals(DieType.NONE, Difficulty.EASY.specialDie());
        assertEquals(DieType.BOOST, Difficulty.MEDIUM.specialDie());
        assertEquals(DieType.ULTRA_BOOST, Difficulty.HARD.specialDie());
        for (Difficulty level : Difficulty.values()) {
            LevelConfig config = LevelConfig.of(level);
            assertEquals(config.diceStrategy().specialDie(), config.specialDie(), level.label());
            assertSame(config.diceStrategy(), LevelConfig.of(level).diceStrategy(),
                    "one shared strategy per level: " + level.label());
        }
        assertEquals("Regular die", Difficulty.EASY.diceDescription());
        assertEquals("Regular + Boost dice", Difficulty.MEDIUM.diceDescription());
        assertEquals("Regular + UltraBoost dice", Difficulty.HARD.diceDescription());
    }

    @Test
    @DisplayName("FR-17: hand-computed examples - Regular alone, Regular x (Boost - 1), Regular x (UltraBoost - 1)")
    void movementExamples() {
        assertEquals(4, REGULAR.movement(4, 0));
        assertEquals(8, BOOST.movement(4, 3), "4 x (3 - 1)");
        assertEquals(5, BOOST.movement(5, 2), "5 x (2 - 1)");
        assertEquals(0, BOOST.movement(6, 1), "a Boost 1 gives a movement of 0");
        assertEquals(30, ULTRA_BOOST.movement(6, 6), "6 x (6 - 1), the largest movement");
        assertEquals(9, ULTRA_BOOST.movement(3, 4), "3 x (4 - 1)");
        assertEquals(0, ULTRA_BOOST.movement(1, 1));
    }

    @Test
    @DisplayName("FR-17: the formula holds for every face of every die; a special 1 always moves nothing")
    void movementForEveryFace() {
        for (int regular = 1; regular <= 6; regular++) {
            assertEquals(regular, REGULAR.movement(regular, 0));
            for (DiceStrategy s : List.of(BOOST, ULTRA_BOOST)) {
                assertEquals(0, s.movement(regular, 1), s + ", Regular " + regular);
                for (int special = 1; special <= s.specialDie().faces(); special++) {
                    assertEquals(regular * (special - 1), s.movement(regular, special),
                            s + ": " + regular + " x (" + special + " - 1)");
                }
            }
        }
    }

    @Test
    @DisplayName("Invalid input: a value that is not on its die is rejected by the formula")
    void movementRejectsImpossibleValues() {
        for (DiceStrategy s : List.of(REGULAR, BOOST, ULTRA_BOOST)) {
            assertThrows(IllegalArgumentException.class, () -> s.movement(0, 1), s.toString());
            assertThrows(IllegalArgumentException.class, () -> s.movement(7, 1), s.toString());
        }
        assertThrows(IllegalArgumentException.class, () -> REGULAR.movement(3, 2), "Easy has no special die");
        assertThrows(IllegalArgumentException.class, () -> BOOST.movement(3, 0));
        assertThrows(IllegalArgumentException.class, () -> BOOST.movement(3, 4), "the Boost die shows 1-3");
        assertThrows(IllegalArgumentException.class, () -> ULTRA_BOOST.movement(3, 0));
        assertThrows(IllegalArgumentException.class, () -> ULTRA_BOOST.movement(3, 7), "the UltraBoost die shows 1-6");
    }

    @Test
    @DisplayName("FR-16: a roll draws the Regular die first, then the special die of the strategy, with its faces")
    void rollDrawsTheRightDiceInOrder() {
        ScriptedRandom r = new ScriptedRandom(5);
        assertEquals(DiceRoll.regularOnly(5), REGULAR.roll(r));
        assertEquals(List.of(6), r.bounds(), "Easy draws one Regular die only");

        r = new ScriptedRandom(4, 3);
        assertEquals(new DiceRoll(4, DieType.BOOST, 3, 8, false, false), BOOST.roll(r));
        assertEquals(List.of(6, 3), r.bounds(), "Regular (6 faces), then Boost (3 faces)");

        r = new ScriptedRandom(6, 6);
        assertEquals(new DiceRoll(6, DieType.ULTRA_BOOST, 6, 30, false, false), ULTRA_BOOST.roll(r));
        assertEquals(List.of(6, 6), r.bounds(), "Regular (6 faces), then UltraBoost (6 faces)");

        r = new ScriptedRandom(2, 1);
        DiceRoll zero = ULTRA_BOOST.roll(r);
        assertEquals(0, zero.movement(), "FR-17: a special 1 gives a movement of 0");
        assertEquals(0, r.remaining());
    }

    @Test
    @DisplayName("FR-16: over 3,000 rolls per level every face appears, values stay on their dice, movement follows FR-17")
    void everyFaceAppears() {
        for (Difficulty level : Difficulty.values()) {
            RandomDice dice = new RandomDice(new Random(level.ordinal() + 17L));
            DieType special = level.specialDie();
            Set<Integer> regulars = new TreeSet<>();
            Set<Integer> specials = new TreeSet<>();
            int zeroMoves = 0;
            for (int i = 0; i < 3_000; i++) {
                DiceRoll roll = dice.roll(level);
                assertEquals(special, roll.specialType(), level.label());
                assertEquals(level.config().diceStrategy().movement(roll.regular(), roll.special()), roll.movement());
                regulars.add(roll.regular());
                specials.add(roll.special());
                zeroMoves += roll.movement() == 0 ? 1 : 0;
            }
            assertEquals(Set.of(1, 2, 3, 4, 5, 6), regulars, level.label());
            Set<Integer> expectedSpecials = new TreeSet<>();
            for (int f = 1; f <= special.faces(); f++) {
                expectedSpecials.add(f);
            }
            assertEquals(special == DieType.NONE ? Set.of(0) : expectedSpecials, specials, level.label());
            assertEquals(special != DieType.NONE, zeroMoves > 0, "movement 0 exists only with a special die");
        }
    }

    @Test
    @DisplayName("Repeatability: a seeded Easy game rolls exactly the values of Iteration 1 (one draw per roll)")
    void easyRollsUnchangedFromIteration1() {
        Random expected = new Random(2026);
        RandomDice dice = new RandomDice(new Random(2026));
        for (int i = 0; i < 500; i++) {
            assertEquals(DiceRoll.regularOnly(expected.nextInt(6) + 1), dice.roll(Difficulty.EASY));
        }
    }

    @Test
    @DisplayName("Context: RandomDice rolls with the strategy of the level it is given")
    void contextUsesTheLevelStrategy() {
        assertEquals(DiceRoll.regularOnly(2), new RandomDice(new ScriptedRandom(2)).roll(Difficulty.EASY));
        assertEquals(new DiceRoll(2, DieType.BOOST, 3, 4, false, false),
                new RandomDice(new ScriptedRandom(2, 3)).roll(Difficulty.MEDIUM));
        assertEquals(new DiceRoll(2, DieType.ULTRA_BOOST, 3, 4, false, false),
                new RandomDice(new ScriptedRandom(2, 3)).roll(Difficulty.HARD));
    }

    @Test
    @DisplayName("Context: rollWith swaps the strategy at run time and passes the game's own generator")
    void contextAcceptsAnyStrategy() {
        assertEquals(new DiceRoll(3, DieType.ULTRA_BOOST, 5, 12, false, false),
                new RandomDice(new ScriptedRandom(3, 5)).rollWith(ULTRA_BOOST),
                "an UltraBoost roll on demand, e.g. for a surprise on Medium");

        Random game = new Random(7);
        DiceRoll fixed = new DiceRoll(2, DieType.BOOST, 2, 2, false, false);
        List<Random> seen = new ArrayList<>();
        DiceStrategy stub = new DiceStrategy() {
            @Override
            public DieType specialDie() {
                return DieType.BOOST;
            }

            @Override
            public int movement(int regular, int special) {
                return regular * (special - 1);
            }

            @Override
            public DiceRoll roll(Random random) {
                seen.add(random);
                return fixed;
            }
        };
        assertSame(fixed, new RandomDice(game).rollWith(stub), "RandomDice adds no rule of its own");
        assertEquals(List.of(game), seen);
    }

    @Test
    @DisplayName("Invalid input: null level, strategy or generator, and a strategy that returns no roll")
    void nullArguments() {
        RandomDice dice = new RandomDice(new Random(1));
        assertThrows(NullPointerException.class, () -> dice.roll((Difficulty) null));
        assertThrows(NullPointerException.class, () -> dice.rollWith(null));
        for (DiceStrategy s : List.of(REGULAR, BOOST, ULTRA_BOOST)) {
            assertThrows(NullPointerException.class, () -> s.roll(null), s.toString());
        }
        DiceStrategy broken = new DiceStrategy() {
            @Override
            public DieType specialDie() {
                return DieType.NONE;
            }

            @Override
            public int movement(int regular, int special) {
                return regular;
            }

            @Override
            public DiceRoll roll(Random random) {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> dice.rollWith(broken));
        assertTrue(dice.roll(Difficulty.EASY).regular() >= 1);
    }
}
