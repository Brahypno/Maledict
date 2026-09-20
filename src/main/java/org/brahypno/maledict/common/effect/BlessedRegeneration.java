package org.brahypno.maledict.common.effect;

/**
 * 「生灵之祝」放大自然回血的那份算术。
 *
 * <p>原版 {@code FoodData#tick} 里有两条自然回血通道，两条最后都走 {@code Player#heal}：
 * <ul>
 *   <li>饱食度回血：饥饿值满 20、饱食度大于 0 且玩家受伤时，每 10 tick 回
 *       {@code min(饱食度, 6) / 6} 点血，并付掉等量的力竭；</li>
 *   <li>饥饿值回血：饥饿值不低于 18 且玩家受伤时，每 80 tick 回 1 点血，付 6 点力竭。</li>
 * </ul>
 * 这两份都是「一份」，本效果每级再补一份：等级 I 共两份（翻倍），等级 II 共三份，依此类推。
 *
 * <p><b>只补量，不改账。</b>原版那份回血、它的节奏与消耗（{@code addExhaustion}）全都不动，
 * 于是放大出来的血量仍然按原版的饱食度与饥饿值付费，效果到期后也不会留下任何残留状态。
 *
 * <p><b>为什么要拿血量差来算。</b>Forge 没有「饱食度回血」事件，{@code LivingHealEvent}
 * 也分不清来源——生命恢复效果的 {@code heal(1)} 与饥饿值回血的 {@code heal(1)} 长得一模一样，
 * 靠事件猜来源必然会误判。所以这里读的是 {@code FoodData#tick} 前后的血量差：那一 tick 里
 * 只有自然回血会动血量，差值就是原版这一份的量。
 *
 * <p>不依赖任何 Minecraft 类型，方便直接单测。
 */
public final class BlessedRegeneration {

    /** 每级补的份数。等级乘数由 {@link #extraHeal} 结算，这里只写单级的值。 */
    public static final float SHARES_PER_LEVEL = 1.0F;

    /**
     * @param healed    原版这一 tick 的自然回血实际回了多少（按 tick 前后的血量差算）
     * @param amplifier 效果等级，0 即等级 I
     * @return 需要额外补发的治疗量；没回血或等级非法时为 0
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
