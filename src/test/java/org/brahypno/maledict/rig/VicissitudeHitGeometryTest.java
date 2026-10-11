package org.brahypno.maledict.rig;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.brahypno.maledict.rig.VicissitudeRig.*;

class VicissitudeHitGeometryTest {
    private final List<SegmentVolume> parts = List.of(
            new SegmentVolume(Segment.BODY, new Box(-1, 0, -1, 1, 3, 1)),
            new SegmentVolume(Segment.HEAD, new Box(-1, 3, -1, 1, 4, 1)));

    @Test void aimingAtTheHeadDoesNotUseTheAttackersFeet() {
        assertEquals(Segment.HEAD, firstSegmentAlongRay(parts, new V3(0, 3.5F, -5), new V3(0, 3.5F, 0)));
        assertEquals(Segment.BODY, firstSegmentAlongRay(parts, new V3(0, 1, -5), new V3(0, 1, 0)));
    }

    @Test void rayMustReachAndActuallyIntersectThePart() {
        assertEquals(Segment.BODY, firstSegmentAlongRay(parts, new V3(0, 3.5F, -5), new V3(0, 3.5F, -2)));
        assertEquals(Segment.BODY, firstSegmentAlongRay(parts, new V3(2, 3.5F, -5), new V3(2, 3.5F, 0)));
        assertEquals(Segment.HEAD, firstSegmentAlongRay(parts, new V3(0, 3.5F, 0), new V3(0, 3.5F, 0)));
    }

    @Test void closestIntersectionWinsEvenWhenTheListIsReversed() {
        var farHead = new SegmentVolume(Segment.HEAD, new Box(-1, 1, 3, 1, 2, 4));
        assertEquals(Segment.BODY, firstSegmentAlongRay(List.of(farHead, parts.get(0)),
                new V3(0, 1.5F, -5), new V3(0, 1.5F, 5)));
    }

    @Test void swingHeightAllowsNormalTargetsButRejectsDifferentFloors() {
        var blade = new BladeSegment(new V3(0, 0.5F, 0), new V3(0, 1, 4));
        assertTrue(blade.overlapsHeight(10, 10, 11.8), "Standing target at blade height");
        assertTrue(blade.overlapsHeight(10, 10, 10.6), "Short or crouching target");
        assertTrue(blade.overlapsHeight(10, 11.5, 13.3), "Jumping into the sweep");
        assertFalse(blade.overlapsHeight(10, 14, 15.8), "Upper floor");
        assertFalse(blade.overlapsHeight(10, 5, 6.8), "Lower floor");
    }
}
