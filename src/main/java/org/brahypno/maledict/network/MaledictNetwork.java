package org.brahypno.maledict.network;

import org.brahypno.maledict.network.clientbound.BossBarStylePacket;
import org.brahypno.maledict.network.clientbound.DelayedVitalsPacket;
import org.brahypno.maledict.network.clientbound.IncursusRiftSkillPacket;
import org.brahypno.maledict.network.clientbound.IncursusSlashPacket;
import org.brahypno.maledict.network.serverbound.InfuseSpiritPacket;
import org.brahypno.maledict.network.clientbound.ProjectileReflectionPacket;
import org.brahypno.maledict.network.serverbound.RadialAttackPacket;
import org.brahypno.maledict.network.clientbound.VicissitudeEffectPacket;
import org.brahypno.maledict.network.clientbound.WellTravelPacket;
import org.brahypno.maledict.network.clientbound.WisdomBlackoutPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.network.clientbound.LoverAxeHitPacket;
import org.brahypno.maledict.network.clientbound.MaidVoicePacket;
import org.brahypno.maledict.network.clientbound.OpenLightfallCodexPacket;
import org.brahypno.maledict.client.item.weapon.lover.LoverAxeHitEffects;
import org.brahypno.maledict.client.item.codex.LightfallCodex;
import org.brahypno.maledict.common.ritual.soulbinding.Soulbinding;
import net.neoforged.neoforge.network.PacketDistributor;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusSwingReach;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;
import org.brahypno.maledict.compat.maid.MaledictMaidIntegration;
import org.brahypno.maledict.compat.maid.client.MaledictMaidVoiceClient;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.brahypno.maledict.client.item.weapon.incursus.IncursusBladeSlashEffects;
import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftClientManager;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class MaledictNetwork {
    /**
     * 3 -> 4: 两条与神侵恶刃视觉相关的报文变了形状。
     *
     * <p>{@code IncursusSlashPacket} 重新带上挥砍者的实体 id 与单次种子：腰环要跟着挥砍者走，
     * 八瓣也要在每个客户端上形状一致。{@code IncursusRiftSkillPacket} 是新增的，
     * 承载原本由 Malum 自己生成的三套裂隙布局（飞腾旋斩、强化上挑、隐藏锋刃连斩）。
     *
     * <p>版本号同时是握手校验：形状变了却沿用旧号，会让旧客户端在解包时炸掉。
     *
     * <p>4 -> 5: 新增 {@code OpenLightfallCodexPacket}（坠光秘典开屏）。1.8.2 的两本 Malum 全典都是
     * 客户端本地开屏，但本模组的法典要等「已学会拘魂仪式」这个只存在于服务端的状态，必须走一趟报文。
     *
     * <p>5 -> 6: 新增 {@code MaidVoicePacket}（弃约之刃的女仆语音）。它照搬 1.20.1 的独立报文，
     * 只发给动手的那一名玩家：TLM 的音色包音频在客户端才解析得出来，服务端既不知道有没有这段音频，
     * 也不该让附近玩家都听见。TLM 缺席时这里只登记一个空处理器（客户端那侧还会再过一道
     * {@code ModList} 闸门），不会出现"注册了一半"的通道。
     *
     * <p>6 -> 7: {@code IncursusSlashPacket} 的倍率换成了这一刀的<b>伤害半径</b>，并新增结算时的
     * 蓄力：半径决定腰环大小（画出来的圈就是打得到的圈），蓄力只压薄厚与收尾。两个字段都改了，
     * 沿用旧号会让旧客户端解错包。
     */
    private static final String PROTOCOL = "7";

    /**
     * 隐藏锋刃裂隙的广播半径；与客户端 {@code IncursusRiftClientManager} 的绘制视距保持一致。
     */
    private static final double RIFT_BROADCAST_RADIUS = 96.0;
    private static final AtomicLong RIFT_SEEDS = new AtomicLong();

    public static void register(IEventBus modBus) {
        modBus.addListener(MaledictNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL);
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
        IPayloadHandler<IncursusRiftSkillPacket> riftHandler = (packet, context) -> {};
        if (FMLEnvironment.dist == Dist.CLIENT) {
            riftHandler = (packet, context) -> context.enqueueWork(() -> IncursusRiftClientManager.addSkill(packet));
        }
        registrar.playToClient(IncursusRiftSkillPacket.TYPE, IncursusRiftSkillPacket.STREAM_CODEC, riftHandler);
        IPayloadHandler<OpenLightfallCodexPacket> codexHandler = (packet, context) -> {};
        if (FMLEnvironment.dist == Dist.CLIENT) {
            codexHandler = (packet, context) -> context.enqueueWork(
                    () -> LightfallCodex.openCodexViaItem(packet.knowsSoulbinding()));
        }
        registrar.playToClient(OpenLightfallCodexPacket.TYPE, OpenLightfallCodexPacket.STREAM_CODEC, codexHandler);
        IPayloadHandler<MaidVoicePacket> maidVoiceHandler = (packet, context) -> {};
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // 女仆语音：先确认 TLM 在场，再去碰它的客户端类。这条闸门不能省——客户端可能没装
            // TLM，而 {@code MaledictMaidVoiceClient} 一被解析就会要求它的类存在。
            maidVoiceHandler = (packet, context) -> context.enqueueWork(() -> {
                if (ModList.get().isLoaded(MaledictMaidIntegration.MAID_MOD_ID)) {
                    MaledictMaidVoiceClient.play(packet);
                }
            });
        }
        registrar.playToClient(MaidVoicePacket.TYPE, MaidVoicePacket.STREAM_CODEC, maidVoiceHandler);
    }

    public static void sendRadialAttack() {
        PacketDistributor.sendToServer(new RadialAttackPacket());
    }

    /**
     * 除实体 id 之外还带一份世界坐标：追踪玩家与挥砍者本人都有这个实体，但实体会在被追踪的
     * 客户端上短暂缺席（区块未加载、刚重生），那一帧就退回服务端下发的腰部坐标。
     *
     * <p>{@code radius} 是这一次挥砍的伤害半径，客户端照它画腰环：范围同步的唯一来源。
     * {@code charge} 只影响薄厚与收尾，客户端不能自己读（{@code attackStrengthTicker} 不同步，
     * 而且服务端结算完就把它清零了），所以必须跟着报文走。两者都在这里先过一道闸：
     * 坏值不进网络，客户端那道闸只防别人改包。
     */
    public static void sendIncursusSlash(ServerPlayer player, double radius, float charge) {
        float settledCharge = Float.isFinite(charge) ? Mth.clamp(charge, 0.0f, 1.0f) : 1.0f;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new IncursusSlashPacket(player.getId(),
                        IncursusSwingReach.centerOf(player),
                        (float) IncursusSwingReach.clampRadius(radius),
                        settledCharge,
                        player.getRandom().nextLong()));
    }

    /**
     * 飞腾两种模式：看得见施法者的客户端就看得到裂隙。
     */
    public static void sendIncursusRiftSkill(
            ServerPlayer owner, RiftMode mode, Vec3 center, Vec3 direction,
            float baseAngle, boolean mirrored) {
        IncursusRiftSkillPacket packet = riftSkillPacket(owner, mode, center, direction, baseAngle, mirrored);
        if (packet == null) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(owner, packet);
    }

    /**
     * 隐藏锋刃：连斩可能由远处的飞镰触发，按效果中心广播。
     *
     * <p>{@code sendToPlayersNear} 已经包含范围内的施法者本人，所以不能再补一条发给本人，
     * 否则同一次连斩叠两份裂隙。
     */
    public static void sendIncursusRiftSkillNear(
            ServerPlayer owner, RiftMode mode, Vec3 center, Vec3 direction,
            float baseAngle, boolean mirrored) {
        IncursusRiftSkillPacket packet = riftSkillPacket(owner, mode, center, direction, baseAngle, mirrored);
        if (packet == null) {
            return;
        }
        PacketDistributor.sendToPlayersNear(
                owner.serverLevel(), null,
                packet.center().x, packet.center().y, packet.center().z,
                RIFT_BROADCAST_RADIUS, packet);
    }

    private static IncursusRiftSkillPacket riftSkillPacket(
            ServerPlayer owner, RiftMode mode, Vec3 center, Vec3 direction,
            float baseAngle, boolean mirrored) {
        if (mode == null || mode == RiftMode.MELEE_RING
                || !isFinite(center) || !Float.isFinite(baseAngle)) {
            return null;
        }
        return new IncursusRiftSkillPacket(
                mode, owner.getId(), center, horizontalDirection(owner, direction),
                baseAngle, mirrored, nextRiftSeed(owner));
    }

    /**
     * 方向只用于朝向，以及强化飞腾那个随长度缩放的纵向间距，所以长度保持原样。
     *
     * <p>只有退化到近似水平零向量时才按玩家朝向补一个单位向量：抬头/低头到极限时
     * {@code getLookAngle().multiply(1, 0, 1)} 会变成零向量，原版 Malum 在这种输入下
     * 会算出 NaN 的刀面，这里给一个良定义的替代。
     */
    private static Vec3 horizontalDirection(ServerPlayer owner, Vec3 direction) {
        double x = direction == null ? 0.0 : direction.x;
        double y = direction == null ? 0.0 : direction.y;
        double z = direction == null ? 0.0 : direction.z;
        double horizontal = Math.sqrt(x * x + z * z);
        if (Double.isFinite(horizontal) && Double.isFinite(y) && horizontal >= 1.0E-4) {
            return new Vec3(x, y, z);
        }
        double yaw = Math.toRadians(owner.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    private static boolean isFinite(Vec3 vector) {
        return vector != null
                && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    /**
     * 单次效果的种子。刻意不碰世界或玩家的随机流：一条视觉通知不该改变其它逻辑抽到的数。
     */
    private static long nextRiftSeed(ServerPlayer owner) {
        return owner.getId() * 0x9E3779B97F4A7C15L
                + owner.level().getGameTime()
                + RIFT_SEEDS.incrementAndGet();
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

    /**
     * 坠光秘典：把「已学会拘魂仪式」一并发过去，虚空页的可见性由它决定。
     *
     * <p>1.20.1 里这个参数来自 {@code Soulbinding.knows(player)}；1.21 的签名收 {@link ServerPlayer}，
     * 调用点本来就只在服务端的 {@code Item#use} 分支里。
     */
    public static void openLightfallCodex(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new OpenLightfallCodexPacket(Soulbinding.knows(player)));
    }

    /**
     * 弃约之刃的女仆语音：一条报文、一个收件人。
     *
     * <p>音色包音频是客户端资源，服务端只负责把「哪一段、哪一包、在哪」告诉动手的那一名玩家；
     * 附近玩家听不到，因为这句话是女仆对被杀的回应，不是环境音。
     */
    public static void sendMaidVoice(ServerPlayer player, ResourceLocation sound, String soundPack, Vec3 pos) {
        PacketDistributor.sendToPlayer(player, new MaidVoicePacket(sound, soundPack, pos.x, pos.y, pos.z));
    }

    private MaledictNetwork() {
    }
}
