package org.brahypno.maledict.common.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Matches ThrowableProjectile's 0.3-block entity hit margin when checking bystanders. */
public final class RavenShotSafety {
    public static boolean mayHit(boolean undead, boolean intendedTarget, boolean raven, boolean trustedPlayer) {
        return !raven && !trustedPlayer && (undead || intendedTarget);
    }

    public static boolean crossesBody(AABB body, Vec3 origin, Vec3 aim) {
        AABB collision = body.inflate(0.3D);
        return collision.contains(origin) || collision.clip(origin, aim).isPresent();
    }

    private RavenShotSafety() {
    }
}
