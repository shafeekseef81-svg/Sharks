package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests for Task 11: the Regular die and the roll value object. The independent turn and dice tests of
 * Task 14 are written by Shafeek Saif.
 *
 * <p>Owner: Rakan Ziyan.
 */
class RegularDiceTest {

    /** A Random that always returns the same value from {@code nextInt(bound)}, to test the edges of the die. */
    private static final class StuckRandom extends Random {
        private final int value;

        StuckRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }

    @Test
    @DisplayName("FR-16/FR-17: an Easy roll is a Regular die of 1-6, no special die, movement = Regular value")
    void easyRollShape() {
        RandomDice dice = new RandomDice(new Random(11));
        for (int i = 0; i < 1_000; i++) {
            DiceRoll r = dice.roll(Difficulty.EASY);
            assertTrue(r.regular() >= 1 && r.regular() <= 6, "value " + r.regular());
            assertEquals(DieType.NONE, r.specialType());
            assertEquals(0, r.special());
            assertEquals(r.regular(), r.movement());
            assertFalse(r.hasSpecial());
            assertFalse(r.cancelled());
            assertFalse(r.surpriseRoll());
        }
    }

    @Test
    @DisplayName("FR-16: the lowest and highest random values map to 1 and 6")
    void dieEdges() {
        assertEquals(1, new RandomDice(new StuckRandom(0)).roll(Difficulty.EASY).regular());
        assertEquals(6, new RandomDice(new StuckRandom(5)).roll(Difficulty.EASY).regular());
    }

    @Test
    @DisplayName("NFR-13: in 10,000 rolls every value 1-6 is within 2 percentage points of 1/6 (5 seeds)")
    void nfr13Uniform() {
        for (long seed = 1; seed <= 5; seed++) {
            RandomDice dice = new RandomDice(new Random(seed));
            int[] counts = new int[7];
            int n = 10_000;
            for (int i = 0; i < n; i++) {
                counts[dice.roll(Difficulty.EASY).regular()]++;
            }
            for (int v = 1; v <= 6; v++) {
                double share = counts[v] / (double) n;
                assertTrue(Math.abs(share - 1.0 / 6) <= 0.02,
                        "seed " + seed + ", value " + v + ": " + String.format("%.4f", share));
            }
        }
    }

    @Test
    @DisplayName("Repeatable: the same seed gives the same sequence of rolls")
    void sameSeedSameRolls() {
        RandomDice a = new RandomDice(new Random(2026));
        RandomDice b = new RandomDice(new Random(2026));
        for (int i = 0; i < 200; i++) {
            assertEquals(a.roll(Difficulty.EASY), b.roll(Difficulty.EASY));
        }
    }

    @Test
    @DisplayName("Iteration 1 scope: Medium and Hard rolls (Boost, UltraBoost) are refused, not invented")
    void specialDiceAreIteration2() {
        RandomDice dice = new RandomDice(new Random(1));
        assertThrows(UnsupportedOperationException.class, () -> dice.roll(Difficulty.MEDIUM));
        assertThrows(UnsupportedOperationException.class, () -> dice.roll(Difficulty.HARD));
    }

    @Test
    @DisplayName("Invalid input: null random generator and null level are rejected")
    void nullArguments() {
        assertThrows(NullPointerException.class, () -> new RandomDice(null));
        assertThrows(NullPointerException.class, () -> new RandomDice(new Random()).roll(null));
    }

    @Test
    @DisplayName("FR-16/FR-17: DiceRoll rejects impossible values")
    void diceRollValidation() {
        assertEquals(1, DiceRoll.regularOnly(1).movement());
        assertEquals(6, DiceRoll.regularOnly(6).movement());
        assertThrows(IllegalArgumentException.class, () -> DiceRoll.regularOnly(0));
        assertThrows(IllegalArgumentException.class, () -> DiceRoll.regularOnly(7));
        assertThrows(IllegalArgumentException.class,
                () -> new DiceRoll(3, DieType.NONE, 0, 4, false, false), "movement must equal the Regular die");
        assertThrows(IllegalArgumentException.class,
                () -> new DiceRoll(3, DieType.NONE, 2, 3, false, false), "no special value without a special die");
        assertThrows(IllegalArgumentException.class,
                () -> new DiceRoll(3, DieType.REGULAR, 3, 6, false, false), "the special die is not a Regular die");
        assertThrows(NullPointerException.class, () -> new DiceRoll(3, null, 0, 3, false, false));
    }

    @Test
    @DisplayName("FR-17: with a special die the movement is Regular x (special - 1); 0 is allowed")
    void diceRollFormulaWithSpecialDie() {
        assertEquals(0, new DiceRoll(5, DieType.BOOST, 1, 0, false, false).movement());
        assertEquals(10, new DiceRoll(5, DieType.BOOST, 3, 10, false, false).movement());
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(5, DieType.BOOST, 4, 15, false, false));
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(5, DieType.BOOST, 2, 10, false, false));
    }
}
