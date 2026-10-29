package org.brahypno.maledict.common.curio.priestess;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Constant-time predicate; callers already have the entity, so no nearby-entity query is needed. */
public final class LifeManifestation {
    public static final double RANGE = 16;
    private static final double RANGE_SQUARED = RANGE * RANGE;

    public static boolean reveals(LivingEntity viewer, Entity candidate) {
        return candidate != viewer && candidate.level() == viewer.level()
                && candidate instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                && !(candidate instanceof SoulEchoEntity)
                && viewer.distanceToSqr(candidate) <= RANGE_SQUARED;
    }

    private LifeManifestation() {}
}
