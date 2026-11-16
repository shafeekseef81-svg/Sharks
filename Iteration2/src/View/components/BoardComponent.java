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
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.Timer;

import Model.Board;
import Model.Cell;
import Model.Direction;
import Model.GameModel;
import Model.Piece;
import Model.Player;
import Model.PlayerColor;
import Model.Square;

/**
 * Java2D Ludo board that follows the official diagrams: castles in the corners, the cross-shaped
 * classic track, outer rings on Medium/Hard, coloured home paths, the centre triangles and the
 * special squares (#, arrows, stars, coloured question marks, gifts). It scales with the window.
 *
 * <p>Positions are kept in "cell units" (x = column, y = row; a cell centre is at +0.5) so that
 * animations keep working while the window is resized.
 */
public class BoardComponent extends JComponent {

    /** Receives clicks on highlighted (legal) pieces. */
    public interface PieceListener {
        void pieceClicked(Piece piece);
    }

    private static final int[][] SPOTS = {{2, 2}, {4, 2}, {2, 4}, {4, 4}};

    private GameModel state;
    private boolean interactive = true;
    private boolean showActiveGlow = true;
    private PieceListener listener;
    private Map<Piece, Integer> targets = Map.of();
    private Piece hovered;
    private final Map<Piece, Point2D.Double> overrides = new HashMap<>();
    private BufferedImage cache;
    private int cacheW;
    private int cacheH;
    private double cacheScale;
    private final Timer pulse;
    private double pulsePhase;
    private String bannerText;
    private Color bannerColor;
    private long bannerStart;

    private int n;
    private double cell;
    private double ox;
    private double oy;

    public BoardComponent() {
        setOpaque(false);
        setPreferredSize(new Dimension(640, 640));
        pulse = new Timer(40, e -> {
            pulsePhase += 0.16;
            repaint();
        });
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (interactive) {
                    Piece p = pieceAt(e.getPoint());
                    setHovered(p != null && targets.containsKey(p) ? p : null);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHovered(null);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!interactive || listener == null) {
                    return;
                }
                Piece p = pieceAt(e.getPoint());
                if (p != null && targets.containsKey(p)) {
                    listener.pieceClicked(p);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // ------------------------------------------------------------------ configuration

    public void setGame(GameModel state) {
        this.state = state;
        this.targets = Map.of();
        this.hovered = null;
        this.overrides.clear();
        refreshStatic();
    }

    /** Non-interactive boards (main menu preview) ignore the mouse and do not pulse. */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        this.showActiveGlow = interactive;
        updatePulse();
    }

    public void setPieceListener(PieceListener listener) {
        this.listener = listener;
    }

    /** Highlights legal pieces with their destination progress (FR-24). */
    public void setTargets(Map<Piece, Integer> targets) {
        this.targets = new HashMap<>(targets);
        this.hovered = null;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    public void clearTargets() {
        setTargets(Map.of());
    }

    public Map<Piece, Integer> targets() {
        return Map.copyOf(targets);
    }

    /** Shows the path preview of a piece (mouse hover; also used by the screenshot tool). */
    public void setHovered(Piece piece) {
        if (piece != hovered) {
            hovered = piece;
            setCursor(piece != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            repaint();
        }
    }

    /** Redraws the cached static layer (call after the board or the players change). */
    public void refreshStatic() {
        cache = null;
        repaint();
    }

    /** Short message over the board, e.g. "Roll again!". */
    public void showBanner(String text, Color color) {
        bannerText = UiKit.ltr(text);
        bannerColor = color;
        bannerStart = System.currentTimeMillis();
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        updatePulse();
    }

    @Override
    public void removeNotify() {
        pulse.stop();
        super.removeNotify();
    }

    private void updatePulse() {
        if (interactive && isDisplayable()) {
            pulse.start();
        } else {
            pulse.stop();
        }
    }

    // ------------------------------------------------------------------ positions (cell units)

    public Point2D.Double cellCenter(Cell c) {
        return new Point2D.Double(c.col() + 0.5, c.row() + 0.5);
    }

    private int[] castleOrigin(PlayerColor color) {
        Board b = state.board();
        int o = b.offset();
        int far = b.size() - o - 6;
        return switch (b.corner(color)) {
            case TOP_LEFT -> new int[] {o, o};
            case TOP_RIGHT -> new int[] {o, far};
            case BOTTOM_RIGHT -> new int[] {far, far};
            case BOTTOM_LEFT -> new int[] {far, o};
        };
    }

    public Point2D.Double castleSpot(Piece piece) {
        int[] origin = castleOrigin(piece.color());
        int[] spot = SPOTS[piece.index()];
        return new Point2D.Double(origin[1] + spot[0], origin[0] + spot[1]);
    }

    public Point2D.Double finishSpot(Piece piece) {
        Board b = state.board();
        double o = b.offset();
        double dx = (piece.index() % 2 == 0 ? -1 : 1);
        double dy = (piece.index() < 2 ? -1 : 1);
        return switch (b.corner(piece.color())) {
            case TOP_LEFT -> new Point2D.Double(o + 6.52 + dx * 0.17, o + 7.5 + dy * 0.3);
            case TOP_RIGHT -> new Point2D.Double(o + 7.5 + dx * 0.3, o + 6.52 + dy * 0.17);
            case BOTTOM_RIGHT -> new Point2D.Double(o + 8.48 + dx * 0.17, o + 7.5 + dy * 0.3);
            case BOTTOM_LEFT -> new Point2D.Double(o + 7.5 + dx * 0.3, o + 8.48 + dy * 0.17);
        };
    }

    /** Current resting position of a piece (castle spot, finish slot or square centre). */
    public Point2D.Double restingPosition(Piece piece) {
        if (piece.inCastle()) {
            return castleSpot(piece);
        }
        if (piece.finished()) {
            return finishSpot(piece);
        }
        return cellCenter(state.board().cellAt(piece.color(), piece.progress()));
    }

    // ------------------------------------------------------------------ animation

    /** Keeps a piece drawn at a position (e.g. a captured piece until the capturing piece arrives). */
    public void holdPiece(Piece piece, Point2D.Double position) {
        overrides.put(piece, position);
        repaint();
    }

    /** Moves a piece square by square with a small hop (NFR-04: at most about 1.6 s). */
    public void animateAlong(Piece piece, List<Cell> cells, Runnable done) {
        if (cells.size() < 2) {
            done.run();
            return;
        }
        int steps = cells.size() - 1;
        int duration = Math.max(260, Math.min(1600, steps * 120));
        Animations.animate(duration, t -> {
            double pos = t * steps;
            int i = Math.min((int) pos, steps - 1);
            double f = pos - i;
            Point2D.Double a = cellCenter(cells.get(i));
            Point2D.Double b = cellCenter(cells.get(i + 1));
            double hop = Math.sin(f * Math.PI) * 0.3;
            overrides.put(piece, new Point2D.Double(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f - hop));
            repaint();
        }, () -> {
            overrides.remove(piece);
            repaint();
            done.run();
        });
    }

    /** Moves a piece in one arc, e.g. out of the castle or back to it. */
    public void animateJump(Piece piece, Point2D.Double from, Point2D.Double to, Runnable done) {
        double dist = from.distance(to);
        int duration = (int) Math.max(380, Math.min(900, 300 + dist * 40));
        Animations.animate(duration, t -> {
            double e = Animations.easeInOut(t);
            double hop = Math.sin(t * Math.PI) * Math.min(2.2, 0.6 + dist * 0.12);
            overrides.put(piece, new Point2D.Double(from.x + (to.x - from.x) * e, from.y + (to.y - from.y) * e - hop));
            repaint();
        }, () -> {
            overrides.remove(piece);
            repaint();
            done.run();
        });
    }

    // ------------------------------------------------------------------ painting

    private void layoutBoard() {
        n = state.board().size();
        double avail = Math.min(getWidth(), getHeight());
        cell = (avail - 10) / (n + 0.35);
        double side = cell * n;
        ox = (getWidth() - side) / 2.0;
        oy = (getHeight() - side) / 2.0;
    }

    private double px(double col) {
        return ox + col * cell;
    }

    private double py(double row) {
        return oy + row * cell;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (state == null || getWidth() < 50 || getHeight() < 50) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        layoutBoard();
        double scale = Math.max(1.0, g2.getTransform().getScaleX());
        int w = getWidth();
        int h = getHeight();
        if (cache == null || cacheW != w || cacheH != h || cacheScale != scale) {
            cache = new BufferedImage((int) Math.ceil(w * scale), (int) Math.ceil(h * scale), BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = cache.createGraphics();
            Theme.antialias(cg);
            cg.scale(scale, scale);
            paintStatic(cg);
            cg.dispose();
            cacheW = w;
            cacheH = h;
            cacheScale = scale;
        }
        g2.drawImage(cache, 0, 0, w, h, null);
        paintDynamic(g2);
        g2.dispose();
    }

    private void paintStatic(Graphics2D g) {
        Board b = state.board();
        double side = n * cell;
        double arc = cell * 0.9;
        for (int i = 6; i >= 1; i--) {
            g.setColor(new Color(0, 0, 0, 14));
            g.fill(new RoundRectangle2D.Double(ox - i * 0.6, oy - i * 0.3 + i * 1.1, side + i * 1.2, side + i * 0.8,
                    arc + i * 2, arc + i * 2));
        }
        RoundRectangle2D outline = new RoundRectangle2D.Double(ox, oy, side, side, arc, arc);
        g.setColor(Color.WHITE);
        g.fill(outline);
        Shape oldClip = g.getClip();
        g.clip(outline);
        BasicStroke grid = new BasicStroke((float) Math.max(1.0, cell * 0.045));
        for (Square s : b.squares()) {
            drawSquare(g, s, grid);
        }
        for (Board.Corner corner : Board.Corner.values()) {
            drawCastle(g, b.colorAt(corner));
        }
        drawCenter(g);
        g.setClip(oldClip);
        g.setColor(Theme.BOARD_LINE);
        g.setStroke(new BasicStroke((float) Math.max(2.0, cell * 0.09)));
        g.draw(outline);
    }

    private void drawSquare(Graphics2D g, Square s, BasicStroke grid) {
        Cell c = s.cell();
        Rectangle2D r = new Rectangle2D.Double(px(c.col()), py(c.row()), cell, cell);
        Color fill = Color.WHITE;
        switch (s.type()) {
            case HOME_PATH, START -> fill = Theme.player(s.owner());
            case QUESTION -> fill = Theme.levelLight(s.questionLevel());
            default -> {
                if (s.isTinted()) {
                    fill = Theme.player(s.owner());
                }
            }
        }
        g.setColor(fill);
        g.fill(r);
        g.setColor(Theme.BOARD_LINE);
        g.setStroke(grid);
        g.draw(r);
        double cx = r.getCenterX();
        double cy = r.getCenterY();
        switch (s.type()) {
            case START -> {
                Shape hash = Icons.glyph("#", Theme.font(Font.BOLD, 40f), cx, cy, cell * 0.58);
                g.setColor(Theme.alpha(Theme.playerDark(s.owner()), 150));
                g.setStroke(new BasicStroke((float) (cell * 0.07)));
                g.draw(hash);
                g.setColor(Color.WHITE);
                g.fill(hash);
            }
            case ARROW -> drawArrow(g, cx, cy, s.arrowDirection(), Theme.player(s.owner()));
            case PROTECTION -> {
                Shape star = Icons.star(cx, cy + cell * 0.03, cell * 0.36, cell * 0.155, 5);
                g.setColor(Color.WHITE);
                g.fill(star);
                g.setColor(Theme.STAR);
                g.setStroke(new BasicStroke((float) Math.max(1.0, cell * 0.055), BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g.draw(star);
            }
            case QUESTION -> {
                g.setColor(Theme.level(s.questionLevel()));
                g.fill(Icons.glyph("?", Theme.font(Font.BOLD, 40f), cx, cy, cell * 0.6));
            }
            case SURPRISE -> Icons.gift(g, r.getX() + cell * 0.17, r.getY() + cell * 0.13, cell * 0.66, Theme.SURPRISE);
            default -> {
                // plain square
            }
        }
    }

    private void drawArrow(Graphics2D g, double cx, double cy, Direction dir, Color color) {
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(-0.32, -0.08);
        arrow.lineTo(0.06, -0.08);
        arrow.lineTo(0.06, -0.24);
        arrow.lineTo(0.34, 0.0);
        arrow.lineTo(0.06, 0.24);
        arrow.lineTo(0.06, 0.08);
        arrow.lineTo(-0.32, 0.08);
        arrow.closePath();
        double angle = switch (dir) {
            case EAST -> 0;
            case SOUTH -> Math.PI / 2;
            case WEST -> Math.PI;
            case NORTH -> -Math.PI / 2;
        };
        AffineTransform at = new AffineTransform();
        at.translate(cx, cy);
        at.rotate(angle);
        at.scale(cell, cell);
        g.setColor(color);
        g.fill(at.createTransformedShape(arrow));
    }

    private Rectangle2D castleRect(PlayerColor color) {
        int[] origin = castleOrigin(color);
        return new Rectangle2D.Double(px(origin[1]), py(origin[0]), cell * 6, cell * 6);
    }

    private void drawCastle(Graphics2D g, PlayerColor color) {
        Rectangle2D r = castleRect(color);
        Player player = state.playerOf(color);
        boolean playing = player != null;
        Color base = Theme.player(color);
        Color dark = Theme.playerDark(color);
        if (!playing) {
            base = Theme.mix(base, new Color(0xC9CED9), 0.55);
            dark = Theme.mix(dark, new Color(0x8C93A8), 0.55);
        }
        double inset = cell * 0.08;
        RoundRectangle2D outer = new RoundRectangle2D.Double(r.getX() + inset, r.getY() + inset,
                r.getWidth() - inset * 2, r.getHeight() - inset * 2, cell * 0.8, cell * 0.8);
        g.setPaint(new GradientPaint((float) r.getX(), (float) r.getY(), Theme.mix(base, Color.WHITE, 0.14),
                (float) r.getMaxX(), (float) r.getMaxY(), base));
        g.fill(outer);
        g.setColor(dark);
        g.setStroke(new BasicStroke((float) Math.max(1.0, cell * 0.05)));
        g.draw(outer);
        RoundRectangle2D inner = new RoundRectangle2D.Double(r.getX() + cell, r.getY() + cell, cell * 4, cell * 4,
                cell * 0.45, cell * 0.45);
        g.setColor(Color.WHITE);
        g.fill(inner);
        g.setColor(dark);
        g.setStroke(new BasicStroke((float) Math.max(1.2, cell * 0.08)));
        g.draw(inner);
        Color spotFill = playing ? Theme.playerLight(color) : new Color(0xEEF0F4);
        for (int[] spot : SPOTS) {
            double sx = r.getX() + spot[0] * cell;
            double sy = r.getY() + spot[1] * cell;
            double sr = cell * 0.62;
            g.setColor(spotFill);
            g.fill(new Ellipse2D.Double(sx - sr, sy - sr, sr * 2, sr * 2));
            g.setColor(Theme.alpha(dark, 90));
            g.setStroke(new BasicStroke((float) Math.max(1.0, cell * 0.04)));
            g.draw(new Ellipse2D.Double(sx - sr, sy - sr, sr * 2, sr * 2));
        }
        String label = playing ? player.name() : "Not playing";
        Color text = playing ? Theme.textOn(color) : new Color(0x5E6784);
        float size = (float) Math.max(12, Math.min(20, cell * 0.5));
        Font font = Theme.font(Font.BOLD, size);
        FontMetrics fm = g.getFontMetrics(font);
        while (fm.stringWidth(label) > cell * 5.5 && size > 12) {
            size -= 0.5f;
            font = Theme.font(Font.BOLD, size);
            fm = g.getFontMetrics(font);
        }
        boolean top = state.board().corner(color) == Board.Corner.TOP_LEFT
                || state.board().corner(color) == Board.Corner.TOP_RIGHT;
        double bandCenter = top ? r.getY() + cell * 0.54 : r.getMaxY() - cell * 0.54;
        g.setFont(font);
        g.setColor(text);
        g.drawString(label, (float) (r.getCenterX() - fm.stringWidth(label) / 2.0),
                (float) (bandCenter + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    private void drawCenter(Graphics2D g) {
        Board b = state.board();
        int o = b.offset();
        double x0 = px(o + 6);
        double y0 = py(o + 6);
        double x1 = px(o + 9);
        double y1 = py(o + 9);
        double cx = (x0 + x1) / 2;
        double cy = (y0 + y1) / 2;
        triangle(g, b.colorAt(Board.Corner.TOP_LEFT), x0, y0, x0, y1, cx, cy);
        triangle(g, b.colorAt(Board.Corner.TOP_RIGHT), x0, y0, x1, y0, cx, cy);
        triangle(g, b.colorAt(Board.Corner.BOTTOM_RIGHT), x1, y0, x1, y1, cx, cy);
        triangle(g, b.colorAt(Board.Corner.BOTTOM_LEFT), x0, y1, x1, y1, cx, cy);
        g.setColor(Theme.BOARD_LINE);
        g.setStroke(new BasicStroke((float) Math.max(1.2, cell * 0.06)));
        g.draw(new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0));
    }

    private void triangle(Graphics2D g, PlayerColor color, double ax, double ay, double bx, double by, double cx,
                          double cy) {
        Path2D t = new Path2D.Double();
        t.moveTo(ax, ay);
        t.lineTo(bx, by);
        t.lineTo(cx, cy);
        t.closePath();
        g.setColor(Theme.player(color));
        g.fill(t);
        g.setColor(Theme.BOARD_LINE);
        g.setStroke(new BasicStroke((float) Math.max(1.0, cell * 0.045)));
        g.draw(t);
    }

    private void paintDynamic(Graphics2D g) {
        if (showActiveGlow && !state.isGameOver()) {
            PlayerColor active = state.currentPlayer().color();
            Rectangle2D r = castleRect(active);
            double a = 0.55 + 0.35 * Math.sin(pulsePhase);
            RoundRectangle2D ring = new RoundRectangle2D.Double(r.getX() + cell * 0.06, r.getY() + cell * 0.06,
                    r.getWidth() - cell * 0.12, r.getHeight() - cell * 0.12, cell * 0.8, cell * 0.8);
            g.setColor(Theme.alpha(Color.WHITE, (int) (150 * a)));
            g.setStroke(new BasicStroke((float) (cell * 0.22)));
            g.draw(ring);
            g.setColor(Theme.alpha(Theme.playerDark(active), (int) (255 * a)));
            g.setStroke(new BasicStroke((float) (cell * 0.09)));
            g.draw(ring);
        }
        if (hovered != null && targets.containsKey(hovered)) {
            drawPreview(g, hovered, targets.get(hovered));
        }
        drawPieces(g);
        drawBanner(g);
    }

    private void drawPreview(Graphics2D g, Piece piece, int target) {
        Board b = state.board();
        Color color = Theme.playerDark(piece.color());
        Point2D.Double dest = target >= b.finishIndex() ? finishSpot(piece) : cellCenter(b.cellAt(piece.color(), target));
        if (piece.inCastle()) {
            Point2D.Double from = castleSpot(piece);
            g.setColor(Theme.alpha(color, 170));
            g.setStroke(new BasicStroke((float) (cell * 0.07), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[] {(float) (cell * 0.18), (float) (cell * 0.14)}, 0f));
            g.draw(new Line2D.Double(px(from.x), py(from.y), px(dest.x), py(dest.y)));
        } else {
            for (int i = piece.progress() + 1; i < target; i++) {
                Point2D.Double p = cellCenter(b.cellAt(piece.color(), i));
                double r = cell * 0.11;
                g.setColor(Theme.alpha(color, 175));
                g.fill(new Ellipse2D.Double(px(p.x) - r, py(p.y) - r, r * 2, r * 2));
            }
        }
        double rr = cell * 0.44;
        g.setColor(Theme.alpha(Theme.player(piece.color()), 70));
        g.fill(new Ellipse2D.Double(px(dest.x) - rr, py(dest.y) - rr, rr * 2, rr * 2));
        g.setColor(color);
        g.setStroke(new BasicStroke((float) (cell * 0.07), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[] {(float) (cell * 0.16), (float) (cell * 0.1)}, (float) (pulsePhase * cell * 0.1)));
        g.draw(new Ellipse2D.Double(px(dest.x) - rr, py(dest.y) - rr, rr * 2, rr * 2));

        int steps = piece.inCastle() ? 0 : target - piece.progress();
        String label = piece.inCastle() ? "Start" : "+" + steps;
        Font font = Theme.font(Font.BOLD, (float) Math.max(12, cell * 0.38));
        FontMetrics fm = g.getFontMetrics(font);
        double w = fm.stringWidth(label) + cell * 0.4;
        double h = fm.getHeight() + 2;
        double lx = Math.max(ox, Math.min(ox + n * cell - w, px(dest.x) - w / 2));
        double ly = Math.max(oy, py(dest.y) - rr - h - 2);
        g.setColor(Theme.playerDark(piece.color()));
        g.fill(new RoundRectangle2D.Double(lx, ly, w, h, h, h));
        g.setColor(Color.WHITE);
        g.setFont(font);
        g.drawString(label, (float) (lx + (w - fm.stringWidth(label)) / 2), (float) (ly + (h + fm.getAscent() - fm.getDescent()) / 2));
    }

    private void drawPieces(Graphics2D g) {
        Board b = state.board();
        Map<Cell, List<Piece>> byCell = new HashMap<>();
        List<Piece> all = new ArrayList<>();
        for (Player p : state.players()) {
            for (Piece piece : p.pieces()) {
                all.add(piece);
                if (piece.onBoard() && !overrides.containsKey(piece)) {
                    byCell.computeIfAbsent(b.cellAt(piece.color(), piece.progress()), k -> new ArrayList<>()).add(piece);
                }
            }
        }
        all.sort((x, y) -> Integer.compare(order(x), order(y)));
        for (Piece piece : all) {
            Point2D.Double pos;
            double scale = 1.0;
            if (overrides.containsKey(piece)) {
                pos = overrides.get(piece);
            } else if (piece.inCastle()) {
                pos = castleSpot(piece);
            } else if (piece.finished()) {
                pos = finishSpot(piece);
                scale = 0.58;
            } else {
                List<Piece> group = byCell.get(b.cellAt(piece.color(), piece.progress()));
                pos = cellCenter(b.cellAt(piece.color(), piece.progress()));
                if (group != null && group.size() > 1) {
                    double[] off = groupOffset(group.indexOf(piece), group.size());
                    pos = new Point2D.Double(pos.x + off[0], pos.y + off[1]);
                    scale = 0.72;
                }
            }
            boolean legal = targets.containsKey(piece);
            double bob = legal ? Math.sin(pulsePhase * 1.3) * 0.05 : 0;
            double x = px(pos.x);
            double y = py(pos.y - bob);
            double r = cell * 0.4 * scale * (piece == hovered ? 1.12 : 1.0);
            if (legal) {
                double gr = r * 1.32 + Math.sin(pulsePhase) * cell * 0.04;
                g.setColor(Theme.alpha(Color.WHITE, 230));
                g.setStroke(new BasicStroke((float) (cell * 0.1)));
                g.draw(new Ellipse2D.Double(x - gr, y - gr - r * 0.12, gr * 2, gr * 2));
                g.setColor(Theme.playerDark(piece.color()));
                g.setStroke(new BasicStroke((float) (cell * 0.055)));
                g.draw(new Ellipse2D.Double(x - gr, y - gr - r * 0.12, gr * 2, gr * 2));
            }
            paintPawn(g, x, y, r, piece.color());
        }
    }

    private int order(Piece p) {
        if (overrides.containsKey(p)) {
            return 3;
        }
        if (p == hovered) {
            return 2;
        }
        return targets.containsKey(p) ? 1 : 0;
    }

    private static double[] groupOffset(int index, int size) {
        if (size == 2) {
            return new double[] {index == 0 ? -0.19 : 0.19, 0};
        }
        if (size == 3) {
            double[][] o = {{-0.19, -0.14}, {0.19, -0.14}, {0, 0.18}};
            return o[Math.min(index, 2)];
        }
        double[][] o = {{-0.19, -0.16}, {0.19, -0.16}, {-0.19, 0.18}, {0.19, 0.18}};
        return o[index % 4];
    }

    /** Small pawn icon for player lists and cards. */
    public static javax.swing.Icon pawnIcon(PlayerColor color, int size) {
        return new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                paintPawn(g2, x + size / 2.0, y + size * 0.55, size * 0.46, color);
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
        };
    }

    /** Pawn in the style of the official diagrams: coloured base, white stem, glossy head. */
    public static void paintPawn(Graphics2D g, double cx, double cy, double r, PlayerColor color) {
        Color base = Theme.player(color);
        Color dark = Theme.playerDark(color);
        Color light = Theme.mix(base, Color.WHITE, 0.5);
        double y = cy + r * 0.12;
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(cx - r * 0.86, y + r * 0.42, r * 1.72, r * 0.5));
        Ellipse2D disk = new Ellipse2D.Double(cx - r * 0.82, y - r * 0.02, r * 1.64, r * 0.86);
        g.setPaint(new GradientPaint((float) cx, (float) (y - r * 0.02), light, (float) cx, (float) (y + r * 0.84), dark));
        g.fill(disk);
        g.setColor(dark);
        g.setStroke(new BasicStroke((float) Math.max(1.0, r * 0.1)));
        g.draw(disk);
        RoundRectangle2D stem = new RoundRectangle2D.Double(cx - r * 0.27, y - r * 0.5, r * 0.54, r * 0.86, r * 0.3, r * 0.3);
        g.setColor(new Color(0xF5F7FB));
        g.fill(stem);
        g.setColor(Theme.alpha(dark, 170));
        g.setStroke(new BasicStroke((float) Math.max(0.8, r * 0.07)));
        g.draw(stem);
        Ellipse2D head = new Ellipse2D.Double(cx - r * 0.55, y - r * 1.16, r * 1.1, r * 1.1);
        g.setPaint(new RadialGradientPaint((float) (cx - r * 0.2), (float) (y - r * 0.85), (float) (r * 0.85),
                new float[] {0f, 0.55f, 1f}, new Color[] {light, base, dark}));
        g.fill(head);
        g.setColor(dark);
        g.setStroke(new BasicStroke((float) Math.max(1.0, r * 0.09)));
        g.draw(head);
        g.setColor(new Color(255, 255, 255, 170));
        g.fill(new Ellipse2D.Double(cx - r * 0.32, y - r * 1.02, r * 0.32, r * 0.22));
    }

    private void drawBanner(Graphics2D g) {
        if (bannerText == null) {
            return;
        }
        double alpha = 1.0;
        if (!Animations.instant) {
            long t = System.currentTimeMillis() - bannerStart;
            if (t > 2300) {
                bannerText = null;
                return;
            }
            alpha = t < 140 ? t / 140.0 : t > 1900 ? (2300 - t) / 400.0 : 1.0;
        }
        Font font = Theme.font(Font.BOLD, (float) Math.max(16, Math.min(24, cell * 0.62)));
        FontMetrics fm = g.getFontMetrics(font);
        double w = fm.stringWidth(bannerText) + 44;
        double h = fm.getHeight() + 18;
        double x = getWidth() / 2.0 - w / 2;
        double y = oy + n * cell * 0.5 - h / 2;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, alpha))));
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fill(new RoundRectangle2D.Double(x + 2, y + 5, w, h, h, h));
        g2.setColor(bannerColor);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, h, h));
        g2.setFont(font);
        boolean lightFill = (bannerColor.getRed() * 299 + bannerColor.getGreen() * 587 + bannerColor.getBlue() * 114) / 1000 > 170;
        g2.setColor(lightFill ? Theme.TEXT : Color.WHITE);
        g2.drawString(bannerText, (float) (x + 22), (float) (y + (h + fm.getAscent() - fm.getDescent()) / 2));
        g2.dispose();
    }

    // ------------------------------------------------------------------ hit testing

    /** The piece drawn under a point, preferring highlighted pieces. */
    public Piece pieceAt(Point p) {
        if (state == null) {
            return null;
        }
        layoutBoard();
        Piece best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player player : state.players()) {
            for (Piece piece : player.pieces()) {
                Point2D.Double pos = overrides.getOrDefault(piece, restingPosition(piece));
                double dist = p.distance(px(pos.x), py(pos.y) - cell * 0.1);
                double radius = cell * (targets.containsKey(piece) ? 0.55 : 0.42);
                if (dist <= radius && (best == null || targets.containsKey(piece) && !targets.containsKey(best)
                        || dist < bestDist && targets.containsKey(piece) == targets.containsKey(best))) {
                    best = piece;
                    bestDist = dist;
                }
            }
        }
        return best;
    }

    /** Screen position of a piece (used by tests to click it). */
    public Point screenPosition(Piece piece) {
        layoutBoard();
        Point2D.Double pos = restingPosition(piece);
        return new Point((int) Math.round(px(pos.x)), (int) Math.round(py(pos.y) - cell * 0.1));
    }
}
