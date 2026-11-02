package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

import components.CustomButton;
import components.CustomPanel;
import components.Icons;
import components.StyledTextField;
import components.Theme;
import components.ToggleChip;
import components.UiKit;
import model.Difficulty;
import model.Question;
import model.QuestionBank;

/** Add/edit form of a question (FR-58, FR-59) with field-level validation messages. */
public class QuestionFormDialog extends CustomPanel {

    /** Values entered in the form. */
    public record FormData(String text, String[] answers, int correctIndex, Difficulty level) {
    }

    private static final int CARD_WIDTH = 680;

    private final Map<Difficulty, ToggleChip> levelChips = new EnumMap<>(Difficulty.class);
    private final JTextArea text = new JTextArea(3, 40);
    private final RoundedArea textBox;
    private final List<StyledTextField> answers = new ArrayList<>();
    private final List<CorrectDot> dots = new ArrayList<>();
    private final List<JLabel> answerErrors = new ArrayList<>();
    private final JLabel levelError = error();
    private final JLabel textError = error();
    private final JLabel correctError = error();
    private final CustomButton save;
    private Difficulty level;
    private int correct = -1;

    public QuestionFormDialog(Question existing, Consumer<FormData> onSave, Runnable onCancel) {
        super(new BorderLayout(), Style.CARD);
        setPadding(22, 26, 20, 26);
        setName("dialog.questionForm");

        JPanel head = new JPanel(new BorderLayout(12, 0));
        head.setOpaque(false);
        JLabel title = UiKit.label(existing == null ? "Add Question" : "Edit Question " + existing.id(),
                Theme.font(Font.BOLD, 22f), Theme.TEXT);
        head.add(title, BorderLayout.WEST);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        chips.setOpaque(false);
        chips.add(UiKit.label("Level  ", Theme.font(Font.BOLD, 14f), Theme.TEXT_MUTED));
        ButtonGroup group = new ButtonGroup();
        for (Difficulty d : Difficulty.values()) {
            ToggleChip chip = new ToggleChip(d.label(), false);
            chip.setName("form.level." + d.name().toLowerCase());
            chip.setSelectedColor(Theme.level(d));
            chip.setToolTipText("Difficulty level of the question");
            chip.addActionListener(e -> {
                level = d;
                levelError.setVisible(false);
            });
            group.add(chip);
            levelChips.put(d, chip);
            chips.add(UiKit.hgap(6));
            chips.add(chip);
        }
        head.add(chips, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        levelError.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        levelError.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        body.add(left(levelError));
        body.add(UiKit.vgap(10));

        body.add(left(label("Question")));
        body.add(UiKit.vgap(5));
        text.setName("form.text");
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(Theme.font(Font.PLAIN, 15f));
        text.setForeground(Theme.TEXT);
        text.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        textBox = new RoundedArea(text);
        body.add(left(textBox));
        body.add(left(textError));
        body.add(UiKit.vgap(10));

        JPanel answersHead = new JPanel(new BorderLayout(12, 0));
        answersHead.setOpaque(false);
        answersHead.add(label("Answers"), BorderLayout.WEST);
        answersHead.add(UiKit.label("Click the circle next to the correct answer", Theme.font(Font.PLAIN, 13f),
                Theme.TEXT_MUTED), BorderLayout.EAST);
        body.add(left(answersHead));
        body.add(UiKit.vgap(5));
        ButtonGroup correctGroup = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            JPanel row = new JPanel(new GridBagLayout());
            row.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(0, 0, 0, 10);
            CorrectDot dot = new CorrectDot(i);
            dot.setName("form.correct." + i);
            int index = i;
            dot.addActionListener(e -> {
                correct = index;
                correctError.setVisible(false);
            });
            correctGroup.add(dot);
            dots.add(dot);
            row.add(dot, c);
            JLabel letter = UiKit.label(String.valueOf((char) ('A' + i)), Theme.font(Font.BOLD, 15f), Theme.TEXT_MUTED);
            row.add(letter, c);
            StyledTextField field = new StyledTextField("Answer " + (char) ('A' + i), null);
            field.setName("form.answer." + i);
            field.setMaxLength(120);
            answers.add(field);
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 0, 0);
            row.add(field, c);
            body.add(left(row));
            JLabel err = error();
            err.setBorder(BorderFactory.createEmptyBorder(2, 74, 0, 0));
            answerErrors.add(err);
            body.add(left(err));
            body.add(UiKit.vgap(6));
        }
        body.add(left(correctError));
        add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        footer.add(UiKit.label("All fields are required.   ", Theme.font(Font.PLAIN, 13f), Theme.TEXT_MUTED));
        CustomButton cancel = new CustomButton("Cancel", CustomButton.Variant.SECONDARY);
        cancel.setName("form.cancel");
        cancel.addActionListener(e -> onCancel.run());
        save = new CustomButton(existing == null ? "Add Question" : "Save Changes",
                Icons.get(Icons.Type.CHECK, 18, Color.WHITE), CustomButton.Variant.PRIMARY);
        save.setName("form.save");
        save.addActionListener(e -> onSave.accept(data()));
        footer.add(cancel);
        footer.add(save);
        add(footer, BorderLayout.SOUTH);

        if (existing != null) {
            setLevel(existing.level());
            text.setText(existing.text());
            for (int i = 0; i < 4; i++) {
                answers.get(i).setText(existing.answer(i));
            }
            setCorrect(existing.correctIndex());
        }
    }

    private static JLabel label(String s) {
        return UiKit.label(s, Theme.font(Font.BOLD, 14.5f), Theme.TEXT);
    }

    /** Error line that only takes space while it shows a message. */
    private static JLabel error() {
        JLabel l = UiKit.label(" ", Theme.font(Font.PLAIN, 12.5f), Theme.DANGER);
        l.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
        l.setVisible(false);
        return l;
    }

    private static void showError(JLabel label, String message) {
        label.setText(message == null ? " " : message);
        label.setVisible(message != null);
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(CARD_WIDTH, super.getPreferredSize().height);
    }

    public FormData data() {
        String[] values = new String[4];
        for (int i = 0; i < 4; i++) {
            values[i] = answers.get(i).getText();
        }
        return new FormData(text.getText(), values, correct, level);
    }

    /** Shows the validation result next to the fields. */
    public void showValidation(QuestionBank.Validation v) {
        showError(levelError, v.levelError());
        showError(textError, v.textError());
        textBox.setError(v.textError() != null);
        for (int i = 0; i < 4; i++) {
            String e = v.answerErrors()[i];
            showError(answerErrors.get(i), e);
            answers.get(i).setError(e != null);
        }
        showError(correctError, v.correctError());
        revalidate();
        repaint();
    }

    // test helpers
    public void setLevel(Difficulty d) {
        level = d;
        levelChips.get(d).setSelected(true);
    }

    public void setQuestionText(String s) {
        text.setText(s);
    }

    public void setAnswer(int i, String s) {
        answers.get(i).setText(s);
    }

    public void setCorrect(int i) {
        correct = i;
        dots.get(i).setSelected(true);
    }

    public CustomButton saveButton() {
        return save;
    }

    /** Text area inside a rounded field border. */
    private static final class RoundedArea extends JPanel {
        private boolean error;
        private final JTextArea area;

        RoundedArea(JTextArea area) {
            super(new BorderLayout());
            this.area = area;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            JScrollPane sp = new JScrollPane(area);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            area.setOpaque(false);
            add(sp, BorderLayout.CENTER);
            setPreferredSize(new Dimension(CARD_WIDTH - 60, 66));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
            area.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    repaint();
                }
            });
        }

        void setError(boolean error) {
            this.error = error;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
            g2.setColor(Color.WHITE);
            g2.fill(shape);
            g2.setColor(error ? Theme.DANGER : area.isFocusOwner() ? Theme.ACCENT : new Color(0xC9D0E0));
            g2.setStroke(new BasicStroke(error || area.isFocusOwner() ? 2f : 1.2f));
            g2.draw(shape);
            if (area.getDocument().getLength() == 0) {
                g2.setFont(area.getFont());
                g2.setColor(new Color(0x8C94AE));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("Type the question, e.g. \"Which UML diagram shows classes?\"", 14, 10 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    /** Round "correct answer" selector. */
    private static final class CorrectDot extends JToggleButton {
        CorrectDot(int index) {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(30, 30));
            setToolTipText("Mark answer " + (char) ('A' + index) + " as the correct answer");
            getAccessibleContext().setAccessibleName("Correct answer " + (char) ('A' + index));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            Ellipse2D circle = new Ellipse2D.Double(3, 3, 24, 24);
            g2.setColor(isSelected() ? Theme.SUCCESS : Color.WHITE);
            g2.fill(circle);
            g2.setColor(isSelected() ? Theme.SUCCESS : new Color(0xAEB6CC));
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(circle);
            if (isSelected()) {
                Icons.paint(g2, Icons.Type.CHECK, 7, 7, 16, Color.WHITE);
            }
            if (isFocusOwner()) {
                g2.setColor(Theme.ACCENT);
                g2.draw(new Ellipse2D.Double(0.5, 0.5, 29, 29));
            }
            g2.dispose();
        }
    }
}
