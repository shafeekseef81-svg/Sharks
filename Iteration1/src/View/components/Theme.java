package View.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

import Model.Difficulty;
import Model.PlayerColor;

/** Colours, fonts and rendering settings shared by every screen. Built-in fonts only. */
public final class Theme {

    public static final Color BG_TOP = new Color(0x0F1B40);
    public static final Color BG_BOTTOM = new Color(0x1D2E6B);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_ALT = new Color(0xF3F5FA);
    public static final Color BORDER = new Color(0xDCE1EC);
    public static final Color TEXT = new Color(0x1B2240);
    public static final Color TEXT_MUTED = new Color(0x5E6784);
    public static final Color TEXT_ON_DARK = Color.WHITE;
    public static final Color TEXT_ON_DARK_MUTED = new Color(0xB9C3E3);
    public static final Color PRIMARY = new Color(0xFF8A1F);
    public static final Color PRIMARY_DARK = new Color(0xF2561B);
    public static final Color ACCENT = new Color(0x3B6EF5);
    public static final Color SUCCESS = new Color(0x15924F);
    public static final Color DANGER = new Color(0xDD3B43);
    public static final Color WARNING = new Color(0xE08A00);
    public static final Color BOARD_LINE = new Color(0x1E2747);
    public static final Color SURPRISE = new Color(0x6A22D2);
    public static final Color STAR = new Color(0x5A6078);
    public static final Color GOLD = new Color(0xF2B705);
    public static final Color SILVER = new Color(0xAEB8C8);
    public static final Color BRONZE = new Color(0xCD8445);

    private static final String[] BODY_FAMILIES = {"Segoe UI", "Helvetica Neue", "Arial", "SansSerif"};
    private static final String[] DISPLAY_FAMILIES = {"Segoe UI Black", "Arial Black", "Segoe UI", "SansSerif"};
    private static final String[] MONO_FAMILIES = {"Consolas", "Menlo", "Monospaced"};
    private static String bodyFamily;
    private static String displayFamily;
    private static String monoFamily;

    private Theme() {
    }

    // ------------------------------------------------------------------ colours

    public static Color player(PlayerColor c) {
        return switch (c) {
            case RED -> new Color(0xE8252B);
            case GREEN -> new Color(0x13A24B);
            case YELLOW -> new Color(0xFFC619);
            case BLUE -> new Color(0x1E8BEA);
        };
    }

    public static Color playerDark(PlayerColor c) {
        return switch (c) {
            case RED -> new Color(0xA8141A);
            case GREEN -> new Color(0x0A7034);
            case YELLOW -> new Color(0xB98400);
            case BLUE -> new Color(0x125EAA);
        };
    }

    public static Color playerLight(PlayerColor c) {
        return switch (c) {
            case RED -> new Color(0xFDE3E3);
            case GREEN -> new Color(0xDCF4E5);
            case YELLOW -> new Color(0xFFF2C2);
            case BLUE -> new Color(0xDCEDFE);
        };
    }

    /** Readable text colour on top of a player colour. */
    public static Color textOn(PlayerColor c) {
        return c == PlayerColor.YELLOW ? TEXT : Color.WHITE;
    }

    /** Question-level colours used on the board: green easy, yellow medium, red hard. */
    public static Color level(Difficulty d) {
        return switch (d) {
            case EASY -> new Color(0x139C47);
            case MEDIUM -> new Color(0xE29A00);
            case HARD -> new Color(0xDD3B43);
        };
    }

    public static Color levelLight(Difficulty d) {
        return switch (d) {
            case EASY -> new Color(0xE0F5E8);
            case MEDIUM -> new Color(0xFFF1BF);
            case HARD -> new Color(0xFDE1E1);
        };
    }

    public static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    // ------------------------------------------------------------------ fonts

    private static synchronized void initFonts() {
        if (bodyFamily != null) {
            return;
        }
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        bodyFamily = pick(available, BODY_FAMILIES);
        displayFamily = pick(available, DISPLAY_FAMILIES);
        monoFamily = pick(available, MONO_FAMILIES);
    }

    private static String pick(Set<String> available, String[] wanted) {
        for (String w : wanted) {
            if (available.contains(w)) {
                return w;
            }
        }
        return wanted[wanted.length - 1];
    }

    /** Body font (Segoe UI on Windows, with fallbacks). */
    public static Font font(int style, float size) {
        initFonts();
        return new Font(bodyFamily, style, 12).deriveFont(style, size);
    }

    /** Heavy display font for big titles. */
    public static Font display(float size) {
        initFonts();
        int style = displayFamily.endsWith("Black") ? Font.PLAIN : Font.BOLD;
        return new Font(displayFamily, style, 12).deriveFont(style, size);
    }

    /** Monospaced digits for the game clock. */
    public static Font mono(int style, float size) {
        initFonts();
        return new Font(monoFamily, style, 12).deriveFont(style, size);
    }

    // ------------------------------------------------------------------ rendering

    public static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    /** System look and feel for standard widgets, with readable defaults (NFR-24: text of at least 12 pt). */
    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // the default look and feel is fine as well
        }
        UIManager.put("ToolTip.font", new FontUIResource(font(Font.PLAIN, 13f)));
        UIManager.put("ToolTip.background", new ColorUIResource(new Color(0x1B2240)));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 9, 6, 9));
        UIManager.put("ScrollBar.width", 12);
    }
}
