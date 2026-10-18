package org.brahypno.maledict.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 「献祭」的时间表：九天九夜 = 216000 tick，每 1200 tick 抽一次，共 180 次。
 *
 * <p>原版 {@code MobEffectInstance#tick} 先按当前剩余时长判定、再递减，所以第一 tick 就出手，
 * 最后一次落在剩余 1200，之后 1200 tick 只是把时长耗完——那一段的尽头才是发成就的
 * {@code MobEffectEvent.Expired}。
 */
class SacrificeScheduleTest {

    @Test
    void nineDaysPulseImmediatelyThenEveryMinuteUntilTheFinalMinute() {
        assertEquals(216000, SacrificeSchedule.DURATION_TICKS);
        int hits = 0;
        int lastHitAt = 0;
        for (int remaining = SacrificeSchedule.DURATION_TICKS; remaining > 0; remaining--) {
            boolean expectedPulse = remaining % 1200 == 0;
            assertEquals(expectedPulse, SacrificeSchedule.isPulse(remaining));
            if (SacrificeSchedule.isPulse(remaining)) {
                hits++;
                lastHitAt = remaining;
            }
        }
        assertEquals(180, hits);
        assertEquals(hits, SacrificeSchedule.pulses());
        assertEquals(1200, lastHitAt);
        assertEquals(1080.0F, SacrificeSchedule.DAMAGE * hits, 0.0001F);
    }

    @Test
    void nothingPulsesOffBeatOrAfterTheEffectIsGone() {
        assertFalse(SacrificeSchedule.isPulse(SacrificeSchedule.INTERVAL_TICKS - 1));
        assertFalse(SacrificeSchedule.isPulse(SacrificeSchedule.INTERVAL_TICKS + 1));
        assertFalse(SacrificeSchedule.isPulse(0));
        assertFalse(SacrificeSchedule.isPulse(-1));
    }
}
