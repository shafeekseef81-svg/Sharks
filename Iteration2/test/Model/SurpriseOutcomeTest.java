package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests of T2-09, part 1: the six surprise outcomes and their probabilities (SRS FR-39).
 * Owner: Mohamed Zereny.
 *
 * <p>The probabilities checked here are the ones the Assignment 3 prototype recorded as SRS Table 2; the SRS
 * table itself is not in the repository, so these tests pin down the values the team is building on rather
 * than verifying them against the approved document.
 *
 * <p>The independent tests of this rule are T2-12 (issue #63, Rakan Ziyan), who did not write this code
 * (SDP 5.3.4). These tests do not replace them.
 */
class SurpriseOutcomeTest {

    // ------------------------------------------------------------------ the probability table (FR-39)

    @Test
    @DisplayName("FR-39: the six outcomes exist, each with a positive chance")
    void sixOutcomesWithPositiveChances() {
        assertEquals(6, SurpriseOutcome.values().length);
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            assertTrue(outcome.percent() > 0, outcome + " must have a positive chance");
            assertFalse(outcome.title().isBlank(), outcome + " needs a title");
            assertFalse(outcome.description().isBlank(), outcome + " needs a description");
        }
    }

    @Test
    @DisplayName("FR-39: the probabilities add up to exactly 100%")
    void probabilitiesAddUpToOneHundred() {
        int sum = 0;
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            sum += outcome.percent();
        }
        assertEquals(100, sum);
        assertEquals(SurpriseOutcome.PROBABILITY_TOTAL, sum);
    }

    @Test
    @DisplayName("FR-39: each outcome keeps the chance of the prototype's Table 2")
    void eachChanceMatchesTableTwo() {
        Map<SurpriseOutcome, Integer> expected = new EnumMap<>(SurpriseOutcome.class);
        expected.put(SurpriseOutcome.JUMP_TO_FINISH, 10);
        expected.put(SurpriseOutcome.OPPONENT_BACK, 10);
        expected.put(SurpriseOutcome.BACK_TO_CASTLE, 10);
        expected.put(SurpriseOutcome.BACK_THREE, 30);
        expected.put(SurpriseOutcome.SKIP_TURN, 30);
        expected.put(SurpriseOutcome.EXTRA_ROLL, 10);
        assertEquals(expected.size(), SurpriseOutcome.values().length);
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            assertEquals(expected.get(outcome), outcome.percent(), outcome.name());
        }
    }

    @Test
    @DisplayName("FR-39: every point of 0..99 maps to exactly one outcome, each getting its own share")
    void everyPercentileIsCoveredOnce() {
        Map<SurpriseOutcome, Integer> counted = new EnumMap<>(SurpriseOutcome.class);
        for (int percentile = 0; percentile < 100; percentile++) {
            SurpriseOutcome outcome = SurpriseOutcome.at(percentile);
            counted.merge(outcome, 1, Integer::sum);
        }
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            assertEquals(outcome.percent(), counted.get(outcome), outcome.name());
        }
    }

    @Test
    @DisplayName("FR-39: the exact boundary of every outcome's share")
    void exactBoundariesOfEveryShare() {
        assertSame(SurpriseOutcome.JUMP_TO_FINISH, SurpriseOutcome.at(0));
        assertSame(SurpriseOutcome.JUMP_TO_FINISH, SurpriseOutcome.at(9));
        assertSame(SurpriseOutcome.OPPONENT_BACK, SurpriseOutcome.at(10));
        assertSame(SurpriseOutcome.OPPONENT_BACK, SurpriseOutcome.at(19));
        assertSame(SurpriseOutcome.BACK_TO_CASTLE, SurpriseOutcome.at(20));
        assertSame(SurpriseOutcome.BACK_TO_CASTLE, SurpriseOutcome.at(29));
        assertSame(SurpriseOutcome.BACK_THREE, SurpriseOutcome.at(30));
        assertSame(SurpriseOutcome.BACK_THREE, SurpriseOutcome.at(59));
        assertSame(SurpriseOutcome.SKIP_TURN, SurpriseOutcome.at(60));
        assertSame(SurpriseOutcome.SKIP_TURN, SurpriseOutcome.at(89));
        assertSame(SurpriseOutcome.EXTRA_ROLL, SurpriseOutcome.at(90));
        assertSame(SurpriseOutcome.EXTRA_ROLL, SurpriseOutcome.at(99));
    }

    @Test
    @DisplayName("A point outside 0..99 is refused instead of silently giving an outcome")
    void percentileOutsideTheRangeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> SurpriseOutcome.at(-1));
        assertThrows(IllegalArgumentException.class, () -> SurpriseOutcome.at(100));
        assertThrows(IllegalArgumentException.class, () -> SurpriseOutcome.at(Integer.MAX_VALUE));
    }

    // ------------------------------------------------------------------ the draw

    @Test
    @DisplayName("FR-39: the draw reads one value of the 0..99 table and nothing else")
    void drawReadsOneHundredSidedValue() {
        ScriptRandom random = new ScriptRandom(30);
        assertSame(SurpriseOutcome.BACK_THREE, SurpriseOutcome.draw(random));
        assertEquals(List.of(100), random.bounds());
    }

    @Test
    @DisplayName("FR-39: the draw gives the outcome of the drawn point, at every boundary")
    void drawFollowsTheTable() {
        int[] points = {0, 9, 10, 19, 20, 29, 30, 59, 60, 89, 90, 99};
        for (int point : points) {
            assertSame(SurpriseOutcome.at(point), SurpriseOutcome.draw(new ScriptRandom(point)),
                    "point " + point);
        }
    }

    @Test
    @DisplayName("NFR-13: the same seed draws the same surprises again")
    void sameSeedRepeatsTheDraws() {
        List<SurpriseOutcome> first = drawMany(new Random(2026), 200);
        List<SurpriseOutcome> second = drawMany(new Random(2026), 200);
        assertEquals(first, second);
    }

    @Test
    @DisplayName("Different seeds do not all draw the same outcome, so the draw really uses the source")
    void differentSeedsDiffer() {
        assertFalse(drawMany(new Random(1), 50).equals(drawMany(new Random(99), 50)));
    }

    @Test
    @DisplayName("FR-39: over many draws every outcome appears roughly as often as its chance")
    void frequenciesAreCloseToTheChances() {
        Map<SurpriseOutcome, Integer> counted = new EnumMap<>(SurpriseOutcome.class);
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            counted.put(outcome, 0);
        }
        int draws = 60_000;
        Random random = new Random(7);
        for (int i = 0; i < draws; i++) {
            counted.merge(SurpriseOutcome.draw(random), 1, Integer::sum);
        }
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            double share = 100.0 * counted.get(outcome) / draws;
            assertTrue(Math.abs(share - outcome.percent()) < 1.0,
                    outcome + " came up " + share + "% instead of about " + outcome.percent() + "%");
        }
    }

    @Test
    @DisplayName("The draw needs a random source")
    void drawNeedsARandomSource() {
        assertThrows(NullPointerException.class, () -> SurpriseOutcome.draw(null));
    }

    // ------------------------------------------------------------------ what each outcome acts on

    @Test
    @DisplayName("Every outcome acts on the own piece, on an opponent piece or on the turn: never two of them")
    void everyOutcomeHasExactlyOneKindOfSubject() {
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            int kinds = (outcome.needsOwnPiece() ? 1 : 0)
                    + (outcome.needsOpponentPiece() ? 1 : 0)
                    + (outcome.affectsTurnOnly() ? 1 : 0);
            assertEquals(1, kinds, outcome + " must belong to exactly one kind");
        }
    }

    @Test
    @DisplayName("The three outcomes that move the own piece, the one that pushes an opponent, the two turn ones")
    void theKindsOfTheSixOutcomes() {
        assertTrue(SurpriseOutcome.JUMP_TO_FINISH.needsOwnPiece());
        assertTrue(SurpriseOutcome.BACK_TO_CASTLE.needsOwnPiece());
        assertTrue(SurpriseOutcome.BACK_THREE.needsOwnPiece());
        assertTrue(SurpriseOutcome.OPPONENT_BACK.needsOpponentPiece());
        assertFalse(SurpriseOutcome.OPPONENT_BACK.needsOwnPiece());
        assertTrue(SurpriseOutcome.SKIP_TURN.affectsTurnOnly());
        assertTrue(SurpriseOutcome.EXTRA_ROLL.affectsTurnOnly());
        assertEquals(3, SurpriseOutcome.BACK_THREE_STEPS);
    }

    // ------------------------------------------------------------------ helpers

    private static List<SurpriseOutcome> drawMany(Random random, int count) {
        List<SurpriseOutcome> drawn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            drawn.add(SurpriseOutcome.draw(random));
        }
        return drawn;
    }

    /**
     * A {@link Random} that returns scripted raw values from {@code nextInt(bound)} and records every bound,
     * so a test can fix a draw exactly and check which die was rolled. Unlike the die-face helper of T2-04,
     * the values here are the raw 0-based results, which is how the 0..99 probability table reads.
     */
    static final class ScriptRandom extends Random {

        private static final long serialVersionUID = 1L;

        private final transient Deque<Integer> values = new ArrayDeque<>();
        private final transient List<Integer> bounds = new ArrayList<>();

        ScriptRandom(int... values) {
            for (int value : values) {
                this.values.add(value);
            }
        }

        @Override
        public int nextInt(int bound) {
            bounds.add(bound);
            if (values.isEmpty()) {
                throw new IllegalStateException("No scripted value left (draw with bound " + bound + ")");
            }
            int value = values.poll();
            if (value < 0 || value >= bound) {
                throw new IllegalArgumentException("Value " + value + " is not 0.." + (bound - 1));
            }
            return value;
        }

        /** @return the bound of every draw so far, in order */
        List<Integer> bounds() {
            return List.copyOf(bounds);
        }

        /** @return how many scripted values are left */
        int remaining() {
            return values.size();
        }
    }
}
