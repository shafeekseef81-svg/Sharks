package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import components.Animations;
import components.CustomButton;
import components.CustomPanel;
import components.Icons;
import components.Theme;
import components.UiKit;
import model.Player;
import model.SurpriseOutcome;

/** Surprise event (FR-39..FR-42): short reveal, then the outcome, its chance and its effect. */
public class SurpriseDialog extends CustomPanel {

    private static final int CARD_WIDTH = 580;

    private final SurpriseOutcome outcome;
    private final OutcomeArt art = new OutcomeArt();
    private final JLabel title = UiKit.label(" ", Theme.font(Font.BOLD, 24f), Theme.TEXT);
    private final JLabel description = UiKit.wrap(" ", CARD_WIDTH - 80, Theme.font(Font.PLAIN, 15f), Theme.TEXT_MUTED);
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
    private final JLabel detailLabel = UiKit.wrap(" ", CARD_WIDTH - 80, Theme.font(Font.BOLD, 14.5f), Theme.TEXT);
    private final JLabel noteLabel = UiKit.wrap(" ", CARD_WIDTH - 80, Theme.font(Font.ITALIC, 13f), Theme.TEXT_MUTED);
    private final CustomButton primary;
    private final String detail;
    private final String note;

    /**
     * @param detail extra line such as the rolled die for "move an opponent", or null
     * @param note   prototype note shown when the effect is simplified, or null
     */
    public SurpriseDialog(SurpriseOutcome outcome, Player player, String detail, String note, boolean demo,
                          Runnable onContinue) {
        super(new BorderLayout(), Style.CARD);
        setPadding(0);
        this.outcome = outcome;
        this.detail = detail;
        this.note = note;
        setName("dialog.surprise");

        JPanel header = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x8D4BFF), getWidth(), 0, new Color(0x5B16C7)));
                g2.fill(new RoundRectangle2D.Double(3, 0, getWidth() - 6, getHeight() + 20, 18, 18));
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 14, 22));
        JLabel head = UiKit.label("SURPRISE!", Theme.font(Font.BOLD, 18f), Color.WHITE);
        head.setIcon(new javax.swing.Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                Icons.gift(g2, x, y, 26, Color.WHITE);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 26;
            }

            @Override
            public int getIconHeight() {
                return 26;
            }
        });
        head.setIconTextGap(10);
        header.add(head, BorderLayout.WEST);
        JPanel tags = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        tags.setOpaque(false);
        if (demo) {
            tags.add(UiKit.pill("PROTOTYPE DEMO", new Color(255, 255, 255, 230), new Color(0x6A22D2)));
        }
        tags.add(UiKit.pill(UiKit.ltr(player.name() + " (" + player.color().label() + ")"), new Color(0, 0, 0, 60), Color.WHITE));
        header.add(tags, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(18, 30, 6, 30));
        body.add(center(art));
        body.add(UiKit.vgap(10));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setName("surprise.title");
        body.add(center(title));
        body.add(UiKit.vgap(6));
        description.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(center(description));
        body.add(UiKit.vgap(12));
        chips.setOpaque(false);
        body.add(center(chips));
        body.add(UiKit.vgap(10));
        detailLabel.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(center(detailLabel));
        noteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(center(noteLabel));
        add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 26, 20, 26));
        primary = new CustomButton("Continue", Icons.get(Icons.Type.ARROW_RIGHT, 18, Color.WHITE),
                CustomButton.Variant.PRIMARY);
        primary.setAccent(new Color(0x7A35EC));
        primary.setName("surprise.continue");
        primary.setPreferredSize(new Dimension(200, 48));
        primary.addActionListener(e -> onContinue.run());
        footer.add(primary);
        add(footer, BorderLayout.SOUTH);

        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "primary");
        getActionMap().put("primary", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (primary.isEnabled()) {
                    primary.doClick();
                }
            }
        });
        reveal();
    }

    private static JComponent center(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(CARD_WIDTH, super.getPreferredSize().height);
    }

    /** Slot-machine style reveal that settles on the drawn outcome. */
    private void reveal() {
        if (Animations.instant) {
            showOutcome(outcome);
            return;
        }
        primary.setEnabled(false);
        title.setText("What will it be?");
        UiKit.setWrapped(description, "Opening the surprise...", CARD_WIDTH - 80);
        SurpriseOutcome[] all = SurpriseOutcome.values();
        int[] tick = {0};
        long start = System.currentTimeMillis();
        Timer timer = new Timer(70, null);
        timer.addActionListener(e -> {
            long t = System.currentTimeMillis() - start;
            if (t > 950) {
                timer.stop();
                showOutcome(outcome);
                return;
            }
            art.show(all[tick[0]++ % all.length], 0.55f);
            timer.setDelay(70 + (int) (t / 9));
        });
        timer.start();
    }

    private void showOutcome(SurpriseOutcome o) {
        art.show(o, 1f);
        title.setText(o.title());
        UiKit.setWrapped(description, o.description(), CARD_WIDTH - 80);
        chips.removeAll();
        chips.add(UiKit.pill(o.percent() + "% chance", new Color(0xEFE6FF), new Color(0x5B16C7)));
        chips.add(UiKit.pill(effectTag(o), new Color(0xEEF1F8), Theme.TEXT));
        if (detail != null) {
            UiKit.setWrapped(detailLabel, detail, CARD_WIDTH - 80);
        } else {
            detailLabel.setText(" ");
        }
        if (note != null) {
            UiKit.setWrapped(noteLabel, note, CARD_WIDTH - 80);
        } else {
            noteLabel.setText(" ");
        }
        primary.setEnabled(true);
        primary.requestFocusInWindow();
        revalidate();
        repaint();
    }

    private static String effectTag(SurpriseOutcome o) {
        return switch (o) {
            case JUMP_TO_FINISH -> "Piece finishes";
            case OPPONENT_BACK -> "Opponent moves back";
            case BACK_TO_CASTLE -> "Piece to castle";
            case BACK_THREE -> "3 steps back";
            case SKIP_TURN -> "Next turn skipped";
            case EXTRA_ROLL -> "Roll again now";
        };
    }

    public CustomButton primaryButton() {
        return primary;
    }

    static Icons.Type iconFor(SurpriseOutcome o) {
        return switch (o) {
            case JUMP_TO_FINISH -> Icons.Type.ROCKET;
            case OPPONENT_BACK -> Icons.Type.PUSH;
            case BACK_TO_CASTLE -> Icons.Type.CASTLE;
            case BACK_THREE -> Icons.Type.STEPS_BACK;
            case SKIP_TURN -> Icons.Type.SKIP;
            case EXTRA_ROLL -> Icons.Type.DICE;
        };
    }

    /** Large round illustration of an outcome. */
    private static final class OutcomeArt extends JComponent {
        private SurpriseOutcome shown;
        private float strength = 1f;

        OutcomeArt() {
            setPreferredSize(new Dimension(110, 110));
            setMaximumSize(new Dimension(110, 110));
        }

        void show(SurpriseOutcome o, float strength) {
            this.shown = o;
            this.strength = strength;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (shown == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            double s = 104;
            double x = (getWidth() - s) / 2;
            g2.setColor(Theme.alpha(new Color(0x8D4BFF), (int) (40 * strength)));
            g2.fill(new Ellipse2D.Double(x - 2, 1, s + 4, s + 4));
            g2.setPaint(new GradientPaint(0, 0, Theme.alpha(new Color(0xA46BFF), (int) (255 * strength)), 0, (float) s,
                    Theme.alpha(new Color(0x5B16C7), (int) (255 * strength))));
            g2.fill(new Ellipse2D.Double(x + 6, 9, s - 12, s - 12));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new Ellipse2D.Double(x + 6, 9, s - 12, s - 12));
            Icons.paint(g2, iconFor(shown), x + s / 2 - 25, 9 + (s - 12) / 2 - 25, 50, Color.WHITE);
            if (shown == SurpriseOutcome.BACK_THREE) {
                g2.setFont(Theme.font(Font.BOLD, 15f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("3", (float) (x + s / 2 - fm.stringWidth("3") / 2.0), 92f);
            }
            g2.dispose();
        }
    }
}
