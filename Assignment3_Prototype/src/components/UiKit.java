package components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Small factory helpers that keep the screens consistent. */
public final class UiKit {

    private UiKit() {
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /** A label that wraps its text at {@code width} pixels. */
    public static JLabel wrap(String text, int width, Font font, Color color) {
        JLabel l = new JLabel();
        l.setFont(font);
        l.setForeground(color);
        setWrapped(l, text, width);
        return l;
    }

    public static void setWrapped(JLabel label, String text, int width) {
        long css = Math.round(width * cssPixelScale());
        label.setText("<html><div style='width:" + css + "px'>" + escape(ltr(text)) + "</div></html>");
    }

    /**
     * Keeps an English sentence left-to-right when it starts with a Hebrew player name. Without the leading
     * left-to-right mark the text direction follows the first letter, which moves words and punctuation around.
     */
    public static String ltr(String text) {
        return text == null || text.startsWith("‎") ? text : "‎" + text;
    }

    private static double cssPixelScale = -1;

    /**
     * Swing's HTML renderer scales CSS pixels (by about 1.33 on current JDKs), so a measured probe
     * converts the wanted width in screen pixels into the CSS value that produces it.
     */
    private static synchronized double cssPixelScale() {
        if (cssPixelScale < 0) {
            JLabel probe = new JLabel("<html><div style='width:300px'>x</div></html>");
            int measured = probe.getPreferredSize().width;
            cssPixelScale = measured > 0 ? 300.0 / measured : 1.0;
        }
        return cssPixelScale;
    }

    /** Escapes user text before it is shown inside HTML labels. */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    public static Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    public static Component hgap(int w) {
        return Box.createRigidArea(new Dimension(w, 0));
    }

    /** Screen header: optional back button, a big title with subtitle, and an optional right-hand component. */
    public static JComponent header(JComponent left, String title, String subtitle, JComponent right) {
        JPanel row = new JPanel(new BorderLayout(18, 0));
        row.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel t = label(title, Theme.font(Font.BOLD, 30f), Theme.TEXT_ON_DARK);
        t.setName("screen.title");
        titles.add(t);
        if (subtitle != null) {
            titles.add(vgap(2));
            titles.add(label(subtitle, Theme.font(Font.PLAIN, 15f), Theme.TEXT_ON_DARK_MUTED));
        }
        if (left != null) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            holder.add(left, BorderLayout.NORTH);
            holder.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            row.add(holder, BorderLayout.WEST);
        }
        row.add(titles, BorderLayout.CENTER);
        if (right != null) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            holder.add(right, BorderLayout.NORTH);
            holder.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            row.add(holder, BorderLayout.EAST);
        }
        return row;
    }

    /** Small rounded tag. */
    public static JLabel pill(String text, Color background, Color foreground) {
        return new Pill(text, background, foreground);
    }

    /** Rounded label used for tags such as the difficulty or "PROTOTYPE DEMO". */
    public static final class Pill extends JLabel {
        private Color background;

        Pill(String text, Color background, Color foreground) {
            super(text);
            this.background = background;
            setForeground(foreground);
            setFont(Theme.font(Font.BOLD, 12f));
            setBorder(BorderFactory.createEmptyBorder(4, 11, 4, 11));
            setOpaque(false);
        }

        public void setPillColors(Color background, Color foreground) {
            this.background = background;
            setForeground(foreground);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(background);
            int h = getHeight();
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), h, h, h));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(getForeground());
            g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2f,
                    (h + fm.getAscent() - fm.getDescent()) / 2f);
            g2.dispose();
        }
    }
}
