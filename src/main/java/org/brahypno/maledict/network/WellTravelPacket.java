package org.brahypno.maledict.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.brahypno.maledict.common.realm.WeepingWellTravel;

/** Starts or clears the native well state without Malum's rejection damage and reward packet. */
public record WellTravelPacket(boolean emerge) implements CustomPacketPayload {
    public static final Type<WellTravelPacket> TYPE = new Type<>(ResourceLocation.parse("maledict:well_travel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WellTravelPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> buffer.writeBoolean(packet.emerge()),
            buffer -> new WellTravelPacket(buffer.readBoolean()));

    @Override
    public Type<WellTravelPacket> type() { return TYPE; }

    public static void handle(WellTravelPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> WeepingWellTravel.applyVisualState(context.player(), packet.emerge()));
    }
}
