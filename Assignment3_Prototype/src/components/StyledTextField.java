package components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/** Rounded text field with placeholder text, an optional leading icon, a length limit and an error state. */
public class StyledTextField extends JTextField {

    private final FieldLook look;

    public StyledTextField(String placeholder, Icon icon) {
        look = new FieldLook(this, placeholder, icon);
    }

    /** Limits the text length while typing (e.g. 12 characters for player names). */
    public void setMaxLength(int max) {
        FieldLook.limit(this, max);
    }

    public void setError(boolean error) {
        look.setError(error);
    }

    public boolean hasError() {
        return look.error;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 42));
    }

    @Override
    protected void paintComponent(Graphics g) {
        look.paintBackground(g);
        super.paintComponent(g);
        look.paintOverlay(g);
    }

    /** Password variant with the same look. */
    public static class Password extends JPasswordField {
        private final FieldLook look;

        public Password(String placeholder, Icon icon) {
            look = new FieldLook(this, placeholder, icon);
        }

        public void setError(boolean error) {
            look.setError(error);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, Math.max(d.height, 42));
        }

        @Override
        protected void paintComponent(Graphics g) {
            look.paintBackground(g);
            super.paintComponent(g);
            look.paintOverlay(g);
        }
    }

    /** Shared painting code of the two field types. */
    private static final class FieldLook {
        private final JTextComponent field;
        private final String placeholder;
        private final Icon icon;
        private boolean error;

        FieldLook(JTextComponent field, String placeholder, Icon icon) {
            this.field = field;
            this.placeholder = placeholder;
            this.icon = icon;
            field.setOpaque(false);
            field.setFont(Theme.font(Font.PLAIN, 15f));
            field.setForeground(Theme.TEXT);
            field.setCaretColor(Theme.TEXT);
            field.setSelectionColor(new Color(0xCFDBFF));
            int left = icon == null ? 14 : 14 + icon.getIconWidth() + 9;
            field.setBorder(BorderFactory.createEmptyBorder(9, left, 9, 14));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    field.repaint();
                }
            });
        }

        void setError(boolean error) {
            this.error = error;
            field.repaint();
        }

        static void limit(JTextComponent field, int max) {
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                        throws BadLocationException {
                    replace(fb, offset, 0, text, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    String add = text == null ? "" : text.replace("\n", " ");
                    int room = max - (fb.getDocument().getLength() - length);
                    if (room <= 0) {
                        java.awt.Toolkit.getDefaultToolkit().beep();
                        return;
                    }
                    if (add.length() > room) {
                        add = add.substring(0, room);
                    }
                    super.replace(fb, offset, length, add, attrs);
                }
            });
        }

        void paintBackground(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = field.getWidth();
            int h = field.getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 12, 12);
            g2.setColor(field.isEnabled() ? Color.WHITE : new Color(0xF1F3F8));
            g2.fill(shape);
            Color border = error ? Theme.DANGER : field.isFocusOwner() ? Theme.ACCENT : new Color(0xC9D0E0);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(field.isFocusOwner() || error ? 2f : 1.2f));
            g2.draw(shape);
            g2.dispose();
        }

        void paintOverlay(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int h = field.getHeight();
            if (icon != null) {
                icon.paintIcon(field, g2, 13, (h - icon.getIconHeight()) / 2);
            }
            if (field.getDocument().getLength() == 0 && placeholder != null) {
                Insets in = field.getInsets();
                g2.setFont(field.getFont());
                g2.setColor(new Color(0x8C94AE));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(placeholder, in.left + 1, (h + fm.getAscent() - fm.getDescent()) / 2f);
            }
            g2.dispose();
        }
    }
}
