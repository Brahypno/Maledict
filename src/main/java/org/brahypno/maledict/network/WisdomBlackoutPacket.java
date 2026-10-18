package org.brahypno.maledict.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.wisdom.WisdomBlackoutClient;


/** 获得智慧的牺牲时，只向获得者发送一次强制黑幕时长。 */
public record WisdomBlackoutPacket(int ticks) implements CustomPacketPayload {
    public static final Type<WisdomBlackoutPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "wisdom_blackout"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WisdomBlackoutPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), WisdomBlackoutPacket::decode);

    @Override
    public Type<WisdomBlackoutPacket> type() { return TYPE; }

    public static void encode(WisdomBlackoutPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.ticks);
    }

    public static WisdomBlackoutPacket decode(RegistryFriendlyByteBuf buffer) {
        return new WisdomBlackoutPacket(buffer.readVarInt());
    }

    public static void handle(WisdomBlackoutPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> WisdomBlackoutClient.start(packet.ticks));
    }
}
