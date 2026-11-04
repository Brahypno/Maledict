package org.brahypno.maledict.common.realm.wither;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * Target goals that submit through the wither's AI controller instead of calling {@code setTarget}.
 * The vanilla target goals write directly, which ChangeLib's AI protection correctly rejects.
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
        if (target == null) {
            target = targetMob;
        }
        if (target == null || wither.isAlliedTo(target) || !wither.canAttack(target)) {
            return false;
        }
        double range = getFollowDistance();
        if (wither.distanceToSqr(target) > range * range) {
            return false;
        }
        if (mustSee) {
            if (wither.getSensing().hasLineOfSight(target)) {
                unseenTicks = 0;
            } else if (++unseenTicks > reducedTickDelay(unseenMemoryTicks)) {
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

/** Vanilla {@code HurtByTargetGoal} behaviour, with the target write routed through the controller. */
final class WeepingWitherHurtByTargetGoal extends WeepingWitherTargetGoal {
    private static final TargetingConditions HURT_BY_TARGETING =
            TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();
    private int timestamp;

    WeepingWitherHurtByTargetGoal(WeepingWitherEntity wither) {
        super(wither, true);
        setUnseenMemoryTicks(300);
    }

    @Override
    public boolean canUse() {
        int stamp = wither.getLastHurtByMobTimestamp();
        LivingEntity attacker = wither.getLastHurtByMob();
        if (stamp == timestamp || attacker == null) {
            return false;
        }
        if (attacker.getType() == EntityType.PLAYER
                && wither.level().getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
            return false;
        }
        return wither.canAttack(attacker, HURT_BY_TARGETING);
    }

    @Override
    public void start() {
        timestamp = wither.getLastHurtByMobTimestamp();
        wither.selectTargetFromGoal(wither.getLastHurtByMob());
        targetMob = wither.getTarget();
        super.start();
    }
}

/** Vanilla living-target scan, with the target write routed through the controller. */
final class WeepingWitherNearestTargetGoal extends WeepingWitherTargetGoal {
    private final TargetingConditions targetConditions;
    private LivingEntity candidate;

    WeepingWitherNearestTargetGoal(WeepingWitherEntity wither) {
        super(wither, false);
        targetConditions = TargetingConditions.forCombat()
                .range(getFollowDistance())
                .selector(target -> target.getMobType() != MobType.UNDEAD && target.attackable());
    }

    @Override
    public boolean canUse() {
        double range = getFollowDistance();
        AABB area = wither.getBoundingBox().inflate(range, 4.0D, range);
        candidate = wither.level().getNearestEntity(LivingEntity.class, targetConditions, wither,
                wither.getX(), wither.getEyeY(), wither.getZ(), area);
        return candidate != null;
    }

    @Override
    public void start() {
        wither.selectTargetFromGoal(candidate);
        super.start();
    }
}
