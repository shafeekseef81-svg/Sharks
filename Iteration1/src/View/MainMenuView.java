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
import java.util.Map;

import javax.swing.BorderFactory;
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

/** Screen 1: main menu (FR-01, FR-02). */
public class MainMenuView extends GradientBackground implements ScreenView {

    private final CustomButton newGame;
    private final CustomButton history;
    private final CustomButton questions;
    private final CustomButton exit;

    public MainMenuView(GameModel previewGame) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(26, 48, 20, 48));

        newGame = menuButton("New Game", Icons.Type.PLAY, CustomButton.Variant.PRIMARY, "menu.newGame");
        newGame.setFont(Theme.font(Font.BOLD, 18f));
        history = menuButton("Game History", Icons.Type.HISTORY, CustomButton.Variant.LIGHT, "menu.history");
        questions = menuButton("Question Management", Icons.Type.LOCK, CustomButton.Variant.LIGHT, "menu.questions");
        questions.setToolTipText("For administrators: requires a login");
        exit = menuButton("Exit", Icons.Type.EXIT, CustomButton.Variant.GHOST, "menu.exit");

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 56);
        center.add(new Hero(previewGame), c);
        c.insets = new Insets(0, 0, 0, 0);
        center.add(menuColumn(), c);
        add(center, BorderLayout.CENTER);
    }

    private JComponent menuColumn() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        Font eyebrowFont = Theme.font(Font.BOLD, 13f).deriveFont(Map.of(TextAttribute.TRACKING, 0.18f));
        JLabel eyebrow = UiKit.label("TEAM SHARKS PRESENTS", eyebrowFont, new Color(0xFFC861));
        col.add(left(eyebrow));
        col.add(UiKit.vgap(6));
        col.add(left(new Title()));
        col.add(UiKit.vgap(8));
        col.add(left(UiKit.label("Learn Software Engineering While Playing", Theme.font(Font.PLAIN, 19f),
                Theme.TEXT_ON_DARK_MUTED)));
        col.add(UiKit.vgap(30));
        for (JButton b : new JButton[] {newGame, history, questions, exit}) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(390, b == newGame ? 60 : 54));
            b.setPreferredSize(new Dimension(390, b == newGame ? 60 : 54));
            col.add(b);
            col.add(UiKit.vgap(12));
        }
        return col;
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

    /** Two-line title with "Ludo" in the four player colours. */
    private static final class Title extends JComponent {
        private final Font top = Theme.display(50f);
        private final Font bottom = Theme.display(68f);

        Title() {
            setPreferredSize(new Dimension(430, 140));
            setMaximumSize(new Dimension(430, 140));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setFont(top);
            FontMetrics fm = g2.getFontMetrics();
            int y1 = fm.getAscent() - 6;
            g2.setColor(new Color(0, 0, 0, 70));
            g2.drawString("Educational", 2, y1 + 3);
            g2.setColor(Color.WHITE);
            g2.drawString("Educational", 0, y1);
            g2.setFont(bottom);
            FontMetrics fm2 = g2.getFontMetrics();
            int y2 = y1 + fm2.getAscent() - 4;
            PlayerColor[] colors = {PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE};
            String word = "Ludo";
            int x = 0;
            for (int i = 0; i < word.length(); i++) {
                String ch = word.substring(i, i + 1);
                g2.setColor(new Color(0, 0, 0, 80));
                g2.drawString(ch, x + 3, y2 + 4);
                g2.setColor(Theme.player(colors[i]));
                g2.drawString(ch, x, y2);
                x += fm2.stringWidth(ch) + 2;
            }
            g2.dispose();
        }
    }

    /** Decorative preview: a small Easy board with a few pieces and two tilted dice. */
    private static final class Hero extends JPanel {
        private final BoardComponent board = new BoardComponent();
        private final DiceComponent die = new DiceComponent(DieType.REGULAR, 86);
        private final DiceComponent boost = new DiceComponent(DieType.BOOST, 74);

        Hero(GameModel preview) {
            super(null);
            setOpaque(false);
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
            setPreferredSize(new Dimension(540, 470));
        }

        @Override
        public void doLayout() {
            int size = Math.min(getWidth() - 90, getHeight() - 20);
            board.setBounds(10, 4, size, size);
            die.setBounds(size - 56, size - 78, 92, 92);
            boost.setBounds(size + 2, size - 150, 80, 80);
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
