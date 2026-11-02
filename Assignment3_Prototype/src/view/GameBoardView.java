package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.font.TextAttribute;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import components.BoardComponent;
import components.CustomButton;
import components.CustomPanel;
import components.DiceComponent;
import components.GradientBackground;
import components.Icons;
import components.Theme;
import components.UiKit;
import model.DiceRoll;
import model.Difficulty;
import model.DieType;
import model.GameState;
import model.Player;
import model.PlayerColor;
import model.SquareType;

/**
 * Screen 3: the game board with the side panels (FR-10..FR-33 as an interactive prototype):
 * players, time and legend on the left; turn, dice and the prototype demo panel on the right.
 */
public class GameBoardView extends GradientBackground implements ScreenView {

    /** Tone of a status message. */
    public enum StatusKind { INFO, SUCCESS, WARNING, ERROR }

    private static final int LEFT_WIDTH = 236;
    private static final int RIGHT_WIDTH = 328;

    private final BoardComponent board = new BoardComponent();
    private final DiceComponent regularDie = new DiceComponent(DieType.REGULAR, 74);
    private final DiceComponent specialDie = new DiceComponent(DieType.BOOST, 74);
    private final JLabel times = UiKit.label("×", Theme.font(Font.BOLD, 26f), Theme.TEXT_ON_DARK_MUTED);
    private final JLabel regularCaption = caption("Regular (1-6)");
    private final JLabel specialCaption = caption("Boost (1-3)");
    private final JPanel specialBox = new JPanel(new BorderLayout(0, 4));
    private final JLabel formula = UiKit.label(" ", Theme.font(Font.BOLD, 18f), Color.WHITE);
    private final JLabel formulaDetail = UiKit.label(" ", Theme.font(Font.PLAIN, 13f), Theme.TEXT_ON_DARK_MUTED);
    private final CustomButton roll = new CustomButton("Roll Dice", Icons.get(Icons.Type.DICE, 20, Color.WHITE),
            CustomButton.Variant.PRIMARY);
    private final CustomButton back = new CustomButton("Main Menu", Icons.get(Icons.Type.BACK, 18, Color.WHITE),
            CustomButton.Variant.LIGHT);
    private final CustomButton demoQuestion = demoButton("Question", Icons.Type.QUESTION, "demo.question");
    private final CustomButton demoSurprise = demoButton("Surprise", Icons.Type.GIFT, "demo.surprise");
    private final CustomButton demoSix = demoButton("Next roll: 6", Icons.Type.DICE, "demo.six");
    private final CustomButton demoResults = demoButton("Show Results", Icons.Type.TROPHY, "demo.results");
    private final JLabel timer = UiKit.label("00:00:00", Theme.mono(Font.BOLD, 27f), Color.WHITE);
    private final UiKit.Pill levelPill = (UiKit.Pill) UiKit.pill("EASY", Color.WHITE, Theme.TEXT);
    private final JLabel boardSize = UiKit.label(" ", Theme.font(Font.BOLD, 14f), Color.WHITE);
    private final JLabel trackInfo = UiKit.label(" ", Theme.font(Font.PLAIN, 13f), Theme.TEXT_ON_DARK_MUTED);
    private final JPanel playerList = new JPanel();
    private final JPanel legend = new JPanel();
    private final TurnCard turnCard = new TurnCard();
    private final JLabel statusIcon = new JLabel();
    private final JLabel statusText = UiKit.label(" ", Theme.font(Font.PLAIN, 14f), Color.WHITE);
    private GameState state;
    private Runnable rollAction;

    public GameBoardView() {
        super(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        add(leftPanel(), BorderLayout.WEST);
        add(board, BorderLayout.CENTER);
        add(rightPanel(), BorderLayout.EAST);
        roll.setName("game.roll");
        back.setName("game.back");
        roll.setToolTipText("Roll the dice (Space)");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "roll");
        getActionMap().put("roll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowing() && roll.isEnabled() && rollAction != null
                        && components.ModalOverlay.openCount() == 0) {
                    rollAction.run();
                }
            }
        });
    }

    private static JLabel caption(String text) {
        JLabel l = UiKit.label(text, Theme.font(Font.PLAIN, 12.5f), Theme.TEXT_ON_DARK_MUTED);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    private static CustomButton demoButton(String text, Icons.Type icon, String name) {
        CustomButton b = new CustomButton(text, Icons.get(icon, 15, new Color(0xD7DEF5)), CustomButton.Variant.DEMO);
        b.setName(name);
        b.setIconTextGap(6);
        return b;
    }

    private static Font eyebrow() {
        return Theme.font(Font.BOLD, 12f).deriveFont(Map.of(TextAttribute.TRACKING, 0.12f));
    }

    // ------------------------------------------------------------------ layout

    private JComponent leftPanel() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(new Dimension(LEFT_WIDTH, 100));

        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.setMaximumSize(new Dimension(LEFT_WIDTH, 42));
        col.add(back);
        col.add(UiKit.vgap(10));

        CustomPanel info = glass(new BorderLayout(0, 6));
        JPanel infoTop = new JPanel(new BorderLayout(8, 0));
        infoTop.setOpaque(false);
        JPanel pillHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pillHolder.setOpaque(false);
        pillHolder.add(levelPill);
        infoTop.add(pillHolder, BorderLayout.WEST);
        infoTop.add(boardSize, BorderLayout.CENTER);
        info.add(infoTop, BorderLayout.NORTH);
        info.add(trackInfo, BorderLayout.CENTER);
        JPanel clock = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        clock.setOpaque(false);
        clock.add(new JLabel(Icons.get(Icons.Type.CLOCK, 22, Theme.TEXT_ON_DARK_MUTED)));
        timer.setName("game.timer");
        clock.add(timer);
        clock.setToolTipText("Elapsed game time (HH:MM:SS)");
        info.add(clock, BorderLayout.SOUTH);
        col.add(info);
        col.add(UiKit.vgap(10));

        CustomPanel players = glass(new BorderLayout(0, 8));
        players.add(UiKit.label("PLAYERS", eyebrow(), Theme.TEXT_ON_DARK_MUTED), BorderLayout.NORTH);
        playerList.setOpaque(false);
        playerList.setLayout(new BoxLayout(playerList, BoxLayout.Y_AXIS));
        players.add(playerList, BorderLayout.CENTER);
        col.add(players);
        col.add(UiKit.vgap(10));

        CustomPanel legendCard = glass(new BorderLayout(0, 8));
        legendCard.add(UiKit.label("BOARD LEGEND", eyebrow(), Theme.TEXT_ON_DARK_MUTED), BorderLayout.NORTH);
        legend.setOpaque(false);
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legendCard.add(legend, BorderLayout.CENTER);
        col.add(legendCard);
        col.add(Box.createVerticalGlue());
        return col;
    }

    private JComponent rightPanel() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(new Dimension(RIGHT_WIDTH, 100));

        turnCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(turnCard);
        col.add(UiKit.vgap(10));

        CustomPanel dice = glass(new BorderLayout(0, 8));
        dice.setPadding(14, 16, 16, 16);
        JPanel diceRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        diceRow.setOpaque(false);
        JPanel regularBox = new JPanel(new BorderLayout(0, 4));
        regularBox.setOpaque(false);
        regularBox.add(regularDie, BorderLayout.CENTER);
        regularBox.add(regularCaption, BorderLayout.SOUTH);
        specialBox.setOpaque(false);
        specialBox.add(specialDie, BorderLayout.CENTER);
        specialBox.add(specialCaption, BorderLayout.SOUTH);
        regularDie.setName("game.regularDie");
        specialDie.setName("game.specialDie");
        diceRow.add(regularBox);
        diceRow.add(times);
        diceRow.add(specialBox);
        dice.add(diceRow, BorderLayout.NORTH);
        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 2));
        texts.setOpaque(false);
        formula.setHorizontalAlignment(SwingConstants.CENTER);
        formula.setName("game.formula");
        formulaDetail.setHorizontalAlignment(SwingConstants.CENTER);
        texts.add(formula);
        texts.add(formulaDetail);
        dice.add(texts, BorderLayout.CENTER);
        roll.setFont(Theme.font(Font.BOLD, 18f));
        roll.setPreferredSize(new Dimension(RIGHT_WIDTH - 32, 54));
        dice.add(roll, BorderLayout.SOUTH);
        col.add(dice);
        col.add(UiKit.vgap(10));

        CustomPanel status = glass(new BorderLayout(10, 0));
        status.setPadding(12, 14, 12, 14);
        statusIcon.setVerticalAlignment(SwingConstants.TOP);
        status.add(statusIcon, BorderLayout.WEST);
        statusText.setName("game.status");
        status.add(statusText, BorderLayout.CENTER);
        col.add(status);
        col.add(Box.createVerticalGlue());
        col.add(UiKit.vgap(10));

        CustomPanel demo = new CustomPanel(new BorderLayout(0, 8), CustomPanel.Style.DASHED) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        demo.setPadding(10, 12, 12, 12);
        JPanel demoHead = new JPanel(new BorderLayout(8, 0));
        demoHead.setOpaque(false);
        demoHead.add(UiKit.pill("PROTOTYPE DEMO", new Color(0x8D5BFF), Color.WHITE), BorderLayout.WEST);
        demoHead.add(UiKit.label("try features quickly", Theme.font(Font.PLAIN, 12.5f), Theme.TEXT_ON_DARK_MUTED),
                BorderLayout.CENTER);
        demo.add(demoHead, BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8));
        grid.setOpaque(false);
        demoQuestion.setToolTipText("Prototype demo: open a question for the current player");
        demoSurprise.setToolTipText("Prototype demo: trigger a surprise event for the current player");
        demoSix.setToolTipText("Prototype demo: the next roll of the regular die shows a 6");
        demoResults.setToolTipText("Prototype demo: end the game now and show the results screen");
        grid.add(demoQuestion);
        grid.add(demoSurprise);
        grid.add(demoSix);
        grid.add(demoResults);
        demo.add(grid, BorderLayout.CENTER);
        demo.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(demo);
        for (Component c : col.getComponents()) {
            if (c instanceof JComponent jc) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        return col;
    }

    /** Glass card that keeps its preferred height inside the side columns. */
    private static CustomPanel glass(java.awt.LayoutManager layout) {
        CustomPanel p = new CustomPanel(layout, CustomPanel.Style.GLASS) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        p.setPadding(12, 14, 12, 14);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    // ------------------------------------------------------------------ binding

    public void setActions(Runnable onRoll, Runnable onBack, Runnable onDemoQuestion, Runnable onDemoSurprise,
                           Runnable onDemoSix, Runnable onDemoResults) {
        this.rollAction = onRoll;
        roll.addActionListener(e -> onRoll.run());
        back.addActionListener(e -> onBack.run());
        demoQuestion.addActionListener(e -> onDemoQuestion.run());
        demoSurprise.addActionListener(e -> onDemoSurprise.run());
        demoSix.addActionListener(e -> onDemoSix.run());
        demoResults.addActionListener(e -> onDemoResults.run());
    }

    public void bind(GameState state) {
        this.state = state;
        board.setGame(state);
        Difficulty d = state.difficulty();
        levelPill.setText(d.label().toUpperCase());
        levelPill.setPillColors(Theme.level(d), Color.WHITE);
        boardSize.setText(d.gridSize() + " x " + d.gridSize() + " board");
        trackInfo.setText("Track: " + d.trackFormula() + " squares");
        boolean special = d.specialDie() != DieType.NONE;
        specialDie.setType(special ? d.specialDie() : DieType.ULTRA_BOOST);
        specialCaption.setText(special ? specialLabel(d.specialDie()) : "UltraBoost (1-6)");
        demoQuestion.setToolTipText(hasQuestions()
                ? "Prototype demo: open a question for the current player"
                : "No question squares on Easy (SRS Table 1). Try a Medium or Hard game to see questions.");
        demoSurprise.setToolTipText(hasSurprises()
                ? "Prototype demo: trigger a surprise event for the current player"
                : "No surprise squares on Easy (SRS Table 1). Try a Medium or Hard game to see surprises.");
        buildLegend();
        resetDice();
        refresh();
    }

    private static String specialLabel(DieType type) {
        return type == DieType.BOOST ? "Boost (1-3)" : "UltraBoost (1-6)";
    }

    private void buildLegend() {
        legend.removeAll();
        Difficulty d = state.difficulty();
        legend.add(legendRow(new LegendIcon(SquareType.START, null), "Start square", 4));
        legend.add(legendRow(new LegendIcon(SquareType.PROTECTION, null), "Protection (safe)", d.protectionSquares()));
        legend.add(legendRow(new LegendIcon(SquareType.ARROW, null), "Arrow: turn inward", d.arrowSquares()));
        if (d.questionSquares() > 0) {
            legend.add(legendRow(new LegendIcon(SquareType.QUESTION, null), "Question E / M / H", d.questionSquares()));
        }
        if (d.surpriseSquares() > 0) {
            legend.add(legendRow(new LegendIcon(SquareType.SURPRISE, null), "Surprise (safe)", d.surpriseSquares()));
        }
        legend.add(legendRow(new LegendIcon(SquareType.HOME_PATH, null), "Home path", 6));
        legend.revalidate();
    }

    private static JComponent legendRow(Icon icon, String text, int count) {
        JPanel row = new JPanel(new BorderLayout(9, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        row.add(new JLabel(icon), BorderLayout.WEST);
        row.add(UiKit.label(text, Theme.font(Font.PLAIN, 13f), Color.WHITE), BorderLayout.CENTER);
        row.add(UiKit.label("x" + count, Theme.font(Font.BOLD, 13f), Theme.TEXT_ON_DARK_MUTED), BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return row;
    }

    /** Updates the turn card, the player list and the roll button colour. */
    public void refresh() {
        if (state == null) {
            return;
        }
        Player current = state.currentPlayer();
        List<Player> players = state.players();
        turnCard.show(current, players.indexOf(current) + 1, players.size(), state.isGameOver());
        roll.setAccent(Theme.player(current.color()));
        roll.setIcon(Icons.get(Icons.Type.DICE, 20, Theme.textOn(current.color())));
        playerList.removeAll();
        for (Player p : players) {
            playerList.add(new PlayerRow(p, p == current && !state.isGameOver()));
            playerList.add(UiKit.vgap(4));
        }
        playerList.revalidate();
        playerList.repaint();
        DiceRoll last = state.lastRoll();
        DieType shown = state.isSurpriseRollPending() ? DieType.ULTRA_BOOST
                : last != null && last.hasSpecial() ? last.specialType() : state.difficulty().specialDie();
        boolean special = shown != DieType.NONE;
        specialBox.setVisible(special);
        times.setVisible(special);
        if (special) {
            specialDie.setType(shown);
            specialCaption.setText(specialLabel(shown));
        }
    }

    /** Dims the dice and shows the hint before a roll. */
    public void resetDice() {
        regularDie.setDimmed(true);
        specialDie.setDimmed(true);
        formula.setText("Roll the dice");
        formulaDetail.setText(state != null && state.difficulty().specialDie() != DieType.NONE
                ? "Steps = Regular x (" + state.difficulty().specialDie().label() + " - 1)"
                : "Steps = Regular die");
    }

    /** Shows the dice values and the calculated movement (FR-17). */
    public void showRoll(DiceRoll r) {
        regularDie.setValue(r.regular());
        regularDie.setDimmed(false);
        if (r.hasSpecial()) {
            specialDie.setType(r.specialType());
            specialDie.setValue(r.special());
            specialDie.setDimmed(false);
        }
        if (r.cancelled()) {
            formula.setText("Third 6 in a row");
            formulaDetail.setText("This roll is cancelled (rule of 6)");
            return;
        }
        if (r.hasSpecial()) {
            formula.setText(r.regular() + " × (" + r.special() + " − 1) = " + r.movement() + " steps");
            formulaDetail.setText(r.surpriseRoll()
                    ? "Surprise extra roll with the UltraBoost die"
                    : r.movement() == 0 ? (r.regular() == 6 ? "0 steps, but a 6 can bring a piece out"
                    : "0 steps: no piece moves this time")
                    : "Home-path pieces move " + r.regular() + " (regular die only)");
        } else {
            formula.setText("Move " + r.movement() + (r.movement() == 1 ? " step" : " steps"));
            formulaDetail.setText(r.regular() == 6 ? "A 6 can bring a piece out of the castle" : "Regular die = " + r.regular());
        }
    }

    public void setStatus(String text, StatusKind kind) {
        Icons.Type icon = switch (kind) {
            case SUCCESS -> Icons.Type.CHECK;
            case WARNING -> Icons.Type.WARNING;
            case ERROR -> Icons.Type.CROSS;
            default -> Icons.Type.INFO;
        };
        Color color = switch (kind) {
            case SUCCESS -> new Color(0x6EE7A5);
            case WARNING -> new Color(0xFFC861);
            case ERROR -> new Color(0xFF9A9E);
            default -> new Color(0xA9C1FF);
        };
        statusIcon.setIcon(Icons.get(icon, 22, color));
        UiKit.setWrapped(statusText, text, RIGHT_WIDTH - 90);
    }

    public String statusText() {
        return statusText.getText().replaceAll("<[^>]+>", "").replace("&amp;", "&");
    }

    public void setElapsed(String hhmmss) {
        timer.setText(hhmmss);
    }

    /** Enables the roll button, the demo shortcuts and Main Menu (disabled only while dice or pieces move). */
    public void setControls(boolean rollEnabled, boolean demoEnabled, boolean backEnabled) {
        roll.setEnabled(rollEnabled);
        back.setEnabled(backEnabled);
        for (CustomButton b : new CustomButton[] {demoQuestion, demoSurprise, demoSix, demoResults}) {
            b.setEnabled(demoEnabled);
        }
        demoQuestion.setEnabled(demoEnabled && hasQuestions());
        demoSurprise.setEnabled(demoEnabled && hasSurprises());
    }

    /** Easy boards have no question squares (SRS Table 1), so the demo Question button stays off there. */
    private boolean hasQuestions() {
        return state == null || state.difficulty().questionSquares() > 0;
    }

    /** Easy boards have no surprise squares either (SRS Table 1). */
    private boolean hasSurprises() {
        return state == null || state.difficulty().surpriseSquares() > 0;
    }

    public CustomButton demoQuestionButton() {
        return demoQuestion;
    }

    public CustomButton demoSurpriseButton() {
        return demoSurprise;
    }

    public BoardComponent board() {
        return board;
    }

    public DiceComponent regularDie() {
        return regularDie;
    }

    public DiceComponent specialDie() {
        return specialDie;
    }

    public CustomButton rollButton() {
        return roll;
    }

    @Override
    public void onShow() {
        board.refreshStatic();
        roll.requestFocusInWindow();
    }

    // ------------------------------------------------------------------ parts

    /** Turn indicator in the active player's colour (FR-15, NFR-24: colour plus name). */
    private static final class TurnCard extends JPanel {
        private final JLabel eyebrowLabel = UiKit.label("CURRENT TURN", eyebrow(), Color.WHITE);
        private final JLabel name = UiKit.label(" ", Theme.font(Font.BOLD, 25f), Color.WHITE);
        private final JLabel detail = UiKit.label(" ", Theme.font(Font.PLAIN, 14f), Color.WHITE);
        private final JLabel pawn = new JLabel();
        private PlayerColor color = PlayerColor.RED;

        TurnCard() {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            JPanel texts = new JPanel();
            texts.setOpaque(false);
            texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
            texts.add(eyebrowLabel);
            texts.add(UiKit.vgap(2));
            name.setName("game.turnName");
            texts.add(name);
            texts.add(detail);
            add(pawn, BorderLayout.WEST);
            add(texts, BorderLayout.CENTER);
            setMaximumSize(new Dimension(RIGHT_WIDTH, 104));
            setPreferredSize(new Dimension(RIGHT_WIDTH, 104));
        }

        void show(Player p, int index, int total, boolean over) {
            color = p.color();
            Color text = Theme.textOn(color);
            eyebrowLabel.setText(over ? "GAME OVER" : "CURRENT TURN");
            eyebrowLabel.setForeground(Theme.alpha(text, 190));
            name.setText(p.name());
            name.setForeground(text);
            detail.setText(color.label() + "  •  Player " + index + " of " + total);
            detail.setForeground(Theme.alpha(text, 220));
            pawn.setIcon(BoardComponent.pawnIcon(color, 54));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            Color base = Theme.player(color);
            g2.setPaint(new GradientPaint(0, 0, Theme.mix(base, Color.WHITE, 0.12), getWidth(), getHeight(),
                    Theme.mix(base, Theme.playerDark(color), 0.45)));
            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 18, 18);
            g2.fill(shape);
            g2.setColor(new Color(255, 255, 255, 70));
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(new RoundRectangle2D.Double(0.7, 0.7, getWidth() - 1.4, getHeight() - 1.4, 18, 18));
            g2.dispose();
        }
    }

    /** Row of the player list: pawn, name, pieces at home, turn/skip/rank tags. */
    private static final class PlayerRow extends JPanel {
        private final boolean active;
        private final PlayerColor color;

        PlayerRow(Player p, boolean active) {
            super(new BorderLayout(8, 0));
            this.active = active;
            this.color = p.color();
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 8));
            add(new JLabel(BoardComponent.pawnIcon(p.color(), 26)), BorderLayout.WEST);
            JLabel name = UiKit.label(p.name(), Theme.font(Font.BOLD, 14f), Color.WHITE);
            name.setToolTipText(p.name() + " (" + p.color().label() + ")");
            add(name, BorderLayout.CENTER);
            JPanel tags = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            tags.setOpaque(false);
            if (p.rank() > 0) {
                tags.add(UiKit.pill("#" + p.rank(), Theme.GOLD, Theme.TEXT));
            } else if (p.skipNextTurn()) {
                tags.add(UiKit.pill("SKIPS", new Color(0xFFC861), Theme.TEXT));
            } else if (active) {
                tags.add(UiKit.pill("TURN", Theme.player(p.color()), Theme.textOn(p.color())));
            }
            tags.add(UiKit.label(p.finishedCount() + "/4", Theme.font(Font.BOLD, 13f), Theme.TEXT_ON_DARK_MUTED));
            tags.setToolTipText("Pieces at the finish");
            add(tags, BorderLayout.EAST);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!active) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(Theme.alpha(Theme.player(color), 70));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
            g2.setColor(Theme.alpha(Theme.player(color), 200));
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(new RoundRectangle2D.Double(0.7, 0.7, getWidth() - 1.4, getHeight() - 1.4, 12, 12));
            g2.dispose();
        }
    }

    /** Small board-cell icon used in the legend. */
    private static final class LegendIcon implements Icon {
        private final SquareType type;

        LegendIcon(SquareType type, Object unused) {
            this.type = type;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            double s = 22;
            Color fill = switch (type) {
                case START -> Theme.player(PlayerColor.RED);
                case HOME_PATH -> Theme.player(PlayerColor.BLUE);
                default -> Color.WHITE;
            };
            if (type == SquareType.QUESTION) {
                double w = s / 3.0;
                Difficulty[] levels = Difficulty.values();
                for (int i = 0; i < 3; i++) {
                    g2.setColor(Theme.levelLight(levels[i]));
                    g2.fill(new Rectangle2D.Double(x + i * w, y, w, s));
                    g2.setColor(Theme.level(levels[i]));
                    g2.fill(Icons.glyph("?", Theme.font(Font.BOLD, 30f), x + i * w + w / 2, y + s / 2, s * 0.55));
                }
                g2.setColor(Theme.BOARD_LINE);
                g2.draw(new Rectangle2D.Double(x, y, s, s));
                g2.dispose();
                return;
            }
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(x, y, s, s, 5, 5));
            g2.setColor(Theme.BOARD_LINE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Double(x, y, s, s, 5, 5));
            switch (type) {
                case START -> {
                    g2.setColor(Color.WHITE);
                    g2.fill(Icons.glyph("#", Theme.font(Font.BOLD, 30f), x + s / 2, y + s / 2, s * 0.6));
                }
                case PROTECTION -> {
                    java.awt.Shape star = Icons.star(x + s / 2, y + s / 2 + 0.6, s * 0.38, s * 0.16, 5);
                    g2.setColor(Theme.STAR);
                    g2.setStroke(new BasicStroke(1.3f));
                    g2.draw(star);
                }
                case ARROW -> Icons.paint(g2, Icons.Type.ARROW_RIGHT, x + 2, y + 2, s - 4, Theme.player(PlayerColor.GREEN));
                case SURPRISE -> Icons.gift(g2, x + 3, y + 2.5, s - 6, Theme.SURPRISE);
                default -> {
                    // plain coloured cell
                }
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 23;
        }

        @Override
        public int getIconHeight() {
            return 23;
        }
    }
}
