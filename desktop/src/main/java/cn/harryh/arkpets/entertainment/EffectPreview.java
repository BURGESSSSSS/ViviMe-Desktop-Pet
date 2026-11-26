/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.entertainment;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.List;
import javax.swing.JPanel;

/** Visualizes the existing target-lock states using original Swing geometry. */
public final class EffectPreview extends JPanel {
    private static final double CYCLE_SECONDS = .85;
    private final DawnTargetLock target = new DawnTargetLock();
    private final Point2D.Float shake = new Point2D.Float();
    private List<String> sequence = List.of();
    private long started;
    private int shot;
    private int impacts;
    private double elapsed;

    public EffectPreview() {
        setBackground(new Color(0xE9EFF2));
        setOpaque(true);
    }

    public void start() {
        started = System.nanoTime();
        sequence = DawnPlan.faces(started);
        shot = -1;
        impacts = 0;
        target.reset();
        repaint();
    }

    public int impacts() { return impacts; }

    public void update() {
        if (started == 0) return;
        double time = (System.nanoTime() - started) / 1e9;
        int index = (int) (time / CYCLE_SECONDS);
        if (index >= sequence.size()) {
            started = 0;
            target.reset();
            repaint();
            return;
        }
        elapsed = time - index * CYCLE_SECONDS;
        if (index != shot) {
            shot = index;
            target.reset();
            target.lock(.23 + .26 * (index % 3), .34 + .27 * (index / 3));
        }
        if (target.state() == DawnTargetLock.State.LOCKED && elapsed >= .24) {
            target.advance(true, false);
            impacts++;
        }
        if (elapsed >= .70) target.advance(false, true);
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        var g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x233E4B));
            g.drawString(started == 0 && impacts == 0 ? "程序绘制的索敌与命中示意"
                    : "命中 " + impacts + " / " + sequence.size(), 18, 25);
            if (started == 0) return;
            int x = (int) (target.x(.5) * getWidth());
            int y = (int) (target.y(.5) * getHeight());
            if (target.state() == DawnTargetLock.State.IMPACT) {
                double age = elapsed - .24;
                DawnImpactShake.offset(age, .8, 1, shake);
                int radius = (int) (12 + 75 * age);
                g.translate(shake.x, shake.y);
                g.setColor(new Color(0x16857C));
                g.setStroke(new BasicStroke(3f));
                g.drawOval(x-radius, y-radius, radius*2, radius*2);
            } else {
                g.setColor(new Color(0xD55B33));
                g.drawOval(x-14, y-14, 28, 28);
                g.drawLine(x-5, y, x+5, y);
                g.drawLine(x, y-5, x, y+5);
            }
        } finally {
            g.dispose();
        }
    }
}
