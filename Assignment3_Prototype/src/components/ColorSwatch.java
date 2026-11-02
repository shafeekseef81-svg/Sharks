package components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import javax.swing.JToggleButton;

import model.PlayerColor;

/** Round colour choice for the setup screen. Disabled when another player already took the colour. */
public class ColorSwatch extends JToggleButton {

    private final PlayerColor color;

    public ColorSwatch(PlayerColor color) {
        this.color = color;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(color.label());
        getAccessibleContext().setAccessibleName(color.label());
        setPreferredSize(new Dimension(38, 38));
        setMinimumSize(new Dimension(38, 38));
    }

    public PlayerColor playerColor() {
        return color;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        double s = Math.min(getWidth(), getHeight());
        double d = s - 10;
        double x = (getWidth() - d) / 2.0;
        double y = (getHeight() - d) / 2.0;
        Color fill = Theme.player(color);
        if (!isEnabled()) {
            fill = Theme.mix(fill, Color.WHITE, 0.65);
        }
        if (isSelected()) {
            g2.setColor(Theme.alpha(Theme.player(color), 70));
            g2.fill(new Ellipse2D.Double(x - 4, y - 4, d + 8, d + 8));
        }
        g2.setColor(fill);
        g2.fill(new Ellipse2D.Double(x, y, d, d));
        g2.setColor(isSelected() ? Theme.playerDark(color) : Theme.alpha(Theme.playerDark(color), 120));
        g2.setStroke(new BasicStroke(isSelected() ? 2.4f : 1.2f));
        g2.draw(new Ellipse2D.Double(x, y, d, d));
        if (isSelected()) {
            g2.setColor(Theme.textOn(color));
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double cx = x + d / 2;
            double cy = y + d / 2;
            g2.draw(new Line2D.Double(cx - d * 0.22, cy, cx - d * 0.05, cy + d * 0.17));
            g2.draw(new Line2D.Double(cx - d * 0.05, cy + d * 0.17, cx + d * 0.24, cy - d * 0.16));
        } else if (!isEnabled()) {
            g2.setColor(new Color(0x7A8199));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(x + d * 0.28, y + d * 0.28, x + d * 0.72, y + d * 0.72));
        }
        if (isFocusOwner()) {
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new Ellipse2D.Double(x - 3.5, y - 3.5, d + 7, d + 7));
        }
        g2.dispose();
    }
}
