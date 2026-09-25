package org.brahypno.maledict.common.curio;

/** 「半血生物」判定：{@code health <= maxHealth * 0.5}，在伤害事件里读到的仍是出手时目标剩的血。 */
public final class HalfHealth {

    public static final float THRESHOLD = 0.5F;

    public static boolean isAtOrBelowHalf(float health, float maxHealth) {
        if (maxHealth <= 0.0F || health <= 0.0F) {
            return false;
        }
        return health <= maxHealth * THRESHOLD;
    }

    private HalfHealth() {
    }
}
