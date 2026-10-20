package org.brahypno.maledict.client.curio.hangman;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import org.brahypno.maledict.common.curio.hangman.HangmanFoolProjectiles;
import org.brahypno.maledict.mixin.ProjectileAccess;
import org.brahypno.maledict.network.ProjectileReflectionPacket;

public final class HangmanReflectionClient {
    public static void accept(ProjectileReflectionPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(packet.entityId()) instanceof Projectile projectile)) {
            return;
        }
        projectile.setPos(packet.position());
        projectile.xo = packet.position().x;
        projectile.yo = packet.position().y;
        projectile.zo = packet.position().z;
        projectile.setOwner(level.getEntity(packet.ownerId()));
        ((ProjectileAccess) projectile).maledict$setLeftOwner(false);
        HangmanFoolProjectiles.applyMotion(projectile, packet.velocity(), false);
        if (projectile instanceof AbstractHurtingProjectile fireball) {
            fireball.xPower = packet.power().x;
            fireball.yPower = packet.power().y;
            fireball.zPower = packet.power().z;
        }
    }

    private HangmanReflectionClient() {
    }
}
