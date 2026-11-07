package View;

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
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;

import View.components.BoardComponent;
import View.components.Confetti;
import View.components.CustomButton;
import View.components.CustomPanel;
import View.components.GradientBackground;
import View.components.Icons;
import View.components.Theme;
import View.components.UiKit;
import Model.Difficulty;
import Model.Player;

/** Results screen (FR-43..FR-47): podium, difficulty, duration, Play Again and Main Menu. */
public class GameResultsView extends JPanel implements ScreenView {

    private final Confetti confetti = new Confetti();
    private final Podium podium = new Podium();
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
    private final JPanel extraPlaces = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    private final JLabel saved = UiKit.label(" ", Theme.font(Font.PLAIN, 14f), Theme.TEXT_ON_DARK_MUTED);
    private final CustomButton playAgain = new CustomButton("Play Again", Icons.get(Icons.Type.REFRESH, 18, Color.WHITE),
            CustomButton.Variant.PRIMARY);
    private final CustomButton menu = new CustomButton("Main Menu", Icons.get(Icons.Type.BACK, 18, Color.WHITE),
            CustomButton.Variant.LIGHT);

    public GameResultsView() {
        setLayout(new OverlayLayout(this));
        GradientBackground content = new GradientBackground(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(30, 40, 26, 40));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel trophy = new JLabel(Icons.get(Icons.Type.TROPHY, 52, Theme.GOLD));
        top.add(center(trophy));
        top.add(UiKit.vgap(6));
        JLabel title = UiKit.label("Game Completed!", Theme.display(42f), Color.WHITE);
        title.setName("results.title");
        top.add(center(title));
        top.add(UiKit.vgap(10));
        chips.setOpaque(false);
        top.add(center(chips));
        content.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        middle.add(UiKit.vgap(8));
        middle.add(center(podium));
        middle.add(UiKit.vgap(10));
        extraPlaces.setOpaque(false);
        middle.add(center(extraPlaces));
        content.add(middle, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        saved.setHorizontalAlignment(SwingConstants.CENTER);
        saved.setName("results.saved");
        bottom.add(center(saved));
        bottom.add(UiKit.vgap(14));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttons.setOpaque(false);
        playAgain.setName("results.playAgain");
        menu.setName("results.menu");
        playAgain.setPreferredSize(new Dimension(210, 52));
        menu.setPreferredSize(new Dimension(190, 52));
        buttons.add(playAgain);
        buttons.add(menu);
        bottom.add(center(buttons));
        content.add(bottom, BorderLayout.SOUTH);

        add(confetti);
        add(content);
    }

    private static JComponent center(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    public void setActions(Runnable onPlayAgain, Runnable onMenu) {
        playAgain.addActionListener(e -> onPlayAgain.run());
        menu.addActionListener(e -> onMenu.run());
    }

    /**
     * Shows the final results (FR-47).
     *
     * @param ranking      players in finishing order (FR-45)
     * @param difficulty   the game level
     * @param durationText the game duration as HH:MM:SS (FR-46)
     */
    public void showResults(List<Player> ranking, Difficulty difficulty, String durationText) {
        chips.removeAll();
        chips.add(UiKit.pill("Difficulty: " + difficulty.label(), Theme.level(difficulty), Color.WHITE));
        chips.add(UiKit.pill("Duration " + durationText, new Color(255, 255, 255, 40), Color.WHITE));
        chips.add(UiKit.pill(ranking.size() + " players", new Color(255, 255, 255, 40), Color.WHITE));
        podium.setRanking(ranking);
        extraPlaces.removeAll();
        for (int i = 3; i < ranking.size(); i++) {
            Player p = ranking.get(i);
            CustomPanel card = new CustomPanel(new BorderLayout(10, 0), CustomPanel.Style.GLASS);
            card.setPadding(8, 14, 8, 16);
            card.add(new JLabel(BoardComponent.pawnIcon(p.color(), 28)), BorderLayout.WEST);
            card.add(UiKit.label((i + 1) + "th place  •  " + p.name() + "  (" + p.color().label() + ")",
                    Theme.font(Font.BOLD, 15f), Color.WHITE), BorderLayout.CENTER);
            extraPlaces.add(card);
        }
        // Saving to the game history (FR-48) is planned for Iteration 3.
        saved.setText(" ");
        revalidate();
        repaint();
    }

    @Override
    public void onShow() {
        confetti.start();
    }

    @Override
    public JButton defaultButton() {
        return playAgain;
    }

    /** Podium for the first three places (2nd, 1st, 3rd from left to right). */
    private static final class Podium extends JComponent {
        private List<Player> ranking = new ArrayList<>();

        Podium() {
            setPreferredSize(new Dimension(660, 312));
            setMaximumSize(new Dimension(660, 312));
        }

        void setRanking(List<Player> ranking) {
            this.ranking = new ArrayList<>(ranking);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int[] order = {1, 0, 2};
            int[] heights = {160, 122, 98};
            Color[] colors = {Theme.GOLD, Theme.SILVER, Theme.BRONZE};
            double colW = 190;
            double gap = 20;
            double startX = (getWidth() - (colW * 3 + gap * 2)) / 2;
            double base = getHeight() - 4;
            for (int slot = 0; slot < 3; slot++) {
                int rank = order[slot];
                if (rank >= ranking.size()) {
                    continue;
                }
                Player p = ranking.get(rank);
                double x = startX + slot * (colW + gap);
                double h = heights[rank];
                double top = base - h;
                Color c = colors[rank];
                g2.setPaint(new GradientPaint(0, (float) top, Theme.mix(c, Color.WHITE, 0.25), 0, (float) base,
                        Theme.mix(c, Color.BLACK, 0.18)));
                g2.fill(new RoundRectangle2D.Double(x, top, colW, h + 14, 16, 16));
                g2.setFont(Theme.display(rank == 0 ? 46f : 38f));
                FontMetrics fm = g2.getFontMetrics();
                String num = String.valueOf(rank + 1);
                g2.setColor(new Color(255, 255, 255, 220));
                g2.drawString(num, (float) (x + colW / 2 - fm.stringWidth(num) / 2.0),
                        (float) (top + (h - 22) / 2 + fm.getAscent() * 0.36));

                double pawnY = top - 64;
                BoardComponent.paintPawn(g2, x + colW / 2, pawnY, 25, p.color());
                if (rank == 0) {
                    Icons.paint(g2, Icons.Type.CROWN, x + colW / 2 - 20, pawnY - 74, 40, Theme.GOLD);
                }
                g2.setFont(Theme.font(Font.BOLD, 18f));
                fm = g2.getFontMetrics();
                g2.setColor(Color.WHITE);
                String name = p.name();
                g2.drawString(name, (float) (x + colW / 2 - fm.stringWidth(name) / 2.0), (float) (top - 14));
                g2.setFont(Theme.font(Font.PLAIN, 13f));
                fm = g2.getFontMetrics();
                g2.setColor(new Color(30, 34, 60, 220));
                String place = placeLabel(rank) + "  •  " + p.color().label();
                g2.drawString(place, (float) (x + colW / 2 - fm.stringWidth(place) / 2.0), (float) (top + h - 12));
            }
            g2.dispose();
        }

        private static String placeLabel(int rank) {
            return switch (rank) {
                case 0 -> "1st place";
                case 1 -> "2nd place";
                default -> "3rd place";
            };
        }
    }
}
