package org.brahypno.maledict.network;

import org.brahypno.maledict.network.clientbound.BossBarStylePacket;
import org.brahypno.maledict.network.clientbound.DelayedVitalsPacket;
import org.brahypno.maledict.network.clientbound.IncursusSlashPacket;
import org.brahypno.maledict.network.serverbound.InfuseSpiritPacket;
import org.brahypno.maledict.network.clientbound.ProjectileReflectionPacket;
import org.brahypno.maledict.network.serverbound.RadialAttackPacket;
import org.brahypno.maledict.network.clientbound.VicissitudeEffectPacket;
import org.brahypno.maledict.network.clientbound.WellTravelPacket;
import org.brahypno.maledict.network.clientbound.WisdomBlackoutPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import org.brahypno.maledict.network.clientbound.LoverAxeHitPacket;
import org.brahypno.maledict.client.item.weapon.lover.LoverAxeHitEffects;
import net.neoforged.neoforge.network.PacketDistributor;
import org.brahypno.maledict.Maledict;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.brahypno.maledict.client.item.weapon.incursus.IncursusBladeSlashEffects;
import java.util.UUID;

public final class MaledictNetwork {
    public static void register(IEventBus modBus) {
        modBus.addListener(MaledictNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("3");
        registrar.playToClient(WellTravelPacket.TYPE, WellTravelPacket.STREAM_CODEC, WellTravelPacket::handle);
        registrar.playToServer(RadialAttackPacket.TYPE, RadialAttackPacket.STREAM_CODEC, RadialAttackPacket::handle);
        registrar.playToServer(InfuseSpiritPacket.TYPE, InfuseSpiritPacket.STREAM_CODEC, InfuseSpiritPacket::handle);
        registrar.playToClient(VicissitudeEffectPacket.TYPE, VicissitudeEffectPacket.STREAM_CODEC, VicissitudeEffectPacket::handle);
        registrar.playToClient(DelayedVitalsPacket.TYPE, DelayedVitalsPacket.STREAM_CODEC, DelayedVitalsPacket::handle);
        registrar.playToClient(BossBarStylePacket.TYPE, BossBarStylePacket.STREAM_CODEC, BossBarStylePacket::handle);
        registrar.playToClient(ProjectileReflectionPacket.TYPE, ProjectileReflectionPacket.STREAM_CODEC, ProjectileReflectionPacket::handle);
        registrar.playToClient(WisdomBlackoutPacket.TYPE, WisdomBlackoutPacket.STREAM_CODEC, WisdomBlackoutPacket::handle);
        // 服务端只注册协议与编解码器；客户端分支才解析粒子处理器。
        IPayloadHandler<IncursusSlashPacket> slashHandler = (packet, context) -> {};
        if (FMLEnvironment.dist == Dist.CLIENT) {
            slashHandler = (packet, context) -> context.enqueueWork(() -> IncursusBladeSlashEffects.spawn(packet));
        }
        registrar.playToClient(IncursusSlashPacket.TYPE, IncursusSlashPacket.STREAM_CODEC, slashHandler);
        IPayloadHandler<LoverAxeHitPacket> loverAxeHandler = (packet, context) -> {};
        if (FMLEnvironment.dist == Dist.CLIENT) {
            loverAxeHandler = (packet, context) -> context.enqueueWork(() -> LoverAxeHitEffects.spawn(packet));
        }
        registrar.playToClient(LoverAxeHitPacket.TYPE, LoverAxeHitPacket.STREAM_CODEC, loverAxeHandler);
    }

    public static void sendRadialAttack() {
        PacketDistributor.sendToServer(new RadialAttackPacket());
    }

    public static void sendIncursusSlash(ServerPlayer player, float scale) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new IncursusSlashPacket(player.position().add(0, player.getBbHeight() * 0.5, 0), scale));
    }

    /** One notification after the entire damage probe, including fallback and lethal hits. */
    public static void sendLoverAxeHit(Player attacker, LivingEntity target) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(attacker,
                new LoverAxeHitPacket(target.getId(), target.position().add(0, target.getBbHeight() * 0.5, 0),
                        Mth.clamp(target.getBbWidth() * 0.5F + 0.3F, 0.5F, 1.4F)));
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
