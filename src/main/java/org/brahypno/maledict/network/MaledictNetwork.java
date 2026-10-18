package org.brahypno.maledict.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.brahypno.maledict.Maledict;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

public final class MaledictNetwork {
    public static void register(IEventBus modBus) {
        modBus.addListener(MaledictNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(WellTravelPacket.TYPE, WellTravelPacket.STREAM_CODEC, WellTravelPacket::handle);
        registrar.playToServer(RadialAttackPacket.TYPE, RadialAttackPacket.STREAM_CODEC, RadialAttackPacket::handle);
        registrar.playToServer(InfuseSpiritPacket.TYPE, InfuseSpiritPacket.STREAM_CODEC, InfuseSpiritPacket::handle);
        registrar.playToClient(VicissitudeEffectPacket.TYPE, VicissitudeEffectPacket.STREAM_CODEC, VicissitudeEffectPacket::handle);
        registrar.playToClient(DelayedVitalsPacket.TYPE, DelayedVitalsPacket.STREAM_CODEC, DelayedVitalsPacket::handle);
        registrar.playToClient(BossBarStylePacket.TYPE, BossBarStylePacket.STREAM_CODEC, BossBarStylePacket::handle);
        registrar.playToClient(ProjectileReflectionPacket.TYPE, ProjectileReflectionPacket.STREAM_CODEC, ProjectileReflectionPacket::handle);
        registrar.playToClient(WisdomBlackoutPacket.TYPE, WisdomBlackoutPacket.STREAM_CODEC, WisdomBlackoutPacket::handle);
    }

    public static void sendRadialAttack() {
        PacketDistributor.sendToServer(new RadialAttackPacket());
    }

    public static void sendProjectileReflection(net.minecraft.world.entity.projectile.Projectile projectile) {
        PacketDistributor.sendToPlayersTrackingEntity(projectile, ProjectileReflectionPacket.of(projectile));
    }


    public static void sendInfuseSpirit(int containerId, int inventorySlot, ItemStack spiritStack) {
        PacketDistributor.sendToServer(new InfuseSpiritPacket(containerId, inventorySlot, spiritStack));
    }

    /** One shot boss presentation event for a single tracking player. */
    public static void sendEffect(ServerPlayer player, VicissitudeEffectPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    /** 延迟池：入队、清零、低频校正、进世界时各发一次，不逐 tick 发。 */
    public static void sendDelayedVitals(ServerPlayer player, float pendingDamage, float pendingHeal) {
        PacketDistributor.sendToPlayer(player,
                     new DelayedVitalsPacket(pendingDamage, pendingHeal));
    }

    /** 血条皮肤：入队补发、阶段切换、离队清映射时各发一次。{@code style} 为 -1 表示删除映射。 */
    public static void sendBossBarStyle(ServerPlayer player, UUID bar, int style) {
        PacketDistributor.sendToPlayer(player,
                     new BossBarStylePacket(bar, style));
    }

    public static void sendWisdomBlackout(ServerPlayer player, int ticks) {
        PacketDistributor.sendToPlayer(player, new WisdomBlackoutPacket(ticks));
    }

    private MaledictNetwork() {
    }
}
