package org.brahypno.maledict.rig;

import org.junit.jupiter.api.Test;
import org.brahypno.maledict.rig.VicissitudeRig.Action;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VicissitudeRigTest {
    /** Mirrors the entity's {@code MELEE_REACH}; duplicated to keep this test free of Minecraft types. */
    private static final double DESIGN_MELEE_REACH = 5.0D;
    private static final double COMMIT_RANGE = 5.75D;
    /** A standing target's torso centre above its own feet: half of a player's 1.8 block hitbox. */
    private static final double STANDING_TARGET_CENTRE_Y = 0.9D;

    @Test
    void releaseAndDelayedReleaseUseTheSameCurve() {
        for (Action action : Action.values()) {
            if (action == Action.NONE) continue;
            assertEquals(1.0F, VicissitudeRig.sampleAction(action, action.releaseTick(), 0).swing());
            for (int delay = 0; delay <= 8; delay++) {
                assertEquals(VicissitudeRig.sampleAction(action, action.releaseTick() + 0.5F, 0),
                        VicissitudeRig.sampleAction(action, action.releaseTick() + 0.5F + delay, delay));
            }
        }
        assertEquals(new VicissitudeRig.ActionSample(0, 0),
                VicissitudeRig.sampleAction(Action.NONE, 10, 0));
        assertEquals(new VicissitudeRig.ActionSample(0, 0),
                VicissitudeRig.sampleAction(Action.SLASH_HORIZONTAL, -1, 0));
    }

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

    /** The windup channel has to be back at zero on the last tick the renderer draws. */
    @Test
    void windupReleasesCompletelyBeforeTheActionEnds() {
        for (Action action : Action.values()) {
            if (action == Action.NONE) {
                continue;
            }
            VicissitudeRig.ActionSample atEnd =
                    VicissitudeRig.sampleAction(action, action.duration() - 1, 0);
            assertTrue(atEnd.wind() <= 0.02F, action + " wind at last tick");
            assertTrue(atEnd.windLag() <= 0.02F, action + " lagged wind at last tick");
        }
    }

    @Test
    void windupPeaksExactlyOnTheImpactFrame() {
        for (Action action : Action.values()) {
            if (action == Action.NONE) {
                continue;
            }
            assertEquals(1.0F, VicissitudeRig.sampleAction(action, action.releaseTick(), 0).wind(),
                         1.0E-4F, action + " wind at impact");
        }
    }

    /** The held value must be the maximum of hold and peak, or the impact frame is not the extreme. */
    @Test
    void strikeCurvePeaksOnTheImpactFrame() {
        for (Action action : Action.values()) {
            if (action == Action.NONE) {
                continue;
            }
            float atImpact =
                    VicissitudeRig.sampleAction(action, action.releaseTick(), 0).swing();
            for (float tick = 0; tick <= action.duration(); tick += 0.25F) {
                assertTrue(VicissitudeRig.sampleAction(action, tick, 0).swing()
                                   <= atImpact + 1.0E-4F,
                           action + " swing at tick " + tick + " exceeds the impact frame");
            }
        }
    }

    /** Every melee hit window lies inside its action and contains the impact frame the pose peaks on. */
    @Test
    void meleeHitWindowsBracketTheImpactFrame() {
        for (Action action : Action.values()) {
            int[] window = action.hitWindow();
            if (!action.isMelee()) {
                assertNull(window, action + " must have no hit window");
                continue;
            }
            assertNotNull(window, action + " must have a hit window");
            assertTrue(window[0] <= action.releaseTick(), action + " window opens after the impact");
            assertTrue(window[1] >= action.releaseTick(), action + " window closes before the impact");
            assertTrue(window[0] >= 0, action + " window starts before the action");
            assertTrue(window[1] < action.duration(), action + " window outlives the action");
        }
    }

    /** Measured from the blade itself: the edge reaches out in front, and no further than the commit range. */
    @Test
    void bladeEdgeReachesWhatTheCommitRangePromises() {
        for (Action action : Action.values()) {
            if (!action.isMelee()) {
                assertNull(blade(action, 0), action + " must have no blade");
                continue;
            }
            double furthest = 0.0D;
            for (int tick = action.hitWindow()[0]; tick <= action.hitWindow()[1]; tick++) {
                VicissitudeRig.BladeSegment blade = blade(action, tick);
                for (VicissitudeRig.V3 point : new VicissitudeRig.V3[]{blade.grip(), blade.tip()}) {
                    furthest = Math.max(furthest,
                                        Math.sqrt(point.x() * point.x() + point.z() * point.z()));
                }
            }
            assertTrue(furthest >= DESIGN_MELEE_REACH - 1.0D,
                       action + " blade only reaches " + furthest);
            assertTrue(furthest <= COMMIT_RANGE,
                       action + " blade reaches " + furthest + ", past the commit range");
        }
    }

    /** Measured on the lowest edge point across the hit window, against a standing target's torso centre. */
    @Test
    void aMeleeSwingCanReachATargetStandingOnTheSameFloor() {
        for (Action action : Action.values()) {
            if (!action.isMelee()) {
                continue;
            }
            double lowest = Double.MAX_VALUE;
            for (int tick = action.hitWindow()[0]; tick <= action.hitWindow()[1]; tick++) {
                VicissitudeRig.BladeSegment blade = blade(action, tick);
                lowest = Math.min(lowest, Math.min(blade.grip().y(), blade.tip().y()));
            }
            assertTrue(lowest <= STANDING_TARGET_CENTRE_Y,
                       action + " keeps its lowest edge point at y " + lowest
                       + ", above a standing target's centre at " + STANDING_TARGET_CENTRE_Y);
        }
    }

    /** 每个动作的每一 tick，两只手都离头部与胸壳至少 0.75 格。 */
    @Test
    void theSwingingHandNeverEntersTheBody() {
        for (Action action : Action.values()) {
            if (action == Action.NONE) {
                continue;
            }
            for (int tick = 0; tick < action.duration(); tick++) {
                VicissitudeRig.Pose pose = VicissitudeRig.newPose();
                sample(pose, action, tick);
                for (Joint hand : new Joint[]{Joint.HAND_LEFT, Joint.HAND_RIGHT}) {
                    VicissitudeRig.V3 fist = origin(pose, hand);
                    for (Joint body : new Joint[]{Joint.HEAD_CORE, Joint.CHEST_SHELL_LEFT,
                            Joint.CHEST_SHELL_RIGHT, Joint.CHEST_SHELL_BACK}) {
                        double gap = distance(fist, origin(pose, body));
                        assertTrue(gap >= 0.75D,
                                   action + " tick " + tick + ": " + hand + " is " + gap
                                   + " blocks from " + body);
                    }
                }
            }
        }
    }

    /** The hand-over between two actions must not be a visible jump. */
    @Test
    void actionsEndCloseToTheRestPose() {
        VicissitudeRig.Pose rest = VicissitudeRig.newPose();
        sample(rest, Action.NONE, 0);
        for (Action action : Action.values()) {
            if (action == Action.NONE) {
                continue;
            }
            VicissitudeRig.Pose last = VicissitudeRig.newPose();
            sample(last, action, action.duration() - 1);
            for (Joint joint : Joint.values()) {
                if (joint == Joint.HALO_ROOT || joint.name().startsWith("HALO_FRAGMENT")) {
                    // The halo turns on the world clock, so its fragments differ every frame: motion, not a step.
                    continue;
                }
                double gap = distance(origin(last, joint), origin(rest, joint));
                assertTrue(gap <= 0.45D,
                           action + " leaves " + joint + " " + gap + " blocks from rest");
            }
        }
    }

    /** Distance to the blade is clamped to the segment, which is what makes the melee test a capsule test. */
    @Test
    void bladeDistanceClampsToTheSegmentEnds() {
        VicissitudeRig.BladeSegment blade = new VicissitudeRig.BladeSegment(
                new VicissitudeRig.V3(0.0F, 0.0F, 0.0F), new VicissitudeRig.V3(0.0F, 0.0F, 2.0F));
        assertEquals(0.5D, VicissitudeRig.distanceToBlade(0.5D, 0.0D, 1.0D, blade, 0, 0, 0, 0),
                     1.0E-6D);
        assertEquals(1.0D, VicissitudeRig.distanceToBlade(0.0D, 0.0D, 3.0D, blade, 0, 0, 0, 0),
                     1.0E-6D);
        assertEquals(2.0D, VicissitudeRig.distanceToBlade(0.0D, 0.0D, -2.0D, blade, 0, 0, 0, 0),
                     1.0E-6D);
    }

    private static VicissitudeRig.BladeSegment blade(Action action, float tick) {
        VicissitudeRig.Pose pose = VicissitudeRig.newPose();
        sample(pose, action, tick);
        return VicissitudeRig.bladeSegment(pose, action, tick);
    }

    private static VicissitudeRig.V3 origin(VicissitudeRig.Pose pose, Joint joint) {
        return VicissitudeRig.toEntityLocal(VicissitudeRig.transform(pose, joint, 0, 0, 0));
    }

    private static double distance(VicissitudeRig.V3 a, VicissitudeRig.V3 b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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
