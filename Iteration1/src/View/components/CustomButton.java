package View.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** Rounded button with hover/press animation, painted entirely by hand so it looks the same everywhere. */
public class CustomButton extends JButton {

    /** Visual styles. */
    public enum Variant {
        /** Main call to action (orange gradient, or an accent colour). */
        PRIMARY,
        /** White button for light cards. */
        SECONDARY,
        /** Translucent button on the dark background. */
        LIGHT,
        /** Text-like button with a subtle outline. */
        GHOST,
        /** Destructive action. */
        DANGER,
        /** Small dashed button (unused since the prototype demo panel was removed; T01 may delete it). */
        DEMO
    }

    private final Variant variant;
    private Color accent;
    private float hover;
    private float hoverTarget;
    private final Timer hoverTimer;
    private int radius = 14;

    public CustomButton(String text, Variant variant) {
        this(text, null, variant);
    }

    public CustomButton(String text, Icon icon, Variant variant) {
        super(text, icon);
        this.variant = variant;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setFont(Theme.font(Font.BOLD, variant == Variant.DEMO ? 13f : 15f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setIconTextGap(9);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(variant == Variant.DEMO
                ? BorderFactory.createEmptyBorder(6, 10, 6, 10)
                : BorderFactory.createEmptyBorder(10, 20, 10, 20));
        hoverTimer = new Timer(15, e -> {
            float step = 0.12f;
            if (Math.abs(hover - hoverTarget) <= step) {
                hover = hoverTarget;
                ((Timer) e.getSource()).stop();
            } else {
                hover += hover < hoverTarget ? step : -step;
            }
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoverTarget = 1f;
                hoverTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverTarget = 0f;
                hoverTimer.start();
            }
        });
    }

    /** Uses a custom colour for a PRIMARY button (e.g. the active player's colour). */
    public void setAccent(Color accent) {
        this.accent = accent;
        repaint();
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, variant == Variant.DEMO ? 32 : 42));
    }

    private Color fillColor() {
        return switch (variant) {
            case PRIMARY -> accent != null ? accent : Theme.PRIMARY;
            case SECONDARY -> Color.WHITE;
            case LIGHT -> new Color(255, 255, 255, 34);
            case GHOST -> new Color(255, 255, 255, 0);
            case DANGER -> Theme.DANGER;
            case DEMO -> new Color(255, 255, 255, 14);
        };
    }

    private Color textColor() {
        return switch (variant) {
            case PRIMARY -> accent != null && isLight(accent) ? Theme.TEXT : Color.WHITE;
            case SECONDARY -> Theme.TEXT;
            case GHOST -> getForeground() != null && !(getForeground() instanceof javax.swing.plaf.UIResource)
                    ? getForeground() : Theme.TEXT_ON_DARK;
            case DEMO -> new Color(0xD7DEF5);
            default -> Color.WHITE;
        };
    }

    private static boolean isLight(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 > 170;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = getHeight();
        boolean enabled = isEnabled();
        boolean pressed = getModel().isArmed() && getModel().isPressed();
        double inset = 1.5;
        double shadow = (variant == Variant.PRIMARY || variant == Variant.SECONDARY || variant == Variant.DANGER)
                && enabled ? 3 : 0;
        RoundRectangle2D shape = new RoundRectangle2D.Double(inset, inset + (pressed ? 1 : 0),
                w - inset * 2, h - inset * 2 - shadow, radius, radius);

        Color fill = fillColor();
        if (!enabled) {
            fill = switch (variant) {
                case PRIMARY, DANGER -> new Color(0x8E97B3);
                case SECONDARY -> new Color(0xE9ECF3);
                default -> new Color(255, 255, 255, 12);
            };
        }
        if (shadow > 0) {
            g2.setColor(new Color(0, 0, 0, 45));
            g2.fill(new RoundRectangle2D.Double(inset, inset + shadow, w - inset * 2, h - inset * 2 - shadow,
                    radius, radius));
        }
        if (enabled && hover > 0) {
            fill = fill.getAlpha() < 255
                    ? Theme.alpha(Color.WHITE, fill.getAlpha() + (int) (34 * hover))
                    : Theme.mix(fill, Color.WHITE, 0.12 * hover);
        }
        if (pressed) {
            fill = fill.getAlpha() < 255 ? Theme.alpha(Color.WHITE, fill.getAlpha() + 30)
                    : Theme.mix(fill, Color.BLACK, 0.1);
        }
        if (variant == Variant.PRIMARY && enabled) {
            Color top = Theme.mix(fill, Color.WHITE, 0.16);
            Color bottom = accent != null ? Theme.mix(fill, Color.BLACK, 0.08) : Theme.mix(fill, Theme.PRIMARY_DARK, 0.65);
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        } else {
            g2.setColor(fill);
        }
        g2.fill(shape);

        if (variant == Variant.SECONDARY) {
            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(shape);
        } else if (variant == Variant.LIGHT || variant == Variant.GHOST) {
            g2.setColor(new Color(255, 255, 255, variant == Variant.GHOST ? 40 + (int) (40 * hover) : 60));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(shape);
        } else if (variant == Variant.DEMO) {
            g2.setColor(new Color(255, 255, 255, 90 + (int) (60 * hover)));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[] {4f, 3f}, 0f));
            g2.draw(shape);
        }
        if (isFocusOwner() && enabled) {
            g2.setColor(variant == Variant.SECONDARY ? Theme.ACCENT : new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Double(inset + 2, inset + 2 + (pressed ? 1 : 0), w - inset * 2 - 4,
                    h - inset * 2 - shadow - 4, radius - 3, radius - 3));
        }

        Color text = textColor();
        if (!enabled) {
            text = variant == Variant.SECONDARY ? new Color(0x9AA3BC) : new Color(255, 255, 255, 150);
        }
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        Icon icon = getIcon();
        String label = getText() == null ? "" : getText();
        int textW = fm.stringWidth(label);
        int iconW = icon == null ? 0 : icon.getIconWidth();
        int gap = icon != null && !label.isEmpty() ? getIconTextGap() : 0;
        int total = iconW + gap + textW;
        Insets in = getInsets();
        int x = getHorizontalAlignment() == SwingConstants.LEFT ? in.left : (w - total) / 2;
        double centerY = (h - shadow) / 2.0 + (pressed ? 1 : 0);
        if (icon != null) {
            icon.paintIcon(this, g2, x, (int) Math.round(centerY - icon.getIconHeight() / 2.0));
        }
        g2.setColor(text);
        g2.drawString(label, x + iconW + gap, (float) (centerY + (fm.getAscent() - fm.getDescent()) / 2.0));
        g2.dispose();
    }
}
