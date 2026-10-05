package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenAerialDodgeTest {
    @Test
    void aHitRaisesTheCeilingAndArrivalEndsTheDodge() {
        RavenAerialDodge dodge = new RavenAerialDodge();
        assertFalse(dodge.active());
        dodge.onHit(64.0D, 62.0D);
        assertTrue(dodge.active());
        assertEquals(66.0D, dodge.targetY());
        assertFalse(dodge.reached(64.0D));
        dodge.tick(65.0D);
        assertTrue(dodge.active());
        dodge.tick(66.0D);
        assertFalse(dodge.active());
    }

    @Test
    void aDodgePinnedUnderACeilingTimesOut() {
        RavenAerialDodge dodge = new RavenAerialDodge();
        dodge.onHit(64.0D, 62.0D);
        for (int tick = 0; tick < RavenAerialDodge.DURATION_TICKS - 1; tick++) {
            dodge.tick(64.0D);
            assertTrue(dodge.active(), "tick " + tick);
        }
        dodge.tick(64.0D);
        assertFalse(dodge.active());
    }

    @Test
    void hitsDuringADodgeOnlyExtendTheTimerAndNeverLowerTheCeiling() {
        RavenAerialDodge dodge = new RavenAerialDodge();
        dodge.onHit(64.0D, 62.0D);
        dodge.tick(65.0D);
        dodge.onHit(65.0D, 40.0D);
        assertEquals(66.0D, dodge.targetY());
        for (int tick = 0; tick < RavenAerialDodge.DURATION_TICKS - 1; tick++) {
            dodge.tick(65.0D);
        }
        assertTrue(dodge.active());
    }

    @Test
    void finishingClearsTheDodgeAndTheNextHitStartsFresh() {
        RavenAerialDodge dodge = new RavenAerialDodge();
        dodge.onHit(64.0D, 62.0D);
        dodge.finish();
        assertFalse(dodge.active());
        dodge.onHit(70.0D, 60.0D);
        assertEquals(71.0D, dodge.targetY());
    }
}
