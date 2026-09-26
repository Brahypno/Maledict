package org.brahypno.maledict.common.vitals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 只钉真正踩过的边界：染色段**末尾**落在奇数半心上是「只剩左半边」，**开头**落在奇数半心上是
 * 「只剩右半边」。首版就是把右半边画到了格子的左半边，进游戏才看出来。
 */
class DelayedVitalsHeartsTest {

    @Test
    void anOddEndLeavesOnlyTheLeftHalfOfTheLastHeart() {
        // 19 血（奇数）扣 3 点 → 盖住 16..19，第 9 颗心只剩左半边。
        DelayedVitalsHearts segment = DelayedVitalsHearts.pendingDamage(19, 3.0F);

        assertEquals(DelayedVitalsHearts.Coverage.FULL, segment.coverage(8));
        assertEquals(DelayedVitalsHearts.Coverage.LEFT_HALF, segment.coverage(9));
    }

    @Test
    void anOddStartLeavesOnlyTheRightHalfOfTheFirstHeart() {
        // 20 血扣 3 点 → 盖住 17..20，第 8 颗心只剩右半边。
        DelayedVitalsHearts segment = DelayedVitalsHearts.pendingDamage(20, 3.0F);

        assertEquals(DelayedVitalsHearts.Coverage.RIGHT_HALF, segment.coverage(8));
        assertEquals(DelayedVitalsHearts.Coverage.FULL, segment.coverage(9));
    }
}
