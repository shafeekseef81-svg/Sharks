package View.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/** Vector icons drawn with Java2D (no image files, no emoji fonts needed). */
public final class Icons {

    /** Available icon shapes. */
    public enum Type {
        PLAY, HISTORY, LOCK, EXIT, BACK, ARROW_RIGHT, DICE, USER, PLUS, EDIT, TRASH, CHECK, CROSS, STAR, GIFT,
        QUESTION, TROPHY, CLOCK, FLAG, CASTLE, SKIP, ROCKET, INFO, WARNING, PUSH, STEPS_BACK, CROWN, BOARD,
        EYE, LIST, REFRESH, SHIELD
    }

    private Icons() {
    }

    public static Icon get(Type type, int size, Color color) {
        return new VectorIcon(type, size, color);
    }

    private record VectorIcon(Type type, int size, Color color) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            Color col = color;
            if (c != null && !c.isEnabled()) {
                col = Theme.alpha(color, 110);
            }
            paint(g2, type, x, y, size, col);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Paints an icon into the square (x, y, s, s). */
    public static void paint(Graphics2D g, Type type, double x, double y, double s, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.scale(s, s);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(0.09f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case PLAY -> g2.fill(poly(0.32, 0.2, 0.32, 0.8, 0.82, 0.5));
            case HISTORY -> {
                g2.draw(new Arc2D.Double(0.14, 0.14, 0.72, 0.72, 110, 290, Arc2D.OPEN));
                g2.fill(poly(0.06, 0.22, 0.34, 0.24, 0.17, 0.46));
                g2.draw(new Line2D.Double(0.5, 0.5, 0.5, 0.3));
                g2.draw(new Line2D.Double(0.5, 0.5, 0.64, 0.6));
            }
            case LOCK -> {
                g2.fill(new RoundRectangle2D.Double(0.2, 0.44, 0.6, 0.44, 0.12, 0.12));
                Path2D shackle = new Path2D.Double();
                shackle.moveTo(0.33, 0.46);
                shackle.lineTo(0.33, 0.32);
                shackle.curveTo(0.33, 0.08, 0.67, 0.08, 0.67, 0.32);
                shackle.lineTo(0.67, 0.46);
                g2.draw(shackle);
            }
            case EXIT -> {
                g2.draw(new RoundRectangle2D.Double(0.14, 0.14, 0.44, 0.72, 0.08, 0.08));
                g2.draw(new Line2D.Double(0.42, 0.5, 0.9, 0.5));
                g2.draw(poly(0.74, 0.34, 0.9, 0.5, 0.74, 0.66, false));
            }
            case BACK -> {
                g2.draw(new Line2D.Double(0.82, 0.5, 0.2, 0.5));
                g2.draw(poly(0.44, 0.26, 0.2, 0.5, 0.44, 0.74, false));
            }
            case ARROW_RIGHT -> {
                g2.draw(new Line2D.Double(0.18, 0.5, 0.8, 0.5));
                g2.draw(poly(0.56, 0.26, 0.8, 0.5, 0.56, 0.74, false));
            }
            case DICE -> {
                g2.draw(new RoundRectangle2D.Double(0.14, 0.14, 0.72, 0.72, 0.22, 0.22));
                dot(g2, 0.33, 0.33, 0.07);
                dot(g2, 0.5, 0.5, 0.07);
                dot(g2, 0.67, 0.67, 0.07);
                dot(g2, 0.67, 0.33, 0.07);
                dot(g2, 0.33, 0.67, 0.07);
            }
            case USER -> {
                dot(g2, 0.5, 0.33, 0.17);
                g2.fill(new Arc2D.Double(0.18, 0.56, 0.64, 0.6, 0, 180, Arc2D.CHORD));
            }
            case PLUS -> {
                g2.draw(new Line2D.Double(0.5, 0.18, 0.5, 0.82));
                g2.draw(new Line2D.Double(0.18, 0.5, 0.82, 0.5));
            }
            case EDIT -> {
                g2.draw(poly(0.18, 0.82, 0.24, 0.62, 0.66, 0.2, 0.8, 0.34, 0.38, 0.76));
                g2.draw(new Line2D.Double(0.58, 0.28, 0.72, 0.42));
            }
            case TRASH -> {
                g2.draw(new Line2D.Double(0.18, 0.28, 0.82, 0.28));
                g2.draw(poly(0.4, 0.27, 0.42, 0.15, 0.58, 0.15, 0.6, 0.27, false));
                g2.draw(poly(0.27, 0.33, 0.32, 0.86, 0.68, 0.86, 0.73, 0.33, false));
                g2.draw(new Line2D.Double(0.44, 0.42, 0.45, 0.74));
                g2.draw(new Line2D.Double(0.56, 0.42, 0.55, 0.74));
            }
            case CHECK -> g2.draw(poly(0.2, 0.52, 0.42, 0.74, 0.82, 0.28, false));
            case CROSS -> {
                g2.draw(new Line2D.Double(0.26, 0.26, 0.74, 0.74));
                g2.draw(new Line2D.Double(0.74, 0.26, 0.26, 0.74));
            }
            case STAR -> g2.fill(star(0.5, 0.54, 0.44, 0.19, 5));
            case GIFT -> gift(g2, 0, 0, 1, color);
            case QUESTION -> {
                g2.fill(new Ellipse2D.Double(0.06, 0.06, 0.88, 0.88));
                boolean light = (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000 > 170;
                g2.setColor(light ? Theme.TEXT : Color.WHITE);
                g2.fill(glyph("?", Theme.font(Font.BOLD, 40f), 0.5, 0.5, 0.62));
            }
            case TROPHY -> {
                g2.fill(new RoundRectangle2D.Double(0.28, 0.12, 0.44, 0.38, 0.1, 0.4));
                g2.draw(new Arc2D.Double(0.12, 0.18, 0.24, 0.24, 90, 180, Arc2D.OPEN));
                g2.draw(new Arc2D.Double(0.64, 0.18, 0.24, 0.24, 90, -180, Arc2D.OPEN));
                g2.fill(new Rectangle2D.Double(0.45, 0.48, 0.1, 0.2));
                g2.fill(new RoundRectangle2D.Double(0.28, 0.68, 0.44, 0.16, 0.06, 0.06));
            }
            case CLOCK -> {
                g2.draw(new Ellipse2D.Double(0.12, 0.12, 0.76, 0.76));
                g2.draw(new Line2D.Double(0.5, 0.5, 0.5, 0.27));
                g2.draw(new Line2D.Double(0.5, 0.5, 0.67, 0.6));
            }
            case FLAG -> {
                g2.draw(new Line2D.Double(0.26, 0.14, 0.26, 0.88));
                g2.fill(poly(0.28, 0.16, 0.82, 0.3, 0.28, 0.5));
            }
            case CASTLE -> {
                g2.fill(new Rectangle2D.Double(0.2, 0.42, 0.6, 0.46));
                g2.fill(new Rectangle2D.Double(0.2, 0.24, 0.14, 0.2));
                g2.fill(new Rectangle2D.Double(0.43, 0.24, 0.14, 0.2));
                g2.fill(new Rectangle2D.Double(0.66, 0.24, 0.14, 0.2));
                g2.setColor(Theme.alpha(Color.WHITE, 210));
                g2.fill(new RoundRectangle2D.Double(0.42, 0.6, 0.16, 0.28, 0.16, 0.16));
            }
            case SKIP -> {
                g2.fill(new RoundRectangle2D.Double(0.27, 0.2, 0.15, 0.6, 0.08, 0.08));
                g2.fill(new RoundRectangle2D.Double(0.58, 0.2, 0.15, 0.6, 0.08, 0.08));
            }
            case ROCKET -> {
                g2.draw(poly(0.24, 0.52, 0.5, 0.26, 0.76, 0.52, false));
                g2.draw(poly(0.24, 0.8, 0.5, 0.54, 0.76, 0.8, false));
            }
            case INFO -> {
                g2.draw(new Ellipse2D.Double(0.1, 0.1, 0.8, 0.8));
                dot(g2, 0.5, 0.31, 0.06);
                g2.draw(new Line2D.Double(0.5, 0.46, 0.5, 0.7));
            }
            case WARNING -> {
                g2.draw(poly(0.5, 0.12, 0.9, 0.84, 0.1, 0.84));
                g2.draw(new Line2D.Double(0.5, 0.38, 0.5, 0.6));
                dot(g2, 0.5, 0.72, 0.05);
            }
            case PUSH -> {
                dot(g2, 0.68, 0.28, 0.12);
                g2.fill(poly(0.56, 0.86, 0.6, 0.44, 0.76, 0.44, 0.8, 0.86));
                g2.draw(new Line2D.Double(0.46, 0.64, 0.1, 0.64));
                g2.draw(poly(0.26, 0.48, 0.1, 0.64, 0.26, 0.8, false));
            }
            case STEPS_BACK -> {
                g2.draw(poly(0.36, 0.28, 0.14, 0.5, 0.36, 0.72, false));
                g2.draw(poly(0.6, 0.28, 0.38, 0.5, 0.6, 0.72, false));
                g2.draw(poly(0.84, 0.28, 0.62, 0.5, 0.84, 0.72, false));
            }
            case CROWN -> g2.fill(poly(0.12, 0.78, 0.12, 0.32, 0.33, 0.54, 0.5, 0.2, 0.67, 0.54, 0.88, 0.32, 0.88, 0.78));
            case BOARD -> {
                g2.draw(new RoundRectangle2D.Double(0.14, 0.14, 0.72, 0.72, 0.12, 0.12));
                g2.draw(new Line2D.Double(0.5, 0.14, 0.5, 0.86));
                g2.draw(new Line2D.Double(0.14, 0.5, 0.86, 0.5));
            }
            case EYE -> {
                Path2D eye = new Path2D.Double();
                eye.moveTo(0.08, 0.5);
                eye.quadTo(0.5, 0.1, 0.92, 0.5);
                eye.quadTo(0.5, 0.9, 0.08, 0.5);
                g2.draw(eye);
                dot(g2, 0.5, 0.5, 0.12);
            }
            case LIST -> {
                for (int i = 0; i < 3; i++) {
                    double yy = 0.28 + i * 0.22;
                    dot(g2, 0.2, yy, 0.05);
                    g2.draw(new Line2D.Double(0.36, yy, 0.84, yy));
                }
            }
            case REFRESH -> {
                g2.draw(new Arc2D.Double(0.16, 0.16, 0.68, 0.68, 60, 290, Arc2D.OPEN));
                g2.fill(poly(0.62, 0.06, 0.86, 0.22, 0.6, 0.34));
            }
            case SHIELD -> {
                Path2D shield = new Path2D.Double();
                shield.moveTo(0.5, 0.1);
                shield.lineTo(0.84, 0.22);
                shield.curveTo(0.84, 0.6, 0.68, 0.8, 0.5, 0.9);
                shield.curveTo(0.32, 0.8, 0.16, 0.6, 0.16, 0.22);
                shield.closePath();
                g2.fill(shield);
            }
        }
        g2.dispose();
    }

    // ------------------------------------------------------------------ shared shapes

    /** A star polygon. */
    public static Shape star(double cx, double cy, double outer, double inner, int points) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < points * 2; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double a = -Math.PI / 2 + i * Math.PI / points;
            double px = cx + Math.cos(a) * r;
            double py = cy + Math.sin(a) * r;
            if (i == 0) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
        p.closePath();
        return p;
    }

    /** Gift box used for surprise squares, drawn into (x, y, s, s). */
    public static void gift(Graphics2D g, double x, double y, double s, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.scale(s, s);
        g2.setStroke(new BasicStroke(0.07f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(color);
        g2.draw(new Ellipse2D.Double(0.27, 0.1, 0.24, 0.2));
        g2.draw(new Ellipse2D.Double(0.49, 0.1, 0.24, 0.2));
        g2.fill(new RoundRectangle2D.Double(0.12, 0.3, 0.76, 0.18, 0.06, 0.06));
        g2.fill(new RoundRectangle2D.Double(0.18, 0.47, 0.64, 0.42, 0.06, 0.06));
        g2.setColor(Color.WHITE);
        g2.fill(new Rectangle2D.Double(0.45, 0.3, 0.1, 0.59));
        g2.fill(new Rectangle2D.Double(0.12, 0.46, 0.76, 0.035));
        g2.dispose();
    }

    /** Outline of text, centred on (cx, cy) and scaled to the given height (unit-space friendly). */
    public static Shape glyph(String text, Font font, double cx, double cy, double height) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Shape outline = font.createGlyphVector(frc, text).getOutline();
        Rectangle2D b = outline.getBounds2D();
        double scale = height / b.getHeight();
        AffineTransform at = new AffineTransform();
        at.translate(cx, cy);
        at.scale(scale, scale);
        at.translate(-b.getCenterX(), -b.getCenterY());
        return at.createTransformedShape(outline);
    }

    private static void dot(Graphics2D g, double cx, double cy, double r) {
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static Shape poly(double... xy) {
        return poly(true, xy);
    }

    private static Shape poly(double x1, double y1, double x2, double y2, double x3, double y3, boolean closed) {
        return poly(closed, x1, y1, x2, y2, x3, y3);
    }

    private static Shape poly(double x1, double y1, double x2, double y2, double x3, double y3, double x4,
                              double y4, boolean closed) {
        return poly(closed, x1, y1, x2, y2, x3, y3, x4, y4);
    }

    private static Shape poly(boolean closed, double... xy) {
        GeneralPath p = new GeneralPath();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        if (closed) {
            p.closePath();
        }
        return p;
    }
}
