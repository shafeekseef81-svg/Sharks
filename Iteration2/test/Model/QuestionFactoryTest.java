package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Developer tests of T2-08: the Factory Method that makes a question of a square's level, and the steps a
 * right or wrong answer moves a piece (SRS FR-34, FR-35, FR-36, FR-37). Owner: Mohamed Zereny.
 *
 * <p>The independent tests of this rule are T2-12 (issue #63, Rakan Ziyan), who did not write this code
 * (SDP 5.3.4). These tests do not replace them.
 */
class QuestionFactoryTest {

    /** A small bank with known questions, so a test can count what it asked. */
    private static QuestionBank bankOf(int easy, int medium, int hard) {
        List<Question> list = new ArrayList<>();
        int n = 0;
        for (Difficulty level : Difficulty.values()) {
            int count = switch (level) {
                case EASY -> easy;
                case MEDIUM -> medium;
                case HARD -> hard;
            };
            for (int i = 0; i < count; i++) {
                n++;
                list.add(new Question("T-" + n, level, level.label() + " question " + i, i % 4,
                        "A" + n, "B" + n, "C" + n, "D" + n));
            }
        }
        return new QuestionBank(list);
    }

    private static Map<Difficulty, QuestionFactory> factories(QuestionBank bank, long seed) {
        return QuestionFactory.forBank(bank, new Random(seed));
    }

    // ---------------------------------------------------------------- the pattern itself

    @Test
    @DisplayName("T2-08 Factory Method: a Creator declares the factory method and each level overrides it")
    void theCreatorAndItsConcreteCreators() throws NoSuchMethodException {
        assertTrue(Modifier.isAbstract(QuestionFactory.class.getModifiers()),
                "the Creator must be abstract: it does not know which question to make");
        Method factoryMethod = QuestionFactory.class.getDeclaredMethod("createQuestion");
        assertTrue(Modifier.isAbstract(factoryMethod.getModifiers()),
                "createQuestion must be the abstract factory method");
        assertTrue(Modifier.isProtected(factoryMethod.getModifiers()),
                "the factory method is for the subclasses, not for the game");
        assertEquals(Question.class, factoryMethod.getReturnType(), "the factory method returns the Product");

        for (Class<?> creator : List.of(EasyQuestionFactory.class, MediumQuestionFactory.class,
                HardQuestionFactory.class)) {
            assertEquals(QuestionFactory.class, creator.getSuperclass(),
                    creator.getSimpleName() + " must be a concrete creator of the pattern");
            assertNotNull(creator.getDeclaredMethod("createQuestion"),
                    creator.getSimpleName() + " must override the factory method");
        }
    }

    @Test
    @DisplayName("T2-08 FR-34: the game asks a Creator, not a level; each concrete creator answers for itself")
    void polymorphicCreation() {
        Map<Difficulty, QuestionFactory> factories = factories(bankOf(3, 3, 3), 1);
        assertInstanceOf(EasyQuestionFactory.class, factories.get(Difficulty.EASY));
        assertInstanceOf(MediumQuestionFactory.class, factories.get(Difficulty.MEDIUM));
        assertInstanceOf(HardQuestionFactory.class, factories.get(Difficulty.HARD));

        for (Difficulty level : Difficulty.values()) {
            // The client holds the abstract Creator and never names a level.
            QuestionFactory creator = factories.get(level);
            Question question = creator.ask();
            assertEquals(level, question.level(), "the creator of " + level.label() + " made another level");
            assertEquals(level, creator.level());
        }
    }

    @Test
    @DisplayName("T2-08: the Creator refuses a product of the wrong level, whatever a subclass returns")
    void theCreatorChecksWhatItsSubclassMade() {
        Question wrong = new Question("X-1", Difficulty.HARD, "Wrong level", 0, "a", "b", "c", "d");
        QuestionFactory broken = new QuestionFactory() {
            @Override
            public Difficulty level() {
                return Difficulty.EASY;
            }

            @Override
            protected Question createQuestion() {
                return wrong;
            }
        };
        IllegalStateException e = assertThrows(IllegalStateException.class, broken::ask);
        assertTrue(e.getMessage().contains("Hard"), e.getMessage());

        QuestionFactory empty = new QuestionFactory() {
            @Override
            public Difficulty level() {
                return Difficulty.EASY;
            }

            @Override
            protected Question createQuestion() {
                return null;
            }
        };
        assertThrows(IllegalStateException.class, empty::ask, "a creator that makes nothing must say so");
    }

    // ---------------------------------------------------------------- FR-34: the question itself

    @Test
    @DisplayName("T2-08 FR-34: every question has four different answers and exactly one correct answer")
    void everyQuestionHasFourAnswersAndOneCorrectOne() {
        QuestionBank bank = QuestionBank.ofCourseQuestions();
        assertTrue(bank.size() >= QuestionBank.MINIMUM_QUESTIONS,
                "the SRS asks for at least " + QuestionBank.MINIMUM_QUESTIONS + " questions, the bank has "
                        + bank.size());
        assertTrue(bank.coversEveryLevel(), "every level must have questions");
        Set<String> ids = new HashSet<>();
        for (Question q : bank.all()) {
            assertTrue(ids.add(q.id()), "two questions share the id " + q.id());
            assertEquals(Question.ANSWER_COUNT, q.answers().size(), q.id());
            assertEquals(Question.ANSWER_COUNT, new HashSet<>(q.answers()).size(), q.id() + " repeats an answer");
            assertFalse(q.text().isBlank(), q.id());
            int correct = 0;
            for (int i = 0; i < Question.ANSWER_COUNT; i++) {
                assertFalse(q.answer(i).isBlank(), q.id() + " answer " + i);
                correct += q.isCorrect(i) ? 1 : 0;
            }
            assertEquals(1, correct, q.id() + " must have exactly one correct answer");
            assertEquals(q.answer(q.correctIndex()), q.correctAnswer(), q.id());
        }
    }

    @Test
    @DisplayName("T2-08 FR-34: a question of the square's level is asked, never of another level")
    void theQuestionMatchesTheSquare() {
        Map<Difficulty, QuestionFactory> factories = factories(QuestionBank.ofCourseQuestions(), 5);
        for (Difficulty level : Difficulty.values()) {
            for (int i = 0; i < 50; i++) {
                assertEquals(level, factories.get(level).ask().level(), level.label() + " ask " + i);
            }
        }
    }

    @Test
    @DisplayName("T2-08 FR-34: a question cannot be built with the wrong number of answers or a bad correct one")
    void invalidQuestionsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question("Q", Difficulty.EASY, "t", 0, "a", "b", "c"), "three answers");
        assertThrows(IllegalArgumentException.class,
                () -> new Question("Q", Difficulty.EASY, "t", 0, "a", "b", "c", "d", "e"), "five answers");
        assertThrows(IllegalArgumentException.class,
                () -> new Question("Q", Difficulty.EASY, "t", 4, "a", "b", "c", "d"), "no such answer");
        assertThrows(IllegalArgumentException.class,
                () -> new Question("Q", Difficulty.EASY, "t", -1, "a", "b", "c", "d"), "no such answer");
        assertThrows(IllegalArgumentException.class,
                () -> new Question("Q", Difficulty.EASY, "t", 0, "a", "a", "c", "d"), "a repeated answer");
        assertThrows(IllegalArgumentException.class,
                () -> new Question("Q", Difficulty.EASY, "t", 0, "a", " ", "c", "d"), "a blank answer");
        assertThrows(IllegalArgumentException.class,
                () -> new Question("Q", Difficulty.EASY, " ", 0, "a", "b", "c", "d"), "no text");
        assertThrows(NullPointerException.class,
                () -> new Question("Q", null, "t", 0, "a", "b", "c", "d"), "no level");
    }

    // ---------------------------------------------------------------- FR-35, FR-36: the effect

    @Test
    @DisplayName("T2-08 FR-35: a correct answer moves 1, 2 or 3 steps forward by the level of the question")
    void correctAnswersMoveForwardByLevel() {
        Map<Difficulty, QuestionFactory> factories = factories(bankOf(4, 4, 4), 3);
        for (Difficulty level : Difficulty.values()) {
            int expected = switch (level) {
                case EASY -> 1;
                case MEDIUM -> 2;
                case HARD -> 3;
            };
            QuestionFactory creator = factories.get(level);
            Question q = creator.ask();
            assertEquals(expected, q.steps(), level.label() + " steps (FR-35)");
            QuestionResult result = creator.judge(q, q.correctIndex());
            assertTrue(result.correct(), level.label());
            assertTrue(result.forward(), level.label());
            assertEquals(expected, result.steps(), level.label() + ": forward steps");
            assertEquals(q.correctAnswer(), result.correctAnswer(), level.label());
            assertEquals(q.correctAnswer(), result.chosenAnswer(), level.label());
        }
    }

    @Test
    @DisplayName("T2-08 FR-35: a wrong answer moves the same 1, 2 or 3 steps backward")
    void wrongAnswersMoveBackwardByLevel() {
        Map<Difficulty, QuestionFactory> factories = factories(bankOf(4, 4, 4), 3);
        for (Difficulty level : Difficulty.values()) {
            QuestionFactory creator = factories.get(level);
            Question q = creator.ask();
            for (int chosen = 0; chosen < Question.ANSWER_COUNT; chosen++) {
                if (chosen == q.correctIndex()) {
                    continue;
                }
                QuestionResult result = creator.judge(q, chosen);
                assertFalse(result.correct(), level.label() + " answer " + chosen);
                assertFalse(result.forward(), level.label() + " answer " + chosen);
                assertEquals(-q.steps(), result.steps(), level.label() + ": backward steps (FR-35)");
                assertEquals(q.answer(chosen), result.chosenAnswer(), level.label());
                assertEquals(q.correctAnswer(), result.correctAnswer(),
                        level.label() + ": the correct answer is shown after a wrong one (FR-36)");
            }
        }
    }

    @Test
    @DisplayName("T2-08 FR-35: not answering counts as a wrong answer and moves backward")
    void noAnswerCountsAsWrong() {
        QuestionFactory creator = factories(bankOf(2, 2, 2), 9).get(Difficulty.MEDIUM);
        Question q = creator.ask();
        QuestionResult result = creator.judge(q, QuestionResult.NO_ANSWER);
        assertFalse(result.correct());
        assertEquals(-2, result.steps());
        assertNull(result.chosenAnswer(), "no answer was chosen");
        assertEquals(q.correctAnswer(), result.correctAnswer(), "the correct answer is still shown (FR-36)");
    }

    @Test
    @DisplayName("T2-08: judging refuses an answer that is not one of the four, or a question of another level")
    void judgingRefusesImpossibleInput() {
        Map<Difficulty, QuestionFactory> factories = factories(bankOf(2, 2, 2), 9);
        QuestionFactory easy = factories.get(Difficulty.EASY);
        Question easyQuestion = easy.ask();
        assertThrows(IllegalArgumentException.class, () -> easy.judge(easyQuestion, 4));
        assertThrows(IllegalArgumentException.class, () -> easy.judge(easyQuestion, -2));
        assertThrows(NullPointerException.class, () -> easy.judge(null, 0));
        Question hardQuestion = factories.get(Difficulty.HARD).ask();
        assertThrows(IllegalArgumentException.class, () -> easy.judge(hardQuestion, 0),
                "an Easy square cannot judge a Hard question");
    }

    @Test
    @DisplayName("T2-08 FR-35: a result cannot be built that disagrees with its own question")
    void aResultMustAgreeWithItsQuestion() {
        Question q = new Question("Q-1", Difficulty.HARD, "t", 1, "a", "b", "c", "d");
        assertThrows(IllegalArgumentException.class, () -> new QuestionResult(q, 1, false, -3),
                "answer 1 is the correct one");
        assertThrows(IllegalArgumentException.class, () -> new QuestionResult(q, 0, true, 3),
                "answer 0 is not the correct one");
        assertThrows(IllegalArgumentException.class, () -> new QuestionResult(q, 1, true, 2),
                "a Hard question moves 3 steps, not 2");
        assertThrows(IllegalArgumentException.class, () -> new QuestionResult(q, 0, false, 3),
                "a wrong answer moves backward");
    }

    // ---------------------------------------------------------------- FR-37: no repeats

    @Test
    @DisplayName("T2-08 FR-37: no question of a level comes up again until all of that level were asked")
    void noRepeatUntilTheLevelIsExhausted() {
        QuestionBank bank = QuestionBank.ofCourseQuestions();
        Map<Difficulty, QuestionFactory> factories = factories(bank, 2026);
        for (Difficulty level : Difficulty.values()) {
            int pool = bank.count(level);
            QuestionFactory creator = factories.get(level);
            for (int round = 1; round <= 3; round++) {
                Set<String> asked = new HashSet<>();
                for (int i = 0; i < pool; i++) {
                    Question q = creator.ask();
                    assertTrue(asked.add(q.id()),
                            level.label() + " round " + round + ": " + q.id() + " was asked twice before all "
                                    + pool + " were used (FR-37)");
                }
                assertEquals(pool, asked.size(), level.label() + " round " + round);
            }
        }
    }

    @Test
    @DisplayName("T2-08 FR-37: the deck is shuffled again once the level is exhausted, and keeps dealing")
    void theDeckReshufflesAfterExhaustion() {
        QuestionBank bank = bankOf(5, 5, 5);
        EasyQuestionFactory creator = new EasyQuestionFactory(bank, new Random(4));
        QuestionDeck deck = creator.deck();
        assertEquals(5, deck.size());
        assertEquals(0, deck.round(), "nothing was dealt yet");

        for (int i = 0; i < 5; i++) {
            creator.ask();
        }
        assertEquals(1, deck.round(), "one full round through the level");
        assertEquals(0, deck.remaining(), "the level is used up");

        creator.ask();
        assertEquals(2, deck.round(), "the deck was shuffled again");
        assertEquals(4, deck.remaining());

        for (int i = 0; i < 200; i++) {
            assertNotNull(creator.ask(), "a long game never runs out of questions");
        }
    }

    @Test
    @DisplayName("T2-08 FR-37: a level with a single question is allowed and simply repeats it")
    void aLevelWithOneQuestion() {
        QuestionFactory creator = factories(bankOf(1, 1, 1), 8).get(Difficulty.EASY);
        String first = creator.ask().id();
        assertEquals(first, creator.ask().id(), "one question is asked again at once; there is no other");
    }

    @Test
    @DisplayName("T2-08 FR-34: a level with no question at all is refused when the game is set up")
    void anEmptyLevelIsRefusedUpFront() {
        QuestionBank noHard = bankOf(2, 2, 0);
        assertEquals(0, noHard.count(Difficulty.HARD));
        assertFalse(noHard.coversEveryLevel());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> QuestionFactory.forBank(noHard, new Random(1)));
        assertTrue(e.getMessage().contains("Hard"), e.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> new QuestionDeck(Difficulty.HARD, List.of(), new Random(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new QuestionDeck(Difficulty.HARD, bankOf(1, 0, 0).byLevel(Difficulty.EASY), new Random(1)),
                "a deck refuses a question of another level");
        assertThrows(IllegalArgumentException.class, () -> new QuestionBank(List.of()));
        assertThrows(NullPointerException.class, () -> QuestionFactory.forBank(null, new Random(1)));
        assertThrows(NullPointerException.class,
                () -> QuestionFactory.forBank(QuestionBank.ofCourseQuestions(), null));
    }

    // ---------------------------------------------------------------- repeatability

    @Test
    @DisplayName("T2-08: the same seed asks the same questions in the same order, a different seed does not")
    void seededGamesRepeatTheirQuestions() {
        List<String> first = askTwenty(2026);
        List<String> again = askTwenty(2026);
        List<String> other = askTwenty(7);
        assertEquals(first, again, "a seeded game must ask the same questions");
        assertNotEquals(first, other, "a different seed should give another order");
    }

    private static List<String> askTwenty(long seed) {
        Map<Difficulty, QuestionFactory> factories = factories(QuestionBank.ofCourseQuestions(), seed);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            for (Difficulty level : Difficulty.values()) {
                ids.add(factories.get(level).ask().id());
            }
        }
        return ids;
    }

    // ---------------------------------------------------------------- fits the rest of the game

    @Test
    @DisplayName("T2-08 FR-34: every question square of a real Medium or Hard board can be answered")
    void everyQuestionSquareOfABoardCanBeAnswered() {
        QuestionBank bank = QuestionBank.ofCourseQuestions();
        for (Difficulty boardLevel : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            for (int seed = 0; seed < 20; seed++) {
                Random random = new Random(seed);
                Board board = BoardFactory.create(boardLevel, random);
                Map<Difficulty, QuestionFactory> factories = QuestionFactory.forBank(bank, random);
                int squares = 0;
                for (Square square : board.squares()) {
                    if (square.type() != SquareType.QUESTION) {
                        continue;
                    }
                    squares++;
                    Difficulty squareLevel = square.questionLevel();
                    assertNotNull(squareLevel, "a question square must carry its level");
                    QuestionFactory creator = factories.get(squareLevel);
                    assertNotNull(creator, "no creator for " + squareLevel);
                    Question q = creator.ask();
                    assertEquals(squareLevel, q.level(), boardLevel.label() + " seed " + seed);
                    assertEquals(squareLevel.questionSteps(), creator.judge(q, q.correctIndex()).steps());
                }
                assertEquals(boardLevel.questionSquares(), squares, boardLevel.label() + " seed " + seed);
            }
        }
    }

    @Test
    @DisplayName("T2-08 NFR-08: a factory is tied to one game and two games do not share their questions")
    void twoGamesHaveTheirOwnFactories() {
        QuestionBank bank = QuestionBank.ofCourseQuestions();
        Map<Difficulty, QuestionFactory> one = QuestionFactory.forBank(bank, new Random(1));
        Map<Difficulty, QuestionFactory> two = QuestionFactory.forBank(bank, new Random(1));
        assertNotSame(one.get(Difficulty.EASY), two.get(Difficulty.EASY));
        assertSame(bank.byLevel(Difficulty.EASY), bank.byLevel(Difficulty.EASY), "the bank itself is shared");
        for (int i = 0; i < 5; i++) {
            assertEquals(one.get(Difficulty.EASY).ask().id(), two.get(Difficulty.EASY).ask().id(),
                    "the same seed, so the same order, but each game has its own deck");
        }
        assertThrows(UnsupportedOperationException.class, () -> bank.all().clear(), "the bank is read-only");
        assertThrows(UnsupportedOperationException.class,
                () -> bank.byLevel(Difficulty.EASY).clear(), "a level is read-only");
    }
}
