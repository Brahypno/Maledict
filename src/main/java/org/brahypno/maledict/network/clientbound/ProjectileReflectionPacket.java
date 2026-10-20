package org.brahypno.maledict.network.clientbound;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.item.HangmanReflectionClient;


/** One reflection notification updates facing, ownership, and fireball acceleration immediately on the client. */
public record ProjectileReflectionPacket(int entityId, int ownerId, Vec3 position, Vec3 velocity, double power) implements CustomPacketPayload {
    public static final Type<ProjectileReflectionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "projectile_reflection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProjectileReflectionPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ProjectileReflectionPacket::decode);

    @Override
    public Type<ProjectileReflectionPacket> type() { return TYPE; }

    public static ProjectileReflectionPacket of(Projectile projectile) {
        double power = projectile instanceof AbstractHurtingProjectile fireball
                ? fireball.accelerationPower : 0.0D;
        return new ProjectileReflectionPacket(projectile.getId(), projectile.getOwner() == null ? -1 : projectile.getOwner().getId(),
                projectile.position(), projectile.getDeltaMovement(), power);
    }

    public static void encode(ProjectileReflectionPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.ownerId);
        writeVector(buffer, packet.position);
        writeVector(buffer, packet.velocity);
        buffer.writeDouble(packet.power);
    }

    public static ProjectileReflectionPacket decode(RegistryFriendlyByteBuf buffer) {
        return new ProjectileReflectionPacket(buffer.readVarInt(), buffer.readVarInt(),
                readVector(buffer), readVector(buffer), buffer.readDouble());
    }

    private static void writeVector(RegistryFriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVector(RegistryFriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(ProjectileReflectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> HangmanReflectionClient.accept(packet));
    }
}
