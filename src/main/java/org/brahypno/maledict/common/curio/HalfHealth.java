package org.brahypno.maledict.common.curio;

/**
 * 「半血生物」这一条判定，对应 tooltip 里的「攻击半血生物时触发魂息虚空」。
 *
 * <p>与 Malum 监视者项链的「攻击满血生物」是同一类判定，只是把线画在半血：
 * 那边读 {@code target.getHealth() >= maxHealth * 0.9875f}，这边读
 * {@code target.getHealth() <= maxHealth * 0.5f}，都在伤害事件里读 {@code getHealth()}——
 * Forge 的伤害事件都在 {@code setHealth} 之前触发，拿到的就是**出手时**目标剩的血。
 *
 * <p>不依赖任何 Minecraft 类型，方便直接单测。
 */
public final class HalfHealth {

    /** 半血线本身。 */
    public static final float THRESHOLD = 0.5F;

    /**
     * @param health    出手时目标的血量
     * @param maxHealth 最大血量
     * @return 是否算「半血生物」
     */
    public static boolean isAtOrBelowHalf(float health, float maxHealth) {
        if (maxHealth <= 0.0F || health <= 0.0F) {
            return false;
        }
        return health <= maxHealth * THRESHOLD;
    }

    private HalfHealth() {
    }
}
