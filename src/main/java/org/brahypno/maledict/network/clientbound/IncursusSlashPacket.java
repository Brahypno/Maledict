package org.brahypno.maledict.network.clientbound;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.Maledict;

/** 一次挥砍共享同一个腰部中心和视觉倍率，发给攻击者及追踪玩家。 */
public record IncursusSlashPacket(Vec3 center, float scale) implements CustomPacketPayload {
    public static final Type<IncursusSlashPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "incursus_slash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, IncursusSlashPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), IncursusSlashPacket::decode);

    @Override
    public Type<IncursusSlashPacket> type() {
        return TYPE;
    }

    public static void encode(IncursusSlashPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeDouble(packet.center.x);
        buffer.writeDouble(packet.center.y);
        buffer.writeDouble(packet.center.z);
        buffer.writeFloat(packet.scale);
    }

    public static IncursusSlashPacket decode(RegistryFriendlyByteBuf buffer) {
        return new IncursusSlashPacket(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat());
    }

}
