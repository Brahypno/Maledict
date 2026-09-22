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
    @Test void feathersFallAndFinishBeforePhaseTwo() {
        float previous = -1;
        for (int tick=0; tick<=24; tick++) {
            assertTrue(VicissitudeFeatherShed.drop(tick) >= previous);
            previous = VicissitudeFeatherShed.drop(tick);
            assertTrue(VicissitudeFeatherShed.scale(tick)>=0);
        }
        assertEquals(0, VicissitudeFeatherShed.scale(24));
        assertEquals(0, VicissitudeFeatherShed.scale(VicissitudeFeatherShed.elapsed(60, 28)));
    }
    @Test void phaseTwoDropsFeatherOnlyCollisionGroups() {
        assertTrue(VicissitudeMeshGeometry.WINGS.stream().anyMatch(b -> b.joint().name().contains("FEATHER_")));
        assertTrue(VicissitudeMeshGeometry.BONE_WINGS.stream().noneMatch(b -> b.joint().name().contains("FEATHER_")));
        var pose=VicissitudeRig.newPose();
        VicissitudeRig.compute(pose,true,1,VicissitudeRig.Action.NONE,0,false,0,0,0,-1);
        assertEquals(2+VicissitudeMeshGeometry.BONE_WINGS.size(),VicissitudeRig.segmentVolumes(pose,0,0,0,0).size());
    }
}
