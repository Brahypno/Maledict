package org.brahypno.maledict.common.vitals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 延迟池：先对冲再排队、两个池子各自的释放曲线（比例 + 地板），以及「池子 ≥ 血量必死」那条性质的来源。 */
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

    /** 回血刻意比掉血慢：同一份量，掉血放得比回血快一倍以上。 */
    @Test
    void healingIsReleasedSlowerThanDamage() {
        assertTrue(DelayedVitalsPool.HEAL_RELEASE_PER_TICK < DelayedVitalsPool.DAMAGE_RELEASE_PER_TICK);

        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netHeal(10.0F);
        assertEquals(10.0F * DelayedVitalsPool.HEAL_RELEASE_PER_TICK + DelayedVitalsPool.RELEASE_FLOOR,
                     pool.releaseHeal(), DELTA);
    }

    /**
     * 地板就是为这一条加的：纯指数要 276 tick 才把 10 点放完（前期特别快、后期特别慢），
     * 加地板后应当在 150 tick 内结清。
     */
    @Test
    void theFloorEndsTheTail() {
        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netDamage(10.0F);

        int ticks = 0;
        while (!pool.isEmpty() && ticks < 1000) {
            pool.releaseDamage();
            ticks++;
        }

        assertTrue(ticks < 150, "10 点的池子应当在地板加持下 150 tick 内结清，实际 " + ticks + " tick");
    }

    /** 一剂治疗先把还没落地的痛抹掉，剩下的才留作待治疗。 */
    @Test
    void aHealCancelsPendingDamageFirst() {
        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netDamage(10.0F);

        assertEquals(0.0F, pool.netHeal(4.0F), DELTA);
        assertEquals(6.0F, pool.pendingDamage(), DELTA);
        assertEquals(0.0F, pool.pendingHeal(), DELTA);
    }

    @Test
    void aBiggerHealLeavesTheRestInTheHealPool() {
        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netDamage(3.0F);

        assertEquals(5.0F, pool.netHeal(8.0F), DELTA);
        assertEquals(0.0F, pool.pendingDamage(), DELTA);
        assertEquals(5.0F, pool.pendingHeal(), DELTA);
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

    /** 累计释放量收敛到初始池子大小：所以池子 ≥ 当前血量就是必死，不存在「永远差一点」。 */
    @Test
    void theTotalReleasedConvergesOnWhatWasQueued() {
        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netDamage(10.0F);

        float released = 0.0F;
        for (int tick = 0; tick < 5000; tick++) {
            released += pool.releaseDamage();
        }

        assertEquals(10.0F, released, 0.02D);
        assertTrue(pool.isEmpty());
    }

    @Test
    void theTotalHealedConvergesOnWhatWasQueued() {
        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netHeal(10.0F);

        float released = 0.0F;
        for (int tick = 0; tick < 5000; tick++) {
            released += pool.releaseHeal();
        }

        assertEquals(10.0F, released, 0.02D);
        assertTrue(pool.isEmpty());
    }

    /** 稳态池子长度 = (入队速率 × (1 - k) - 地板) / k，也就是「不会无限增长」。 */
    @Test
    void aSteadyIncomingRateSettlesOnABoundedPool() {
        float perTick = 1.0F;
        float rate = DelayedVitalsPool.DAMAGE_RELEASE_PER_TICK;
        float expected = (perTick * (1.0F - rate) - DelayedVitalsPool.RELEASE_FLOOR) / rate;

        DelayedVitalsPool pool = new DelayedVitalsPool();
        for (int tick = 0; tick < 2000; tick++) {
            pool.netDamage(perTick);
            pool.releaseDamage();
        }

        assertEquals(expected, pool.pendingDamage(), 0.5D);
    }

    @Test
    void aTinyRemainderIsTreatedAsSettled() {
        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netDamage(DelayedVitalsPool.EMPTY_THRESHOLD / 2.0F);

        pool.releaseDamage();

        assertTrue(pool.isEmpty());
        assertEquals(0.0F, pool.pendingDamage());
    }

    @Test
    void clearAndSetOverwriteBothPools() {
        DelayedVitalsPool pool = new DelayedVitalsPool();
        pool.netDamage(3.0F);
        pool.netHeal(5.0F);

        pool.set(-1.0F, 2.0F);
        assertEquals(0.0F, pool.pendingDamage(), DELTA);
        assertEquals(2.0F, pool.pendingHeal(), DELTA);

        pool.clear();
        assertTrue(pool.isEmpty());
    }
}
