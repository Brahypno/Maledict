package org.brahypno.maledict.common.rune;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 无忧符文的倍率：血量比例差多少、这一击重多少，以及「方向对称」这条性质。 */
class SharedFateTest {

    private static final double DELTA = 1.0E-4D;

    /** 中间值线性：满血打半血是 1 + 0.70 × 0.5 = 1.35。 */
    @Test
    void theBonusIsLinearInTheGap() {
        assertEquals(1.35F, SharedFate.multiplier(1.0F, 0.5F), DELTA);
        assertEquals(1.07F, SharedFate.multiplier(0.9F, 0.8F), DELTA);
    }

}
