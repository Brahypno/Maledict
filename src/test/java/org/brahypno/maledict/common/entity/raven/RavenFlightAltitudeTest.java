package org.brahypno.maledict.common.entity.raven;

import org.brahypno.maledict.common.entity.raven.RavenFlightAltitude;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenFlightAltitudeTest {

    @Test
    void followingCruisesHighWhileFarAndDropsToShoulderHeightUpClose() {
        assertEquals(74.0D, RavenFlightAltitude.followY(70.0D, 64.01D));
        assertEquals(74.0D, RavenFlightAltitude.followY(70.0D, 400.0D));
        assertEquals(71.0D, RavenFlightAltitude.followY(70.0D, 64.0D));
        assertEquals(71.0D, RavenFlightAltitude.followY(70.0D, 1.0D));
    }
}
