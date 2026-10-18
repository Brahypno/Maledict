package org.brahypno.maledict.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.combat.IncursusBladeAttack;


public record RadialAttackPacket() implements CustomPacketPayload {
    public static final Type<RadialAttackPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "radial_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadialAttackPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), RadialAttackPacket::decode);

    @Override
    public Type<RadialAttackPacket> type() { return TYPE; }

    static void encode(RadialAttackPacket packet, RegistryFriendlyByteBuf buffer) {
    }

    static RadialAttackPacket decode(RegistryFriendlyByteBuf buffer) {
        return new RadialAttackPacket();
    }

    static void handle(RadialAttackPacket packet, IPayloadContext context) {
        ServerPlayer sender = context.player() instanceof ServerPlayer player ? player : null;
        if (sender != null) {
            context.enqueueWork(() -> IncursusBladeAttack.perform(sender));
        }
    }
}
