package org.brahypno.maledict.rig;

import org.junit.jupiter.api.Test;
import org.brahypno.maledict.rig.VicissitudeRig.Action;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VicissitudeRigTest {
    /**
     * Mirrors the entity's {@code MELEE_REACH}; duplicated rather than imported so
     * this test stays free of Minecraft types.
     */
    private static final double DESIGN_MELEE_REACH = 5.0D;
    /**
     * Mirrors the entity's {@code MELEE_COMMIT_RANGE}.
     */
    private static final double COMMIT_RANGE = 5.75D;
    /**
     * A standing target's torso centre above its own feet: half of a player's 1.8 block hitbox.
     */
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

    /**
     * The whole point of the reworked curves: the windup channel has to be back at zero on the
     * last tick the renderer draws. It used to latch at 1 until the action ended, so every pose
     * coefficient the windup contributed was still applied on the final frame and the next frame
     * dropped all of them at once - the arms jumped most of the windup amplitude in one tick.
     */
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

    /**
     * The strike curves peak before they hold, so the held value must be the maximum of the two
     * or the impact frame is not the extreme of the swing.
     */
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

    /**
     * Every melee action deals damage across a window of ticks, the window lies inside the action,
     * and it contains the impact frame the pose peaks on. A window that missed the peak would mean
     * damage resolved on a pose the curve does not call extended.
     */
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

    /**
     * Melee reach is whatever the blade does, so the test measures the blade rather than restating
     * a constant: the edge has to stay inside the range the entity is willing to commit a swing
     * from, and it has to actually reach out in front rather than hanging beside the body.
     */
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

    /**
     * A melee swing must be able to reach a target standing on the same floor as the boss.
     *
     * <p>This is the measurement behind {@code MELEE_DIVE_ABOVE_TARGET}. The blade hangs off a
     * shoulder far above the entity origin, so if the pose keeps the edge above a standing target's
     * hitbox the boss can hover in range forever and never land a hit - which is exactly what
     * happened: at the phase-two cruise altitude the edge swept 2.0 to 3.7 blocks up while a
     * standing player ends at 1.8, so every swing passed over their head.
     *
     * <p>The assertion is deliberately on the <b>lowest</b> edge point across the hit window and
     * against a target's torso centre, not against a tuned altitude constant: it says the pose
     * itself reaches down into a body standing on the boss's own floor.
     */
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
            // A standing target's torso centre is half its height above its feet. The edge has to
            // come in under that line for the capsule radius to have anything to close.
            assertTrue(lowest <= STANDING_TARGET_CENTRE_Y,
                       action + " keeps its lowest edge point at y " + lowest
                       + ", above a standing target's centre at " + STANDING_TARGET_CENTRE_Y);
        }
    }

    /**
     * The fist used to pass within a tenth of a block of the crystal head on the way back from a
     * heavy attack, which is the clipping that was visible in game. The recovery guard is what
     * keeps it clear; this pins the clearance so the guard cannot be tuned away silently.
     */
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
                    // The halo turns on the world clock, not on the action clock, so its fragments
                    // are somewhere different on every frame. That is continuous motion, not a step.
                    continue;
                }
                double gap = distance(origin(last, joint), origin(rest, joint));
                assertTrue(gap <= 0.45D,
                           action + " leaves " + joint + " " + gap + " blocks from rest");
            }
        }
    }

    /**
     * Distance to the blade is clamped to the segment, not to the infinite line through it: a
     * point beyond the tip is as far away as the tip itself, which is what makes the melee test a
     * capsule test rather than a ray test.
     */
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
        // Constant idle clock: the idle layer is clock driven and continuous, so varying it with
        // the sample tick would mix two different animations into every measurement. Holding it
        // still measures the action's own contribution, which is what these assertions are about.
        VicissitudeRig.compute(pose, true, 1, action, tick, false, 0, 1000, 0, -1);
    }

    private static void assertAnchorsEqual(VicissitudeRig.Pose expected, VicissitudeRig.Pose actual) {
        for (Joint joint : Joint.values()) {
            assertEquals(VicissitudeRig.worldPoint(expected, joint, 0, 0, 0, 10, 20, 30, 73),
                    VicissitudeRig.worldPoint(actual, joint, 0, 0, 0, 10, 20, 30, 73), joint.name());
        }
    }
}
