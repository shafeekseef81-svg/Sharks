package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import View.components.CustomButton;
import View.components.CustomPanel;
import View.components.Icons;
import View.components.Theme;
import View.components.UiKit;

/** Confirmation or information card shown in a modal overlay. */
public class ConfirmDialog extends CustomPanel {

    private final CustomButton confirm;
    private final CustomButton cancel;

    /**
     * @param cancelText null for a single-button information dialog
     */
    public ConfirmDialog(String title, String message, String confirmText, String cancelText, boolean danger,
                         Icons.Type icon, Runnable onConfirm, Runnable onCancel) {
        super(new BorderLayout(0, 18), Style.CARD);
        setPadding(24, 26, 22, 26);
        Color accent = danger ? Theme.DANGER : Theme.ACCENT;

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        top.add(new IconBadge(icon, accent), BorderLayout.WEST);
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel t = UiKit.label(title, Theme.font(Font.BOLD, 20f), Theme.TEXT);
        texts.add(t);
        texts.add(UiKit.vgap(6));
        texts.add(UiKit.wrap(message, 330, Theme.font(Font.PLAIN, 14.5f), Theme.TEXT_MUTED));
        top.add(texts, BorderLayout.CENTER);
        add(top, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        cancel = cancelText == null ? null : new CustomButton(cancelText, CustomButton.Variant.SECONDARY);
        confirm = new CustomButton(confirmText, danger ? CustomButton.Variant.DANGER : CustomButton.Variant.PRIMARY);
        confirm.setName("dialog.confirm");
        if (cancel != null) {
            cancel.setName("dialog.cancel");
            cancel.addActionListener(e -> onCancel.run());
            buttons.add(cancel);
        }
        confirm.addActionListener(e -> onConfirm.run());
        buttons.add(confirm);
        add(buttons, BorderLayout.SOUTH);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "confirm");
        getActionMap().put("confirm", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                confirm.doClick();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(470, super.getPreferredSize().height);
    }

    public CustomButton confirmButton() {
        return confirm;
    }

    public CustomButton cancelButton() {
        return cancel;
    }

    /** Round coloured badge with an icon. */
    static final class IconBadge extends JComponent {
        private final Icons.Type icon;
        private final Color color;

        IconBadge(Icons.Type icon, Color color) {
            this.icon = icon;
            this.color = color;
            setPreferredSize(new Dimension(52, 52));
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(Theme.alpha(color, 30));
            g2.fill(new Ellipse2D.Double(0, 0, 52, 52));
            Icons.paint(g2, icon, 13, 13, 26, color);
            g2.dispose();
        }
    }
}
