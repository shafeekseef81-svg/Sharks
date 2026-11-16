package Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Places the protection squares of a new board at random (FR-11, FR-12 partial). Task 08, owner: Rakan Ziyan.
 *
 * <p>Rules, from the SRS:
 * <ul>
 *   <li>protection squares lie on the shared track only (FR-12): never on a start square, an arrow or a home
 *       path square (FR-33: a home path has no special squares);</li>
 *   <li>they never overlap another special square (FR-12);</li>
 *   <li>a new random set is chosen for every game (FR-12); every free shared-track square has the same
 *       chance;</li>
 *   <li>the number comes from the level (SRS Table 1: Easy 4, Medium 8, Hard 12).</li>
 * </ul>
 * Question and surprise squares (also FR-12) are Iteration 2 and are not placed here.
 *
 * <p><b>Integration (Task 07, Shafeek Saif):</b> {@link BoardFactory} calls
 * {@link #place(int, Collection, Random)} once, after the start squares, arrows and home paths are set,
 * with the count read from the level configuration. This class does not depend on {@code BoardFactory} or
 * {@code LevelConfig}.
 */
public final class ProtectionSquarePlacer {

    private ProtectionSquarePlacer() {
    }

    /**
     * Places as many protection squares as the level requires ({@link Difficulty#protectionSquares()}).
     *
     * @see #place(int, Collection, Random)
     */
    public static List<Square> place(Difficulty level, Collection<Square> squares, Random random) {
        Objects.requireNonNull(level, "level");
        return place(level.protectionSquares(), squares, random);
    }

    /**
     * Turns {@code count} free shared-track squares, chosen at random, into protection squares.
     *
     * @param count   number of protection squares, 0 or more
     * @param squares all squares of the board being built
     * @param random  random source; a fixed seed gives the same board, which makes tests repeatable
     * @return the squares that became protection squares, in the order they were chosen
     * @throws IllegalArgumentException if {@code count} is negative or larger than the number of free
     *         shared-track squares; then no square is changed
     * @throws NullPointerException if an argument or a square is null
     */
    public static List<Square> place(int count, Collection<Square> squares, Random random) {
        Objects.requireNonNull(squares, "squares");
        Objects.requireNonNull(random, "random");
        if (count < 0) {
            throw new IllegalArgumentException("The number of protection squares cannot be negative: " + count);
        }
        List<Square> free = new ArrayList<>();
        for (Square s : squares) {
            if (isFree(Objects.requireNonNull(s, "square"))) {
                free.add(s);
            }
        }
        if (count > free.size()) {
            throw new IllegalArgumentException(
                    "Cannot place " + count + " protection squares on " + free.size() + " free track squares");
        }
        Collections.shuffle(free, random);
        List<Square> chosen = List.copyOf(free.subList(0, count));
        for (Square s : chosen) {
            s.setType(SquareType.PROTECTION);
        }
        return chosen;
    }

    /**
     * A square can become a protection square if it is a plain shared-track square: not a start square,
     * arrow, home path square or other special square, and not the tinted classic start cell of the
     * Medium/Hard boards (drawn in a player's colour).
     *
     * @param square a square of the board
     * @return {@code true} if a protection square may be placed there
     */
    public static boolean isFree(Square square) {
        return square.type() == SquareType.TRACK && square.ring() >= 0 && !square.isTinted();
    }
}
