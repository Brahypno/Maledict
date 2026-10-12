package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.client.item.HangmanReflectionClient;

import java.util.function.Supplier;

/** One reflection notification updates facing, ownership, and fireball acceleration immediately on the client. */
public record ProjectileReflectionPacket(int entityId, int ownerId, Vec3 position, Vec3 velocity, Vec3 power) {
    public static ProjectileReflectionPacket of(Projectile projectile) {
        Vec3 power = projectile instanceof AbstractHurtingProjectile fireball
                ? new Vec3(fireball.xPower, fireball.yPower, fireball.zPower) : Vec3.ZERO;
        return new ProjectileReflectionPacket(projectile.getId(), projectile.getOwner() == null ? -1 : projectile.getOwner().getId(),
                projectile.position(), projectile.getDeltaMovement(), power);
    }

    public static void encode(ProjectileReflectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.ownerId);
        writeVector(buffer, packet.position);
        writeVector(buffer, packet.velocity);
        writeVector(buffer, packet.power);
    }

    public static ProjectileReflectionPacket decode(FriendlyByteBuf buffer) {
        return new ProjectileReflectionPacket(buffer.readVarInt(), buffer.readVarInt(),
                readVector(buffer), readVector(buffer), readVector(buffer));
    }

    private static void writeVector(FriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVector(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(ProjectileReflectionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> HangmanReflectionClient.accept(packet)));
        context.setPacketHandled(true);
    }
}
