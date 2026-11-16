package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import View.components.BoardComponent;
import View.components.CustomButton;
import View.components.DiceComponent;
import View.components.GradientBackground;
import View.components.Icons;
import View.components.Theme;
import View.components.UiKit;
import Model.DieType;
import Model.GameModel;
import Model.PlayerColor;

/**
 * Screen 1: main menu (FR-01, FR-02).
 *
 * <p>The screen has two parts side by side: a decorative board preview and the menu column with the title
 * and the four options. The menu column is the part the player needs (FR-01), so it always keeps its
 * preferred width and never gives room away; the preview is decoration and takes whatever is left over.
 * This keeps the menu readable on a small window and with large system fonts (NFR-23, NFR-24).
 */
public class MainMenuView extends GradientBackground implements ScreenView {

    /** Space between the board preview and the menu column. */
    private static final int COLUMN_GAP = 56;
    /**
     * Pixels kept free when the preview is measured. Without it the layout would need exactly the width
     * it has, and one pixel of difference in the font metrics of a computer would be enough for
     * {@link GridBagLayout} to fall back to minimum sizes and squeeze the menu column.
     */
    private static final int SLACK = 4;

    private final CustomButton newGame;
    private final CustomButton history;
    private final CustomButton questions;
    private final CustomButton exit;
    private final Hero hero;
    private final JComponent column;

    public MainMenuView(GameModel previewGame) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(26, 48, 20, 48));

        newGame = menuButton("New Game", Icons.Type.PLAY, CustomButton.Variant.PRIMARY, "menu.newGame");
        newGame.setFont(Theme.font(Font.BOLD, 18f));
        history = menuButton("Game History", Icons.Type.HISTORY, CustomButton.Variant.LIGHT, "menu.history");
        questions = menuButton("Question Management", Icons.Type.LOCK, CustomButton.Variant.LIGHT, "menu.questions");
        // Both options are offered in full (FR-01) but their screens arrive in Iteration 3 (FR-02 partial).
        // The old tip promised a login that does not exist in this iteration.
        history.setToolTipText("The game history is part of Iteration 3 of the project.");
        questions.setToolTipText("Question management with administrator login is part of Iteration 3.");
        exit = menuButton("Exit", Icons.Type.EXIT, CustomButton.Variant.GHOST, "menu.exit");

        hero = new Hero(previewGame);
        column = menuColumn();

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, COLUMN_GAP);
        center.add(hero, c);
        c.insets = new Insets(0, 0, 0, 0);
        center.add(column, c);
        add(center, BorderLayout.CENTER);
    }

    /**
     * Tells the board preview how much room is left for it before the screen is laid out, so the menu
     * column can always be shown at its full width. Everything below is laid out as usual afterwards.
     *
     * <p>This only passes two numbers on; it does not hide or resize anything itself. Changing the state
     * of a component while the screen is being laid out is what made the preview disappear in the running
     * application even though every test showed it.
     */
    @Override
    public void doLayout() {
        Insets in = getInsets();
        int spare = getWidth() - in.left - in.right - column.getPreferredSize().width - COLUMN_GAP - SLACK;
        hero.setBudget(spare, getHeight() - in.top - in.bottom);
        super.doLayout();
    }

    private JComponent menuColumn() {
        // The width of this column is its minimum as well: the title, the subtitle and the option labels
        // must never be cut off or shortened to "...", however small the window is (FR-01).
        JPanel col = new JPanel() {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(getPreferredSize().width, super.getMinimumSize().height);
            }
        };
        col.setName("menu.column");
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        Font eyebrowFont = Theme.font(Font.BOLD, 13f).deriveFont(Map.of(TextAttribute.TRACKING, 0.18f));
        JLabel eyebrow = UiKit.label("TEAM SHARKS PRESENTS", eyebrowFont, new Color(0xFFC861));
        eyebrow.setName("menu.eyebrow");
        col.add(left(eyebrow));
        col.add(gap(6));
        col.add(left(new Title()));
        col.add(gap(8));
        JLabel subtitle = UiKit.label("Learn Software Engineering While Playing", Theme.font(Font.PLAIN, 19f),
                Theme.TEXT_ON_DARK_MUTED);
        subtitle.setName("menu.subtitle");
        col.add(left(subtitle));
        col.add(gap(30));
        for (JButton b : new JButton[] {newGame, history, questions, exit}) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(390, b == newGame ? 60 : 54));
            b.setPreferredSize(new Dimension(390, b == newGame ? 60 : 54));
            boolean laterIteration = b == history || b == questions;
            JComponent option = laterIteration ? new Option(b, b.getName() + ".badge") : b;
            option.setAlignmentX(Component.LEFT_ALIGNMENT);
            col.add(option);
            col.add(gap(12));
        }
        return col;
    }

    /**
     * Vertical space that may be squeezed when the window is short. A rigid area would keep its height and
     * push the Exit button past the bottom edge; this one gives its space up before the buttons shrink.
     */
    private static Component gap(int height) {
        return new Box.Filler(new Dimension(0, 0), new Dimension(0, height), new Dimension(0, height));
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static CustomButton menuButton(String text, Icons.Type icon, CustomButton.Variant variant, String name) {
        CustomButton b = new CustomButton(text, Icons.get(icon, 20, variant == CustomButton.Variant.PRIMARY
                ? Color.WHITE : new Color(0xE3E8F8)), variant);
        b.setName(name);
        return b;
    }

    public void setActions(Runnable onNewGame, Runnable onHistory, Runnable onQuestions, Runnable onExit) {
        newGame.addActionListener(e -> onNewGame.run());
        history.addActionListener(e -> onHistory.run());
        questions.addActionListener(e -> onQuestions.run());
        exit.addActionListener(e -> onExit.run());
    }

    @Override
    public JButton defaultButton() {
        return newGame;
    }

    /**
     * A menu option whose screen is only delivered in Iteration 3, with a small "Soon" tag at its right
     * end. The option keeps its label, stays enabled and still opens its notice when clicked (FR-01); the
     * tag only hints beforehand at what the notice will say (FR-02 is partial in Iteration 1). Which
     * iteration brings the feature is spelled out in the tip of the option and in the notice itself.
     *
     * <p>The tag is painted on top of the button and lets every mouse event through, so hovering and
     * clicking work exactly as they do on the other options.
     */
    private static final class Option extends JPanel {

        /** Distance of the tag from the right edge of the button. */
        private static final int RIGHT_MARGIN = 14;

        private final JButton button;
        private final ComingSoonTag tag;

        Option(JButton button, String tagName) {
            super(null);
            this.button = button;
            this.tag = new ComingSoonTag();
            tag.setName(tagName);
            setOpaque(false);
            add(tag);        // added first, so it is painted on top of the button
            add(button);
        }

        @Override
        public void doLayout() {
            button.setBounds(0, 0, getWidth(), getHeight());
            Dimension size = tag.getPreferredSize();
            int x = getWidth() - size.width - RIGHT_MARGIN;
            if (x < 0) {
                // No room for the tag on a very narrow button: show the option without it.
                tag.setBounds(0, 0, 0, 0);
            } else {
                tag.setBounds(x, (getHeight() - size.height) / 2, size.width, size.height);
            }
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return false;        // the tag overlaps the button
        }

        @Override
        public Dimension getPreferredSize() {
            return button.getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return button.getMinimumSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return button.getMaximumSize();
        }
    }

    /**
     * Small translucent "Soon" tag. It is invisible to the mouse, see {@link #contains}.
     *
     * <p>The word is short on purpose. The label of an option is centred over the whole button, so a
     * longer tag such as "Iteration 3" would run into "Question Management" at the 12pt that NFR-24 asks
     * for. Which iteration brings the feature is in the tip of the option and in the notice it opens.
     */
    private static final class ComingSoonTag extends JComponent {

        private static final String TEXT = "Soon";
        /** NFR-24: no text below 12pt. */
        private static final float FONT_SIZE = 12f;
        private static final int PADDING_X = 9;
        private static final int PADDING_Y = 3;

        ComingSoonTag() {
            setFont(Theme.font(Font.BOLD, FONT_SIZE).deriveFont(Map.of(TextAttribute.TRACKING, 0.08f)));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(TEXT) + PADDING_X * 2,
                    fm.getAscent() + fm.getDescent() + PADDING_Y * 2);
        }

        /**
         * Always false, so the tag never swallows a mouse event: Swing looks for the deepest component
         * that contains the point, skips this one and finds the button underneath. Hover and click on the
         * option keep working (FR-01).
         */
        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(255, 255, 255, 28));
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Theme.TEXT_ON_DARK_MUTED);
            g2.drawString(TEXT, (w - fm.stringWidth(TEXT)) / 2f,
                    (h + fm.getAscent() - fm.getDescent()) / 2f);
            g2.dispose();
        }
    }

    /**
     * Two-line title with "Ludo" in the four player colours.
     *
     * <p>The title measures itself with {@link FontMetrics} instead of using a fixed box. The display font
     * depends on the computer: Segoe UI Black (Windows) needs exactly 140px of height, while Arial Black
     * (macOS, where Segoe UI Black does not exist) needs 146px, so in the old fixed 430x140 box the tail of
     * "Ludo" was cut off there. The measured size is also the minimum size, so the text stays whole when the
     * window is made smaller (FR-01, NFR-23).
     */
    private static final class Title extends JComponent {

        /** The size this title was designed at, kept as the preferred size so the menu looks unchanged. */
        private static final int DESIGN_WIDTH = 430;
        private static final int DESIGN_HEIGHT = 140;

        private static final String TOP_LINE = "Educational";
        private static final String BOTTOM_LINE = "Ludo";

        /** The drop shadow of the top line is drawn 2px to the right and 3px down. */
        private static final int TOP_SHADOW_X = 2;
        private static final int TOP_SHADOW_Y = 3;
        /** The drop shadow of "Ludo" is drawn 3px to the right and 4px down. */
        private static final int BOTTOM_SHADOW_X = 3;
        private static final int BOTTOM_SHADOW_Y = 4;
        /** Space between the letters of "Ludo". */
        private static final int LETTER_GAP = 2;

        private final Font top = Theme.display(50f);
        private final Font bottom = Theme.display(68f);
        /** x position of every letter of "Ludo", measured once and reused when painting. */
        private final int[] letterX = new int[BOTTOM_LINE.length()];
        private final int baselineTop;
        private final int baselineBottom;

        Title() {
            setName("menu.title");
            BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = probe.createGraphics();
            // The same rendering hints as paintComponent, so the measurement matches the painting.
            Theme.antialias(g);
            FontMetrics fmTop = g.getFontMetrics(top);
            FontMetrics fmBottom = g.getFontMetrics(bottom);
            baselineTop = fmTop.getAscent() - 6;
            baselineBottom = baselineTop + fmBottom.getAscent() - 4;
            int right = fmTop.stringWidth(TOP_LINE) + TOP_SHADOW_X;
            int x = 0;
            for (int i = 0; i < BOTTOM_LINE.length(); i++) {
                letterX[i] = x;
                int letter = fmBottom.stringWidth(BOTTOM_LINE.substring(i, i + 1));
                right = Math.max(right, x + letter + BOTTOM_SHADOW_X);
                x += letter + LETTER_GAP;
            }
            // No letter of "Ludo" reaches below the baseline, so the descent is spare room on top of the
            // shadow. It keeps the measurement safe if the painting font metrics round differently.
            Dimension needed = new Dimension(right,
                    baselineBottom + BOTTOM_SHADOW_Y + fmBottom.getDescent());
            g.dispose();

            Dimension preferred = new Dimension(Math.max(DESIGN_WIDTH, needed.width),
                    Math.max(DESIGN_HEIGHT, needed.height));
            setPreferredSize(preferred);
            setMaximumSize(new Dimension(preferred));
            setMinimumSize(needed);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setFont(top);
            g2.setColor(new Color(0, 0, 0, 70));
            g2.drawString(TOP_LINE, TOP_SHADOW_X, baselineTop + TOP_SHADOW_Y);
            g2.setColor(Color.WHITE);
            g2.drawString(TOP_LINE, 0, baselineTop);
            g2.setFont(bottom);
            PlayerColor[] colors = {PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE};
            for (int i = 0; i < BOTTOM_LINE.length(); i++) {
                String ch = BOTTOM_LINE.substring(i, i + 1);
                g2.setColor(new Color(0, 0, 0, 80));
                g2.drawString(ch, letterX[i] + BOTTOM_SHADOW_X, baselineBottom + BOTTOM_SHADOW_Y);
                g2.setColor(Theme.player(colors[i]));
                g2.drawString(ch, letterX[i], baselineBottom);
            }
            g2.dispose();
        }
    }

    /**
     * Decorative preview: a small Easy board with a few pieces and two tilted dice.
     *
     * <p>The preview takes the room the menu column does not need ({@link #setBudget}). It shrinks with the
     * window and keeps its proportions, and it stays on the screen at every supported window size: the
     * room it asks for is only ever what is left over, so it cannot push the menu column off the screen
     * and never has to be hidden. It also reports a real minimum size: without one it collapsed to a
     * single pixel, because {@link GridBagLayout} lays its children out at their minimum size once space
     * runs short, and a panel with no layout manager reports almost nothing (NFR-23).
     */
    private static final class Hero extends JPanel {

        /** The size the preview was designed at. */
        private static final int DESIGN_WIDTH = 540;
        private static final int DESIGN_HEIGHT = 470;
        /** The smallest the preview may become; it is far below any supported window size. */
        private static final int MIN_WIDTH = 200;
        private static final int MIN_HEIGHT = 200;
        /** The board size at the designed size; the dice are placed and scaled relative to it. */
        private static final int DESIGN_BOARD = 450;

        private final BoardComponent board = new BoardComponent();
        private final DiceComponent die = new DiceComponent(DieType.REGULAR, 86);
        private final DiceComponent boost = new DiceComponent(DieType.BOOST, 74);
        private int budgetWidth = DESIGN_WIDTH;
        private int budgetHeight = DESIGN_HEIGHT;

        Hero(GameModel preview) {
            super(null);
            setName("menu.hero");
            setOpaque(false);
            board.setName("menu.board");
            board.setInteractive(false);
            board.setGame(preview);
            die.setValue(6);
            die.setDimmed(false);
            die.setTilt(-0.32);
            boost.setValue(3);
            boost.setDimmed(false);
            boost.setTilt(0.26);
            add(die);
            add(boost);
            add(board);
        }

        /**
         * Room the screen can spare for the preview. It only records the numbers: it must not change the
         * state of any component, because it is called while the screen is being laid out.
         */
        void setBudget(int width, int height) {
            budgetWidth = width;
            budgetHeight = height;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(clamp(budgetWidth, MIN_WIDTH, DESIGN_WIDTH),
                    clamp(budgetHeight, MIN_HEIGHT, DESIGN_HEIGHT));
        }

        /**
         * The minimum keeps the width the screen offered. The room offered is only what is left over, so
         * asking for it cannot push anything off the screen, and the board stays as large as it can even
         * when {@link GridBagLayout} falls back to minimum sizes in a short window.
         */
        @Override
        public Dimension getMinimumSize() {
            return isMinimumSizeSet() ? super.getMinimumSize()
                    : new Dimension(clamp(budgetWidth, MIN_WIDTH, DESIGN_WIDTH), MIN_HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(DESIGN_WIDTH, DESIGN_HEIGHT);
        }

        private static int clamp(int value, int low, int high) {
            return Math.max(low, Math.min(high, value));
        }

        @Override
        public void doLayout() {
            int size = Math.max(0, Math.min(getWidth() - 90, getHeight() - 20));
            board.setBounds(10, 4, size, size);
            // The dice shrink together with the board, so the preview keeps its proportions in a smaller
            // window. At the designed size the factor is 1 and the positions are exactly the designed ones.
            double scale = Math.min(1.0, size / (double) DESIGN_BOARD);
            die.setBounds(size - scaled(56, scale), size - scaled(78, scale),
                    scaled(92, scale), scaled(92, scale));
            boost.setBounds(size + 2, size - scaled(150, scale), scaled(80, scale), scaled(80, scale));
        }

        private static int scaled(int value, double scale) {
            return (int) Math.round(value * scale);
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            double r = getWidth() * 0.55;
            double cx = getWidth() * 0.47;
            double cy = getHeight() * 0.48;
            g2.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) r, new float[] {0f, 1f},
                    new Color[] {new Color(120, 150, 255, 70), new Color(120, 150, 255, 0)}));
            g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            g2.dispose();
        }
    }
}
