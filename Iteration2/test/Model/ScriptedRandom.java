package Model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Test helper: a {@link Random} whose {@code nextInt(bound)} returns scripted die faces (1..bound) and records the
 * bound of every draw, so a test can fix both dice of a roll and check which dice were rolled, in which order.
 *
 * <p>Owner: Shafeek Saif (T2-04).
 */
final class ScriptedRandom extends Random {

    private final Deque<Integer> faces = new ArrayDeque<>();
    private final List<Integer> bounds = new ArrayList<>();

    /** @param faces the die faces to return, in order */
    ScriptedRandom(int... faces) {
        for (int f : faces) {
            this.faces.add(f);
        }
    }

    @Override
    public int nextInt(int bound) {
        bounds.add(bound);
        if (faces.isEmpty()) {
            throw new IllegalStateException("No scripted face left (draw of a die with " + bound + " faces)");
        }
        int face = faces.poll();
        if (face < 1 || face > bound) {
            throw new IllegalArgumentException("Face " + face + " is not on a die with " + bound + " faces");
        }
        return face - 1;
    }

    /** @return the number of faces of every die drawn so far, in order */
    List<Integer> bounds() {
        return List.copyOf(bounds);
    }

    /** @return how many scripted faces are left */
    int remaining() {
        return faces.size();
    }
}
