package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Chooses attacks that fit the target's position and pursues between casts.
 */
final class DemiurgeCombatGoal extends Goal {
    private static final int RECOVERY_PAUSE = 8;
    private static final int REPOSITION_INTERVAL = 5;
    private static final int PILLAR_RECHECK_INTERVAL = 10;
    private static final double HOLD_DISTANCE = 12;
    /**
     * The charge only travels about eighteen blocks inside its 20-43 damage window.
     */
    private static final double CHARGE_RANGE = 17;
    /**
     * The twenty-tick windup must be able to turn the body onto this cone.
     */
    private static final double CHARGE_CONE_DEGREES = 70;
    private static final double LASER_CONE_DEGREES = 105;
    private static final DemiurgeAttack[] ATTACKS = DemiurgeAttack.values();

    private final DemiurgeStatueEntity statue;
    private final int[] cooldowns = new int[ATTACKS.length];
    private final int[] casts = new int[ATTACKS.length];
    private int globalCooldown;
    private int repositionAt;
    private int pillarRecheckAt;
    private DemiurgeAttack lastAttack = DemiurgeAttack.NONE;

    DemiurgeCombatGoal(DemiurgeStatueEntity statue) {
        this.statue = statue;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return statue.canFight() && (statue.attack() != DemiurgeAttack.NONE
                                     || statue.getTarget() != null && statue.canAttack(statue.getTarget()));
    }

    @Override
    public boolean canContinueToUse() {return canUse();}

    @Override
    public boolean requiresUpdateEveryTick() {return true;}

    @Override
    public void stop() {
        statue.getNavigation().stop();
        if (statue.attack() != DemiurgeAttack.NONE)
            statue.finishAttack();
        globalCooldown = 0;
    }

    @Override
    public void tick() {
        tickCooldowns();
        var target = statue.getTarget();
        if (statue.attack() != DemiurgeAttack.NONE) {
            if (statue.attackAge(0) >= statue.attack().duration) {
                // Remember externally started casts as well as casts selected by this goal.
                lastAttack = statue.attack();
                statue.finishAttack();
                globalCooldown = RECOVERY_PAUSE;
            } else {
                if (target != null && statue.canAttack(target))
                    statue.aimAttack(target);
                return;
            }
        }
        if (target == null || !statue.canAttack(target))
            return;
        if (globalCooldown == 0 && tryAttack(target))
            return;
        moveBetweenAttacks(target);
    }

    private void tickCooldowns() {
        if (globalCooldown > 0)
            globalCooldown--;
        for (int index = 0; index < cooldowns.length; index++) {
            if (cooldowns[index] > 0)
                cooldowns[index]--;
        }
    }

    private boolean tryAttack(LivingEntity target) {
        double[] weight = new double[ATTACKS.length];
        Vec3 local = statue.attackLocalPosition(target.position());
        double distance = local.length();
        double bearing = Math.toDegrees(Math.atan2(local.x, local.z));
        boolean close = distance < 12;

        // Only charge when the target is actually reachable and inside the
        // windup's turn cone. The old selection could start a charge from the
        // side or from 40 blocks away, where the twenty-tick aim and the
        // damage window could never meet the player.
        if (distance <= CHARGE_RANGE && Math.abs(bearing) <= CHARGE_CONE_DEGREES) {
            weight[DemiurgeAttack.CHARGE.ordinal()] = close ? 1.7 : 1.2;
        }
        if (distance <= DemiurgeAttack.LASER_RANGE && Math.abs(bearing) <= LASER_CONE_DEGREES) {
            weight[DemiurgeAttack.LASER.ordinal()] = close ? .8 : 1.6;
        }
        if (statue.canStartRain(target))
            weight[DemiurgeAttack.ROAR.ordinal()] = 2.4;
        if (cooldowns[DemiurgeAttack.SWEEP.ordinal()] == 0) {
            if (lastAttack != DemiurgeAttack.SWEEP && begin(DemiurgeAttack.SWEEP, target))
                return true;
            if (lastAttack == DemiurgeAttack.SWEEP)
                weight[DemiurgeAttack.SWEEP.ordinal()] = 1;
        }
        if (distance <= 36 && statue.tickCount >= pillarRecheckAt) {
            if (casts[DemiurgeAttack.FALSE_CREATION.ordinal()] == 0) {
                if (begin(DemiurgeAttack.FALSE_CREATION, target))
                    return true;
            } else {
                weight[DemiurgeAttack.FALSE_CREATION.ordinal()] = 4;
            }
        }

        int cut = statue.bodyCut();
        if (cut >= 4) {
            // The body starts failing: trade direct physical pressure for
            // material control while the short sweep remains a close guard.
            weight[DemiurgeAttack.FALSE_CREATION.ordinal()] *= 1.5;
        }
        if (cut >= 8) {
            weight[DemiurgeAttack.FALSE_CREATION.ordinal()] *= 1.4;
            weight[DemiurgeAttack.CHARGE.ordinal()] *= .65;
        }
        if (cut >= 11) {
            weight[DemiurgeAttack.FALSE_CREATION.ordinal()] *= 1.25;
            weight[DemiurgeAttack.ROAR.ordinal()] *= 1.1;
        }

        for (var attack : ATTACKS) {
            if (attack == DemiurgeAttack.NONE)
                continue;
            if (cooldowns[attack.ordinal()] > 0)
                weight[attack.ordinal()] = 0;
                // A small first-use bias keeps all five moves observable without
                // turning the selection back into a fixed rotation.
            else if (casts[attack.ordinal()] == 0)
                weight[attack.ordinal()] *= 1.6;
        }
        double repeatWeight = weight[lastAttack.ordinal()];
        weight[lastAttack.ordinal()] = 0;
        for (DemiurgeAttack choice; (choice = roll(weight)) != DemiurgeAttack.NONE; ) {
            if (begin(choice, target))
                return true;
            // The terrain or the swept pose can reject a cast. Try the remaining moves this tick.
            weight[choice.ordinal()] = 0;
        }
        return repeatWeight > 0 && begin(lastAttack, target);
    }

    private boolean begin(DemiurgeAttack choice, LivingEntity target) {
        if (!statue.beginAttack(choice, target)) {
            if (choice == DemiurgeAttack.FALSE_CREATION)
                pillarRecheckAt = statue.tickCount + PILLAR_RECHECK_INTERVAL;
            return false;
        }
        casts[choice.ordinal()]++;
        cooldowns[choice.ordinal()] = choice.duration + switch (choice) {
            case CHARGE, ROAR, SWEEP -> 22;
            case LASER -> 46;
            case FALSE_CREATION -> 64;
            case NONE -> 0;
        };
        return true;
    }

    private DemiurgeAttack roll(double[] weight) {
        double total = 0;
        for (double value : weight)
            total += value;
        if (total <= 0)
            return DemiurgeAttack.NONE;
        double roll = statue.getRandom().nextDouble() * total;
        for (var attack : ATTACKS) {
            if (weight[attack.ordinal()] <= 0)
                continue;
            roll -= weight[attack.ordinal()];
            if (roll <= 0)
                return attack;
        }
        return DemiurgeAttack.NONE;
    }

    private void navigateTo(LivingEntity target, double speed) {
        // A forward intermediate waypoint is the cheapest way around the body,
        // but for a target outside the turn cone it makes the boss walk away
        // first. Pivot in place until the target is in the cone, then close in.
        if (!canSteerDirectly(target)) {
            statue.getNavigation().stop();
            statue.turnArc(target);
            return;
        }
        Vec3 desired = target.position();
        Vec3 reachable = DemiurgePatrolSteering.reachableWaypoint(
                statue.position(), statue.getYRot(), desired, statue.patrolBody());
        if (reachable == null) {
            if (statue.getNavigation().isDone())
                statue.getNavigation().moveTo(target, speed);
            return;
        }
        statue.getNavigation().moveTo(reachable.x, reachable.y, reachable.z, speed);
    }

    private double bearingTo(LivingEntity target) {
        Vec3 local = statue.attackLocalPosition(target.position());
        return Math.toDegrees(Math.atan2(local.x, local.z));
    }

    /**
     * Matches the direct-turn cone used by {@code clearDestination}.
     */
    private boolean canSteerDirectly(LivingEntity target) {
        Vec3 local = statue.attackLocalPosition(target.position());
        double horizontal = Math.hypot(local.x, local.z);
        double maximum = Math.toDegrees(Math.atan2(horizontal, 2 * DemiurgePatrolSteering.TURN_RADIUS));
        return Math.abs(bearingTo(target)) <= maximum;
    }

    private void moveBetweenAttacks(LivingEntity target) {
        // Facing is updated every tick, not on the five-tick waypoint throttle:
        // a 4-degree step every five ticks would take eleven seconds to turn
        // around, which reads as the boss refusing to engage.
        if (!canSteerDirectly(target)) {
            statue.getNavigation().stop();
            statue.turnArc(target);
            return;
        }
        if (statue.tickCount < repositionAt)
            return;
        repositionAt = statue.tickCount + REPOSITION_INTERVAL;
        double distance = statue.distanceToSqr(target);
        if (distance > HOLD_DISTANCE * HOLD_DISTANCE) {
            navigateTo(target, distance > DemiurgeAttack.LASER_RANGE * DemiurgeAttack.LASER_RANGE ? 1.25 : 1.0);
            return;
        }
        if (!statue.getNavigation().isDone())
            return;
        // Circle the target at roughly the current distance instead of stepping
        // forward past it. The old forward-biased point made a close strafe look
        // like a disengage.
        Vec3 toTarget = target.position().subtract(statue.position());
        Vec3 tangent = new Vec3(-toTarget.z, 0, toTarget.x);
        if (tangent.lengthSqr() < 1.0E-4)
            tangent = new Vec3(1, 0, 0);
        tangent = tangent.normalize();
        var body = statue.patrolBody();
        for (int attempt = 0; attempt < 8; attempt++) {
            double lateral = (statue.getRandom().nextBoolean() ? 1 : -1)
                             * (3 + statue.getRandom().nextDouble() * 4);
            Vec3 desired = statue.position().add(tangent.scale(lateral));
            Vec3 point = LandRandomPos.getPosTowards(statue, 12, 3, desired);
            if (point == null || point.distanceToSqr(statue.position()) < 9
                || point.distanceToSqr(target.position()) < 9
                || point.distanceToSqr(target.position()) > (HOLD_DISTANCE + 3) * (HOLD_DISTANCE + 3))
                continue;
            Vec3 reachable = DemiurgePatrolSteering.reachableWaypoint(
                    statue.position(), statue.getYRot(), point, body);
            if (reachable == null)
                continue;
            if (statue.getNavigation().moveTo(reachable.x, reachable.y, reachable.z, 1.0))
                return;
        }
    }
}
