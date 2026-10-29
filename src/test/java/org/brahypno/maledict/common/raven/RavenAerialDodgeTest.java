package org.brahypno.maledict.common.raven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenAerialDodgeTest {

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

}
