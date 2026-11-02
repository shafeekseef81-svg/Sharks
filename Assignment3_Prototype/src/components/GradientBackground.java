package components;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;

import javax.swing.JPanel;

import model.PlayerColor;

/** Navy gradient screen background with soft glows in the four Ludo colours. */
public class GradientBackground extends JPanel {

    public GradientBackground(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(Theme.BG_TOP);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, Theme.BG_TOP, w * 0.3f, h, Theme.BG_BOTTOM));
        g2.fillRect(0, 0, w, h);
        glow(g2, -w * 0.08, -h * 0.15, Math.max(w, h) * 0.42, Theme.player(PlayerColor.RED));
        glow(g2, w * 0.86, -h * 0.2, Math.max(w, h) * 0.38, Theme.player(PlayerColor.GREEN));
        glow(g2, w * 0.9, h * 0.82, Math.max(w, h) * 0.4, Theme.player(PlayerColor.YELLOW));
        glow(g2, -w * 0.1, h * 0.78, Math.max(w, h) * 0.42, Theme.player(PlayerColor.BLUE));
        g2.setColor(new Color(255, 255, 255, 9));
        for (int y = 18; y < h; y += 36) {
            for (int x = 18; x < w; x += 36) {
                g2.fill(new Ellipse2D.Double(x - 1.2, y - 1.2, 2.4, 2.4));
            }
        }
        g2.dispose();
    }

    private static void glow(Graphics2D g2, double cx, double cy, double r, Color color) {
        RadialGradientPaint paint = new RadialGradientPaint((float) cx, (float) cy, (float) r,
                new float[] {0f, 1f}, new Color[] {Theme.alpha(color, 58), Theme.alpha(color, 0)});
        g2.setPaint(paint);
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }
}
