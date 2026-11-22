package org.brahypno.maledict.network.clientbound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;

/**
 * Plays one maid-voice sound on exactly one client; used by the Forsaken Vow attack.
 *
 * <p>1.20.1 用 Forge 的 {@code SimpleChannel} + {@code DistExecutor.unsafeRunWhenOn}；1.21 换成
 * {@link CustomPacketPayload} + {@link StreamCodec}，客户端的解析留在 {@code MaledictNetwork} 的
 * 分支里（这里只编解码，服务端不加载任何客户端类）。
 *
 * <p>为什么仍然自建报文、不直接发 TLM 自己的 {@code PlayMaidSoundAtPosPackage}：
 * 那个包只会把声音交给它自己的实例播放，没有「音色包缺这一条音频就退回原版受伤音」的降级路径，
 * 而 TLM 的 {@code maid.ai.hurt_player} 声明的是一段空音频（只有字幕）。这里要保住 1.20.1 的
 * 行为，所以字段与语义原样搬过来，客户端那侧才复用 TLM 的 {@code MaidSoundInstanceAtPos}。
 */
public record MaidVoicePacket(ResourceLocation sound, String soundPack, double x, double y, double z)
        implements CustomPacketPayload {
    public static final Type<MaidVoicePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "maid_voice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaidVoicePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), MaidVoicePacket::decode);

    @Override
    public Type<MaidVoicePacket> type() {
        return TYPE;
    }

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
}
