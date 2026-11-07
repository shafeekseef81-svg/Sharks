package View.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.Timer;

import Model.PlayerColor;

/** Short confetti celebration for the results screen. Transparent to the mouse. */
public class Confetti extends JComponent {

    private record Particle(double x, double y, double vx, double vy, double rot, double vr, double w, double h,
                            Color color) {
    }

    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private Timer timer;
    private long start;

    public Confetti() {
        setOpaque(false);
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    public void start() {
        stop();
        particles.clear();
        if (Animations.instant) {
            return;
        }
        Color[] colors = {
                Theme.player(PlayerColor.RED), Theme.player(PlayerColor.GREEN), Theme.player(PlayerColor.YELLOW),
                Theme.player(PlayerColor.BLUE), Theme.GOLD, Color.WHITE
        };
        int w = Math.max(400, getWidth());
        for (int i = 0; i < 140; i++) {
            particles.add(new Particle(random.nextDouble() * w, -random.nextDouble() * 400 - 20,
                    (random.nextDouble() - 0.5) * 1.6, 1.6 + random.nextDouble() * 2.4,
                    random.nextDouble() * Math.PI, (random.nextDouble() - 0.5) * 0.2,
                    6 + random.nextDouble() * 6, 3 + random.nextDouble() * 4, colors[random.nextInt(colors.length)]));
        }
        start = System.currentTimeMillis();
        timer = new Timer(16, e -> step());
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        particles.clear();
        repaint();
    }

    private void step() {
        List<Particle> next = new ArrayList<>();
        for (Particle p : particles) {
            Particle q = new Particle(p.x + p.vx + Math.sin((p.y + p.rot * 40) / 30) * 0.6, p.y + p.vy, p.vx, p.vy,
                    p.rot + p.vr, p.vr, p.w, p.h, p.color);
            if (q.y < getHeight() + 20) {
                next.add(q);
            }
        }
        particles.clear();
        particles.addAll(next);
        if (particles.isEmpty() || System.currentTimeMillis() - start > 7000) {
            stop();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (particles.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        for (Particle p : particles) {
            AffineTransform old = g2.getTransform();
            g2.translate(p.x, p.y);
            g2.rotate(p.rot);
            g2.setColor(p.color);
            g2.fill(new Rectangle2D.Double(-p.w / 2, -p.h / 2, p.w, p.h * Math.abs(Math.cos(p.rot * 2)) + 1));
            g2.setTransform(old);
        }
        g2.dispose();
    }
}
