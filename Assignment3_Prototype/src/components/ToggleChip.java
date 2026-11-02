package components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;

/** Pill-shaped toggle used for segmented choices and filters. Works on light or dark backgrounds. */
public class ToggleChip extends JToggleButton {

    private final boolean onDark;
    private Color selectedColor = Theme.ACCENT;
    private boolean hover;

    public ToggleChip(String text, boolean onDark) {
        super(text);
        this.onDark = onDark;
        setFont(Theme.font(Font.BOLD, 14f));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public void setSelectedColor(Color selectedColor) {
        this.selectedColor = selectedColor;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 48), Math.max(d.height, 36));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, h - 2, h - 2);
        Color text;
        if (isSelected()) {
            g2.setColor(isEnabled() ? selectedColor : Theme.alpha(selectedColor, 120));
            g2.fill(shape);
            text = isLight(selectedColor) ? Theme.TEXT : Color.WHITE;
        } else {
            if (onDark) {
                g2.setColor(new Color(255, 255, 255, hover ? 40 : 18));
                g2.fill(shape);
                g2.setColor(new Color(255, 255, 255, 70));
            } else {
                g2.setColor(hover ? new Color(0xEEF2FB) : Color.WHITE);
                g2.fill(shape);
                g2.setColor(Theme.BORDER);
            }
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(shape);
            text = onDark ? Theme.TEXT_ON_DARK : Theme.TEXT;
        }
        if (!isEnabled()) {
            text = Theme.alpha(text, 110);
        }
        if (isFocusOwner()) {
            g2.setColor(onDark || isSelected() ? new Color(255, 255, 255, 190) : Theme.ACCENT);
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Double(3, 3, w - 6, h - 6, h - 6, h - 6));
        }
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String label = getText();
        g2.setColor(text);
        g2.drawString(label, (w - fm.stringWidth(label)) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f);
        g2.dispose();
    }

    private static boolean isLight(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 > 170;
    }
}
