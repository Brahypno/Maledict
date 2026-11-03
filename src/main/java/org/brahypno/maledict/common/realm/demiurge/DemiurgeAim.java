package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.phys.Vec3;

/** The neck may turn sixty degrees each way while the body catches up. */
public final class DemiurgeAim {
    public static Vec3 constrain(Vec3 desired, float bodyYaw) {
        Vec3 local = desired.normalize().yRot((float) Math.toRadians(bodyYaw));
        double yaw = Math.atan2(local.x, local.z);
        double pitch = Math.asin(Math.max(-1, Math.min(1, local.y)));
        yaw = Math.max(-Math.PI / 3, Math.min(Math.PI / 3, yaw));
        pitch = Math.max(-Math.toRadians(75), Math.min(Math.toRadians(75), pitch));
        return new Vec3(Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch))
                .yRot((float) Math.toRadians(-bodyYaw));
    }

    private DemiurgeAim() {}
}
