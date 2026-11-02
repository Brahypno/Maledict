package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Pursue between casts, then let the synchronized server timeline own movement and aiming. */
final class DemiurgeCombatGoal extends Goal {
    private final DemiurgeStatueEntity statue;
    private int cooldown;
    private int nextAttack;

    DemiurgeCombatGoal(DemiurgeStatueEntity statue) {
        this.statue = statue;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        return statue.canFight() && statue.getTarget() != null && statue.canAttack(statue.getTarget());
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
            } else {
                statue.aimAttack(target);
            }
            return;
        }
        if (cooldown > 0) cooldown--;
        // Charge can break a wall and tears can pass through one. Laser clipping
        // owns occlusion; a blocked sight line must not disable the entire moveset.
        if (statue.distanceToSqr(target) > 28 * 28) {
            if (statue.tickCount % 10 == 0) statue.getNavigation().moveTo(target, 1.25);
            return;
        }
        statue.getNavigation().stop();
        if (cooldown == 0) {
            DemiurgeAttack attack = DemiurgeAttack.values()[1 + nextAttack++ % 3];
            statue.beginAttack(attack, target);
        }
    }
}
