package controller;

import java.util.Arrays;

import components.Icons;
import components.ModalOverlay;
import model.AdminAuthenticator;
import model.Question;
import model.QuestionBank;
import view.QuestionFormDialog;
import view.QuestionManagementView;

/** Administrator flows: simulated login, logout and question add/edit/delete (FR-51..FR-62). */
public final class AdminController {

    private final NavigationController nav;
    private final AdminAuthenticator auth;
    private final QuestionBank bank;
    private QuestionFormDialog lastForm;

    public AdminController(NavigationController nav, AdminAuthenticator auth, QuestionBank bank) {
        this.nav = nav;
        this.auth = auth;
        this.bank = bank;
    }

    public void login(String username, char[] password) {
        AdminAuthenticator.Result result = auth.login(username, password);
        Arrays.fill(password, '\0');
        switch (result) {
            case SUCCESS -> nav.showQuestionManagement();
            case FAILURE -> nav.loginView().showError("Incorrect username or password. Please try again.");
            case LOCKED -> nav.loginView().showLocked(auth::lockRemainingMillis);
        }
    }

    /** Ends the administrator session (FR-54). */
    public void logout() {
        auth.logout();
        nav.showMainMenu();
    }

    public void addQuestion() {
        openForm(null);
    }

    public void editQuestion(Question question) {
        openForm(question);
    }

    /** Last opened form (used by the self-test and the screenshot tool). */
    public QuestionFormDialog lastForm() {
        return lastForm;
    }

    private void openForm(Question existing) {
        ModalOverlay[] holder = new ModalOverlay[1];
        QuestionFormDialog[] form = new QuestionFormDialog[1];
        form[0] = new QuestionFormDialog(existing, data -> {
            QuestionBank.Validation v = QuestionBank.validate(data.text(), data.answers(), data.correctIndex(),
                    data.level());
            if (!v.isValid()) {
                form[0].showValidation(v);
                return;
            }
            Question q;
            if (existing == null) {
                q = bank.add(data.text(), data.answers(), data.correctIndex(), data.level());
            } else {
                bank.update(existing, data.text(), data.answers(), data.correctIndex(), data.level());
                q = existing;
            }
            holder[0].close();
            QuestionManagementView view = nav.questionsView();
            view.refresh();
            view.select(q);
            view.showMessage(existing == null ? "Question " + q.id() + " added." : "Question " + q.id() + " saved.");
        }, () -> holder[0].close());
        lastForm = form[0];
        holder[0] = nav.overlay(form[0]);
        holder[0].setOnEscape(() -> holder[0].close());
    }

    /** Delete after confirmation, unless fewer than 30 questions would remain (FR-60). */
    public void deleteQuestion(Question question) {
        if (!bank.canDelete()) {
            nav.info("This question cannot be deleted",
                    "The question bank must keep at least " + QuestionBank.MINIMUM_QUESTIONS
                            + " questions. Add a new question first, then delete this one.", Icons.Type.INFO);
            return;
        }
        String text = question.text().length() > 70 ? question.text().substring(0, 67) + "..." : question.text();
        nav.confirm("Delete question " + question.id() + "?",
                "\"" + text + "\" will be removed from the question bank.", "Delete", "Cancel", true,
                Icons.Type.TRASH, () -> {
                    bank.delete(question);
                    nav.questionsView().refresh();
                    nav.questionsView().showMessage("Question " + question.id() + " deleted.");
                });
    }
}
