package components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

import javax.swing.JComponent;

import model.DieType;

/** A die drawn with Java2D: white regular die, orange Boost die, purple UltraBoost die. */
public class DiceComponent extends JComponent {

    private static final int[][][] PIPS = {
            {},
            {{1, 1}},
            {{0, 0}, {2, 2}},
            {{0, 0}, {1, 1}, {2, 2}},
            {{0, 0}, {0, 2}, {2, 0}, {2, 2}},
            {{0, 0}, {0, 2}, {1, 1}, {2, 0}, {2, 2}},
            {{0, 0}, {0, 2}, {1, 0}, {1, 2}, {2, 0}, {2, 2}},
    };

    private final Random random = new Random();
    private DieType type;
    private int value;
    private double tilt;
    private double bounce;
    private boolean dimmed = true;

    public DiceComponent(DieType type, int size) {
        this.type = type;
        setOpaque(false);
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
    }

    public void setType(DieType type) {
        this.type = type;
        repaint();
    }

    public DieType getType() {
        return type;
    }

    /** Shows a face; 0 shows an empty die. */
    public void setValue(int value) {
        this.value = value;
        repaint();
    }

    public int getValue() {
        return value;
    }

    /** Static rotation in radians (decorative dice on the main menu). */
    public void setTilt(double radians) {
        this.tilt = radians;
        repaint();
    }

    /** Faded look used before the first roll of a turn. */
    public void setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
        repaint();
    }

    /** Short tumbling animation that ends on {@code finalValue} (NFR-03: under one second). */
    public void roll(int finalValue, Runnable onDone) {
        dimmed = false;
        int faces = Math.max(1, type.faces());
        int[] lastFace = {0};
        Animations.animate(620, t -> {
            int frame = (int) (t * 12);
            if (t < 1.0 && frame != lastFace[0]) {
                lastFace[0] = frame;
                value = 1 + random.nextInt(faces);
                tilt = (random.nextDouble() - 0.5) * 0.5 * (1 - t);
            }
            bounce = Math.sin(t * Math.PI * 3) * (1 - t) * 0.12;
            if (t >= 1.0) {
                value = finalValue;
                tilt = 0;
                bounce = 0;
            }
            repaint();
        }, onDone);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        double s = Math.min(getWidth(), getHeight()) - 6;
        double x = (getWidth() - s) / 2.0;
        double y = (getHeight() - s) / 2.0 - bounce * s;
        g2.rotate(tilt, getWidth() / 2.0, getHeight() / 2.0);
        if (dimmed) {
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.55f));
        }
        Color top;
        Color bottom;
        Color pip;
        Color edge;
        switch (type) {
            case BOOST -> {
                top = new Color(0xFFB347);
                bottom = new Color(0xF2661B);
                pip = Color.WHITE;
                edge = new Color(0xB8440C);
            }
            case ULTRA_BOOST -> {
                top = new Color(0xB57BFF);
                bottom = new Color(0x6A22D2);
                pip = Color.WHITE;
                edge = new Color(0x4B1599);
            }
            default -> {
                top = Color.WHITE;
                bottom = new Color(0xE6EAF3);
                pip = new Color(0x1B2240);
                edge = new Color(0xB9C1D6);
            }
        }
        double arc = s * 0.24;
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fill(new RoundRectangle2D.Double(x + 1, y + s * 0.06, s, s, arc, arc));
        g2.setPaint(new GradientPaint((float) x, (float) y, top, (float) x, (float) (y + s), bottom));
        RoundRectangle2D body = new RoundRectangle2D.Double(x, y, s, s, arc, arc);
        g2.fill(body);
        g2.setColor(edge);
        g2.setStroke(new BasicStroke((float) Math.max(1.2, s * 0.03)));
        g2.draw(body);
        g2.setColor(pip);
        if (value <= 0) {
            g2.setFont(Theme.font(Font.BOLD, (float) (s * 0.5)));
            g2.setColor(Theme.alpha(pip, 120));
            String q = "?";
            int w = g2.getFontMetrics().stringWidth(q);
            g2.drawString(q, (float) (x + (s - w) / 2), (float) (y + s * 0.68));
        } else {
            int[][] layout = PIPS[Math.min(6, value)];
            double r = s * 0.085;
            for (int[] p : layout) {
                double cx = x + s * (0.25 + p[1] * 0.25);
                double cy = y + s * (0.25 + p[0] * 0.25);
                g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            }
        }
        g2.dispose();
    }
}
