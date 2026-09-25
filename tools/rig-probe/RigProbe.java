import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRig.Action;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;

import java.util.Locale;

/** Acceptance readout for the rig: reach, smoothness and clearance, plus the dash settle. */
public final class RigProbe {
    public static void main(String[] args) {
        System.out.printf(Locale.ROOT, "%-18s %7s %7s %8s %8s %8s %10s%n",
                "action", "maxEdge", "impactF", "stepMax", "stepEnd", "minBody", "where");
        VicissitudeRig.Pose rest = pose(Action.NONE, 0);
        for (Action action : Action.values()) {
            if (action == Action.NONE) {
                continue;
            }
            double maxEdge = 0.0D;
            double impactForward = 0.0D;
            if (action.isMelee()) {
                for (int tick = action.hitWindow()[0]; tick <= action.hitWindow()[1]; tick++) {
                    VicissitudeRig.BladeSegment blade =
                            VicissitudeRig.bladeSegment(pose(action, tick), action, tick);
                    for (VicissitudeRig.V3 point : new VicissitudeRig.V3[]{blade.grip(), blade.tip()}) {
                        maxEdge = Math.max(maxEdge,
                                Math.sqrt(point.x() * point.x() + point.z() * point.z()));
                    }
                }
                impactForward = VicissitudeRig
                        .bladeSegment(pose(action, action.releaseTick()), action, action.releaseTick())
                        .tip().z();
            }

            double stepMax = 0.0D;
            Joint stepJoint = Joint.ROOT;
            int stepTick = -1;
            for (int tick = 1; tick < action.duration(); tick++) {
                for (Joint joint : Joint.values()) {
                    double d = dist(origin(pose(action, tick), joint),
                            origin(pose(action, tick - 1), joint));
                    if (d > stepMax) {
                        stepMax = d;
                        stepJoint = joint;
                        stepTick = tick;
                    }
                }
            }

            VicissitudeRig.Pose last = pose(action, action.duration() - 1);
            double stepEnd = 0.0D;
            Joint endJoint = Joint.ROOT;
            for (Joint joint : Joint.values()) {
                double d = dist(origin(last, joint), origin(rest, joint));
                if (d > stepEnd) {
                    stepEnd = d;
                    endJoint = joint;
                }
            }

            double minBody = Double.MAX_VALUE;
            String where = "";
            for (int tick = 0; tick < action.duration(); tick++) {
                VicissitudeRig.Pose pose = pose(action, tick);
                for (Joint hand : new Joint[]{Joint.HAND_LEFT, Joint.HAND_RIGHT}) {
                    for (Joint body : new Joint[]{Joint.HEAD_CORE, Joint.HEAD_SHELL_TOP,
                            Joint.HEAD_SHELL_LEFT, Joint.HEAD_SHELL_RIGHT,
                            Joint.CHEST_SHELL_LEFT, Joint.CHEST_SHELL_RIGHT,
                            Joint.CHEST_SHELL_BACK}) {
                        double d = dist(origin(pose, hand), origin(pose, body));
                        if (d < minBody) {
                            minBody = d;
                            where = hand.name().replace("HAND_", "") + "/"
                                    + body.name().replace("_SHELL", "").replace("CHEST_", "")
                                            .replace("HEAD_", "").replace("_CORE", "")
                                    + "@" + tick;
                        }
                    }
                }
            }
            System.out.printf(Locale.ROOT, "%-18s %7.2f %7.2f %8.3f %8.3f %8.2f %10s%n",
                    action, maxEdge, impactForward, stepMax, stepEnd, minBody, where);
            if (stepMax > 0.55D) {
                System.out.printf(Locale.ROOT,
                        "    ! largest per-tick move %.3f on %s at tick %d%n",
                        stepMax, stepJoint, stepTick);
            }
            if (stepEnd > 0.30D) {
                System.out.printf(Locale.ROOT, "    ! hand-over step %.3f on %s%n", stepEnd, endJoint);
            }
        }

        System.out.println();
        System.out.println("=== melee edge height: where the blade crosses a target on the boss's floor ===");
        System.out.printf(Locale.ROOT, "%-18s %9s %9s %9s %9s%n",
                "action", "edgeGripY", "edgeTipY", "declared", "lowest");
        for (Action action : Action.values()) {
            if (!action.isMelee()) {
                continue;
            }
            double lowestGrip = Double.MAX_VALUE;
            double lowestTip = Double.MAX_VALUE;
            double lowest = Double.MAX_VALUE;
            int worstTick = -1;
            for (int tick = action.hitWindow()[0]; tick <= action.hitWindow()[1]; tick++) {
                VicissitudeRig.BladeSegment blade =
                        VicissitudeRig.bladeSegment(pose(action, tick), action, tick);
                if (blade.grip().y() < lowestGrip) {
                    lowestGrip = blade.grip().y();
                }
                if (blade.tip().y() < lowestTip) {
                    lowestTip = blade.tip().y();
                }
                lowest = Math.min(lowest, Math.min(blade.grip().y(), blade.tip().y()));
                if (lowest == blade.grip().y() || lowest == blade.tip().y()) {
                    worstTick = tick;
                }
            }
            System.out.printf(Locale.ROOT, "%-18s %9.2f %9.2f %9.2f %9.2f  (tick %d)%n",
                    action, lowestGrip, lowestTip, action.bladeHeightAboveFeet(), lowest,
                    worstTick);
        }

        // Simulate the melee test against a real victim volume, using the very stand-off the entity
        // computes: the boss stands at (target distance - the swing's forward reach) so the edge
        // crosses the victim rather than falling short in front of it or sweeping past it. The rig
        // probe has no entity, so the boss is placed by hand - this is the only way to see, offline,
        // whether the swing lands before running the game.
        System.out.println();
        System.out.println("=== simulated melee: boss stands at (distance - bladeForwardReach) ===");
        System.out.println("victim 1.8 tall, 0.6 wide, standing at the given distance straight ahead");
        System.out.printf(Locale.ROOT, "%-18s %7s %9s %8s %9s %9s %6s%n",
                "action", "standoff", "gapPoint", "gapBox", "limitPoint", "limitBox", "hit?");
        double victimHeight = 1.8D;
        double victimWidth = 0.6D;
        double victimCentre = victimHeight * 0.5D;
        double capsuleRadius = 0.35D;
        double pointLimit = victimWidth * 0.5D + capsuleRadius;
        double boxLimit = capsuleRadius;
        for (Action action : Action.values()) {
            if (!action.isMelee()) {
                continue;
            }
            double reach = action.bladeForwardReach();
            double bossY = victimCentre - action.bladeHeightAboveFeet();
            // The tightest geometry: inside the commit range, so the stand-off is what decides.
            double distance = reach;
            double standoff = Math.max(0.0D, distance - reach);
            double victimZ = distance;
            double worstPoint = Double.MAX_VALUE;
            double worstBox = Double.MAX_VALUE;
            for (int tick = action.hitWindow()[0]; tick <= action.hitWindow()[1]; tick++) {
                VicissitudeRig.BladeSegment world =
                        lift(VicissitudeRig.bladeSegment(pose(action, tick), action, tick), bossY);
                worstPoint = Math.min(worstPoint,
                        VicissitudeRig.distanceToBlade(0.0D, victimCentre, victimZ, world,
                                                       0.0D, bossY, 0.0D, 0.0F));
                VicissitudeRig.Box box = VicissitudeRig.Box.of(
                        -victimWidth * 0.5D, 0.0D, victimZ - victimWidth * 0.5D,
                        victimWidth * 0.5D, victimHeight, victimZ + victimWidth * 0.5D);
                worstBox = Math.min(worstBox,
                        VicissitudeRig.distanceToBladeBox(box, world, 0.0D, bossY, 0.0D, 0.0F));
            }
            System.out.printf(Locale.ROOT,
                    "%-18s %7.2f %9.2f %8.2f %9.2f %9.2f %6s%n",
                    action, standoff, worstPoint, worstBox, pointLimit, boxLimit,
                    worstPoint <= pointLimit ? "yes" : (worstBox <= boxLimit ? "boxOnly" : "NO"));
        }

        System.out.println();
        System.out.println("=== full geometry at the impact frame (world, boss feet at bossY) ===");
        for (Action action : Action.values()) {
            if (!action.isMelee()) {
                continue;
            }
            double bossY = victimCentre - action.bladeHeightAboveFeet();
            int impact = action.releaseTick();
            VicissitudeRig.BladeSegment world =
                    lift(VicissitudeRig.bladeSegment(pose(action, impact), action, impact), bossY);
            System.out.printf(Locale.ROOT,
                    "%-17s bossY %5.2f | grip(x %5.2f y %5.2f z %5.2f) "
                    + "tip(x %5.2f y %5.2f z %5.2f) | victim centre (0, %5.2f, 3.0)%n",
                    action, bossY, world.grip().x(), world.grip().y(), world.grip().z(),
                    world.tip().x(), world.tip().y(), world.tip().z(), victimCentre);
        }

        // Where does the victim have to be relative to the boss for the blade to cross its volume?
        // This band is what the approach has to sit on: closer and the blade sweeps down in front of
        // the victim or past it, further and it falls short behind the tip. Clearances are printed in
        // hundredths of a block; the melee test's tolerance is BLADE_HIT_RADIUS = 0.75.
        System.out.println();
        System.out.println("=== victim offset ahead of the boss -> gap to its volume ===");
        System.out.println("gap in 1/100 block: gapPoint / gapBox, box limit 75");
        double[] offsets = {1.5D, 2.0D, 2.5D, 3.0D, 3.5D, 4.0D, 4.5D, 5.0D};
        System.out.printf(Locale.ROOT, "%-18s", "action");
        for (double o : offsets) {
            System.out.printf(Locale.ROOT, "%9.1f", o);
        }
        System.out.println();
        for (Action action : Action.values()) {
            if (!action.isMelee()) {
                continue;
            }
            double bossY = victimCentre - action.bladeHeightAboveFeet();
            int impact = action.releaseTick();
            VicissitudeRig.BladeSegment world =
                    lift(VicissitudeRig.bladeSegment(pose(action, impact), action, impact), bossY);
            System.out.printf(Locale.ROOT, "%-18s", action);
            for (double o : offsets) {
                VicissitudeRig.Box box = VicissitudeRig.Box.of(
                        -victimWidth * 0.5D, 0.0D, o - victimWidth * 0.5D,
                        victimWidth * 0.5D, victimHeight, o + victimWidth * 0.5D);
                double gapBox = VicissitudeRig.distanceToBladeBox(box, world, 0.0D, bossY, 0.0D, 0.0F);
                double gapPoint = VicissitudeRig.distanceToBlade(0.0D, victimCentre, o, world,
                                                                 0.0D, bossY, 0.0D, 0.0F);
                System.out.printf(Locale.ROOT, "%9s",
                        String.format(Locale.ROOT, "%.0f/%.0f", gapPoint * 100, gapBox * 100));
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("=== blade edge height per tick across each hit window ===");
        for (Action action : Action.values()) {
            if (!action.isMelee()) {
                continue;
            }
            System.out.println("-- " + action + " window " + action.hitWindow()[0]
                    + ".." + action.hitWindow()[1] + " release " + action.releaseTick());
            for (int tick = action.hitWindow()[0]; tick <= action.hitWindow()[1]; tick++) {
                VicissitudeRig.BladeSegment blade =
                        VicissitudeRig.bladeSegment(pose(action, tick), action, tick);
                System.out.printf(Locale.ROOT,
                        "   t%-3d gripY %6.2f tipY %6.2f  forward %5.2f  edgeLen %5.2f%n",
                        tick, blade.grip().y(), blade.tip().y(), blade.tip().z(),
                        blade.length());
            }
        }

        System.out.println();
        System.out.println("=== DASH off-arm settle, last frames ===");
        for (int tick = 38; tick < 50; tick++) {
            VicissitudeRig.Pose pose = pose(Action.DASH, tick);
            System.out.printf(Locale.ROOT,
                    "t%-3d handZ %7.3f  offsetFromRest %6.3f  stepFromPrev %6.3f  armLeftX %7.2f%n",
                    tick, origin(pose, Joint.HAND_LEFT).z(),
                    dist(origin(pose, Joint.HAND_LEFT), origin(rest, Joint.HAND_LEFT)),
                    dist(origin(pose, Joint.HAND_LEFT),
                            origin(pose(Action.DASH, tick - 1), Joint.HAND_LEFT)),
                    pose.rotX(Joint.ARM_LEFT));
        }
    }

    private static VicissitudeRig.Pose pose(Action action, float tick) {
        VicissitudeRig.Pose pose = VicissitudeRig.newPose();
        VicissitudeRig.compute(pose, true, 1.0F, action, tick, false, 0.0F, 1000.0F, 0.0F, -1.0F);
        return pose;
    }

    private static VicissitudeRig.V3 origin(VicissitudeRig.Pose pose, Joint joint) {
        return VicissitudeRig.toEntityLocal(VicissitudeRig.transform(pose, joint, 0, 0, 0));
    }

    /**
     * Lifts an entity-local blade segment to world space for a boss standing at {@code bossY} with
     * no yaw. The entity-local numbers are already blocks, so this is a plain offset.
     */
    private static VicissitudeRig.BladeSegment lift(VicissitudeRig.BladeSegment local,
                                                    double bossY) {
        return new VicissitudeRig.BladeSegment(
                new VicissitudeRig.V3(local.grip().x(), (float) bossY + local.grip().y(),
                                      local.grip().z()),
                new VicissitudeRig.V3(local.tip().x(), (float) bossY + local.tip().y(),
                                      local.tip().z()));
    }

    private static double dist(VicissitudeRig.V3 a, VicissitudeRig.V3 b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
