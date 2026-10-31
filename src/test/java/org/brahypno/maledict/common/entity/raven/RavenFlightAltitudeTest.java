package org.brahypno.maledict.common.entity.raven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenFlightAltitudeTest {

    private static double reach(double attackerWidth) {
        return Math.sqrt(Math.pow(attackerWidth * 2.0D, 2.0D) + 1.0D);
    }

    @Test
    void followingCruisesHighWhileFarAndDropsToShoulderHeightUpClose() {
        assertEquals(74.0D, RavenFlightAltitude.followY(70.0D, 64.01D));
        assertEquals(74.0D, RavenFlightAltitude.followY(70.0D, 400.0D));
        assertEquals(71.0D, RavenFlightAltitude.followY(70.0D, 64.0D));
        assertEquals(71.0D, RavenFlightAltitude.followY(70.0D, 1.0D));
    }
}
