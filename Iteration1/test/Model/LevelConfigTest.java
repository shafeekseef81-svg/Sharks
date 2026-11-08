package Model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link LevelConfig} against SRS Table 1 (Task 07). */
class LevelConfigTest {

    @Test
    @DisplayName("NFR-19: every Difficulty has a configuration")
    void everyLevelHasConfiguration() {
        for (Difficulty level : Difficulty.values()) {
            assertNotNull(LevelConfig.of(level), level.name());
            assertSame(LevelConfig.of(level), level.config(), level.name());
        }
    }

    @Test
    @DisplayName("NFR-19: a null level is refused")
    void nullLevelIsRefused() {
        assertThrows(NullPointerException.class, () -> LevelConfig.of(null));
    }

    @Test
    @DisplayName("Table 1, Easy: 52 track squares, 6 home squares, 4 start, 4 arrow, 4 protection, Regular die")
    void easyMatchesTable1() {
        LevelConfig easy = LevelConfig.of(Difficulty.EASY);
        assertEquals(15, easy.gridSize());
        assertEquals(0, easy.outerRings());
        assertArrayEquals(new int[] {52}, easy.trackSizes());
        assertEquals(52, easy.totalTrackSquares());
        assertEquals("52", easy.trackFormula());
        assertEquals(6, easy.homePathSquares());
        assertEquals(4, easy.startingSquares());
        assertEquals(4, easy.arrowSquares());
        assertEquals(4, easy.protectionSquares());
        assertEquals(0, easy.questionSquares());
        assertEquals(0, easy.questionsPerLevel());
        assertEquals(0, easy.surpriseSquares());
        assertEquals(DieType.NONE, easy.specialDie());
        assertEquals("Regular die", easy.diceDescription());
    }

    @Test
    @DisplayName("Table 1: the Medium and Hard values used by the setup screen are kept")
    void mediumAndHardMatchTable1() {
        LevelConfig medium = LevelConfig.of(Difficulty.MEDIUM);
        assertEquals("52 + 64 = 116", medium.trackFormula());
        assertEquals(8, medium.protectionSquares());
        assertEquals(8, medium.arrowSquares());
        assertEquals(6, medium.questionSquares());
        assertEquals(2, medium.questionsPerLevel());
        assertEquals(2, medium.surpriseSquares());
        assertEquals(DieType.BOOST, medium.specialDie());

        LevelConfig hard = LevelConfig.of(Difficulty.HARD);
        assertEquals("52 + 64 + 72 = 188", hard.trackFormula());
        assertEquals(12, hard.protectionSquares());
        assertEquals(12, hard.arrowSquares());
        assertEquals(9, hard.questionSquares());
        assertEquals(3, hard.questionsPerLevel());
        assertEquals(3, hard.surpriseSquares());
        assertEquals(DieType.ULTRA_BOOST, hard.specialDie());
    }

    @Test
    @DisplayName("FR-35: a question moves a piece 1, 2 or 3 steps by level")
    void questionStepsByLevel() {
        assertEquals(1, LevelConfig.of(Difficulty.EASY).questionSteps());
        assertEquals(2, LevelConfig.of(Difficulty.MEDIUM).questionSteps());
        assertEquals(3, LevelConfig.of(Difficulty.HARD).questionSteps());
    }

    @Test
    @DisplayName("NFR-19: changing the returned track sizes does not change the configuration")
    void trackSizesAreCopied() {
        LevelConfig.of(Difficulty.EASY).trackSizes()[0] = 99;
        assertEquals(52, LevelConfig.of(Difficulty.EASY).trackSizes()[0]);
    }

    @Test
    @DisplayName("NFR-19: Difficulty keeps its accessors and reads every value from LevelConfig")
    void difficultyReadsFromConfig() {
        for (Difficulty level : Difficulty.values()) {
            LevelConfig config = LevelConfig.of(level);
            assertEquals(config.gridSize(), level.gridSize());
            assertEquals(config.outerRings(), level.outerRings());
            assertArrayEquals(config.trackSizes(), level.trackSizes());
            assertEquals(config.totalTrackSquares(), level.totalTrackSquares());
            assertEquals(config.trackFormula(), level.trackFormula());
            assertEquals(config.protectionSquares(), level.protectionSquares());
            assertEquals(config.arrowSquares(), level.arrowSquares());
            assertEquals(config.questionSquares(), level.questionSquares());
            assertEquals(config.questionsPerLevel(), level.questionsPerLevel());
            assertEquals(config.surpriseSquares(), level.surpriseSquares());
            assertEquals(config.specialDie(), level.specialDie());
            assertEquals(config.questionSteps(), level.questionSteps());
            assertEquals(config.diceDescription(), level.diceDescription());
        }
    }

    @Test
    @DisplayName("Difficulty stays the level identifier: EASY, MEDIUM, HARD with their labels")
    void difficultyIdentifiesTheLevels() {
        assertArrayEquals(new Difficulty[] {Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD}, Difficulty.values());
        assertEquals("Easy", Difficulty.EASY.label());
        assertEquals("Medium", Difficulty.MEDIUM.label());
        assertEquals("Hard", Difficulty.HARD.label());
    }
}
