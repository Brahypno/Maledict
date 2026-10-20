package org.brahypno.maledict.common.curio.hangman;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.mixin.ProjectileAccess;

public final class HangmanFoolProjectiles {
    private static final String REFLECTED_TICK = "maledict:hangman_reflected_tick";

    public static double reflectionChance(float health, float maxHealth, double baseChance) {
        return Mth.clamp(baseChance + 1.0D - health / maxHealth, 0.0D, 1.0D);
    }

    /** Full reversal, without normalizing the velocity or calling shoot() (which adds spread). */
    public static void reflect(Projectile projectile, LivingEntity wearer, Vec3 impact) {
        Vec3 velocity = projectile.getDeltaMovement().reverse();
        Vec3 position = impact.add(velocity.normalize().scale(projectile.getBbWidth() * 0.5D + 0.05D));
        projectile.setOwner(wearer);
        ((ProjectileAccess) projectile).maledict$setLeftOwner(false);
        projectile.setPos(position);
        applyMotion(projectile, velocity, true);
        projectile.getPersistentData().putLong(REFLECTED_TICK, projectile.level().getGameTime());
        projectile.hasImpulse = true;
    }

    /** Used on both sides: reset previous rotations too, so arrows never interpolate tail-first. */
    public static void applyMotion(Projectile projectile, Vec3 velocity, boolean reversePower) {
        projectile.setDeltaMovement(velocity);
        float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG);
        projectile.setYRot(yaw);
        projectile.setXRot(pitch);
        projectile.yRotO = yaw;
        projectile.xRotO = pitch;
        if (reversePower && projectile instanceof AbstractHurtingProjectile fireball) {
            fireball.xPower = -fireball.xPower;
            fireball.yPower = -fireball.yPower;
            fireball.zPower = -fireball.zPower;
        }
    }

    public static boolean wasReflected(Projectile projectile) {
        return hasBeenReflected(projectile)
                && projectile.getPersistentData().getLong(REFLECTED_TICK) == projectile.level().getGameTime();
    }

    public static boolean hasBeenReflected(Projectile projectile) {
        return projectile.getPersistentData().contains(REFLECTED_TICK);
    }

    private HangmanFoolProjectiles() {
    }
}
