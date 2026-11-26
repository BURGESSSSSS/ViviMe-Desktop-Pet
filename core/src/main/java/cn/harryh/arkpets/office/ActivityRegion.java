/** Copyright (c) 2022-2026, Harry Huang; 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import cn.harryh.arkpets.utils.Plane;

/** Computes screen-space activity regions without assuming a zero monitor origin. */
public record ActivityRegion(float x, float y, float width, float height) {
    public enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    public enum Mode { OFFICE, ENTERTAINMENT }

    public static Corner corner(String value) {
        try { return Corner.valueOf(value); }
        catch (RuntimeException ignored) { return Corner.BOTTOM_RIGHT; }
    }

    public static Mode mode(String value) {
        try { return Mode.valueOf(value); }
        catch (RuntimeException ignored) { return Mode.OFFICE; }
    }

    /** Gets the default one-eighth office region or the internal full work area. */
    public static ActivityRegion of(float x, float y, float width, float height, Mode mode, Corner corner) {
        return of(x, y, width, height, mode, corner, .5f, .25f);
    }

    public static ActivityRegion of(float x, float y, float width, float height, Mode mode, Corner corner,
                                    float widthRatio, float heightRatio) {
        if (!(width > 0 && height > 0)) throw new IllegalArgumentException("Invalid work area");
        if (mode == Mode.ENTERTAINMENT) return new ActivityRegion(x, y, width, height);
        if (!Float.isFinite(widthRatio) || widthRatio <= 0 || widthRatio > 1) widthRatio = .5f;
        if (!Float.isFinite(heightRatio) || heightRatio <= 0 || heightRatio > 1) heightRatio = .25f;
        float w = (float) Math.floor(width * widthRatio), h = (float) Math.floor(height * heightRatio);
        boolean right = corner == Corner.TOP_RIGHT || corner == Corner.BOTTOM_RIGHT;
        boolean bottom = corner == Corner.BOTTOM_LEFT || corner == Corner.BOTTOM_RIGHT;
        return new ActivityRegion(right ? x + width - w : x, bottom ? y + height - h : y, w, h);
    }

    /** Gets conservative physics bounds for the whole fitted canvas and all animation offsets. */
    public Plane.RectArea physicsArea(float margin, float maxOffset) {
        return new Plane.RectArea(x + margin, x + width - margin,
                -y - margin, -(y + height) + margin + maxOffset);
    }

    /** Moves one coordinate toward its goal with a bounded speed and no overshoot. */
    public static float approach(float current, float goal, float maxDistance) {
        return current + Math.copySign(Math.min(Math.abs(goal - current), Math.max(0, maxDistance)), goal - current);
    }

    public boolean contains(float left, float top, float w, float h, float epsilon) {
        return left >= x - epsilon && top >= y - epsilon
                && left + w <= x + width + epsilon && top + h <= y + height + epsilon;
    }
}
