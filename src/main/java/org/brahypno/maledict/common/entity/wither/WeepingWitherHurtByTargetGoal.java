package org.brahypno.maledict.common.entity.wither;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.GameRules;

/**
 * Vanilla {@code HurtByTargetGoal} behaviour, with the target write routed through the controller.
 */
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
        if (stamp == timestamp || attacker == null){
            return false;
        }
        if (attacker.getType() == EntityType.PLAYER
            && wither.level().getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)){
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
