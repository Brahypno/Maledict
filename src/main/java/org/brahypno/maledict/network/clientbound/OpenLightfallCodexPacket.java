package org.brahypno.maledict.network.clientbound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;

/**
 * 打开坠光秘典：由服务端下发「已学会拘魂仪式」这一个布尔量。
 *
 * <p>1.20.1 用 Forge 的 {@code SimpleChannel} + {@code DistExecutor.unsafeRunWhenOn}；
 * 1.21 换成 {@link CustomPacketPayload} + {@link StreamCodec}，客户端的解析留在
 * {@code MaledictNetwork} 的分支里（这里只编解码），避免服务端加载屏幕那一层类。
 *
 * <p>只有这一个字段，没有必要多带：视野选择（奥术/虚空/坠光）是纯客户端记忆，存在
 * {@code LightfallCodex} 里，与哪台服务端无关。
 */
public record OpenLightfallCodexPacket(boolean knowsSoulbinding) implements CustomPacketPayload {
    public static final Type<OpenLightfallCodexPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "open_lightfall_codex"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLightfallCodexPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), OpenLightfallCodexPacket::decode);

    @Override
    public Type<OpenLightfallCodexPacket> type() {
        return TYPE;
    }

    public static void encode(OpenLightfallCodexPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.knowsSoulbinding);
    }

    public static OpenLightfallCodexPacket decode(FriendlyByteBuf buffer) {
        return new OpenLightfallCodexPacket(buffer.readBoolean());
    }
}
