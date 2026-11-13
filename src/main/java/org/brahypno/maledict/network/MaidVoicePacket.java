package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.compat.maid.client.MaledictMaidVoiceClient;

import java.util.function.Supplier;

/**
 * Plays one maid-voice sound on exactly one client; used by the Forsaken Vow attack.
 */
public record MaidVoicePacket(ResourceLocation sound, String soundPack, double x, double y, double z) {
    public static void encode(MaidVoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.sound);
        buffer.writeUtf(packet.soundPack);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
    }

    public static MaidVoicePacket decode(FriendlyByteBuf buffer) {
        return new MaidVoicePacket(buffer.readResourceLocation(), buffer.readUtf(),
                                   buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(MaidVoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (ModList.get().isLoaded("touhou_little_maid")){
                MaledictMaidVoiceClient.play(packet);
            }
        }));
        context.setPacketHandled(true);
    }
}
