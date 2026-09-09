package org.brahypno.maledict.common.combat;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RemembranceBowProjectileEffect {
    private static final String PHASE_COUNT = Maledict.MODID + ":phase_count";
    private static final String PHASE_RANGE = Maledict.MODID + ":phase_range";
    private static final double SEARCH_STEP = 0.05D;

    public static void markForPhasing(AbstractArrow arrow, double range) {
        arrow.getPersistentData().putInt(PHASE_COUNT, 1);
        arrow.getPersistentData().putDouble(PHASE_RANGE, range);
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void phaseThroughBlock(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != HitResult.Type.BLOCK
                || !(event.getProjectile() instanceof AbstractArrow arrow)
                || arrow.level().isClientSide
                || arrow.getPersistentData().getInt(PHASE_COUNT) <= 0) {
            return;
        }

        Vec3 velocity = arrow.getDeltaMovement();
        double range = arrow.getPersistentData().getDouble(PHASE_RANGE);
        if (velocity.lengthSqr() < 1.0E-7D || range <= 0.0D) {
            return;
        }

        Vec3 direction = velocity.normalize();
        Vec3 impact = event.getRayTraceResult().getLocation();
        Vec3 exit = findExit(arrow, impact, direction, range);
        if (exit == null) {
            return;
        }

        arrow.getPersistentData().putInt(PHASE_COUNT,
                arrow.getPersistentData().getInt(PHASE_COUNT) - 1);
        // AbstractArrow.tick() applies one more full velocity movement after the
        // impact hook. Offset it here so this tick ends at the nearest exit,
        // instead of skipping an unchecked segment beyond the wall.
        Vec3 compensatedPosition = exit.subtract(velocity);
        arrow.setPos(compensatedPosition.x, compensatedPosition.y, compensatedPosition.z);
        arrow.setDeltaMovement(velocity);
        arrow.hasImpulse = true;
        event.setCanceled(true);
    }

    private static Vec3 findExit(AbstractArrow arrow, Vec3 impact, Vec3 direction, double range) {
        AABB currentBounds = arrow.getBoundingBox();
        Vec3 currentPosition = arrow.position();
        for (double distance = SEARCH_STEP; distance <= range + 1.0E-7D; distance += SEARCH_STEP) {
            Vec3 candidate = impact.add(direction.scale(Math.min(distance, range)));
            AABB candidateBounds = currentBounds.move(candidate.subtract(currentPosition));
            if (hasNoBlockCollision(arrow, candidateBounds)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean hasNoBlockCollision(AbstractArrow arrow, AABB bounds) {
        for (VoxelShape shape : arrow.level().getBlockCollisions(arrow, bounds)) {
            if (!shape.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private RemembranceBowProjectileEffect() {
    }
}
