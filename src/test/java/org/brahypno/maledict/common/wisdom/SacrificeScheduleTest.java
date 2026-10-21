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
    void halfInitialDurationHalvesTheInterval() {
        int initial = SacrificeSchedule.DURATION_TICKS / 2;
        assertEquals(1, SacrificeSchedule.targetPulses(initial, initial));
        assertEquals(1, SacrificeSchedule.targetPulses(initial, initial - 599));
        assertEquals(2, SacrificeSchedule.targetPulses(initial, initial - 600));
        assertEquals(3, SacrificeSchedule.targetPulses(initial, initial - 1200));
        assertEquals(180, SacrificeSchedule.targetPulses(initial, 0));
    }

    @Test
    void tenServerSecondsConsumingThirtyEffectSecondsMakesTheNextHitLandAtTwentySeconds() {
        int initial = SacrificeSchedule.DURATION_TICKS;
        assertEquals(1, SacrificeSchedule.targetPulses(initial, initial - 30 * 20));
        assertEquals(1, SacrificeSchedule.targetPulses(initial, initial - 399 * 3));
        assertEquals(2, SacrificeSchedule.targetPulses(initial, initial - 400 * 3));
        assertEquals(4, SacrificeSchedule.targetPulses(initial, initial - 1200 * 3));
    }

    @Test
    void shortInitialDurationAndAccelerationMultiply() {
        int initial = SacrificeSchedule.DURATION_TICKS / 2;
        assertEquals(1, SacrificeSchedule.targetPulses(initial, initial - 199 * 3));
        assertEquals(2, SacrificeSchedule.targetPulses(initial, initial - 200 * 3));
        assertEquals(7, SacrificeSchedule.targetPulses(initial, initial - 1200 * 3));
    }

    @Test
    void speedChangesOnlyAdvanceByTheEffectTimeConsumed() {
        int initial = SacrificeSchedule.DURATION_TICKS;
        // First ten seconds consume thirty, then the remaining fifty consume fifty.
        assertEquals(2, SacrificeSchedule.targetPulses(initial, initial - 80 * 20));
        assertEquals(2, SacrificeSchedule.targetPulses(initial, initial - 120 * 20 + 1));
        assertEquals(3, SacrificeSchedule.targetPulses(initial, initial - 120 * 20));
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

    @Test
    void standardDurationRemainsNineDaysAndEveryHitRemainsSix() {
        assertEquals(9 * 24000, SacrificeSchedule.DURATION_TICKS);
        assertEquals(6.0F, SacrificeSchedule.DAMAGE);
    }
}
