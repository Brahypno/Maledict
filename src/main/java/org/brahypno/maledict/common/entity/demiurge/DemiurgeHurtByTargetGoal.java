package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.GameRules;

/**
 * Vanilla {@code HurtByTargetGoal} behaviour, with the target write routed through the boss.
 */
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
