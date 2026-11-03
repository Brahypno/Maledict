package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Pursue between casts, then let the synchronized server timeline own movement and aiming. */
final class DemiurgeCombatGoal extends Goal {
    private final DemiurgeStatueEntity statue;
    private int cooldown;
    private int nextAttack;
    private int repositionAt;

    DemiurgeCombatGoal(DemiurgeStatueEntity statue) {
        this.statue = statue;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        return statue.canFight() && (statue.attack() == DemiurgeAttack.ROAR
                || statue.getTarget() != null && statue.canAttack(statue.getTarget()));
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void stop() {
        statue.getNavigation().stop();
        statue.finishAttack();
        cooldown = DemiurgeAttack.COOLDOWN;
    }

    @Override public void tick() {
        var target = statue.getTarget();
        if (statue.attack() != DemiurgeAttack.NONE) {
            statue.getNavigation().stop();
            if (statue.attackAge(0) >= statue.attack().duration) {
                statue.finishAttack();
                cooldown = DemiurgeAttack.COOLDOWN;
            } else if (target != null) {
                statue.aimAttack(target);
            }
            return;
        }
        if (target == null) return;
        if (cooldown > 0) cooldown--;
        if (cooldown > 0) {
            moveBetweenAttacks();
            return;
        }
        // Charge can break a wall and tears can pass through one. Laser clipping
        // owns occlusion; a blocked sight line must not disable the entire moveset.
        if (statue.distanceToSqr(target) > 28 * 28) {
            if (statue.tickCount % 10 == 0) statue.getNavigation().moveTo(target, 1.25);
            return;
        }
        statue.getNavigation().stop();
        if (cooldown == 0) {
            DemiurgeAttack attack = DemiurgeAttack.values()[1 + nextAttack++ % 3];
            if (attack == DemiurgeAttack.ROAR && !statue.canStartRain(target)) {
                attack = DemiurgeAttack.values()[1 + nextAttack++ % 3];
            }
            statue.beginAttack(attack, target);
        }
    }

    private void moveBetweenAttacks() {
        if (statue.tickCount < repositionAt) return;
        repositionAt = statue.tickCount + 5;
        var target = statue.getTarget();
        if (statue.distanceToSqr(target) > 18 * 18) {
            statue.getNavigation().moveTo(target, 1.0);
            return;
        }
        if (!statue.getNavigation().isDone()) return;
        Vec3 forward = Vec3.directionFromRotation(0, statue.getYRot());
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        var body = statue.patrolBody();
        for (int attempt = 0; attempt < 8; attempt++) {
            double lateral = (statue.getRandom().nextBoolean() ? 1 : -1) * (4 + statue.getRandom().nextDouble() * 4);
            Vec3 desired = statue.position().add(forward.scale(10)).add(side.scale(lateral));
            Vec3 point = LandRandomPos.getPosTowards(statue, 12, 3, desired);
            if (point == null || point.distanceToSqr(statue.position()) < 36
                    || point.distanceToSqr(target.position()) < 36
                    || !DemiurgePatrolSteering.clearDestination(point, statue.position(), statue.getYRot(), body)) continue;
            if (statue.getNavigation().moveTo(point.x, point.y, point.z, 1.0)) return;
        }
    }
}
