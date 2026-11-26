/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lays out deterministic ransom-note glyphs in a single centered line. */
final class BattleMenuTypography {
    private static final double[] MAIN_OFFSETS = {-1.8, 1.8, -1.2, 2.0, -1.5};
    private static final double[] MAIN_ANGLES = {3.2, -2.8, 2.4, -3.5, 2.0};
    private static final double[] SMALL_OFFSETS = {-.8, .9, -.6, .8};
    private static final double[] SMALL_ANGLES = {1.8, -1.6, 1.4, -1.8};
    private static final double BLOCK_PADDING = 3;
    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(null, true, true);

    private record Key(String text, Font font, boolean main) {}
    private record Glyph(Shape outline, double advance) {}
    private record Line(List<Glyph> glyphs, double width, double tracking, Shape accent) {}

    private final Map<Key, Line> lines = new HashMap<>();

    void drawRansomText(Graphics2D graphics, String text, Font font, boolean main, double firstAngle,
                        int direction, Color accentInk, Color firstInk, Color remainingInk) {
        if (text.isEmpty()) return;
        Line line = lines.computeIfAbsent(new Key(text, font, main), BattleMenuTypography::layout);
        double[] offsets = main ? MAIN_OFFSETS : SMALL_OFFSETS;
        double[] angles = main ? MAIN_ANGLES : SMALL_ANGLES;
        Graphics2D g = (Graphics2D) graphics.create();
        AffineTransform base = g.getTransform();
        double x = -line.width() / 2;
        for (int i = 0; i < line.glyphs().size(); i++) {
            Glyph glyph = line.glyphs().get(i);
            int variation = (i + offsets.length - 1) % offsets.length;
            g.setTransform(base);
            g.translate(x + glyph.advance() / 2, i == 0 ? 0 : offsets[variation]);
            g.rotate(Math.toRadians(direction * (i == 0 ? firstAngle : angles[variation])));
            if (i == 0) {
                g.setColor(accentInk);
                g.fill(line.accent());
            }
            g.setColor(i == 0 ? firstInk : remainingInk);
            g.fill(glyph.outline());
            x += glyph.advance() + line.tracking();
        }
        g.dispose();
    }

    private static Line layout(Key key) {
        int[] characters = key.text().codePoints().toArray();
        List<Glyph> glyphs = new ArrayList<>(characters.length);
        double tracking = key.main() ? -.8 : 0;
        double width = tracking * (characters.length - 1);
        Shape accent = null;
        for (int i = 0; i < characters.length; i++) {
            Font font = key.font();
            if (i == 0) font = font.deriveFont(font.getSize2D() * (key.main() ? 1.30f : 1.14f));
            var glyphVector = font.createGlyphVector(FONT_CONTEXT, Character.toString(characters[i]));
            Shape outline = glyphVector.getOutline();
            var ink = outline.getBounds2D();
            var centered = AffineTransform.getTranslateInstance(-ink.getCenterX(), -ink.getCenterY())
                    .createTransformedShape(outline);
            double advance = glyphVector.getGlyphPosition(glyphVector.getNumGlyphs()).getX();
            if (i == 0) {
                double halfWidth = ink.getWidth() / 2;
                double halfHeight = ink.getHeight() / 2;
                if (key.main()) {
                    double x = halfWidth + BLOCK_PADDING;
                    double y = halfHeight + BLOCK_PADDING;
                    accent = quadrilateral(-x, -y, x + 1, -y - 1, x, y, -x - 1, y + 1);
                    advance = Math.max(advance, 2 * (x + 1));
                } else {
                    double y = halfHeight + 1;
                    accent = quadrilateral(-halfWidth, y, halfWidth, y - 1,
                            halfWidth, y + 1.5, -halfWidth, y + 2.5);
                }
            }
            glyphs.add(new Glyph(centered, advance));
            width += advance;
        }
        return new Line(glyphs, width, tracking, accent);
    }

    private static Shape quadrilateral(double x0, double y0, double x1, double y1,
                                       double x2, double y2, double x3, double y3) {
        var path = new Path2D.Float();
        path.moveTo(x0, y0);
        path.lineTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.closePath();
        return path;
    }
}
