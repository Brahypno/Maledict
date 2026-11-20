package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftClientManager;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;

import java.util.function.Supplier;

/**
 * 一次技能裂隙的通知：模式、锚点、方向与两个角度参数。
 *
 * <p>与普通挥砍的报文分开，是因为飞腾与隐藏锋刃的轨迹来自 Malum 的粒子公式，需要的输入完全不同：
 * 圆周位置由服务端捕获的方向决定，刀面朝外靠 {@code direction}，每道刀光的自旋靠
 * {@code baseAngle}/{@code mirrored}——这两个量原本由 Malum 自己在触发那一刻抽出来，
 * 换成裂隙渲染后必须原样带过来，否则"随机基础角 + 镜像"的分布就变了。
 *
 * <p>{@code seed} 是服务端生成的单次种子，客户端据此重建同一批刀痕；
 * 客户端不自行抽随机数，所以同一次技能在所有客户端上形状一致。
 *
 * <p>{@code direction} 不做归一化：强化飞腾的纵向间距直接乘它的长度，
 * 服务端下发的是 {@code getLookAngle().multiply(1, 0, 1)}。
 */
public record IncursusRiftSkillPacket(
        RiftMode mode,
        int ownerId,
        Vec3 center,
        Vec3 direction,
        float baseAngle,
        boolean mirrored,
        long seed) {

    public static void encode(IncursusRiftSkillPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.mode);
        buffer.writeVarInt(packet.ownerId);
        buffer.writeDouble(packet.center.x);
        buffer.writeDouble(packet.center.y);
        buffer.writeDouble(packet.center.z);
        buffer.writeFloat((float) packet.direction.x);
        buffer.writeFloat((float) packet.direction.y);
        buffer.writeFloat((float) packet.direction.z);
        buffer.writeFloat(packet.baseAngle);
        buffer.writeBoolean(packet.mirrored);
        buffer.writeLong(packet.seed);
    }

    public static IncursusRiftSkillPacket decode(FriendlyByteBuf buffer) {
        return new IncursusRiftSkillPacket(
                buffer.readEnum(RiftMode.class),
                buffer.readVarInt(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readLong());
    }

    public static void handle(IncursusRiftSkillPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                                               () -> () -> IncursusRiftClientManager.addSkill(packet)));
        context.setPacketHandled(true);
    }
}
