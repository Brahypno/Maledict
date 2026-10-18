package org.brahypno.maledict.client.infrared;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.BlightedRealm;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * 微光视觉的客户端驱动：每 tick 判一次「该不该亮」，每帧把强度推向目标，剩下的交给
 * {@link InfraredPostProcessor}。
 *
 * <p><b>开关是眼槽本身，不是某个饰品。</b>本模组的 {@code delusion}（视界）槽就是那台设备：
 * 只要第一格在（贤者献祭补出来的），而且它在 Curios 界面里那只眼睛（渲染开关）是亮的，设备就算开着
 * ——<b>槽空着也算</b>，那只眼睛管的是整格槽位，与槽里放没放饰品无关。所以这里读的是槽位与它的渲染开关
 * （{@link ICurioStacksHandler#getRenders()}），不走物品的 {@code curioTick}。只看本地玩家：
 * 别人的眼槽不会改本客户端的画面。
 *
 * <p>亮度取相机所在方块而不是玩家脚下：第一人称、第三人称、旁观、自由视角下相机都可能离开身体，
 * 取脚下会在贴脸看暗处时还判成亮。阈值用迟滞（<=6 进，>=8 出，7 保持），免得站在阈值上闪。
 *
 * <p>夜视与伽马覆盖提供正常亮度时，后处理退场。遮挡雾另由 {@link #onRenderFog} 处理，
 * 不依赖后处理强度，所以亮处或有夜视时仍能看穿失明、水、岩浆与细雪。
 * 黑暗另外会周期性压暗光照图；视界槽对黑暗的免疫在服务端效果事件里处理。
 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class InfraredClientHandler {

    /** 眼槽的 Curios 标识，见 {@code data/maledict/curios/slots/delusion.json}。 */
    private static final String EYE_SLOT = "delusion";

    /** 原版亮度设置是 0.0 到 1.0；超出这个范围才算被外部改过（fullbright 之类）。 */
    private static final double GAMMA_LOWER_BOUND = -0.0001D;
    private static final double GAMMA_UPPER_BOUND = 1.0001D;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;

        // 槽位是唯一的信息源：饰品被摘下、渲染开关被关掉，都在这一句里变成「设备关了」。
        InfraredClientState.setEyeSlotEnabled(player != null && isEyeSlotDeviceOn(player));

        boolean requested = false;
        if (player != null && level != null && !BlightedRealm.is(level)
                && InfraredClientState.isEyeSlotEnabled() && InfraredSettings.enabled()) {
            Camera camera = minecraft.gameRenderer.getMainCamera();
            if (camera.isInitialized()) {
                BlockPos cameraPos = BlockPos.containing(camera.getPosition());
                // 客户端的 skyDarken 只在建关卡时算过一次（ClientLevel 构造里的 updateSkyBrightness），
                // 之后随时间流逝不再刷新，直接读会永远停在进世界那一刻的天色：白天进档，晚上在户外也照样是 15 亮。
                // 所以这里按原版自己的算法刷新一次，别把公式抄进来。
                level.updateSkyBrightness();
                int brightness = level.getRawBrightness(cameraPos, level.getSkyDarken());
                InfraredClientState.setDarknessActive(
                        InfraredCurve.darknessAfter(InfraredClientState.isDarknessActive(), brightness));
                // 只在正常视觉亮度不足时提亮。失明和液体的遮挡雾独立处理，
                // 因而不会为了推雾把已有夜视、或本来足够亮的画面再转成灰度。
                requested = InfraredCurve.needsEnhancement(InfraredClientState.isDarknessActive(),
                        player.hasEffect(MobEffects.NIGHT_VISION), isGammaOverridden(minecraft));
            }
        }
        InfraredClientState.setRequested(requested);
        if (level != null && BlightedRealm.is(level)) {
            InfraredClientState.setStrength(0);
            InfraredClientState.setDarknessActive(false);
            InfraredPostProcessor.INSTANCE.setActive(false);
        }
    }

    /**
     * 每帧推进强度。优先级留在默认档：Lodestone 的 {@code PostProcessHandler} 挂在
     * {@code AFTER_LEVEL} 的最低优先级上，所以这里改完的值就是本帧真正上传给着色器的值。
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        if (Minecraft.getInstance().level != null && BlightedRealm.is(Minecraft.getInstance().level)) {
            InfraredClientState.setStrength(0);
            InfraredPostProcessor.INSTANCE.setActive(false);
            return;
        }
        // intensity 是淡入之后能到的上限：调低它是"整层效果轻一点"，不是"慢一点"。
        float target = InfraredClientState.isRequested() ? InfraredSettings.intensity() : 0.0F;
        // 帧时长按真实时间换算：掉帧时不会比高帧率时淡得慢。
        float deltaSeconds = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks() / 20.0F;
        float strength = InfraredCurve.approach(InfraredClientState.strength(), target, deltaSeconds);
        InfraredClientState.setStrength(strength);

        // 淡出期间必须继续激活，否则后处理会在还看得见余量的时候被关掉，fade 直接断在半路。
        InfraredPostProcessor.INSTANCE.setActive(strength > InfraredCurve.ACTIVE_EPSILON);
    }

    /**
     * 推掉挡视线的雾。失明与黑暗会把雾拉近（失明 5 格、黑暗 15 格），
     * 设备开着时把雾推回正常视距。黑暗还会压暗光照图，这里只处理雾。水、岩浆、细雪同理，
     * 不一起推掉的话水下中了失明还是只能看 5 格。用 Forge 的雾事件，不必 mixin，也不碰玩家的效果本身。
     *
     * <p><b>空气里的普通视距雾不动</b>：那是画面的远景层次，不是谁在挡你。所以空气这一支只在玩家身上
     * 真有失明/黑暗时动手——这时候原版那一支雾必定是效果的雾（空气分支里效果判断排在普通地形雾前面）。
     * 液体与细雪按相机的 FogType 直接处理，不必等环境变暗或后处理淡入。
     *
     * <p>推雾由设备开关控制，不依赖后处理强度：画面本来够亮、或已有夜视时，
     * 后处理可以完全关闭而仍然移走遮挡雾。配置将 intensity 设为 0 时仍关闭整套画面处理。
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !InfraredClientState.isEyeSlotEnabled() || !InfraredSettings.enabled()
                || InfraredSettings.intensity() <= InfraredCurve.ACTIVE_EPSILON) {
            return;
        }
        if (event.getType() == FogType.NONE && !hasSightLoss(player)) {
            return;
        }
        float renderDistance = Minecraft.getInstance().gameRenderer.getRenderDistance();
        if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(renderDistance);
        } else {
            // 原版地形雾的远景带：照抄它那三行（含地形那一支的 32 格下限），免得推出一段比原版更硬的地平线。
            float terrain = Math.max(renderDistance, 32.0F);
            event.setNearPlaneDistance(terrain - Mth.clamp(terrain / 10.0F, 4.0F, 64.0F));
            event.setFarPlaneDistance(terrain);
        }
        event.setFogShape(FogShape.CYLINDER);
        // 事件取消之后 Forge 才会把这几个值重新写进 RenderSystem，见 ForgeHooksClient#onFogRender。
        event.setCanceled(true);
    }

    /** 失明或黑暗的近距离雾；与是否需要提亮画面是两个独立判断。 */
    private static boolean hasSightLoss(LocalPlayer player) {
        return player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.DARKNESS);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clearState();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearState();
        }
    }

    private static void clearState() {
        InfraredClientState.reset();
        InfraredPostProcessor.INSTANCE.setActive(false);
    }

    /** 只做最好情况的兼容判断：改光影、改光照图、走 mixin 的 fullbright 没有通用探测手段。 */
    private static boolean isGammaOverridden(Minecraft minecraft) {
        double gamma = minecraft.options.gamma().get();
        return gamma < GAMMA_LOWER_BOUND || gamma > GAMMA_UPPER_BOUND;
    }

    /**
     * 眼槽这台的开关：第一格视界槽在（贤者献祭补出的那一格）且它的渲染开关是亮的。
     * 空槽也算开着——那只眼睛管的是整格槽位，不看槽里有没有饰品；多出来的格不参与判断。
     */
    private static boolean isEyeSlotDeviceOn(LocalPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                        .map(handler -> handler.getStacksHandler(EYE_SLOT)
                                                .map(stacks -> InfraredEyeSlot.isOn(stacks.getSlots(),
                                                                                    stacks.getRenders()))
                                                .orElse(false))
                        .orElse(false);
    }

    private InfraredClientHandler() {
    }
}
