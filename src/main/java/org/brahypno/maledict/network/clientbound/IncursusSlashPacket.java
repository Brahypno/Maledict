package org.brahypno.maledict.network.clientbound;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.Maledict;

/**
 * 一次挥砍共享同一个腰部锚点、伤害半径、蓄力与随机种子，发给攻击者及追踪玩家。
 *
 * <p>{@code radius} 就是服务端这一次挥砍的伤害半径（{@code IncursusSwingReach}）：客户端拿它画
 * 腰环，于是画出来的那一圈与真正能打到的圈是同一个数。服务端已经封顶，客户端仍会再夹一次。
 *
 * <p>{@code charge} 是服务端结算这一刀时用的攻击强度（0..1），只改薄厚与收尾，不改半径。
 * 客户端读不到它（{@code attackStrengthTicker} 不同步，而且服务端结算完就清零了），所以跟着报文走。
 *
 * <p>种子由服务端生成，八瓣的扰动因此在所有客户端上完全一致；客户端不自行抽随机数。
 *
 * <p>{@code ownerId} 是挥砍者的实体 id：客户端按它每帧读取<b>本地</b>插值后的腰部位置，
 * {@code center} 只是实体不在本地世界时的兜底世界坐标。没有 id 时腰环会被钉死在下发瞬间的
 * 快照上，玩家在效果持续的十二 tick 里跳起、被击退或位移，就会看到刀光留在原地远处。
 */
public record IncursusSlashPacket(int ownerId, Vec3 center, float radius, float charge, long seed)
        implements CustomPacketPayload {
    public static final Type<IncursusSlashPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "incursus_slash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, IncursusSlashPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), IncursusSlashPacket::decode);

    @Override
    public Type<IncursusSlashPacket> type() {
        return TYPE;
    }

    public static void encode(IncursusSlashPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.ownerId);
        buffer.writeDouble(packet.center.x);
        buffer.writeDouble(packet.center.y);
        buffer.writeDouble(packet.center.z);
        buffer.writeFloat(packet.radius);
        buffer.writeFloat(packet.charge);
        buffer.writeLong(packet.seed);
    }

    public static IncursusSlashPacket decode(RegistryFriendlyByteBuf buffer) {
        return new IncursusSlashPacket(
                buffer.readVarInt(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readLong());
    }

}
