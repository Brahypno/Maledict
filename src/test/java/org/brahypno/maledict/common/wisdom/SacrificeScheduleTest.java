package org.brahypno.maledict.common.wisdom;

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
