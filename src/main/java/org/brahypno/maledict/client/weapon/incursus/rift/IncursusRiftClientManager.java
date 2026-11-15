package org.brahypno.maledict.client.weapon.incursus.rift;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.IncursusSlashPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端活跃裂隙花实例：接收挥砍通知、按 tick 推进年龄、负责视距与数量上限。
 *
 * <p>一次挥砍只创建一个实例；实例自己按 {@code age + partialTick} 求值，不生成任何需要独立 tick 的实体。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IncursusRiftClientManager {
    /** 超过这个距离的实例不再绘制，也不会被创建：挥砍只发生在玩家身边，远处实例没有意义。 */
    private static final double MAX_RENDER_DISTANCE = 96.0;

    private static final List<IncursusRiftInstance> ACTIVE = new ArrayList<>();

    public static void add(IncursusSlashPacket packet) {
        if (packet == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        ACTIVE.add(IncursusRiftInstance.create(
                packet.center(), packet.scale(), packet.seed(),
                lifetimeTicks(), petalCount(), configFadeStart()));
        trim();
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

    /** 旧粒子是 noClip，默认不遮挡；配置打开后才按世界深度裁剪。 */
    public static boolean respectWorldDepth() {
        return MaledictConfig.INCURSUS_RIFT_DEPTH_TEST.get();
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static int lifetimeTicks() {
        return Math.max(1, MaledictConfig.INCURSUS_RIFT_LIFETIME.get());
    }

    private static int petalCount() {
        return Math.max(1, MaledictConfig.INCURSUS_RIFT_PETALS.get());
    }

    private static float configFadeStart() {
        return MaledictConfig.INCURSUS_RIFT_FADE_START.get().floatValue();
    }

    private static void trim() {
        int max = Math.max(1, MaledictConfig.INCURSUS_RIFT_MAX_INSTANCES.get());
        while (ACTIVE.size() > max) {
            ACTIVE.remove(0);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused() || minecraft.player == null) {
            return;
        }
        Vec3 eye = minecraft.player.position();
        double limit = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
        ACTIVE.removeIf(instance -> instance.isExpired() || instance.center().distanceToSqr(eye) > limit);
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
        if (Minecraft.getInstance().level == null) {
            clear();
        }
    }

    private IncursusRiftClientManager() {
    }
}
