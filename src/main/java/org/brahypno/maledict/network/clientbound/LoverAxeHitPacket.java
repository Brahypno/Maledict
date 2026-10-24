package org.brahypno.maledict.network.clientbound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;

/** A position snapshot also presents lethal hits whose target has already left the client world. */
public record LoverAxeHitPacket(int targetId, Vec3 center, float radius) implements CustomPacketPayload {
    public static final Type<LoverAxeHitPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "lover_axe_hit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoverAxeHitPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), LoverAxeHitPacket::decode);

    @Override
    public Type<LoverAxeHitPacket> type() {
        return TYPE;
    }

    public static void encode(LoverAxeHitPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetId);
        buffer.writeDouble(packet.center.x);
        buffer.writeDouble(packet.center.y);
        buffer.writeDouble(packet.center.z);
        buffer.writeFloat(packet.radius);
    }

    public static LoverAxeHitPacket decode(FriendlyByteBuf buffer) {
        return new LoverAxeHitPacket(buffer.readVarInt(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat());
    }

}
