package org.brahypno.maledict.network;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusSwingReach;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;
import org.brahypno.maledict.common.ritual.soulbinding.Soulbinding;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class MaledictNetwork {
    /**
     * 6 -> 7: IncursusSlashPacket carries the swing seed so every client builds the same eight petals.
     *
     * <p>7 -> 8: IncursusSlashPacket carries the swing owner's entity id, so the waist ring follows
     * the player instead of the position snapshot taken when the swing was sent.
     *
     * <p>8 -> 9: IncursusRiftSkillPacket carries the three Malum-derived rift layouts (ascension
     * spin, ascension uppercut, hidden blade flurry) that used to be spawned by Malum itself.
     *
     * <p>9 -> 10: IncursusSlashPacket carries the swing's damage radius instead of a charge scale,
     * so the waist ring drawn by the client covers exactly the sphere the server hits.
     *
     * <p>10 -> 11: IncursusSlashPacket carries the charge the swing was settled at, so a light tap can
     * draw a thinner, shorter bloom without touching the radius.
     */
    private static final String PROTOCOL = "11";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    /**
     * 隐藏锋刃裂隙的广播半径；与客户端 {@code IncursusRiftClientManager} 的绘制视距保持一致。
     */
    private static final double RIFT_BROADCAST_RADIUS = 96.0;
    private static final AtomicLong RIFT_SEEDS = new AtomicLong();

    public static void register() {
        CHANNEL.registerMessage(
                0,
                RadialAttackPacket.class,
                RadialAttackPacket::encode,
                RadialAttackPacket::decode,
                RadialAttackPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(
                1,
                InfuseSpiritPacket.class,
                InfuseSpiritPacket::encode,
                InfuseSpiritPacket::decode,
                InfuseSpiritPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(
                2,
                VicissitudeEffectPacket.class,
                VicissitudeEffectPacket::encode,
                VicissitudeEffectPacket::decode,
                VicissitudeEffectPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(
                3,
                DelayedVitalsPacket.class,
                DelayedVitalsPacket::encode,
                DelayedVitalsPacket::decode,
                DelayedVitalsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(
                4,
                BossBarStylePacket.class,
                BossBarStylePacket::encode,
                BossBarStylePacket::decode,
                BossBarStylePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(5, ProjectileReflectionPacket.class,
                                ProjectileReflectionPacket::encode, ProjectileReflectionPacket::decode,
                                ProjectileReflectionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6, WisdomBlackoutPacket.class,
                                WisdomBlackoutPacket::encode, WisdomBlackoutPacket::decode,
                                WisdomBlackoutPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(7, IncursusSlashPacket.class,
                                IncursusSlashPacket::encode, IncursusSlashPacket::decode,
                                IncursusSlashPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(8, LoverAxeHitPacket.class,
                                LoverAxeHitPacket::encode, LoverAxeHitPacket::decode,
                                LoverAxeHitPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(9, OpenLightfallCodexPacket.class,
                                OpenLightfallCodexPacket::encode, OpenLightfallCodexPacket::decode,
                                OpenLightfallCodexPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(10, MaidVoicePacket.class,
                                MaidVoicePacket::encode, MaidVoicePacket::decode,
                                MaidVoicePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(11, IncursusRiftSkillPacket.class,
                                IncursusRiftSkillPacket::encode, IncursusRiftSkillPacket::decode,
                                IncursusRiftSkillPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendRadialAttack() {
        CHANNEL.sendToServer(new RadialAttackPacket());
    }

    /**
     * 除实体 id 之外还带一份世界坐标：追踪玩家与挥砍者本人都有这个实体，但实体会在被追踪的
     * 客户端上短暂缺席（区块未加载、刚重生），那一帧就退回服务端下发的腰部坐标。
     *
     * <p>{@code radius} 是这一次挥砍的伤害半径，客户端照它画腰环：范围同步的唯一来源。
     * {@code charge} 只影响薄厚与收尾，客户端不能自己读（{@code attackStrengthTicker} 不同步，
     * 而且服务端结算完就把它清零了），所以必须跟着报文走。
     */
    public static void sendIncursusSlash(ServerPlayer player, double radius, float charge) {
        // 报文里的蓄力必须是有限且在 [0,1]：坏值不进网络，客户端那道闸只防别人改包。
        float settledCharge = Float.isFinite(charge) ? Mth.clamp(charge, 0.0f, 1.0f) : 1.0f;
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
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
        if (packet == null){
            return;
        }
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> owner), packet);
    }

    /**
     * 隐藏锋刃：连斩可能由远处的飞镰触发，按效果中心广播。
     *
     * <p>{@code NEAR} 已经包含范围内的施法者本人，所以不能再补一条发给本人，否则同一次连斩叠两份裂隙。
     */
    public static void sendIncursusRiftSkillNear(
            ServerPlayer owner, RiftMode mode, Vec3 center, Vec3 direction,
            float baseAngle, boolean mirrored) {
        IncursusRiftSkillPacket packet = riftSkillPacket(owner, mode, center, direction, baseAngle, mirrored);
        if (packet == null){
            return;
        }
        ResourceKey<Level> dimension = owner.level().dimension();
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                packet.center().x, packet.center().y, packet.center().z,
                RIFT_BROADCAST_RADIUS, dimension)), packet);
    }

    private static IncursusRiftSkillPacket riftSkillPacket(
            ServerPlayer owner, RiftMode mode, Vec3 center, Vec3 direction,
            float baseAngle, boolean mirrored) {
        if (mode == null || mode == RiftMode.MELEE_RING
            || !isFinite(center) || !Float.isFinite(baseAngle)){
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
        if (Double.isFinite(horizontal) && Double.isFinite(y) && horizontal >= 1.0E-4){
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

    /**
     * One notification after the entire damage probe, including fallback and lethal hits.
     */
    public static void sendLoverAxeHit(Player attacker, LivingEntity target) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> attacker),
                     new LoverAxeHitPacket(target.getId(), target.position().add(0, target.getBbHeight() * 0.5, 0),
                                           Mth.clamp(target.getBbWidth() * 0.5F + 0.3F, 0.5F, 1.4F)));
    }

    public static void sendProjectileReflection(net.minecraft.world.entity.projectile.Projectile projectile) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> projectile), ProjectileReflectionPacket.of(projectile));
    }


    public static void sendInfuseSpirit(int containerId, int inventorySlot, ItemStack spiritStack) {
        CHANNEL.sendToServer(new InfuseSpiritPacket(containerId, inventorySlot, spiritStack));
    }

    /**
     * One shot boss presentation event for a single tracking player.
     */
    public static void sendEffect(ServerPlayer player, VicissitudeEffectPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * 延迟池：入队、清零、低频校正、进世界时各发一次，不逐 tick 发。
     */
    public static void sendDelayedVitals(ServerPlayer player, float pendingDamage, float pendingHeal) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                     new DelayedVitalsPacket(pendingDamage, pendingHeal));
    }

    /**
     * 血条皮肤：入队补发、阶段切换、离队清映射时各发一次。{@code style} 为 -1 表示删除映射。
     */
    public static void sendBossBarStyle(ServerPlayer player, UUID bar, int style) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                     new BossBarStylePacket(bar, style));
    }

    public static void sendWisdomBlackout(ServerPlayer player, int ticks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WisdomBlackoutPacket(ticks));
    }

    /**
     * One maid-voice sound for exactly the attacking player, with no nearby-player broadcast.
     */
    public static void sendMaidVoice(ServerPlayer player, ResourceLocation sound, String soundPack, Vec3 pos) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                     new MaidVoicePacket(sound, soundPack, pos.x, pos.y, pos.z));
    }

    public static void openLightfallCodex(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                     new OpenLightfallCodexPacket(Soulbinding.knows(player)));
    }

    private MaledictNetwork() {
    }
}
