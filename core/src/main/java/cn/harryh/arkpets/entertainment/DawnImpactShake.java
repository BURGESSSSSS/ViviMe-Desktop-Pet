/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.entertainment;

import java.awt.geom.Point2D;

/** Screen-pixel displacement for a sharp kick followed by two diminishing rebounds. */
final class DawnImpactShake {
    static final double DURATION = .35;
    static final double PIXELS_PER_STRENGTH = 7.2;
    private static final double[] TIMES = {0, .045, .096, .151, .219, .278, DURATION};
    private static final double[] X = {.8, .31, -.56, .34, -.15, .05, 0};
    private static final double[] Y = {-.6, -.24, .35, -.20, .1, -.03, 0};

    private DawnImpactShake() {}

    static void offset(double age, double strength, int facing, Point2D.Float out) {
        out.setLocation(0, 0);
        if (age < 0 || age >= DURATION) return;
        int i = 1;
        while (age >= TIMES[i]) i++;
        double u = (age - TIMES[i-1]) / (TIMES[i] - TIMES[i-1]);
        double blend = u*u*(3-2*u);
        double amplitude = PIXELS_PER_STRENGTH * strength;
        out.setLocation((float)(facing * amplitude * (X[i-1] + (X[i] - X[i-1]) * blend)),
                (float)(amplitude * (Y[i-1] + (Y[i] - Y[i-1]) * blend)));
    }
}
