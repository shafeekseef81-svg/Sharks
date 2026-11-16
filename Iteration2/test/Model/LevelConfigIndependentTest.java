package Model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Independent tests for Task 07 (LevelConfig and the Easy board factory; NFR-19, FR-10 P, FR-11 P).
 *
 * <p>Written by a member who did not write Task 07, as the Definition of Done asks (SDP 5.3.4). The author tests
 * in {@code LevelConfigTest} check the values against SRS Table 1; these tests check instead that the board
 * {@link BoardFactory} actually builds agrees with the values {@link LevelConfig} publishes, for many random
 * boards, so that the two cannot drift apart.
 *
 * <p>Owner: Rakan Ziyan.
 */
class LevelConfigIndependentTest {

    private static final int BOARDS = 200;

    @Test
    @DisplayName("T07 independent: every random Easy board matches LevelConfig (size, track, special squares)")
    void easyBoardsMatchTheConfiguration() {
        LevelConfig easy = LevelConfig.of(Difficulty.EASY);
        for (int seed = 0; seed < BOARDS; seed++) {
            Board board = BoardFactory.create(Difficulty.EASY, new Random(seed));
            String at = "seed " + seed;
            assertEquals(Difficulty.EASY, board.difficulty(), at);
            assertEquals(easy.gridSize(), board.size(), at);
            assertEquals(easy.totalTrackSquares(), board.trackSquareCount(), at);
            assertEquals(easy.startingSquares(), board.count(SquareType.START), at);
            assertEquals(easy.protectionSquares(), board.count(SquareType.PROTECTION), at);
            assertEquals(easy.arrowSquares(), board.count(SquareType.ARROW), at);
            assertEquals(easy.questionSquares(), board.count(SquareType.QUESTION), at);
            assertEquals(easy.surpriseSquares(), board.count(SquareType.SURPRISE), at);
            assertEquals(easy.homePathSquares(), board.finishIndex() - board.homeStartIndex() + 1, at);
        }
    }

    @Test
    @DisplayName("T07 independent: Easy is the classic board - one 52-square track, no outer rings, no special die")
    void easyIsTheClassicBoard() {
        LevelConfig easy = LevelConfig.of(Difficulty.EASY);
        assertEquals(15, easy.gridSize());
        assertEquals(0, easy.outerRings());
        assertArrayEquals(new int[] {52}, easy.trackSizes());
        assertEquals("52", easy.trackFormula());
        assertEquals(DieType.NONE, easy.specialDie());
    }

    @Test
    @DisplayName("T07 independent: the settings cannot be changed from outside (trackSizes is a copy)")
    void trackSizesIsADefensiveCopy() {
        LevelConfig medium = LevelConfig.of(Difficulty.MEDIUM);
        int[] first = medium.trackSizes();
        assertNotSame(first, medium.trackSizes());
        first[0] = 999;
        assertArrayEquals(new int[] {52, 64}, medium.trackSizes());
        assertEquals(116, medium.totalTrackSquares());
    }

    @Test
    @DisplayName("T07 independent: every player colour gets a path ending at the same finish index")
    void everyColourHasAFullPath() {
        Board board = BoardFactory.create(Difficulty.EASY, new Random(7));
        for (PlayerColor color : PlayerColor.values()) {
            assertEquals(board.finishIndex() + 1, board.path(color).size(), color.name());
        }
    }
}
