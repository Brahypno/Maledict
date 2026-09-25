package org.brahypno.maledict.common.effect;

/**
 * 「生灵之祝」放大自然回血的那份算术：每级把该 tick 的自然回血量再补一份，原版那份回血与力竭开销一概不动。
 * Forge 没有能区分来源的回血事件（生命恢复与饥饿值回血的 {@code heal(1)} 长得一样），所以只能读 {@code FoodData#tick} 前后的血量差。
 */
public final class BlessedRegeneration {

    /** 每级补的份数，等级乘数由 {@link #extraHeal} 结算。 */
    public static final float SHARES_PER_LEVEL = 1.0F;

    /**
     * @param healed    这一 tick 原版自然回血的实际回血量（tick 前后的血量差）
     * @param amplifier 效果等级，0 即等级 I
     * @return 额外补发的治疗量；没回血或等级非法时为 0
     */
    public static float extraHeal(float healed, int amplifier) {
        if (healed <= 0.0F || amplifier < 0) {
            return 0.0F;
        }
        return healed * SHARES_PER_LEVEL * (amplifier + 1);
    }

    private BlessedRegeneration() {
    }
}
