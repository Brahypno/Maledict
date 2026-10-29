package org.brahypno.maledict.common.vicissitude.rig;

import org.junit.jupiter.api.Test;
import org.brahypno.maledict.common.vicissitude.rig.VicissitudeRig.Action;
import org.brahypno.maledict.common.vicissitude.rig.VicissitudeRigData.Joint;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VicissitudeRigTest {

    @Test
    void poseAtReleaseDoesNotDependOnRenderFrequencyOrPreviousAction() {
        for (Action action : Action.values()) {
            VicissitudeRig.Pose direct = VicissitudeRig.newPose();
            sample(direct, action, action.releaseTick());
            for (int framesPerTick : new int[]{1, 3, 12}) {
                VicissitudeRig.Pose rendered = VicissitudeRig.newPose();
                sample(rendered, Action.HEAVY_ATTACK, 28);
                for (int frame = 0; frame < action.releaseTick() * framesPerTick; frame++) {
                    sample(rendered, action, frame / (float) framesPerTick);
                }
                sample(rendered, action, action.releaseTick());
                assertAnchorsEqual(direct, rendered);
            }
        }
    }

    @Test
    void poseCopyPreservesSolvedAnchorsWithoutAnotherSolve() {
        VicissitudeRig.Pose source = VicissitudeRig.newPose();
        VicissitudeRig.Pose copy = VicissitudeRig.newPose();
        sample(source, Action.SCYTHE_THROW, 16.5F);
        copy.copyFrom(source);
        assertAnchorsEqual(source, copy);
        sample(source, Action.NONE, 0);
        VicissitudeRig.Pose expected = VicissitudeRig.newPose();
        sample(expected, Action.SCYTHE_THROW, 16.5F);
        assertAnchorsEqual(expected, copy);
    }

    private static void sample(VicissitudeRig.Pose pose, Action action, float tick) {
        // Constant idle clock: the idle layer is clock driven, so a varying clock would mix two
        // animations into every measurement; these assertions are about the action's contribution.
        VicissitudeRig.compute(pose, true, 1, action, tick, false, 0, 1000, 0, -1);
    }

    private static void assertAnchorsEqual(VicissitudeRig.Pose expected, VicissitudeRig.Pose actual) {
        for (Joint joint : Joint.values()) {
            assertEquals(VicissitudeRig.worldPoint(expected, joint, 0, 0, 0, 10, 20, 30, 73),
                    VicissitudeRig.worldPoint(actual, joint, 0, 0, 0, 10, 20, 30, 73), joint.name());
        }
    }
}
