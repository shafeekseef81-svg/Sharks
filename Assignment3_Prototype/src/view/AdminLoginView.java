package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.geom.Ellipse2D;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import components.CustomButton;
import components.CustomPanel;
import components.GradientBackground;
import components.Icons;
import components.StyledTextField;
import components.Theme;
import components.UiKit;
import data.DemoData;

/** Administrator login (FR-51..FR-53, NFR-17). Authentication is simulated in this prototype. */
public class AdminLoginView extends GradientBackground implements ScreenView {

    private static final int CARD_WIDTH = 440;

    private final StyledTextField username = new StyledTextField("Username",
            Icons.get(Icons.Type.USER, 17, new Color(0x8C94AE)));
    private final StyledTextField.Password password = new StyledTextField.Password("Password",
            Icons.get(Icons.Type.LOCK, 17, new Color(0x8C94AE)));
    private final CustomButton login = new CustomButton("Log In", Icons.get(Icons.Type.ARROW_RIGHT, 18, Color.WHITE),
            CustomButton.Variant.PRIMARY);
    private final CustomButton back = new CustomButton("Back to Main Menu", CustomButton.Variant.SECONDARY);
    private final CustomPanel errorBox = new CustomPanel(new BorderLayout(10, 0), CustomPanel.Style.SOLID);
    private final JLabel errorText = UiKit.label(" ", Theme.font(Font.BOLD, 14f), new Color(0xA3161D));
    private Timer lockTimer;

    public AdminLoginView() {
        super(new GridBagLayout());
        CustomPanel card = new CustomPanel(new BorderLayout(), CustomPanel.Style.CARD);
        card.setPadding(26, 30, 24, 30);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        body.add(center(new LockBadge()));
        body.add(UiKit.vgap(10));
        JLabel title = UiKit.label("Administrator Login", Theme.font(Font.BOLD, 25f), Theme.TEXT);
        title.setName("screen.title");
        body.add(center(title));
        body.add(UiKit.vgap(4));
        body.add(center(UiKit.label("Question management is for administrators only.", Theme.font(Font.PLAIN, 14.5f),
                Theme.TEXT_MUTED)));
        body.add(UiKit.vgap(20));

        body.add(left(UiKit.label("Username", Theme.font(Font.BOLD, 13.5f), Theme.TEXT)));
        body.add(UiKit.vgap(6));
        username.setName("login.username");
        body.add(left(username));
        body.add(UiKit.vgap(14));
        body.add(left(UiKit.label("Password", Theme.font(Font.BOLD, 13.5f), Theme.TEXT)));
        body.add(UiKit.vgap(6));
        password.setName("login.password");
        body.add(left(password));
        body.add(UiKit.vgap(14));

        errorBox.setFill(new Color(0xFDE7E7));
        errorBox.setRadius(12);
        errorBox.setPadding(10, 12, 10, 12);
        errorBox.add(new JLabel(Icons.get(Icons.Type.WARNING, 20, Theme.DANGER)), BorderLayout.WEST);
        errorText.setName("login.error");
        errorBox.add(errorText, BorderLayout.CENTER);
        errorBox.setVisible(false);
        body.add(left(errorBox));
        body.add(UiKit.vgap(4));

        login.setName("login.submit");
        login.setFont(Theme.font(Font.BOLD, 16f));
        body.add(left(login));
        body.add(UiKit.vgap(10));
        back.setName("login.back");
        body.add(left(back));
        body.add(UiKit.vgap(18));

        CustomPanel demo = new CustomPanel(new BorderLayout(10, 0), CustomPanel.Style.SOLID);
        demo.setFill(new Color(0xEEF3FF));
        demo.setRadius(12);
        demo.setPadding(10, 12, 10, 12);
        demo.add(new JLabel(Icons.get(Icons.Type.INFO, 20, Theme.ACCENT)), BorderLayout.WEST);
        demo.add(UiKit.wrap("Prototype demo account: username " + DemoData.ADMIN_USERNAME + ", password "
                + DemoData.ADMIN_PASSWORD + ". The login is simulated (no real security yet).",
                CARD_WIDTH - 120, Theme.font(Font.PLAIN, 13f), new Color(0x2B3A70)), BorderLayout.CENTER);
        body.add(left(demo));

        for (Component c : body.getComponents()) {
            if (c instanceof JComponent jc && !(c instanceof LockBadge) && jc.getAlignmentX() == Component.LEFT_ALIGNMENT) {
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, jc.getPreferredSize().height));
            }
        }
        body.add(left((JComponent) javax.swing.Box.createHorizontalStrut(CARD_WIDTH)));
        card.add(body, BorderLayout.CENTER);
        add(card);
        password.addActionListener(e -> login.doClick());
        username.addActionListener(e -> password.requestFocusInWindow());
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JComponent center(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JLabel l) {
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height));
        }
        return c;
    }

    public void setActions(Runnable onBack, BiConsumer<String, char[]> onLogin) {
        back.addActionListener(e -> onBack.run());
        login.addActionListener(e -> onLogin.accept(username.getText(), password.getPassword()));
    }

    /** Generic error, without telling which credential was wrong (FR-53). */
    public void showError(String message) {
        errorText.setText(message);
        errorBox.setVisible(true);
        username.setError(true);
        password.setError(true);
        password.setText("");
        password.requestFocusInWindow();
        revalidate();
    }

    /** Disables the login during the lockout and shows a countdown (NFR-17). */
    public void showLocked(LongSupplier remainingMillis) {
        login.setEnabled(false);
        password.setText("");
        if (lockTimer != null) {
            lockTimer.stop();
        }
        Runnable update = () -> {
            long left = remainingMillis.getAsLong();
            if (left <= 0) {
                lockTimer.stop();
                login.setEnabled(true);
                errorText.setText("You can try to log in again.");
                return;
            }
            errorText.setText("Too many failed attempts. Try again in " + (left + 999) / 1000 + " s.");
        };
        lockTimer = new Timer(250, e -> update.run());
        errorBox.setVisible(true);
        username.setError(true);
        password.setError(true);
        update.run();
        lockTimer.start();
        revalidate();
    }

    /** Clears the form (called when the screen is shown). */
    public void reset() {
        if (lockTimer != null && login.isEnabled()) {
            lockTimer.stop();
        }
        username.setText("");
        password.setText("");
        username.setError(false);
        password.setError(false);
        if (login.isEnabled()) {
            errorBox.setVisible(false);
        }
    }

    public StyledTextField usernameField() {
        return username;
    }

    public StyledTextField.Password passwordField() {
        return password;
    }

    public CustomButton loginButton() {
        return login;
    }

    public boolean isErrorVisible() {
        return errorBox.isVisible();
    }

    public String errorMessage() {
        return errorText.getText();
    }

    @Override
    public void onShow() {
        reset();
        username.requestFocusInWindow();
    }

    @Override
    public JButton defaultButton() {
        return login;
    }

    /** Round lock badge at the top of the card. */
    private static final class LockBadge extends JComponent {
        LockBadge() {
            setPreferredSize(new Dimension(CARD_WIDTH, 70));
            setMinimumSize(new Dimension(70, 70));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            double s = 68;
            double x = (getWidth() - s) / 2;
            g2.setColor(new Color(0xE7EDFF));
            g2.fill(new Ellipse2D.Double(x, 1, s, s));
            Icons.paint(g2, Icons.Type.SHIELD, x + 14, 15, 40, Theme.ACCENT);
            Icons.paint(g2, Icons.Type.LOCK, x + 24, 25, 20, Color.WHITE);
            g2.dispose();
        }
    }
}
