package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.EnumSet;

/**
 * Target goals that submit through the boss's AI controller instead of walking the vanilla
 * target-writing path. The vanilla target goals write directly, which ChangeLib's AI protection
 * correctly rejects.
 */
abstract class DemiurgeTargetGoal extends TargetGoal {
    protected final DemiurgeStatueEntity statue;
    private int unseenTicks;

    protected DemiurgeTargetGoal(DemiurgeStatueEntity statue, boolean mustSee) {
        super(statue, mustSee);
        this.statue = statue;
        setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = statue.getTarget();
        if (target == null) {
            target = targetMob;
        }
        if (target == null || statue.isAlliedTo(target) || !statue.canAttack(target)) {
            return false;
        }
        double range = getFollowDistance();
        if (statue.distanceToSqr(target) > range * range) {
            return false;
        }
        if (mustSee) {
            if (statue.getSensing().hasLineOfSight(target)) {
                unseenTicks = 0;
            } else if (++unseenTicks > reducedTickDelay(unseenMemoryTicks)) {
                return false;
            }
        }
        statue.selectTargetFromGoal(target);
        return true;
    }

    @Override
    public void stop() {
        statue.selectTargetFromGoal(null);
        targetMob = null;
    }
}
