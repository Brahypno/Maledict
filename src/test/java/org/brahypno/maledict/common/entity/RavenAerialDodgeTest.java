package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenAerialDodgeTest {
    @Test
    void aLevelHitMovesSidewaysAndBackThenArrivalEndsTheDodge() {
        RavenAerialDodge dodge = new RavenAerialDodge();
        assertFalse(dodge.active());
        dodge.onHit(0, 64, 0, 1, 0, false, true);
        assertTrue(dodge.active());
        assertEquals(64, dodge.targetY());
        assertTrue(dodge.targetX() > 3);
        assertEquals(2, dodge.targetZ(), 1.0E-6);
        assertFalse(dodge.reached(0, 64, 0));
        dodge.tick(1, 64, 0);
        assertTrue(dodge.active());
        dodge.tick(dodge.targetX(), dodge.targetY(), dodge.targetZ());
        assertFalse(dodge.active());
    }

    @Test
    void onlyAKnownSourceBelowTheBirdAddsHeight() {
        assertFalse(RavenAerialDodge.fromBelow(64, 64));
        assertFalse(RavenAerialDodge.fromBelow(64, 70));
        assertFalse(RavenAerialDodge.fromBelow(64, 63.8));
        assertTrue(RavenAerialDodge.fromBelow(64, 62));
        RavenAerialDodge dodge = new RavenAerialDodge();
        dodge.onHit(0, 64, 0, 1, 0, true, false);
        assertEquals(65.25, dodge.targetY());
        assertTrue(dodge.targetX() > 3);
        assertEquals(-2, dodge.targetZ(), 1.0E-6);
    }

    @Test
    void aDodgePinnedAgainstAnObstacleTimesOut() {
        RavenAerialDodge dodge = new RavenAerialDodge();
        dodge.onHit(0, 64, 0, 1, 0, true, true);
        for (int tick = 0; tick < RavenAerialDodge.DURATION_TICKS - 1; tick++) {
            dodge.tick(0, 64, 0);
            assertTrue(dodge.active(), "tick " + tick);
        }
        dodge.tick(0, 64, 0);
        assertFalse(dodge.active());
    }

    @Test
    void repeatedHitsKeepTheDestinationAndStillEndAtTheOriginalDeadline() {
        RavenAerialDodge dodge = new RavenAerialDodge();
        dodge.onHit(0, 64, 0, 1, 0, true, true);
        double x = dodge.targetX(), z = dodge.targetZ();
        for (int tick = 0; tick < RavenAerialDodge.DURATION_TICKS; tick++) {
            assertFalse(dodge.onHit(1, 65, 0, -1, 1, true, false));
            assertEquals(65.25, dodge.targetY());
            assertEquals(x, dodge.targetX());
            assertEquals(z, dodge.targetZ());
            dodge.tick(0, 64, 0);
        }
        assertFalse(dodge.active());
    }

    @Test
    void finishingClearsTheDodgeAndTheNextHitStartsFresh() {
        RavenAerialDodge dodge = new RavenAerialDodge();
        dodge.onHit(0, 64, 0, 1, 0, true, true);
        dodge.finish();
        assertFalse(dodge.active());
        dodge.onHit(1, 70, 0, 1, 0, false, true);
        assertEquals(70, dodge.targetY());
    }

    @Test
    void anUnknownHorizontalDirectionStillProducesAFiniteSideBackDodge() {
        RavenAerialDodge dodge = new RavenAerialDodge();
        dodge.onHit(0, 64, 0, 0, 0, false, true);
        assertTrue(Double.isFinite(dodge.targetX()));
        assertTrue(Double.isFinite(dodge.targetZ()));
        assertEquals(RavenAerialDodge.DISTANCE, Math.hypot(dodge.targetX(), dodge.targetZ()), 1.0E-6);
    }
}
