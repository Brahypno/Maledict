package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import static org.brahypno.maledict.common.entity.RavenGroundTravel.Action.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RavenGroundTravelTest {
    @Test
    void foodEightBlocksAwayIsReachedByFlightThenLandingAndHopping() {
        assertEquals(TAKE_OFF, RavenGroundTravel.next(false, 8 * 8));
        assertEquals(FLY, RavenGroundTravel.next(true, 8 * 8));
        assertEquals(FLY, RavenGroundTravel.next(true, 3 * 3));
        assertEquals(LAND, RavenGroundTravel.next(true, 2 * 2));
        assertEquals(HOP, RavenGroundTravel.next(false, 1 * 1));
    }

    @Test
    void fourBlockTakeoffAndTwoBlockLandingBoundariesAreDistinct() {
        assertEquals(HOP, RavenGroundTravel.next(false, 16.0D));
        assertEquals(TAKE_OFF, RavenGroundTravel.next(false, 16.01D));
        assertEquals(FLY, RavenGroundTravel.next(true, 4.01D));
        assertEquals(LAND, RavenGroundTravel.next(true, 4.0D));
        assertEquals(HOP, RavenGroundTravel.next(false, 4.01D));
    }
}
