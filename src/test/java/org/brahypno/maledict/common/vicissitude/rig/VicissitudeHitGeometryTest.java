package org.brahypno.maledict.common.vicissitude.rig;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.brahypno.maledict.common.vicissitude.rig.VicissitudeRig.*;

class VicissitudeHitGeometryTest {
    private final List<SegmentVolume> parts = List.of(
            new SegmentVolume(Segment.BODY, new Box(-1, 0, -1, 1, 3, 1)),
            new SegmentVolume(Segment.HEAD, new Box(-1, 3, -1, 1, 4, 1)));

    @Test void closestIntersectionWinsEvenWhenTheListIsReversed() {
        var farHead = new SegmentVolume(Segment.HEAD, new Box(-1, 1, 3, 1, 2, 4));
        assertEquals(Segment.BODY, firstSegmentAlongRay(List.of(farHead, parts.get(0)),
                new V3(0, 1.5F, -5), new V3(0, 1.5F, 5)));
    }

}
