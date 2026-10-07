package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenFlightAltitudeTest {
    @Test
    void hoverPointClearsTheEnemyFeetAndNeverComesDown() {
        // A grounded bird still lifts off first, even with the enemy far below.
        assertEquals(65.25D, RavenFlightAltitude.hoverY(64.0D, true, 50.0D));
        // Otherwise the combat clearance over the enemy's feet decides.
        assertEquals(66.0D, RavenFlightAltitude.hoverY(64.0D, false, 62.0D));
        assertEquals(66.0D, RavenFlightAltitude.hoverY(64.0D, true, 62.0D));
        // A bird already higher than it needs to be is not pulled back down.
        assertEquals(70.0D, RavenFlightAltitude.hoverY(70.0D, false, 62.0D));
    }

    @Test
    void clearanceBeatsEveryMeleeReachThatHuntsARaven() {
        // Vanilla melee reach is sqrt((attackerWidth * 2)^2 + targetWidth) to the target's feet.
        assertEquals(1.56D, reach(0.6D), 0.01D);
        assertEquals(2.97D, reach(1.4D), 0.01D);
        assertEquals(4.02D, reach(1.95D), 0.01D);
        assertEquals(4.0D, RavenFlightAltitude.COMBAT_CLEARANCE);
        assertTrue(RavenFlightAltitude.COMBAT_CLEARANCE > reach(1.4D));
    }

    private static double reach(double attackerWidth) {
        return Math.sqrt(Math.pow(attackerWidth * 2.0D, 2.0D) + 1.0D);
    }

    @Test
    void idleFlightRaisesTheWalkingDestinationByFourToSixBlocks() {
        assertEquals(4.0D, RavenFlightAltitude.idleFlightLift(0));
        assertEquals(5.0D, RavenFlightAltitude.idleFlightLift(1));
        assertEquals(6.0D, RavenFlightAltitude.idleFlightLift(2));
    }

    @Test
    void followingCruisesHighWhileFarAndDropsToShoulderHeightUpClose() {
        assertEquals(74.0D, RavenFlightAltitude.followY(70.0D, 64.01D));
        assertEquals(74.0D, RavenFlightAltitude.followY(70.0D, 400.0D));
        assertEquals(71.0D, RavenFlightAltitude.followY(70.0D, 64.0D));
        assertEquals(71.0D, RavenFlightAltitude.followY(70.0D, 1.0D));
    }
}
