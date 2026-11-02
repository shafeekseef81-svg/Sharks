package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import components.BoardComponent;
import components.CustomButton;
import components.CustomPanel;
import components.Icons;
import components.Theme;
import components.UiKit;
import model.Difficulty;
import model.Player;
import model.Question;

/**
 * Educational question (FR-34..FR-36): the question, four answers, Submit, then clear feedback
 * with the correct answer and the movement effect, and Continue.
 */
public class QuestionDialog extends CustomPanel {

    private static final int CARD_WIDTH = 700;

    private final Question question;
    private final int steps;
    private final boolean hasPiece;
    private final List<AnswerOption> options = new ArrayList<>();
    private final ButtonGroup group = new ButtonGroup();
    private final CustomButton primary;
    private final JLabel hint = UiKit.label("Choose an answer, then press Submit.", Theme.font(Font.PLAIN, 13.5f),
            Theme.TEXT_MUTED);
    private final JPanel feedback = new JPanel(new BorderLayout(12, 0));
    private int selected = -1;
    private boolean answered;

    public QuestionDialog(Question question, Player player, boolean demo, boolean hasPiece,
                          Consumer<Boolean> onContinue) {
        super(new BorderLayout(), Style.CARD);
        setPadding(0);
        this.question = question;
        this.steps = question.level().questionSteps();
        this.hasPiece = hasPiece;
        setName("dialog.question");

        add(new Header(question.level(), demo, steps), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(16, 26, 8, 26));
        JLabel who = UiKit.label(UiKit.ltr(player.name() + " answers this question  •  everyone can read along"),
                Theme.font(Font.PLAIN, 13.5f), Theme.TEXT_MUTED);
        who.setIcon(BoardComponent.pawnIcon(player.color(), 22));
        who.setIconTextGap(6);
        body.add(left(who));
        body.add(UiKit.vgap(10));
        JLabel text = UiKit.wrap(question.text(), CARD_WIDTH - 60, Theme.font(Font.BOLD, 19f), Theme.TEXT);
        text.setName("question.text");
        body.add(left(text));
        body.add(UiKit.vgap(14));
        for (int i = 0; i < 4; i++) {
            AnswerOption o = new AnswerOption(i, question.answer(i));
            o.setName("question.option." + i);
            int index = i;
            o.addActionListener(e -> select(index));
            group.add(o);
            options.add(o);
            body.add(left(o));
            body.add(UiKit.vgap(8));
        }
        feedback.setOpaque(false);
        feedback.setVisible(false);
        body.add(left(feedback));
        add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 26, 20, 26));
        primary = new CustomButton("Submit Answer", Icons.get(Icons.Type.CHECK, 18, Color.WHITE),
                CustomButton.Variant.PRIMARY);
        primary.setName("question.submit");
        primary.setEnabled(false);
        primary.setPreferredSize(new Dimension(190, 48));
        primary.addActionListener(e -> {
            if (answered) {
                onContinue.accept(isCorrect());
            } else {
                submit();
            }
        });
        footer.add(hint, BorderLayout.CENTER);
        footer.add(primary, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        for (int i = 0; i < 4; i++) {
            int index = i;
            AbstractAction choose = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!answered) {
                        options.get(index).setSelected(true);
                        select(index);
                    }
                }
            };
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, 0), "choose" + i);
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_A + i, 0), "choose" + i);
            getActionMap().put("choose" + i, choose);
        }
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "primary");
        getActionMap().put("primary", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (primary.isEnabled()) {
                    primary.doClick();
                }
            }
        });
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(CARD_WIDTH, super.getPreferredSize().height);
    }

    /** Selects an answer (also used by the self-test). */
    public void select(int index) {
        if (answered) {
            return;
        }
        selected = index;
        options.get(index).setSelected(true);
        primary.setEnabled(true);
        hint.setText("Answer " + (char) ('A' + index) + " selected. Press Submit Answer.");
    }

    /** Checks the answer and shows the feedback (FR-36). */
    public void submit() {
        if (answered || selected < 0) {
            return;
        }
        answered = true;
        boolean correct = isCorrect();
        for (AnswerOption o : options) {
            o.reveal(o.index == question.correctIndex(), o.index == selected);
        }
        Color accent = correct ? Theme.SUCCESS : Theme.DANGER;
        feedback.removeAll();
        CustomPanel box = new CustomPanel(new BorderLayout(12, 0), Style.SOLID);
        box.setFill(correct ? new Color(0xE3F6EA) : new Color(0xFDE7E7));
        box.setRadius(14);
        box.setPadding(12, 14, 12, 14);
        box.add(new JLabel(Icons.get(correct ? Icons.Type.CHECK : Icons.Type.CROSS, 28, accent)), BorderLayout.WEST);
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel title = UiKit.label(correct ? "Correct! Well done." : "Not quite.", Theme.font(Font.BOLD, 17f),
                Theme.mix(accent, Color.BLACK, 0.2));
        title.setName("question.result");
        texts.add(title);
        String correctText = question.correctAnswer();
        String answer = (char) ('A' + question.correctIndex()) + ": " + correctText
                + (correctText.matches(".*[.!?]$") ? "" : ".");
        String effect;
        if (!hasPiece) {
            effect = "You have no piece on the board, so nothing moves this time.";
        } else if (correct) {
            effect = "Your piece moves " + steps + (steps == 1 ? " step" : " steps") + " forward.";
        } else {
            effect = "Your piece moves " + steps + (steps == 1 ? " step" : " steps") + " back.";
        }
        String message = (correct ? "The answer is " : "The correct answer is ") + answer + "  " + effect;
        texts.add(UiKit.wrap(message, CARD_WIDTH - 140, Theme.font(Font.PLAIN, 14f), Theme.TEXT));
        box.add(texts, BorderLayout.CENTER);
        feedback.add(box, BorderLayout.CENTER);
        feedback.setVisible(true);
        primary.setText("Continue");
        primary.setIcon(Icons.get(Icons.Type.ARROW_RIGHT, 18, Color.WHITE));
        hint.setText(correct ? "Great! Press Continue to apply the move." : "Press Continue to apply the move.");
        revalidate();
        repaint();
        primary.requestFocusInWindow();
    }

    public boolean isAnswered() {
        return answered;
    }

    public boolean isCorrect() {
        return selected == question.correctIndex();
    }

    public CustomButton primaryButton() {
        return primary;
    }

    public Question question() {
        return question;
    }

    // ------------------------------------------------------------------ parts

    /** Coloured header with the level of the question and the movement effect. */
    private static final class Header extends JPanel {
        private final Difficulty level;

        Header(Difficulty level, boolean demo, int steps) {
            super(new BorderLayout(10, 0));
            this.level = level;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 22, 14, 22));
            JLabel title = UiKit.label("QUESTION  •  " + level.label().toUpperCase() + " LEVEL",
                    Theme.font(Font.BOLD, 16f), Color.WHITE);
            title.setIcon(Icons.get(Icons.Type.QUESTION, 26, Theme.alpha(Color.WHITE, 235)));
            title.setIconTextGap(10);
            add(title, BorderLayout.WEST);
            JPanel tags = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
            tags.setOpaque(false);
            if (demo) {
                tags.add(UiKit.pill("PROTOTYPE DEMO", new Color(255, 255, 255, 230), new Color(0x6A22D2)));
            }
            tags.add(UiKit.pill("Correct +" + steps + "   Wrong −" + steps, new Color(0, 0, 0, 60), Color.WHITE));
            add(tags, BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            Color c = Theme.level(level);
            g2.setPaint(new GradientPaint(0, 0, Theme.mix(c, Color.WHITE, 0.1), getWidth(), 0,
                    Theme.mix(c, Color.BLACK, 0.18)));
            int w = getWidth();
            int h = getHeight();
            g2.fill(new RoundRectangle2D.Double(3, 0, w - 6, h + 20, 18, 18));
            g2.dispose();
        }
    }

    /** One answer option: letter badge and wrapped text; shows correct/wrong colours after Submit. */
    private static final class AnswerOption extends JToggleButton {
        private final int index;
        private final String answer;
        private boolean hover;
        private boolean revealed;
        private boolean correct;
        private boolean chosen;

        AnswerOption(int index, String answer) {
            this.index = index;
            this.answer = answer;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Theme.font(Font.PLAIN, 15.5f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName("Answer " + (char) ('A' + index) + ": " + answer);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void reveal(boolean isCorrect, boolean isChosen) {
            revealed = true;
            correct = isCorrect;
            chosen = isChosen;
            setEnabled(false);
            setCursor(Cursor.getDefaultCursor());
            repaint();
        }

        private List<String> lines(FontMetrics fm, int width) {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : answer.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(candidate) > width && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
            return lines;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int n = lines(fm, CARD_WIDTH - 52 - 120).size();
            return new Dimension(CARD_WIDTH - 52, Math.max(50, 20 + n * fm.getHeight()));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth();
            int h = getHeight();
            Color bg = Color.WHITE;
            Color border = Theme.BORDER;
            Color badge = new Color(0xEEF1F8);
            Color badgeText = Theme.TEXT_MUTED;
            if (revealed && correct) {
                bg = new Color(0xE3F6EA);
                border = Theme.SUCCESS;
                badge = Theme.SUCCESS;
                badgeText = Color.WHITE;
            } else if (revealed && chosen) {
                bg = new Color(0xFDE7E7);
                border = Theme.DANGER;
                badge = Theme.DANGER;
                badgeText = Color.WHITE;
            } else if (isSelected()) {
                bg = new Color(0xEAF0FF);
                border = Theme.ACCENT;
                badge = Theme.ACCENT;
                badgeText = Color.WHITE;
            } else if (hover && !revealed) {
                bg = new Color(0xF6F8FD);
                border = new Color(0xB9C4E4);
            }
            RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 14, 14);
            g2.setColor(bg);
            g2.fill(shape);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(isSelected() || revealed && (correct || chosen) ? 2.2f : 1.2f));
            g2.draw(shape);
            if (isFocusOwner() && !revealed) {
                g2.setColor(Theme.alpha(Theme.ACCENT, 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(4, 4, w - 8, h - 8, 11, 11));
            }
            double bx = 14;
            double by = h / 2.0 - 15;
            g2.setColor(badge);
            g2.fill(new Ellipse2D.Double(bx, by, 30, 30));
            g2.setFont(Theme.font(Font.BOLD, 15f));
            FontMetrics bfm = g2.getFontMetrics();
            String letter = String.valueOf((char) ('A' + index));
            g2.setColor(badgeText);
            g2.drawString(letter, (float) (bx + 15 - bfm.stringWidth(letter) / 2.0),
                    (float) (by + 15 + (bfm.getAscent() - bfm.getDescent()) / 2.0));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            List<String> lines = lines(fm, w - 120);
            double total = lines.size() * fm.getHeight();
            double y = (h - total) / 2 + fm.getAscent();
            g2.setColor(Theme.TEXT);
            for (String line : lines) {
                g2.drawString(line, 58, (float) y);
                y += fm.getHeight();
            }
            if (revealed && (correct || chosen)) {
                Icons.paint(g2, correct ? Icons.Type.CHECK : Icons.Type.CROSS, w - 42, h / 2.0 - 12, 24,
                        correct ? Theme.SUCCESS : Theme.DANGER);
            }
            g2.dispose();
        }
    }
}
