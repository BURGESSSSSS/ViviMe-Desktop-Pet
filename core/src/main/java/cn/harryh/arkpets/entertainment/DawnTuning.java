/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.entertainment;

/** Keeps desktop travel and marker adaptations separate from the original attack cadence. */
final class DawnTuning {
    static final double ORIGINAL_SPEED=154.320987654*10, SPEED_MULTIPLIER=2;
    static double flight(double distance){return Math.max(.09,distance/(ORIGINAL_SPEED*SPEED_MULTIPLIER));}
    static double progress(double elapsed,double duration){return Math.max(0,Math.min(1,elapsed/duration));}
    static double lock(double trackTime){return Math.max(0,Math.min(1,1-(.53333336-trackTime)/.32));}
    static boolean markerVisible(boolean valid,int shots,int live){return valid&&(shots<6||live>0);}
}
