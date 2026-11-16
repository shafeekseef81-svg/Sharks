package Model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Test helper: dice that return a fixed sequence of Regular-die values, so rule tests are repeatable
 * (SDP 6.2; used by Tasks 14, 20, 21 and 25). Easy level only: no special die, so movement = Regular die.
 *
 * <p>Test infrastructure from Phase A; owner Rakan Ziyan.
 */
public final class FixedDice implements Dice {

    private final Deque<Integer> values = new ArrayDeque<>();

    /** @param regularValues the Regular-die values to return, in order (each 1-6) */
    public FixedDice(int... regularValues) {
        for (int v : regularValues) {
            if (v < 1 || v > 6) {
                throw new IllegalArgumentException("A Regular die shows 1-6, not " + v);
            }
            values.add(v);
        }
    }

    @Override
    public DiceRoll roll(Difficulty level) {
        if (values.isEmpty()) {
            throw new IllegalStateException("FixedDice has no more values");
        }
        int regular = values.poll();
        return new DiceRoll(regular, DieType.NONE, 0, regular, false, false);
    }

    /** @return how many values are left */
    public int remaining() {
        return values.size();
    }
}
