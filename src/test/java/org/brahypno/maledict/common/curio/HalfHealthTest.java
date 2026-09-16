package org.brahypno.maledict.common.curio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link HalfHealth} 的判定表。
 *
 * <p>这些数值就是启蒙之年那条 tooltip 的字面意思——「攻击半血生物时触发魂息虚空」，
 * 与监视者项链的「攻击满血生物」相对。
 */
class HalfHealthTest {

    private static final float MAX_HEALTH = 100.0F;

    @Test
    void aWoundedCreatureCounts() {
        assertTrue(HalfHealth.isAtOrBelowHalf(40.0F, MAX_HEALTH));
    }

    @Test
    void landingExactlyOnTheLineCounts() {
        assertTrue(HalfHealth.isAtOrBelowHalf(50.0F, MAX_HEALTH));
    }

    @Test
    void aHealthyCreatureDoesNotCount() {
        assertFalse(HalfHealth.isAtOrBelowHalf(50.1F, MAX_HEALTH));
        assertFalse(HalfHealth.isAtOrBelowHalf(100.0F, MAX_HEALTH));
    }

    /** 半血怪每一下都算：判定只看出手时的血量，与这一下打掉多少无关。 */
    @Test
    void aWoundedCreatureCountsAgainAndAgain() {
        assertTrue(HalfHealth.isAtOrBelowHalf(50.0F, MAX_HEALTH));
        assertTrue(HalfHealth.isAtOrBelowHalf(49.0F, MAX_HEALTH));
        assertTrue(HalfHealth.isAtOrBelowHalf(1.0F, MAX_HEALTH));
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

    /** 血量上限不同，半血线跟着走：10 点上限的怪在 5 点及以下才算。 */
    @Test
    void theLineFollowsTheMaximumHealth() {
        assertTrue(HalfHealth.isAtOrBelowHalf(5.0F, 10.0F));
        assertFalse(HalfHealth.isAtOrBelowHalf(6.0F, 10.0F));
        assertTrue(HalfHealth.isAtOrBelowHalf(250.0F, 500.0F));
        assertFalse(HalfHealth.isAtOrBelowHalf(251.0F, 500.0F));
    }
}
