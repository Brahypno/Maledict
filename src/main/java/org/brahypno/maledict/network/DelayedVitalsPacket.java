package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.client.DelayedVitalsClient;

import java.util.function.Supplier;

/**
 * 延迟池的同步。
 *
 * <p>只在这几种时机发：入队（同 tick 合并成一包）、清零、每 40 tick 一次的校正、玩家进世界。
 * 池子每 tick 的衰减是确定性公式，两端跑同一份，所以两次入队之间不需要任何包。
 */
public record DelayedVitalsPacket(float pendingDamage, float pendingHeal) {

    public static void encode(DelayedVitalsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.pendingDamage);
        buffer.writeFloat(packet.pendingHeal);
    }

    public static DelayedVitalsPacket decode(FriendlyByteBuf buffer) {
        return new DelayedVitalsPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(DelayedVitalsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DelayedVitalsClient.accept(packet)));
        context.setPacketHandled(true);
    }
}
