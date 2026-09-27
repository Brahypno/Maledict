package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.client.VicissitudeBossBarOverlay;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 把"这条血条该用哪套皮肤"告诉客户端。
 *
 * <p><b>为什么这条非要发包。</b>{@code ServerBossEvent} 在 1.20.1 只有三参数的公开构造函数，
 * 血条 UUID 是内部 {@code Mth.createInsecureUUID()} 随机生成的；客户端收到的
 * {@code LerpingBossEvent} 只有那个随机 UUID，**认不出"这条血条是我的"**，
 * 也没法从实体 UUID 或阶段反推。所以样式号只能在服务端是真值，客户端由这个包镜像。
 *
 * <p>样式号只在三个时机发：血条入队（{@code addPlayer}）、阶段切换（{@code setStyle}）、
 * 离队（{@code removePlayer}，发 {@link #STYLE_NONE} 让客户端删掉映射，避免 UUID 越攒越多）。
 */
public record BossBarStylePacket(UUID bar, int style) {
    /** 删掉这条血条的样式映射，回到原版渲染。 */
    public static final int STYLE_NONE = -1;

    public static void encode(BossBarStylePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.bar);
        buffer.writeVarInt(packet.style);
    }

    public static BossBarStylePacket decode(FriendlyByteBuf buffer) {
        return new BossBarStylePacket(buffer.readUUID(), buffer.readVarInt());
    }

    public static void handle(BossBarStylePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> VicissitudeBossBarOverlay.accept(packet)));
        context.setPacketHandled(true);
    }
}
