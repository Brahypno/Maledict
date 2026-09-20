package org.brahypno.maledict.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link BlessedRegeneration} 的份数表。
 *
 * <p>「一份」就是原版自然回血每 tick 实际回掉的血量：饱食度回血是
 * {@code min(饱食度, 6) / 6}，饥饿值回血恒为 1 点。等级 I 额外补一份（也就是翻倍），
 * 等级 II 额外补两份，依此类推。
 */
class BlessedRegenerationTest {

    private static final float EPSILON = 0.0001F;

    /** 等级 I：饥饿值回血 1 点 -> 再补 1 点，两倍。 */
    @Test
    void levelOneDoublesTheHungerShare() {
        assertEquals(1.0F, BlessedRegeneration.extraHeal(1.0F, 0), EPSILON);
    }

    /** 等级 II：三倍。 */
    @Test
    void levelTwoTriplesIt() {
        assertEquals(2.0F, BlessedRegeneration.extraHeal(1.0F, 1), EPSILON);
    }

    /** 饱食度回血是分数：0.5 点的那一份，等级 I 再补 0.5 点。 */
    @Test
    void fractionalSaturationSharesScaleTheSameWay() {
        assertEquals(0.5F, BlessedRegeneration.extraHeal(0.5F, 0), EPSILON);
        assertEquals(1.5F, BlessedRegeneration.extraHeal(0.5F, 2), EPSILON);
    }

    /** 饱食度不足 6 时原版只回 {@code 饱食度 / 6}，放大后依然是同一个比例。 */
    @Test
    void aFullSaturationTickScalesToo() {
        assertEquals(1.0F, BlessedRegeneration.extraHeal(1.0F, 0), EPSILON);
        assertEquals(0.3333F, BlessedRegeneration.extraHeal(0.3333F, 0), EPSILON);
    }

    /** 这一 tick 没回血（比如力竭扣了饱食度那一下）就不补。 */
    @Test
    void noHealingMeansNoExtra() {
        assertEquals(0.0F, BlessedRegeneration.extraHeal(0.0F, 0), EPSILON);
    }

    /** 掉血那一 tick 的差值是负数，同样不补。 */
    @Test
    void aWoundedTickGivesNothing() {
        assertEquals(0.0F, BlessedRegeneration.extraHeal(-3.0F, 4), EPSILON);
    }

    /** 等级非法时不生效，免得把负的份数补回去。 */
    @Test
    void anInvalidAmplifierGivesNothing() {
        assertEquals(0.0F, BlessedRegeneration.extraHeal(1.0F, -1), EPSILON);
    }
}
