package org.brahypno.maledict.common.entity.wither;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;

/**
 * Vanilla living-target scan, with the target write routed through the controller. The selector used
 * {@code MobType.UNDEAD} on 1.20.1; 1.21 has no {@code MobType} surface, so it reads the UNDEAD tag.
 */
final class WeepingWitherNearestTargetGoal extends WeepingWitherTargetGoal {
    private final TargetingConditions targetConditions;
    private LivingEntity candidate;

    WeepingWitherNearestTargetGoal(WeepingWitherEntity wither) {
        super(wither, false);
        targetConditions = TargetingConditions.forCombat()
                                              .range(getFollowDistance())
                                              .selector(target -> !target.getType().is(EntityTypeTags.UNDEAD)
                                                                  && target.attackable());
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
