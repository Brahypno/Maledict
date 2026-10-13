package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.client.wisdom.WisdomBlackoutClient;

import java.util.function.Supplier;

/** 获得智慧的牺牲时，只向获得者发送一次强制黑幕时长。 */
public record WisdomBlackoutPacket(int ticks) {
    public static void encode(WisdomBlackoutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.ticks);
    }

    public static WisdomBlackoutPacket decode(FriendlyByteBuf buffer) {
        return new WisdomBlackoutPacket(buffer.readVarInt());
    }

    public static void handle(WisdomBlackoutPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> WisdomBlackoutClient.start(packet.ticks)));
        context.setPacketHandled(true);
    }
}
