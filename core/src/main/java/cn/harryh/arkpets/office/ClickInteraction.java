/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

/** Recognizes three completed, hit-tested clicks without allocating an event queue. */
public final class ClickInteraction {
    public static final long WINDOW_NANOS = 2_500_000_000L;
    public static final long COOLDOWN_NANOS = 60_000_000_000L;
    private long first, second, last, triggered;
    private int count;
    private boolean cooling;

    /** Accepts only clicks already validated by the existing input pipeline. */
    public boolean click(long now) {
        if (count > 0 && now < last) clear();
        last = now;
        if (cooling) {
            if (now - triggered < COOLDOWN_NANOS) { clear(); return false; }
            cooling = false;
        }
        if (count > 0 && now - first > WINDOW_NANOS) {
            if (count == 2 && now - second <= WINDOW_NANOS) { first = second; count = 1; }
            else count = 0;
        }
        if (count == 0) { first = now; count = 1; return false; }
        if (count == 1) { second = now; count = 2; return false; }
        clear();
        return true;
    }

    /** Starts cooldown only after the arbiter has actually accepted the reaction. */
    public void triggered(long now) { triggered = now; cooling = true; clear(); }
    /** Drops a partial gesture without bypassing an existing cooldown. */
    public void clear() { count = 0; }
    public int count() { return count; }
    public double cooldownSeconds(long now) {
        return cooling ? Math.max(0, (COOLDOWN_NANOS - (now - triggered)) / 1e9) : 0;
    }
}
