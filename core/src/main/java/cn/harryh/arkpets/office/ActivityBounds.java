/** Copyright (c) 2022-2026, Harry Huang; 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import cn.harryh.arkpets.utils.Plane;

/** Controls per-pet boundary suspension during dragging and bounded smooth re-entry. */
public final class ActivityBounds {
    private Plane.RectArea area;
    private boolean returning;

    public void setArea(Plane.RectArea area) { this.area = area; }
    public Plane.RectArea getArea() { return area; }
    public boolean isReturning() { return returning; }

    public float targetX(Plane plane, float width) {
        return Math.max(area.left(), Math.min(plane.getX(), area.right() - width));
    }

    public float targetY(Plane plane, float height) {
        return Math.max(area.bottom(), Math.min(plane.getY(), area.top() - height));
    }

    /** Prepares a frame; returns true while the normal physics update must be suspended. */
    public boolean prepare(Plane plane, float width, float height, boolean dragging, float dt) {
        if (area == null) return false;
        if (width > area.getWidth() || height > area.getHeight())
            throw new IllegalStateException("Pet canvas does not fit the activity region; reduce display scale");
        if (dragging) {
            plane.setMovementArea(null);
            returning = false;
            return false;
        }
        float tx = targetX(plane, width), ty = targetY(plane, height);
        if (Math.abs(tx - plane.getX()) > 0.01f || Math.abs(ty - plane.getY()) > 0.01f) returning = true;
        if (returning) {
            plane.setMovementArea(null);
            plane.stopMotion();
            float dx = tx - plane.getX(), dy = ty - plane.getY();
            float distance = (float) Math.hypot(dx, dy);
            float step = Math.min(distance, 480f * Math.min(0.05f, Math.max(0, dt)));
            if (distance > 0.01f)
                plane.changePosition(0, plane.getX() + dx / distance * step, plane.getY() + dy / distance * step);
            if (distance <= step + 0.01f) {
                plane.changePosition(0, tx, ty);
                returning = false;
                plane.setMovementArea(area);
            }
            return true;
        }
        plane.setMovementArea(area);
        return false;
    }
}
