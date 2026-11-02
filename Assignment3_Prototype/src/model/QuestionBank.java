package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * In-memory question bank (SRS FR-56..FR-61). The prototype keeps changes for the current
 * session only; there is no file or encryption.
 */
public final class QuestionBank {

    public static final int MINIMUM_QUESTIONS = 30;

    /** Field-level validation messages; null means the field is fine. */
    public record Validation(String textError, String[] answerErrors, String correctError, String levelError) {

        public boolean isValid() {
            if (textError != null || correctError != null || levelError != null) {
                return false;
            }
            for (String e : answerErrors) {
                if (e != null) {
                    return false;
                }
            }
            return true;
        }
    }

    private final List<Question> questions = new ArrayList<>();
    private int nextNumber = 1;

    public QuestionBank(Collection<Question> initial) {
        for (Question q : initial) {
            questions.add(q);
            try {
                nextNumber = Math.max(nextNumber, Integer.parseInt(q.id().replaceAll("\\D", "")) + 1);
            } catch (NumberFormatException ignored) {
                // ids without a number are kept as they are
            }
        }
    }

    public List<Question> all() {
        return Collections.unmodifiableList(questions);
    }

    public List<Question> byLevel(Difficulty level) {
        return questions.stream().filter(q -> q.level() == level).toList();
    }

    public int size() {
        return questions.size();
    }

    public int count(Difficulty level) {
        return (int) questions.stream().filter(q -> q.level() == level).count();
    }

    /** Validation rules of the question form (FR-58): text, four different answers, a correct answer, a level. */
    public static Validation validate(String text, String[] answers, int correctIndex, Difficulty level) {
        String textError = text == null || text.isBlank() ? "Enter the question text." : null;
        String[] answerErrors = new String[4];
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            String a = answers == null || answers[i] == null ? "" : answers[i].trim();
            if (a.isEmpty()) {
                answerErrors[i] = "Answer " + (char) ('A' + i) + " is required.";
            } else if (!seen.add(a.toLowerCase(Locale.ROOT))) {
                answerErrors[i] = "Answer " + (char) ('A' + i) + " repeats another answer.";
            }
        }
        String correctError = correctIndex < 0 || correctIndex > 3 ? "Select the correct answer." : null;
        String levelError = level == null ? "Choose a difficulty level." : null;
        return new Validation(textError, answerErrors, correctError, levelError);
    }

    public Question add(String text, String[] answers, int correctIndex, Difficulty level) {
        requireValid(text, answers, correctIndex, level);
        Question q = new Question(String.format("Q-%03d", nextNumber++), level, text, answers, correctIndex);
        questions.add(q);
        return q;
    }

    public void update(Question question, String text, String[] answers, int correctIndex, Difficulty level) {
        requireValid(text, answers, correctIndex, level);
        question.set(level, text, answers, correctIndex);
    }

    /** Deleting is refused when fewer than 30 questions would remain (FR-60). */
    public boolean canDelete() {
        return questions.size() > MINIMUM_QUESTIONS;
    }

    public void delete(Question question) {
        if (!canDelete()) {
            throw new IllegalStateException("At least " + MINIMUM_QUESTIONS + " questions must remain.");
        }
        questions.remove(question);
    }

    /** A per-game deck: no question repeats until all of its level were asked (FR-37). */
    public Deck newDeck(Random random) {
        return new Deck(new ArrayList<>(questions), random);
    }

    private static void requireValid(String text, String[] answers, int correctIndex, Difficulty level) {
        if (!validate(text, answers, correctIndex, level).isValid()) {
            throw new IllegalArgumentException("Invalid question");
        }
    }

    /** Shuffled questions of each level for one game. Changes to the bank apply from the next game (FR-61). */
    public static final class Deck {
        private final List<Question> snapshot;
        private final Random random;
        private final Map<Difficulty, Deque<Question>> queues = new EnumMap<>(Difficulty.class);

        private Deck(List<Question> snapshot, Random random) {
            this.snapshot = snapshot;
            this.random = random;
        }

        public Question next(Difficulty level) {
            Deque<Question> queue = queues.computeIfAbsent(level, l -> new ArrayDeque<>());
            if (queue.isEmpty()) {
                List<Question> pool = new ArrayList<>(snapshot.stream().filter(q -> q.level() == level).toList());
                if (pool.isEmpty()) {
                    pool.addAll(snapshot);
                }
                Collections.shuffle(pool, random);
                queue.addAll(pool);
            }
            return queue.poll();
        }
    }
}
