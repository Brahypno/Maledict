package org.brahypno.maledict.common.curio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 「半血生物」判定：出手时血量不高于上限的一半，对应 tooltip 的「攻击半血生物时触发魂息虚空」。 */
class HalfHealthTest {

    private static final float MAX_HEALTH = 100.0F;

    @Test
    void theLineIsHalfTheMaximum() {
        assertTrue(HalfHealth.isAtOrBelowHalf(50.0F, MAX_HEALTH));
        assertTrue(HalfHealth.isAtOrBelowHalf(40.0F, MAX_HEALTH));
        assertFalse(HalfHealth.isAtOrBelowHalf(50.1F, MAX_HEALTH));
        assertFalse(HalfHealth.isAtOrBelowHalf(100.0F, MAX_HEALTH));
        assertTrue(HalfHealth.isAtOrBelowHalf(5.0F, 10.0F));
        assertFalse(HalfHealth.isAtOrBelowHalf(6.0F, 10.0F));
        assertTrue(HalfHealth.isAtOrBelowHalf(250.0F, 500.0F));
        assertFalse(HalfHealth.isAtOrBelowHalf(251.0F, 500.0F));
    }

    @Test
    void aDeadCreatureDoesNotCount() {
        assertFalse(HalfHealth.isAtOrBelowHalf(0.0F, MAX_HEALTH));
        assertFalse(HalfHealth.isAtOrBelowHalf(-1.0F, MAX_HEALTH));
    }

    @Test
    void aTargetWithNoMaximumHealthDoesNotCount() {
        assertFalse(HalfHealth.isAtOrBelowHalf(0.0F, 0.0F));
        assertFalse(HalfHealth.isAtOrBelowHalf(10.0F, 0.0F));
    }
}
