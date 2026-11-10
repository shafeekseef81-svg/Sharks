package View;

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
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import View.components.ColorSwatch;
import View.components.CustomButton;
import View.components.CustomPanel;
import View.components.GradientBackground;
import View.components.Icons;
import View.components.StyledTextField;
import View.components.Theme;
import View.components.ToggleChip;
import View.components.UiKit;
import Model.Difficulty;
import Model.DieType;
import Model.GameSettings;
import Model.PlayerColor;

/** Screen 2: game setup (FR-04..FR-09) with inline validation messages. */
public class GameSetupView extends GradientBackground implements ScreenView {

    private static final PlayerColor[] DEFAULT_COLORS = {PlayerColor.RED, PlayerColor.YELLOW, PlayerColor.GREEN,
            PlayerColor.BLUE};

    private final CustomButton back = new CustomButton("Main Menu", Icons.get(Icons.Type.BACK, 18, Color.WHITE),
            CustomButton.Variant.LIGHT);
    private final CustomButton start = new CustomButton("Start Game", Icons.get(Icons.Type.PLAY, 18, Color.WHITE),
            CustomButton.Variant.PRIMARY);
    private final Map<Integer, ToggleChip> countChips = new java.util.HashMap<>();
    private final Map<Difficulty, DifficultyCard> difficultyCards = new EnumMap<>(Difficulty.class);
    private final List<PlayerRow> rows = new ArrayList<>();
    private final JLabel summary = UiKit.label(" ", Theme.font(Font.BOLD, 14f), new Color(0xFFB4B4));
    private int playerCount = 2;
    private Difficulty difficulty = Difficulty.EASY;
    private BiFunction<Difficulty, List<GameSettings.PlayerSetup>, List<GameSettings.FieldError>> startHandler;

    public GameSetupView() {
        super(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(22, 40, 20, 40));
        back.setName("setup.back");
        start.setName("setup.start");
        start.setFont(Theme.font(Font.BOLD, 17f));
        add(UiKit.header(back, "New Game",
                "Choose the number of players and the difficulty, then enter each player's name and colour.", null),
                BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.43;
        c.insets = new Insets(0, 0, 0, 18);
        center.add(optionsCard(), c);
        c.weightx = 0.57;
        c.insets = new Insets(0, 0, 0, 0);
        center.add(playersCard(), c);
        add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setOpaque(false);
        summary.setName("setup.summary");
        footer.add(summary, BorderLayout.CENTER);
        start.setPreferredSize(new Dimension(220, 54));
        footer.add(start, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        start.addActionListener(e -> submit());
        setPlayerCount(2);
        setDifficulty(Difficulty.EASY);
    }

    private JComponent optionsCard() {
        CustomPanel card = new CustomPanel(new BorderLayout(), CustomPanel.Style.CARD);
        card.setPadding(18, 20, 18, 20);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(section("Number of players"));
        body.add(UiKit.vgap(10));
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chips.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (int n = 2; n <= 4; n++) {
            ToggleChip chip = new ToggleChip(n + " players", false);
            chip.setName("setup.players." + n);
            int count = n;
            chip.addActionListener(e -> setPlayerCount(count));
            group.add(chip);
            countChips.put(n, chip);
            chips.add(chip);
            chips.add(UiKit.hgap(8));
        }
        chips.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(chips);
        body.add(UiKit.vgap(20));
        body.add(section("Difficulty"));
        body.add(UiKit.vgap(10));
        ButtonGroup levels = new ButtonGroup();
        for (Difficulty d : Difficulty.values()) {
            DifficultyCard dc = new DifficultyCard(d);
            dc.setName("setup.level." + d.name().toLowerCase());
            dc.addActionListener(e -> setDifficulty(d));
            if (!playable(d)) {
                // FR-05 is partial in Iteration 1: the level is shown with its board and dice, but it
                // cannot be chosen, because its dice are added in Iteration 2 (SDP 5.2.1, US-2.1).
                dc.setEnabled(false);
                dc.setCursor(Cursor.getDefaultCursor());
                dc.setToolTipText(d.label() + " needs the " + d.specialDie().label()
                        + " die, which is added in Iteration 2. Iteration 1 plays Easy.");
            }
            levels.add(dc);
            difficultyCards.put(d, dc);
            dc.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(dc);
            body.add(UiKit.vgap(8));
        }
        card.add(body, BorderLayout.NORTH);
        return card;
    }

    private JComponent playersCard() {
        CustomPanel card = new CustomPanel(new BorderLayout(), CustomPanel.Style.CARD);
        card.setPadding(18, 22, 12, 22);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(section("Players"), BorderLayout.WEST);
        head.add(UiKit.label("Turn order follows this list", Theme.font(Font.PLAIN, 13f), Theme.TEXT_MUTED),
                BorderLayout.EAST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        body.add(head);
        body.add(UiKit.vgap(12));
        for (int i = 0; i < 4; i++) {
            PlayerRow row = new PlayerRow(i, DEFAULT_COLORS[i]);
            rows.add(row);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(row);
        }
        card.add(body, BorderLayout.NORTH);
        return card;
    }

    private static JLabel section(String text) {
        JLabel l = UiKit.label(text, Theme.font(Font.BOLD, 16f), Theme.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ------------------------------------------------------------------ behaviour

    public void setStartHandler(
            BiFunction<Difficulty, List<GameSettings.PlayerSetup>, List<GameSettings.FieldError>> handler) {
        this.startHandler = handler;
    }

    public void setBackAction(Runnable onBack) {
        back.addActionListener(e -> onBack.run());
    }

    public void setPlayerCount(int count) {
        playerCount = count;
        countChips.get(count).setSelected(true);
        for (int i = 0; i < rows.size(); i++) {
            PlayerRow row = rows.get(i);
            boolean visible = i < count;
            if (visible && !row.isVisible()) {
                for (int j = 0; j < i; j++) {
                    if (rows.get(j).color == row.color) {
                        row.setColor(firstFreeColor(i));
                        break;
                    }
                }
            }
            row.setVisible(visible);
            if (!visible) {
                row.clearError();
            }
        }
        refreshSwatches();
        summary.setText(" ");
        revalidate();
        repaint();
    }

    /**
     * Whether a level can be played in this iteration. Easy rolls the Regular die only; Medium and Hard
     * need the Boost and UltraBoost dice, which {@code RandomDice} refuses until Iteration 2 (FR-05
     * partial). Asking the level itself keeps the screen and the dice in step: as soon as the dice of
     * Iteration 2 arrive, the levels unlock here on their own.
     *
     * @param d the level
     * @return {@code true} if a game of this level can be played now
     */
    public static boolean playable(Difficulty d) {
        return d != null && d.specialDie() == DieType.NONE;
    }

    /**
     * Chooses the level. A level that cannot be played yet is ignored and the current choice stays, the
     * same as clicking its card, which is disabled (FR-05 partial in Iteration 1).
     *
     * @param d the level to choose
     */
    public void setDifficulty(Difficulty d) {
        if (!playable(d)) {
            return;
        }
        difficulty = d;
        difficultyCards.get(d).setSelected(true);
        repaint();
    }

    /** @return the chosen level */
    public Difficulty difficulty() {
        return difficulty;
    }

    public void setPlayerName(int index, String name) {
        rows.get(index).name.setText(name);
    }

    public void setPlayerColor(int index, PlayerColor color) {
        rows.get(index).setColor(color);
        refreshSwatches();
    }

    public int playerCount() {
        return playerCount;
    }

    /** Validation messages currently shown (used by the self-test). */
    public List<String> visibleErrors() {
        List<String> list = new ArrayList<>();
        for (PlayerRow row : rows) {
            if (row.isVisible() && !row.error.getText().isBlank()) {
                list.add(row.error.getText());
            }
        }
        return list;
    }

    private PlayerColor firstFreeColor(int forRow) {
        for (PlayerColor c : PlayerColor.values()) {
            boolean taken = false;
            for (int j = 0; j < playerCount && j < rows.size(); j++) {
                if (j != forRow && rows.get(j).isVisible() && rows.get(j).color == c) {
                    taken = true;
                }
            }
            if (!taken) {
                return c;
            }
        }
        return PlayerColor.values()[forRow];
    }

    private void refreshSwatches() {
        for (int i = 0; i < rows.size(); i++) {
            PlayerRow row = rows.get(i);
            for (ColorSwatch sw : row.swatches) {
                String owner = null;
                for (int j = 0; j < playerCount; j++) {
                    if (j != i && rows.get(j).color == sw.playerColor()) {
                        owner = "Player " + (j + 1);
                    }
                }
                sw.setEnabled(owner == null);
                sw.setToolTipText(owner == null ? sw.playerColor().label() : sw.playerColor().label() + " is taken by " + owner);
            }
            row.repaint();
        }
    }

    private void submit() {
        List<GameSettings.PlayerSetup> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            PlayerRow row = rows.get(i);
            players.add(new GameSettings.PlayerSetup(row.name.getText(), row.color));
        }
        for (PlayerRow row : rows) {
            row.clearError();
        }
        if (!playable(difficulty)) {
            // Cannot normally happen: the card of such a level is disabled. The guard is here so that no
            // route can start a game whose first roll would fail (FR-05 partial, SDP Iteration 1).
            summary.setText(difficulty.label() + " is added in Iteration 2. Choose Easy to play now.");
            return;
        }
        List<GameSettings.FieldError> errors = startHandler == null ? List.of() : startHandler.apply(difficulty, players);
        if (errors.isEmpty()) {
            summary.setText(" ");
            return;
        }
        for (GameSettings.FieldError e : errors) {
            if (e.player() >= 0 && e.player() < rows.size()) {
                rows.get(e.player()).showError(e.field(), e.message());
            }
        }
        summary.setText(errors.size() == 1 ? "Please fix the highlighted field to start the game."
                : "Please fix the " + errors.size() + " highlighted fields to start the game.");
        for (GameSettings.FieldError e : errors) {
            if (e.player() >= 0 && "name".equals(e.field())) {
                rows.get(e.player()).name.requestFocusInWindow();
                break;
            }
        }
    }

    @Override
    public JButton defaultButton() {
        return start;
    }

    @Override
    public void onShow() {
        summary.setText(" ");
        for (PlayerRow row : rows) {
            row.clearError();
        }
    }

    // ------------------------------------------------------------------ parts

    /** One player: number badge, name field with counter, colour swatches and an error line. */
    private final class PlayerRow extends JPanel {
        private final StyledTextField name = new StyledTextField("Player name (1-12 characters)",
                Icons.get(Icons.Type.USER, 16, new Color(0x8C94AE)));
        private final JLabel counter = UiKit.label("0/12", Theme.font(Font.PLAIN, 12.5f), Theme.TEXT_MUTED);
        private final JLabel error = UiKit.label(" ", Theme.font(Font.PLAIN, 13f), Theme.DANGER);
        private final List<ColorSwatch> swatches = new ArrayList<>();
        private final Badge badge;
        private PlayerColor color;

        PlayerRow(int index, PlayerColor initial) {
            super(new BorderLayout(0, 2));
            setOpaque(false);
            this.color = initial;
            badge = new Badge(index + 1);
            name.setName("setup.name." + index);
            name.setMaxLength(GameSettings.MAX_NAME_LENGTH);
            name.setColumns(12);
            name.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    changed();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    changed();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    changed();
                }
            });
            JPanel line = new JPanel(new GridBagLayout());
            line.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(0, 0, 0, 12);
            line.add(badge, c);
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 0, 8);
            line.add(name, c);
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            counter.setPreferredSize(new Dimension(40, 20));
            line.add(counter, c);
            ButtonGroup group = new ButtonGroup();
            c.insets = new Insets(0, 0, 0, 2);
            for (PlayerColor pc : PlayerColor.values()) {
                ColorSwatch sw = new ColorSwatch(pc);
                sw.setName("setup.color." + index + "." + pc.name().toLowerCase());
                sw.setSelected(pc == initial);
                sw.addActionListener(e -> {
                    setColor(pc);
                    refreshSwatches();
                });
                group.add(sw);
                swatches.add(sw);
                line.add(sw, c);
            }
            add(line, BorderLayout.CENTER);
            error.setBorder(BorderFactory.createEmptyBorder(0, 50, 6, 0));
            error.setName("setup.error." + index);
            add(error, BorderLayout.SOUTH);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        }

        void setColor(PlayerColor c) {
            color = c;
            for (ColorSwatch sw : swatches) {
                sw.setSelected(sw.playerColor() == c);
            }
            badge.repaint();
        }

        private void changed() {
            counter.setText(name.getText().length() + "/12");
            if (name.hasError()) {
                name.setError(false);
                error.setText(" ");
            }
        }

        void showError(String field, String message) {
            if ("name".equals(field)) {
                name.setError(true);
            }
            String current = error.getText().isBlank() ? "" : error.getText() + "  ";
            error.setText(current + message);
        }

        void clearError() {
            name.setError(false);
            error.setText(" ");
        }

        /** Circle with the player number in the chosen colour. */
        private final class Badge extends JComponent {
            private final int number;

            Badge(int number) {
                this.number = number;
                setPreferredSize(new Dimension(38, 38));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(Theme.player(color));
                g2.fill(new Ellipse2D.Double(2, 2, 34, 34));
                g2.setColor(Theme.playerDark(color));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(2, 2, 34, 34));
                g2.setFont(Theme.font(Font.BOLD, 16f));
                FontMetrics fm = g2.getFontMetrics();
                String s = String.valueOf(number);
                g2.setColor(Theme.textOn(color));
                g2.drawString(s, 19 - fm.stringWidth(s) / 2f, 19 + (fm.getAscent() - fm.getDescent()) / 2f);
                g2.dispose();
            }
        }
    }

    /** Selectable card describing a difficulty level (board size, special squares, dice). */
    private static final class DifficultyCard extends JToggleButton {
        private final Difficulty level;
        private boolean hover;

        DifficultyCard(Difficulty level) {
            this.level = level;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(420, 80));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            getAccessibleContext().setAccessibleName(level.label());
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

        private String line1() {
            return switch (level) {
                case EASY -> "Classic 15 x 15 board  •  52 track squares";
                case MEDIUM -> "17 x 17 board  •  52 + 64 track squares";
                case HARD -> "19 x 19 board  •  52 + 64 + 72 track squares";
            };
        }

        private String line2() {
            return switch (level) {
                case EASY -> "4 protection  •  4 arrows  •  Regular die";
                case MEDIUM -> "6 questions  •  2 surprises  •  Regular + Boost dice";
                case HARD -> "9 questions  •  3 surprises  •  Regular + UltraBoost dice";
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth();
            int h = getHeight();
            boolean available = isEnabled();
            Color accent = available ? Theme.level(level) : Theme.alpha(Theme.level(level), 90);
            RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 14, 14);
            g2.setColor(!available ? new Color(0xF4F6FA)
                    : isSelected() ? Theme.levelLight(level) : hover ? new Color(0xF6F8FC) : Color.WHITE);
            g2.fill(shape);
            g2.setColor(isSelected() && available ? accent : Theme.BORDER);
            g2.setStroke(new BasicStroke(isSelected() && available ? 2.4f : 1.2f));
            g2.draw(shape);
            if (isFocusOwner()) {
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(4, 4, w - 8, h - 8, 11, 11));
            }
            // ring icon: one square per track ring
            double ix = 16;
            double iy = (h - 40) / 2.0;
            int rings = level.outerRings() + 1;
            for (int i = 0; i < rings; i++) {
                double inset = i * 6;
                g2.setColor(i == rings - 1 ? accent : Theme.alpha(accent, 120));
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(new Rectangle2D.Double(ix + inset, iy + inset, 40 - inset * 2, 40 - inset * 2));
            }
            int tx = 74;
            g2.setFont(Theme.font(Font.BOLD, 17f));
            g2.setColor(available ? Theme.TEXT : Theme.TEXT_MUTED);
            g2.drawString(level.label(), tx, 26);
            if (!available) {
                // Says plainly why the level cannot be chosen yet (FR-05 partial).
                Font tagFont = Theme.font(Font.BOLD, 12f);
                String tag = "Iteration 2";
                FontMetrics tagMetrics = g2.getFontMetrics(tagFont);
                int tagWidth = tagMetrics.stringWidth(tag) + 18;
                int tagHeight = tagMetrics.getAscent() + tagMetrics.getDescent() + 6;
                int tagX = tx + g2.getFontMetrics(Theme.font(Font.BOLD, 17f)).stringWidth(level.label()) + 10;
                int tagY = 26 - tagMetrics.getAscent();
                g2.setColor(new Color(0xE4E8F0));
                g2.fillRoundRect(tagX, tagY, tagWidth, tagHeight, tagHeight, tagHeight);
                g2.setFont(tagFont);
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(tag, tagX + 9, tagY + tagMetrics.getAscent() + 3);
            }
            g2.setFont(Theme.font(Font.PLAIN, 13f));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(line1(), tx, 47);
            g2.drawString(line2(), tx, 66);
            if (isSelected() && available) {
                g2.setColor(accent);
                g2.fill(new Ellipse2D.Double(w - 34, h / 2.0 - 11, 22, 22));
                Icons.paint(g2, Icons.Type.CHECK, w - 30, h / 2.0 - 7, 14, Color.WHITE);
            }
            g2.dispose();
        }
    }
}
