package org.brahypno.maledict.common.entity.wither;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.EnumSet;

/**
 * Target goals that submit through the wither's AI controller instead of calling {@code setTarget}.
 * The vanilla target goals write directly, which ChangeLib's AI protection correctly rejects.
 * <p>
 * One class per file in 1.21: an auxiliary (package-private) class in a multi-class source file
 * cannot be referenced from another file by the Java 21 compiler used here.
 */
abstract class WeepingWitherTargetGoal extends TargetGoal {
    protected final WeepingWitherEntity wither;
    private int unseenTicks;

    protected WeepingWitherTargetGoal(WeepingWitherEntity wither, boolean mustSee) {
        super(wither, mustSee);
        this.wither = wither;
        setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = wither.getTarget();
        if (target == null){
            target = targetMob;
        }
        if (target == null || wither.isAlliedTo(target) || !wither.canAttack(target)){
            return false;
        }
        double range = getFollowDistance();
        if (wither.distanceToSqr(target) > range * range){
            return false;
        }
        if (mustSee){
            if (wither.getSensing().hasLineOfSight(target)){
                unseenTicks = 0;
            }else if (++unseenTicks > reducedTickDelay(unseenMemoryTicks)){
                return false;
            }
        }
        wither.selectTargetFromGoal(target);
        return true;
    }

    @Override
    public void stop() {
        wither.selectTargetFromGoal(null);
        targetMob = null;
    }
}
