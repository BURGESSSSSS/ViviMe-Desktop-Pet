/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.vivime.demo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/** A character drawn from original geometric shapes. */
final class PlaceholderPet extends JComponent {
    @Override protected void paintComponent(Graphics graphics) {
        var g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(18, 38, 47, 55));
            g.fillOval(16, 89, 62, 10);
            g.setColor(new Color(0x12363D));
            g.fillRoundRect(25, 73, 18, 21, 8, 8);
            g.fillRoundRect(51, 73, 18, 21, 8, 8);
            g.fillRect(45, 13, 4, 17);
            g.setColor(new Color(0xF3A754));
            g.fillOval(40, 4, 14, 14);
            g.setColor(new Color(0x167C76));
            g.fillRoundRect(10, 25, 74, 59, 24, 24);
            g.setColor(new Color(0xD8F0E9));
            g.fillRoundRect(19, 36, 56, 31, 17, 17);
            g.setColor(new Color(0x14383F));
            g.fillOval(29, 46, 7, 9);
            g.fillOval(58, 46, 7, 9);
            g.drawArc(42, 48, 10, 9, 190, 160);
        } finally {
            g.dispose();
        }
    }
}
