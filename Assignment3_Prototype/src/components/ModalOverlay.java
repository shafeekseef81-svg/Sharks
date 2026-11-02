package components;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * In-window modal dialog: a dimmed layer over the whole window with a centred card.
 * Blocks mouse input to the screen below and keeps keyboard focus inside the card.
 */
public final class ModalOverlay extends JPanel {

    private static final List<ModalOverlay> OPEN = new ArrayList<>();

    private final JLayeredPane host;
    private final JComponent card;
    private final ComponentAdapter resizer;
    private float alpha = 1f;
    private Runnable onEscape;
    private Runnable onClose;

    private ModalOverlay(JLayeredPane host, JComponent card) {
        super(null);
        this.host = host;
        this.card = card;
        setOpaque(false);
        add(card);
        MouseAdapter blocker = new MouseAdapter() { };
        addMouseListener(blocker);
        addMouseMotionListener(blocker);
        addMouseWheelListener(e -> { });
        setFocusCycleRoot(true);
        resizer = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setBounds(0, 0, host.getWidth(), host.getHeight());
                revalidate();
            }
        };
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        getActionMap().put("escape", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (onEscape != null && top() == ModalOverlay.this) {
                    onEscape.run();
                }
            }
        });
    }

    /** Shows {@code card} centred over the window. */
    public static ModalOverlay show(JLayeredPane host, JComponent card) {
        ModalOverlay overlay = new ModalOverlay(host, card);
        overlay.setBounds(0, 0, host.getWidth(), host.getHeight());
        host.add(overlay, Integer.valueOf(JLayeredPane.MODAL_LAYER + OPEN.size()));
        host.addComponentListener(overlay.resizer);
        OPEN.add(overlay);
        host.revalidate();
        host.repaint();
        if (!Animations.instant) {
            overlay.alpha = 0f;
            Animations.animate(170, t -> {
                overlay.alpha = (float) Animations.easeOut(t);
                overlay.repaint();
            }, null);
        }
        SwingUtilities.invokeLater(() -> {
            Component first = overlay.getFocusTraversalPolicy().getDefaultComponent(overlay);
            if (first != null) {
                first.requestFocusInWindow();
            }
        });
        return overlay;
    }

    /** Centres the card at its preferred size, clamped to the window so it never collapses. */
    @Override
    public void doLayout() {
        java.awt.Dimension pref = card.getPreferredSize();
        int w = Math.min(pref.width, getWidth() - 24);
        int h = Math.min(pref.height, getHeight() - 24);
        card.setBounds((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
    }

    /** Action for the Escape key (e.g. Cancel); null disables it. */
    public void setOnEscape(Runnable onEscape) {
        this.onEscape = onEscape;
    }

    /** Runs after the overlay has been closed. */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public JComponent card() {
        return card;
    }

    public void close() {
        if (!OPEN.remove(this)) {
            return;
        }
        host.removeComponentListener(resizer);
        host.remove(this);
        host.revalidate();
        host.repaint();
        if (onClose != null) {
            onClose.run();
        }
    }

    public static int openCount() {
        return OPEN.size();
    }

    /** The top-most open overlay, or null. */
    public static ModalOverlay top() {
        return OPEN.isEmpty() ? null : OPEN.get(OPEN.size() - 1);
    }

    /** Closes everything (used when the screen changes). */
    public static void closeAll() {
        for (ModalOverlay o : List.copyOf(OPEN)) {
            o.close();
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
        g2.setColor(new Color(6, 10, 28, 150));
        g2.fillRect(0, 0, getWidth(), getHeight());
        super.paint(g2);
        g2.dispose();
    }
}
