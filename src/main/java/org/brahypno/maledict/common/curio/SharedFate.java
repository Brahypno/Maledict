package org.brahypno.maledict.common.curio;

/**
 * 「分担命运」的数学：双方的血量比例差得越远，这一记越重 —— 而且**方向对称**，
 * 谁打谁都乘同一个倍率，所以它天然是「一对人」的属性，不是某一方的属性。
 *
 * <p>只有不碰 Minecraft 的纯函数，倍率可以单独跑测试（与 {@code DelayedVitalsPool}、{@code RottenBone}
 * 一个分工）。事件落点在 {@link BlissRuneEvents}。
 */
public final class SharedFate {

    /**
     * 比例差满格时的额外伤害：一边满血、一边空血即 {@code |1 − 0| = 1}，倍率到顶
     * {@code 1 + 0.70 = 1.70}。比例相同时是 {@code 1.0}（不做任何事）。
     */
    public static final float BONUS_PER_RATIO = 0.70F;

    /** 一记伤害的倍率：{@code 1 + 0.70 × |攻方血量比 − 守方血量比|}。 */
    public static float multiplier(float attackerHealthRatio, float victimHealthRatio) {
        return 1.0F + BONUS_PER_RATIO * Math.abs(clamp(attackerHealthRatio) - clamp(victimHealthRatio));
    }

    /**
     * 血量比：{@code 当前血量 / 血量上限}，夹到 {@code [0, 1]}。
     *
     * <p>夹这一下是为了「不参与异常状态」：血量上限被别的模组临时削到 0 时不除出 {@code NaN}
     * （按满血算），血量被写到上限之上时也不当成比满血更多。
     */
    public static float healthRatio(float health, float maxHealth) {
        return maxHealth <= 0.0F ? 1.0F : clamp(health / maxHealth);
    }

    private static float clamp(float ratio) {
        return Math.min(1.0F, Math.max(0.0F, ratio));
    }

    private SharedFate() {
    }
}
