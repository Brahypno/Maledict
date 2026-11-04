package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.GameRules;

import java.util.EnumSet;

/**
 * Target goals that submit through the boss's AI controller instead of calling {@code setTarget}.
 * The vanilla target goals write directly, which ChangeLib's AI protection correctly rejects.
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

/** Vanilla {@code HurtByTargetGoal} behaviour, with the target write routed through the controller. */
final class DemiurgeHurtByTargetGoal extends DemiurgeTargetGoal {
    private static final TargetingConditions HURT_BY_TARGETING =
            TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();
    private int timestamp;

    DemiurgeHurtByTargetGoal(DemiurgeStatueEntity statue) {
        super(statue, true);
        setUnseenMemoryTicks(300);
    }

    @Override
    public boolean canUse() {
        int stamp = statue.getLastHurtByMobTimestamp();
        LivingEntity attacker = statue.getLastHurtByMob();
        if (stamp == timestamp || attacker == null) {
            return false;
        }
        if (attacker.getType() == EntityType.PLAYER
                && statue.level().getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
            return false;
        }
        return statue.canAttack(attacker, HURT_BY_TARGETING);
    }

    @Override
    public void start() {
        timestamp = statue.getLastHurtByMobTimestamp();
        statue.selectTargetFromGoal(statue.getLastHurtByMob());
        targetMob = statue.getTarget();
        super.start();
    }
}

/** Vanilla nearest-player targeting, with the target write routed through the controller. */
final class DemiurgeNearestPlayerTargetGoal extends DemiurgeTargetGoal {
    private final TargetingConditions targetConditions;
    private LivingEntity candidate;

    DemiurgeNearestPlayerTargetGoal(DemiurgeStatueEntity statue) {
        super(statue, true);
        targetConditions = TargetingConditions.forCombat()
                .range(getFollowDistance())
                .selector(target -> statue.canFight());
    }

    @Override
    public boolean canUse() {
        if (statue.getRandom().nextInt(10) != 0) {
            return false;
        }
        candidate = statue.level().getNearestPlayer(targetConditions, statue,
                statue.getX(), statue.getEyeY(), statue.getZ());
        return candidate != null;
    }

    @Override
    public void start() {
        statue.selectTargetFromGoal(candidate);
        super.start();
    }
}
