package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

/**
 * Vanilla nearest-player targeting, with the target write routed through the boss.
 */
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
