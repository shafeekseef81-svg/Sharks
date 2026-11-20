package Model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The questions a game can ask, grouped by level (SRS FR-34). Task T2-08, owner: Mohamed Zereny.
 *
 * <p>A bank is read-only and is fixed for the whole game: {@link QuestionFactory} and its concrete creators
 * take their questions from it. A test can build a bank of its own, which is how the tests of this task ask
 * known questions instead of the course ones.
 *
 * <p>This is the part of the prototype's {@code QuestionBank} that Iteration 2 needs, taken through the
 * reuse gate (SDP 6.1). Adding, editing, deleting and validating questions (FR-56..FR-61), and the rule
 * that at least 30 must remain, belong to the administrator screen of Iteration 3 and are deliberately left
 * out; a bank that cannot change also cannot change in the middle of a game (FR-61).
 */
public final class QuestionBank {

    /** The SRS expects at least this many questions over the three levels (section 2.5, dependency 2). */
    public static final int MINIMUM_QUESTIONS = 30;

    private final List<Question> questions;
    private final Map<Difficulty, List<Question>> byLevel;

    /**
     * @param questions the questions of this bank, at least one
     * @throws NullPointerException if the list or a question is null
     * @throws IllegalArgumentException if the list is empty or two questions share an id
     */
    public QuestionBank(List<Question> questions) {
        Objects.requireNonNull(questions, "questions");
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("A question bank needs at least one question");
        }
        Map<Difficulty, List<Question>> grouped = new EnumMap<>(Difficulty.class);
        for (Difficulty level : Difficulty.values()) {
            grouped.put(level, new ArrayList<>());
        }
        List<String> ids = new ArrayList<>();
        for (Question q : questions) {
            Objects.requireNonNull(q, "question");
            if (ids.contains(q.id())) {
                throw new IllegalArgumentException("Two questions share the id " + q.id());
            }
            ids.add(q.id());
            grouped.get(q.level()).add(q);
        }
        this.questions = List.copyOf(questions);
        Map<Difficulty, List<Question>> copies = new EnumMap<>(Difficulty.class);
        grouped.forEach((level, list) -> copies.put(level, List.copyOf(list)));
        this.byLevel = Map.copyOf(copies);
    }

    /**
     * The bank of the real game: the course questions of {@link CourseQuestions}.
     *
     * @return a bank with every course question
     */
    public static QuestionBank ofCourseQuestions() {
        return new QuestionBank(CourseQuestions.all());
    }

    /** @return every question of the bank, in the order it was built with */
    public List<Question> all() {
        return questions;
    }

    /**
     * @param level a question level
     * @return the questions of that level, possibly none
     * @throws NullPointerException if {@code level} is null
     */
    public List<Question> byLevel(Difficulty level) {
        return byLevel.get(Objects.requireNonNull(level, "level"));
    }

    /** @return how many questions the bank holds */
    public int size() {
        return questions.size();
    }

    /**
     * @param level a question level
     * @return how many questions that level has
     */
    public int count(Difficulty level) {
        return byLevel(level).size();
    }

    /**
     * Whether this bank can supply every level, which a game needs before it can put question squares on
     * the board (FR-34).
     *
     * @return {@code true} if every level has at least one question
     */
    public boolean coversEveryLevel() {
        for (Difficulty level : Difficulty.values()) {
            if (byLevel(level).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "QuestionBank of " + size() + " questions";
    }
}
