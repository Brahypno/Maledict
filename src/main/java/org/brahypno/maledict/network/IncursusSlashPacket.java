package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.client.item.weapon.incursus.IncursusBladeSlashEffects;

import java.util.function.Supplier;

/**
 * 一次挥砍共享同一个腰部中心、视觉倍率与随机种子，发给攻击者及追踪玩家。
 *
 * <p>种子由服务端生成，八瓣的扰动因此在所有客户端上完全一致；客户端不自行抽随机数。
 */
public record IncursusSlashPacket(Vec3 center, float scale, long seed) {
    public static void encode(IncursusSlashPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.center.x);
        buffer.writeDouble(packet.center.y);
        buffer.writeDouble(packet.center.z);
        buffer.writeFloat(packet.scale);
        buffer.writeLong(packet.seed);
    }

    public static IncursusSlashPacket decode(FriendlyByteBuf buffer) {
        return new IncursusSlashPacket(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(),
                buffer.readLong());
    }

    public static void handle(IncursusSlashPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                                               () -> () -> IncursusBladeSlashEffects.spawn(packet)));
        context.setPacketHandled(true);
    }
}
