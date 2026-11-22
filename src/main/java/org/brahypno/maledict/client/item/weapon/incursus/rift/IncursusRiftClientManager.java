package org.brahypno.maledict.client.item.weapon.incursus.rift;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.item.weapon.incursus.rift.patterns.AscensionSpinPattern;
import org.brahypno.maledict.client.item.weapon.incursus.rift.patterns.AscensionUppercutPattern;
import org.brahypno.maledict.client.item.weapon.incursus.rift.patterns.HiddenBladeFlurryPattern;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusSwingReach;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.IncursusRiftSkillPacket;
import org.brahypno.maledict.network.IncursusSlashPacket;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端活跃裂隙实例：接收通知、按模式建刀痕、按 tick 推进年龄、负责视距与数量上限。
 *
 * <p>四种模式共用一套材质与 RenderType，差别只在 {@link RiftStrokePattern}：
 * 普通挥砍由旧报文 {@link IncursusSlashPacket} 触发、锚点跟随挥砍者；
 * 飞腾两种与隐藏锋刃由 {@link IncursusRiftSkillPacket} 触发、锚点固定在触发时的世界坐标。
 *
 * <p>刀痕贴图缺失、shader 不可用或玩家关掉配置时，技能模式会退回一次性生成的 Malum 原版刀光
 * （{@link RiftLegacySlashes}），而不是让整个技能没有视觉。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IncursusRiftClientManager {
    /**
     * 超过这个距离的实例不再绘制，也不会被创建。
     */
    private static final double MAX_RENDER_DISTANCE = 96.0;
    /**
     * 开发期对照开关，只在 -D 里给，不进玩家配置。
     */
    private static final String LEGACY_SWITCH = "maledict.incursus.legacySlash";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<IncursusRiftInstance> ACTIVE = new ArrayList<>();
    private static boolean fallbackLogged;

    /**
     * 普通挥砍：八瓣水平腰环，形状与改造前逐顶点一致——它走的是旧的整实例模型，
     * 不经过 {@link RiftStroke}。半径取服务端这一次挥砍的伤害半径，并再过一次闸：
     * 报文里的坏值不能变成铺满屏幕的四边形。
     */
    public static void add(IncursusSlashPacket packet) {
        if (packet == null){
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null){
            return;
        }
        ACTIVE.add(IncursusRiftInstance.createMelee(
                packet.ownerId(), packet.center(), IncursusSwingReach.clampRadius(packet.radius()),
                packet.seed(), lifetimeTicks(), petalCount(), configFadeStart()));
        trim();
    }

    /**
     * 技能模式：飞腾旋斩 48 道、强化上挑 12 道、隐藏锋刃 300 道，全部由报文种子在本地重建。
     */
    public static void addSkill(IncursusRiftSkillPacket packet) {
        if (packet == null || packet.mode() == RiftMode.MELEE_RING){
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null){
            return;
        }
        IncursusRiftInstance instance = IncursusRiftInstance.createSkill(
                packet.mode(), packet.ownerId(), packet.center(), packet.seed(),
                patternFor(packet).generate());
        if (!riftReady()){
            // 降级：把同一批刀痕交回 Malum 的原版刀光，数量与位置都不变。
            RiftLegacySlashes.spawn(instance);
            return;
        }
        ACTIVE.add(instance);
        trim();
    }

    private static RiftStrokePattern patternFor(IncursusRiftSkillPacket packet) {
        Vec3 direction = packet.direction();
        if (packet.mode() == RiftMode.ASCENSION_UPPERCUT){
            return new AscensionUppercutPattern(
                    packet.seed(), direction, packet.baseAngle(), packet.mirrored(), configFadeStart());
        }
        if (packet.mode() == RiftMode.HIDDEN_BLADE_FLURRY){
            return new HiddenBladeFlurryPattern(
                    packet.seed(), direction, packet.baseAngle(), packet.mirrored(), configFadeStart());
        }
        return new AscensionSpinPattern(
                packet.seed(), direction, packet.baseAngle(), packet.mirrored(), configFadeStart());
    }

    /**
     * 裂隙渲染这条路是否可用：配置开启、未被开发期开关强制关掉、刀痕贴图在位。
     *
     * <p>贴图缺失只警告一次：这是 Malum 改名才会出现的情况，每一次挥砍都刷一条日志没有意义。
     */
    public static boolean riftReady() {
        if (Boolean.getBoolean(LEGACY_SWITCH)){
            return false;
        }
        try {
            if (!enabled()){
                return false;
            }
            if (IncursusRiftRenderTypes.maskAvailable(0)){
                return true;
            }
            if (!fallbackLogged){
                fallbackLogged = true;
                LOGGER.warn("Incursus rift mask (malum:slash/heavy_slash_00) not found; "
                            + "falling back to the old slash particles.");
            }
            return false;
        }
        catch (RuntimeException ignored) {
            // 配置尚未加载：宁可画旧粒子，也不要因为一条视觉通知崩掉客户端。
            return false;
        }
    }

    /**
     * 这一帧锚点该待的世界坐标。
     *
     * <p>跟随模式取挥砍者的<b>本地</b>腰部位置并按 partialTick 插值：腰环属于挥砍者，
     * 他跳起、被击退、上台阶或单纯因为延迟与自己的服务端坐标错开，花心都必须跟着走。
     * 固定模式直接用报文里的世界坐标——Malum 那三种效果本来就是一次性生成后独立飞走的粒子群，
     * 迁移到裂隙渲染不能顺手让它们追着施法者跑。
     */
    public static Vec3 anchorOf(IncursusRiftInstance instance, float partialTick) {
        if (instance.anchorMode() == RiftAnchorMode.FIXED_WORLD){
            return instance.center();
        }
        Entity owner = ownerOf(instance);
        if (owner == null){
            return instance.center();
        }
        // 与 EntityRenderer 一致的插值：xo/yo/zo 是上一 tick 的位置。
        return new Vec3(
                Mth.lerp(partialTick, owner.xo, owner.getX()),
                Mth.lerp(partialTick, owner.yo, owner.getY()) + owner.getBbHeight() * 0.5,
                Mth.lerp(partialTick, owner.zo, owner.getZ()));
    }

    /**
     * 挥砍者；只认活着的生物，否则返回 null 让调用方退回兜底坐标。
     */
    private static Entity ownerOf(IncursusRiftInstance instance) {
        Level level = Minecraft.getInstance().level;
        if (level == null){
            return null;
        }
        Entity owner = level.getEntity(instance.ownerId());
        return owner instanceof LivingEntity living && !living.isRemoved() ? living : null;
    }

    public static List<IncursusRiftInstance> activeInstances() {
        return Collections.unmodifiableList(ACTIVE);
    }

    public static int activeCount() {
        return ACTIVE.size();
    }

    public static boolean enabled() {
        return MaledictConfig.INCURSUS_RIFT_ENABLED.get();
    }

    public static boolean useShader() {
        return MaledictConfig.INCURSUS_RIFT_SHADER.get() && IncursusRiftShaders.available();
    }

    public static float intensity() {
        return MaledictConfig.INCURSUS_RIFT_INTENSITY.get().floatValue();
    }

    public static float petalHalfSize() {
        return MaledictConfig.INCURSUS_RIFT_PETAL_SIZE.get().floatValue();
    }

    public static float radialSpeed() {
        return MaledictConfig.INCURSUS_RIFT_RADIAL_SPEED.get().floatValue();
    }

    public static float widen() {
        return MaledictConfig.INCURSUS_RIFT_WIDEN.get().floatValue();
    }

    public static float starStrength() {
        return MaledictConfig.INCURSUS_RIFT_STARS.get().floatValue();
    }

    /**
     * 旧粒子是 noClip，默认不遮挡；配置打开后才按世界深度裁剪。
     */
    public static boolean respectWorldDepth() {
        return MaledictConfig.INCURSUS_RIFT_DEPTH_TEST.get();
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static float configFadeStart() {
        return MaledictConfig.INCURSUS_RIFT_FADE_START.get().floatValue();
    }

    private static int lifetimeTicks() {
        return Math.max(1, MaledictConfig.INCURSUS_RIFT_LIFETIME.get());
    }

    private static int petalCount() {
        return Math.max(1, MaledictConfig.INCURSUS_RIFT_PETALS.get());
    }

    private static void trim() {
        int max = Math.max(1, MaledictConfig.INCURSUS_RIFT_MAX_INSTANCES.get());
        while (ACTIVE.size() > max) {
            ACTIVE.remove(0);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END){
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused() || minecraft.player == null){
            return;
        }
        Vec3 eye = minecraft.player.position();
        double limit = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
        ACTIVE.removeIf(instance -> instance.isExpired() || anchorOf(instance, 1.0F).distanceToSqr(eye) > limit);
        for (IncursusRiftInstance instance : ACTIVE) {
            instance.tick();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (Minecraft.getInstance().level == null){
            clear();
        }
    }

    private IncursusRiftClientManager() {
    }
}
