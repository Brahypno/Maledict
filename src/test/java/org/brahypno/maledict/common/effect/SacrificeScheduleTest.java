package org.brahypno.maledict.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(1080.0F, pulses * SacrificeSchedule.DAMAGE);
        assertEquals(pulses, SacrificeSchedule.targetPulses(initial, 0));
    }

}
