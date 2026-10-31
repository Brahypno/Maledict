package org.brahypno.maledict.common.vitals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 延迟池：伤害按比例加地板释放，新伤先对冲待治疗量。 */
class DelayedVitalsPoolTest {

    private static final double DELTA = 1.0E-4D;

    @Test
    void aQueuedAmountIsReleasedByRatioPlusFloor() {
        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netDamage(10.0F);

        float expected = 10.0F * DelayedVitalsPool.DAMAGE_RELEASE_PER_TICK + DelayedVitalsPool.RELEASE_FLOOR;
        assertEquals(expected, pool.releaseDamage(), DELTA);
        assertEquals(10.0F - expected, pool.pendingDamage(), DELTA);
    }

    /** 反过来也一样：新挨的打先吃掉攒着的治疗。 */
    @Test
    void aNewWoundCancelsPendingHealFirst() {
        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netHeal(6.0F);

        assertEquals(4.0F, pool.netDamage(10.0F), DELTA);
        assertEquals(4.0F, pool.pendingDamage(), DELTA);
        assertEquals(0.0F, pool.pendingHeal(), DELTA);
    }

}
