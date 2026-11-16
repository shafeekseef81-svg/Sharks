package View.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/** Rounded card with an optional soft shadow, a "glass" style for the dark background, or a dashed outline. */
public class CustomPanel extends JPanel {

    /** Card styles. */
    public enum Style {
        /** White card with shadow. */
        CARD,
        /** Translucent card on the dark background. */
        GLASS,
        /** Dashed translucent box. */
        DASHED,
        /** Solid colour set with {@link #setFill(Color)}. */
        SOLID
    }

    private static final int SHADOW = 6;
    private final Style style;
    private Color fill = Color.WHITE;
    private Color outline;
    private int radius = 18;

    public CustomPanel(LayoutManager layout, Style style) {
        super(layout);
        this.style = style;
        setOpaque(false);
        setPadding(16);
    }

    /** Inner padding; the card style adds room for its shadow. */
    public final void setPadding(int padding) {
        setPadding(padding, padding, padding, padding);
    }

    public final void setPadding(int top, int left, int bottom, int right) {
        int extra = style == Style.CARD ? SHADOW / 2 : 0;
        setBorder(BorderFactory.createEmptyBorder(top + extra, left + extra, bottom + extra * 2, right + extra));
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    public void setOutline(Color outline) {
        this.outline = outline;
        repaint();
    }

    public void setRadius(int radius) {
        this.radius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = getHeight();
        switch (style) {
            case CARD -> {
                for (int i = 0; i < SHADOW; i++) {
                    g2.setColor(new Color(10, 20, 50, 9 + i * 2));
                    g2.fill(new RoundRectangle2D.Double(SHADOW / 2.0 - i / 2.0 + 0.5 * i, i * 0.6 + 1,
                            w - SHADOW, h - SHADOW + 1, radius + SHADOW - i, radius + SHADOW - i));
                }
                RoundRectangle2D card = new RoundRectangle2D.Double(SHADOW / 2.0, 0, w - SHADOW, h - SHADOW,
                        radius, radius);
                g2.setColor(fill);
                g2.fill(card);
                g2.setColor(outline != null ? outline : new Color(0, 0, 0, 18));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(card);
            }
            case GLASS -> {
                RoundRectangle2D card = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, radius, radius);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fill(card);
                g2.setColor(outline != null ? outline : new Color(255, 255, 255, 42));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(card);
            }
            case DASHED -> {
                RoundRectangle2D card = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, radius, radius);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fill(card);
                g2.setColor(outline != null ? outline : new Color(255, 255, 255, 80));
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                        new float[] {6f, 4f}, 0f));
                g2.draw(card);
            }
            case SOLID -> {
                RoundRectangle2D card = new RoundRectangle2D.Double(0, 0, w, h, radius, radius);
                g2.setColor(fill);
                g2.fill(card);
                if (outline != null) {
                    g2.setColor(outline);
                    g2.draw(card);
                }
            }
        }
        g2.dispose();
    }
}
