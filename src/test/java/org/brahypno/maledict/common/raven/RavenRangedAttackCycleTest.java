package org.brahypno.maledict.common.raven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenRangedAttackCycleTest {

    @Test
    void losingSightOrLeavingHoverRestartsTheFullWindup() {
        RavenRangedAttackCycle cycle = new RavenRangedAttackCycle();
        for (int tick = 0; tick < 9; tick++) {
            assertFalse(cycle.tick(true, tick));
        }
        assertFalse(cycle.tick(false, 9));
        for (int tick = 10; tick < 19; tick++) {
            assertFalse(cycle.tick(true, tick));
        }
        assertTrue(cycle.tick(true, 19));
        cycle.cancelCharge();
        assertFalse(cycle.tick(true, 20));
    }

}
