package org.brahypno.maledict.common.curio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 无忧符文的倍率：血量比例差多少、这一击重多少，以及「方向对称」这条性质。 */
class SharedFateTest {

    private static final double DELTA = 1.0E-4D;

    /** 比例相同（谁都满血、或者都在半血）时什么都不做，倍率正好是 1。 */
    @Test
    void equalRatiosDoNothing() {
        assertEquals(1.0F, SharedFate.multiplier(1.0F, 1.0F), DELTA);
        assertEquals(1.0F, SharedFate.multiplier(0.5F, 0.5F), DELTA);
        assertEquals(1.0F, SharedFate.multiplier(0.0F, 0.0F), DELTA);
    }

    /** 差满格（一边满血、一边空血）到顶：1 + 0.70。 */
    @Test
    void theWidestGapHitsTheCap() {
        assertEquals(1.70F, SharedFate.multiplier(1.0F, 0.0F), DELTA);
        assertEquals(1.70F, SharedFate.multiplier(0.0F, 1.0F), DELTA);
    }

    /** 中间值线性：满血打半血是 1 + 0.70 × 0.5 = 1.35。 */
    @Test
    void theBonusIsLinearInTheGap() {
        assertEquals(1.35F, SharedFate.multiplier(1.0F, 0.5F), DELTA);
        assertEquals(1.07F, SharedFate.multiplier(0.9F, 0.8F), DELTA);
    }

    /**
     * 公式是对称的：谁打谁都是同一个倍率。
     *
     * <p>这一条是「两边都戴着不会变成平方」的地基 —— 符文是这一对关系的属性，不是每一方各乘一次。
     */
    @Test
    void theMultiplierIsSymmetric() {
        for (float attacker = 0.0F; attacker <= 1.0F; attacker += 0.1F) {
            for (float victim = 0.0F; victim <= 1.0F; victim += 0.1F) {
                assertEquals(SharedFate.multiplier(attacker, victim),
                             SharedFate.multiplier(victim, attacker), DELTA);
            }
        }
    }

    /** 血量比就是当前血量 / 上限，夹在 [0, 1]。 */
    @Test
    void healthRatioIsClamped() {
        assertEquals(1.0F, SharedFate.healthRatio(20.0F, 20.0F), DELTA);
        assertEquals(0.5F, SharedFate.healthRatio(10.0F, 20.0F), DELTA);
        assertEquals(0.0F, SharedFate.healthRatio(0.0F, 20.0F), DELTA);
        // 上限被别的模组临时削到 0：按满血算，不除出 NaN。
        assertEquals(1.0F, SharedFate.healthRatio(10.0F, 0.0F), DELTA);
        assertTrue(Float.isFinite(SharedFate.multiplier(SharedFate.healthRatio(10.0F, 0.0F),
                                                        SharedFate.healthRatio(1.0F, 20.0F))));
        // 血量被写到上限之上（别的模组的黄心之类）也不当成比满血更多。
        assertEquals(1.0F, SharedFate.healthRatio(30.0F, 20.0F), DELTA);
    }

    /** 倍率永远 ≥ 1：这枚符文只加伤，不减伤。 */
    @Test
    void theMultiplierNeverReducesDamage() {
        for (float attacker = 0.0F; attacker <= 1.0F; attacker += 0.05F) {
            for (float victim = 0.0F; victim <= 1.0F; victim += 0.05F) {
                assertTrue(SharedFate.multiplier(attacker, victim) >= 1.0F);
                assertTrue(SharedFate.multiplier(attacker, victim) <= 1.0F + SharedFate.BONUS_PER_RATIO);
            }
        }
    }
}
