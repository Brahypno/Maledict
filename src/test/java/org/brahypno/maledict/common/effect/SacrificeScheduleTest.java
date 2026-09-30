package org.brahypno.maledict.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「献祭」的时间表：九天九夜 = 216000 tick，每 1200 tick 抽一次，共 180 次。
 *
 * <p>原版 {@code MobEffectInstance#tick} 先按当前剩余时长判定、再递减，所以第一 tick 就出手，
 * 最后一次落在剩余 1200，之后 1200 tick 只是把时长耗完——那一段的尽头才是发成就的
 * {@code MobEffectEvent.Expired}。
 */
class SacrificeScheduleTest {

    @Test
    void nineDaysIsNineTimesTwentyMinutes() {
        assertEquals(24000, SacrificeSchedule.TICKS_PER_DAY);
        assertEquals(216000, SacrificeSchedule.DURATION_TICKS);
        assertEquals(SacrificeSchedule.TICKS_PER_DAY * SacrificeSchedule.DAYS,
                     SacrificeSchedule.DURATION_TICKS);
    }

    @Test
    void oneHitPerMinuteAcrossTheWholeNineDays() {
        assertEquals(9 * 20, SacrificeSchedule.pulses());
    }

    /** 扣除顺序：先判定再递减，于是施加效果的当 tick 就是第一下。 */
    @Test
    void theTickTheEffectLandsIsTheFirstHit() {
        assertTrue(SacrificeSchedule.isPulse(SacrificeSchedule.DURATION_TICKS));
    }

    @Test
    void theLastHitLandsOneMinuteBeforeTheEnd() {
        int hits = 0;
        int lastHitAt = 0;
        for (int duration = SacrificeSchedule.DURATION_TICKS; duration > 0; duration--) {
            if (SacrificeSchedule.isPulse(duration)) {
                hits++;
                lastHitAt = duration;
            }
        }

        assertEquals(SacrificeSchedule.pulses(), hits);
        assertEquals(SacrificeSchedule.INTERVAL_TICKS, lastHitAt);
    }

    /** 熬满九天九夜的代价：180 下 × 6 点 = 1080 点，也就是 54 条命。 */
    @Test
    void theWholeRiteCostsNineDaysOfBlood() {
        assertEquals(1080.0F, SacrificeSchedule.DAMAGE * SacrificeSchedule.pulses(), 0.0001F);
    }

    @Test
    void nothingPulsesOffBeatOrAfterTheEffectIsGone() {
        assertFalse(SacrificeSchedule.isPulse(SacrificeSchedule.INTERVAL_TICKS - 1));
        assertFalse(SacrificeSchedule.isPulse(SacrificeSchedule.INTERVAL_TICKS + 1));
        assertFalse(SacrificeSchedule.isPulse(0));
        assertFalse(SacrificeSchedule.isPulse(-1));
    }
}
