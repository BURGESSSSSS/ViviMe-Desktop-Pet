/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.animations;

/** Lists reviewed user actions; no unreviewed skill names are exposed. */
public enum PetAction {
    WALK("走两步"), SIT("坐下休息"), SLEEP("睡觉（约7秒）"), INTERACT("互动一下"), RELAX("恢复待机"),
    ANGRY("生气了", true), HOP("吓一跳", true), SPECIAL("原版特殊演出（约12秒）");
    public final String label;
    public final boolean battle;
    PetAction(String label){this(label,false);}
    PetAction(String label,boolean battle){this.label=label;this.battle=battle;}
}
