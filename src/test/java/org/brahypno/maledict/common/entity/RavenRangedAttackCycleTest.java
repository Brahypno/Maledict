package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenRangedAttackCycleTest {
    @Test
    void continuousHoverReleasesAfterWindupAndEverySixtyTicks() {
        RavenRangedAttackCycle cycle = new RavenRangedAttackCycle();
        for (int tick = 0; tick < 130; tick++) {
            assertEquals(tick == 9 || tick == 69 || tick == 129, cycle.tick(true, tick), "tick " + tick);
        }
    }

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

    @Test
    void hoverRequiresAVisibleEnemyBetweenThreeAndTwelveBlocks() {
        assertFalse(RavenRangedAttackCycle.canHover(8.99D, true));
        assertTrue(RavenRangedAttackCycle.canHover(9.0D, true));
        assertTrue(RavenRangedAttackCycle.canHover(144.0D, true));
        assertFalse(RavenRangedAttackCycle.canHover(144.01D, true));
        assertFalse(RavenRangedAttackCycle.canHover(64.0D, false));
    }
}
