package org.brahypno.maledict.common.effect;

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
        assertEquals(1080.0F, pulses * SacrificeSchedule.DAMAGE);
        assertEquals(pulses, SacrificeSchedule.targetPulses(initial, 0));
    }

    @Test
    void halfDurationHalvesThePulseIntervalAndAccelerationCrossesMorePulseBoundaries() {
        int initial = SacrificeSchedule.DURATION_TICKS / 2;
        assertEquals(1, SacrificeSchedule.targetPulses(initial, initial));
        assertEquals(1, SacrificeSchedule.targetPulses(initial, initial - 599));
        assertEquals(2, SacrificeSchedule.targetPulses(initial, initial - 600));
        assertEquals(3, SacrificeSchedule.targetPulses(initial, initial - 1200));
        assertEquals(7, SacrificeSchedule.targetPulses(initial, initial - 3 * 1200));
    }

    @Test
    void expirySettlesSkippedAndFractionalIntervalsWithoutExceedingTheBudget() {
        for (int initial : new int[]{1, 1000, 1234, 108001, SacrificeSchedule.DURATION_TICKS}) {
            int pulses = 0;
            for (int remaining = initial; remaining > 0; remaining -= 3 * 1200) {
                int target = SacrificeSchedule.targetPulses(initial, remaining);
                assertTrue(target >= pulses && target <= SacrificeSchedule.PULSE_COUNT);
                pulses = target;
            }
            assertEquals(180, SacrificeSchedule.targetPulses(initial, 0));
            assertEquals(180, SacrificeSchedule.targetPulses(initial, -100));
        }
    }

}
