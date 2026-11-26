package org.brahypno.maledict.common.ritual.wisdom;

import org.brahypno.maledict.common.ritual.wisdom.SacrificeSchedule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SacrificeScheduleTest {
    @Test
    void normalRiteStillPaysSixImmediatelyAndOncePerMinute() {
        int initial = SacrificeSchedule.DURATION_TICKS;
        int pulses = 0;
        for (int elapsed = 0; elapsed < initial; elapsed += SacrificeSchedule.INTERVAL_TICKS) {
            int target = SacrificeSchedule.targetPulses(initial, initial - elapsed);
            assertEquals(1, target - pulses);
            pulses = target;
        }
        assertEquals(180, pulses);
        assertEquals(1080.0F, pulses * SacrificeSchedule.DAMAGE);
        assertEquals(pulses, SacrificeSchedule.targetPulses(initial, 0));

        // 渡鸦取眼把这 180 次折成一笔：剩余次数 × 6，挨过一半就只剩 540，走完或超了都是 0。
        assertEquals(1080.0F, SacrificeSchedule.remainingDamage(0));
        assertEquals(540.0F, SacrificeSchedule.remainingDamage(90));
        assertEquals(6.0F, SacrificeSchedule.remainingDamage(179));
        assertEquals(0.0F, SacrificeSchedule.remainingDamage(180));
        assertEquals(0.0F, SacrificeSchedule.remainingDamage(231));
        assertEquals(1080.0F, SacrificeSchedule.remainingDamage(-5));
    }

    @Test
    void expiryIncludesSkippedAndFractionalIntervalsWithoutExceeding180Hits() {
        for (int initial : new int[]{1, 1000, 1234, 108001, SacrificeSchedule.DURATION_TICKS}) {
            int pulses = 0;
            for (int remaining = initial; remaining > 0; remaining -= 3 * 1200) {
                int target = SacrificeSchedule.targetPulses(initial, remaining);
                assertTrue(target >= pulses && target <= 180);
                pulses = target;
            }
            pulses += SacrificeSchedule.targetPulses(initial, 0) - pulses;
            assertEquals(180, pulses);
            assertEquals(180, SacrificeSchedule.targetPulses(initial, -100));
        }
    }

}
