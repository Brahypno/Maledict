package org.brahypno.maledict.rig;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VicissitudeFeatherShedTest {
    @Test void releaseIsContinuousAndStaggered() {
        assertEquals(0, VicissitudeFeatherShed.elapsed(12, 15));
        assertEquals(0, VicissitudeFeatherShed.drop(0));
        assertEquals(1, VicissitudeFeatherShed.scale(0));
        assertTrue(VicissitudeFeatherShed.elapsed(20, 12) > VicissitudeFeatherShed.elapsed(20, 19));
    }
    /** 落羽在 24 tick 内落完：最后一 tick 还有余量，到点归零，不会拖进二阶段。 */
    @Test void theFallFinishesWithinTwentyFourTicks() {
        assertTrue(VicissitudeFeatherShed.scale(23.0F) > 0.0F);
        assertEquals(0.0F, VicissitudeFeatherShed.scale(24.0F));
        assertEquals(0.0F, VicissitudeFeatherShed.scale(VicissitudeFeatherShed.elapsed(60, 28)));
    }
    @Test void phaseTwoDropsFeatherOnlyCollisionGroups() {
        assertEquals(8, VicissitudeMeshGeometry.WINGS.stream()
                .filter(b -> b.joint().name().contains("FEATHER_")).count());
        assertTrue(VicissitudeMeshGeometry.BONE_WINGS.stream().noneMatch(b -> b.joint().name().contains("FEATHER_")));
        var pose=VicissitudeRig.newPose();
        VicissitudeRig.compute(pose,true,1,VicissitudeRig.Action.NONE,0,false,0,0,0,-1);
        assertEquals(2+VicissitudeMeshGeometry.BONE_WINGS.size(),VicissitudeRig.segmentVolumes(pose,0,0,0,0).size());
    }
}
