package org.brahypno.maledict.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SacrificeScheduleTest {
    @Test
    void normalRiteStillPaysSixImmediatelyAndOncePerMinute() {
        int initial = SacrificeSchedule.DURATION_TICKS;
        double paid = 0.0D;
        for (int elapsed = 0; elapsed < initial; elapsed += SacrificeSchedule.INTERVAL_TICKS) {
            double target = SacrificeSchedule.targetDamage(initial, initial - elapsed);
            assertEquals(6.0D, target - paid, 0.000001D);
            paid = target;
        }
        assertEquals(1080.0D, paid);
        assertEquals(paid, SacrificeSchedule.targetDamage(initial, 0));
    }

    @Test
    void halfInitialDurationDoublesTheDamage() {
        int initial = SacrificeSchedule.DURATION_TICKS / 2;
        assertEquals(12.0D, SacrificeSchedule.targetDamage(initial, initial));
        assertEquals(24.0D, SacrificeSchedule.targetDamage(initial, initial - 1200));
        assertEquals(1080.0D, SacrificeSchedule.targetDamage(initial, 0));
    }

    @Test
    void tenServerSecondsConsumingThirtyEffectSecondsTriplesTheDamageRate() {
        int initial = SacrificeSchedule.DURATION_TICKS;
        double first = SacrificeSchedule.targetDamage(initial, initial);
        double normal = SacrificeSchedule.targetDamage(initial, initial - 10 * 20) - first;
        double accelerated = SacrificeSchedule.targetDamage(initial, initial - 30 * 20) - first;
        assertEquals(1.0D, normal);
        assertEquals(3.0D, accelerated);
        assertEquals(normal * 3.0D, accelerated);
    }

    @Test
    void shortInitialDurationAndAccelerationMultiply() {
        int initial = SacrificeSchedule.DURATION_TICKS / 2;
        double first = SacrificeSchedule.targetDamage(initial, initial);
        assertEquals(36.0D, SacrificeSchedule.targetDamage(initial, initial - 3 * 1200) - first);
    }

    @Test
    void speedChangesOnlyChargeTheProgressActuallyConsumed() {
        int initial = SacrificeSchedule.DURATION_TICKS;
        // First ten seconds consume thirty, then the remaining fifty consume fifty.
        assertEquals(8.0D, SacrificeSchedule.targetDamage(initial, initial - 80 * 20)
                - SacrificeSchedule.targetDamage(initial, initial));
    }

    @Test
    void expirySettlesSkippedAndFractionalIntervalsWithoutExceedingTheBudget() {
        for (int initial : new int[]{1, 1000, 1234, 108001, SacrificeSchedule.DURATION_TICKS}) {
            double paid = 0.0D;
            for (int remaining = initial; remaining > 0; remaining -= 3 * 1200) {
                double target = SacrificeSchedule.targetDamage(initial, remaining);
                assertTrue(target >= paid && target <= 1080.0D);
                paid = target;
            }
            paid += SacrificeSchedule.targetDamage(initial, 0) - paid;
            assertEquals(1080.0D, paid);
            assertEquals(1080.0D, SacrificeSchedule.targetDamage(initial, -100));
        }
    }

    @Test
    void completionRequiresTheWholeDamageBudget() {
        assertFalse(SacrificeSchedule.isComplete(0.0D));
        assertFalse(SacrificeSchedule.isComplete(1079.0D));
        assertTrue(SacrificeSchedule.isComplete(1080.0D));
    }
}
