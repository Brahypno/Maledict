package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.client.codex.LightfallCodex;

import java.util.function.Supplier;

public record OpenLightfallCodexPacket(boolean knowsSoulbinding) {
    public static void encode(OpenLightfallCodexPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.knowsSoulbinding);
    }

    public static OpenLightfallCodexPacket decode(FriendlyByteBuf buffer) {
        return new OpenLightfallCodexPacket(buffer.readBoolean());
    }

    public static void handle(OpenLightfallCodexPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> LightfallCodex.openCodexViaItem(packet.knowsSoulbinding)));
        context.setPacketHandled(true);
    }
}
