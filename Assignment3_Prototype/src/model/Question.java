package model;

/** A multiple-choice question with four answers (SRS FR-64). */
public final class Question {
    private final String id;
    private String text;
    private final String[] answers = new String[4];
    private int correctIndex;
    private Difficulty level;

    public Question(String id, Difficulty level, String text, String[] answers, int correctIndex) {
        this.id = id;
        set(level, text, answers, correctIndex);
    }

    final void set(Difficulty level, String text, String[] answers, int correctIndex) {
        this.level = level;
        this.text = text.trim();
        for (int i = 0; i < 4; i++) {
            this.answers[i] = answers[i].trim();
        }
        this.correctIndex = correctIndex;
    }

    /** Unique identifier such as "Q-007". */
    public String id() {
        return id;
    }

    public String text() {
        return text;
    }

    public String answer(int index) {
        return answers[index];
    }

    public String[] answers() {
        return answers.clone();
    }

    public int correctIndex() {
        return correctIndex;
    }

    public String correctAnswer() {
        return answers[correctIndex];
    }

    public Difficulty level() {
        return level;
    }
}
