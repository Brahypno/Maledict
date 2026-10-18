package org.brahypno.maledict.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.vitals.DelayedVitalsClient;


/**
 * 延迟池的同步。
 *
 * <p>只在这几种时机发：入队（同 tick 合并成一包）、清零、每 40 tick 一次的校正、玩家进世界。
 * 池子每 tick 的衰减是确定性公式，两端跑同一份，所以两次入队之间不需要任何包。
 */
public record DelayedVitalsPacket(float pendingDamage, float pendingHeal) implements CustomPacketPayload {
    public static final Type<DelayedVitalsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "delayed_vitals"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DelayedVitalsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), DelayedVitalsPacket::decode);

    @Override
    public Type<DelayedVitalsPacket> type() { return TYPE; }


    public static void encode(DelayedVitalsPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(packet.pendingDamage);
        buffer.writeFloat(packet.pendingHeal);
    }

    public static DelayedVitalsPacket decode(RegistryFriendlyByteBuf buffer) {
        return new DelayedVitalsPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(DelayedVitalsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> DelayedVitalsClient.accept(packet));
    }
}
