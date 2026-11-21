package org.brahypno.maledict.client.item.weapon.incursus.rift;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.brahypno.maledict.Maledict;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * 裂隙花专用 core shader 的注册与 uniform 下发。
 *
 * <p>该事件只在 MOD 总线上触发，而且资源重载时会重新触发一次并换掉实例，
 * 所以这里不缓存构造出来时的对象，只保存加载回调交付的那一个。
 *
 * <p>1.21 侧只换包名：{@code RegisterShadersEvent} 在
 * {@code net.neoforged.neoforge.client.event}，`Dist` 在 {@code net.neoforged.api.distmarker}；
 * {@code ShaderInstance(ResourceProvider, ResourceLocation, VertexFormat)}、{@code safeGetUniform}、
 * {@code AbstractUniform} 在 1.21.1 都还在。
 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class IncursusRiftShaders {
    public static final ResourceLocation RIFT_SHADER =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "incursus_rift");

    private static final String UNIFORM_TIME = "RiftTime";
    private static final String UNIFORM_WIDEN = "RiftWiden";
    private static final String UNIFORM_STARS = "RiftStars";
    private static final String UNIFORM_CAMERA = "RiftCam";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile ShaderInstance instance;
    private static volatile boolean failed;
    private static volatile AbstractUniform nullUniform;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            // 1.21 把 POSITION_COLOR_TEX 改名为 POSITION_TEX_COLOR；元素顺序仍是 Position、UV0、Color，
            // 与 incursus_rift.json 里的 attributes 列表一致。
            ShaderInstance shader = new ShaderInstance(
                    event.getResourceProvider(), RIFT_SHADER, DefaultVertexFormat.POSITION_TEX_COLOR);
            // GLSL 编译失败时 ShaderInstance 不抛异常，而是把对应 Program 留成 null；
            // 这种半成品注册进去只会在链接/绘制阶段炸掉，所以这里提前判掉。
            if (shader.getVertexProgram() == null || shader.getFragmentProgram() == null){
                instance = null;
                failed = true;
                LOGGER.error("Incursus rift shader {} did not compile; see the shader log above. "
                             + "Falling back to the plain textured path.", RIFT_SHADER);
                shader.close();
                return;
            }
            event.registerShader(shader, loaded -> {
                if (failed){
                    LOGGER.info("Incursus rift shader {} loaded after an earlier failure.", loaded.getName());
                }
                instance = loaded;
                failed = false;
                // 换过实例后旧 RenderType 里缓存的 ShaderStateShard 必须重算。
                IncursusRiftRenderTypes.invalidate();
            });
        }
        catch (IOException exception) {
            // Shader 资源坏了不该拖垮整个客户端：标记失败后由调用方退回原版 position_tex_color。
            instance = null;
            failed = true;
            LOGGER.error("Incursus rift shader {} failed to load; falling back to the plain textured path.",
                         RIFT_SHADER, exception);
        }
    }

    /**
     * 当前可用的 shader；未加载或加载失败时为 null。
     */
    public static ShaderInstance shader() {
        return instance;
    }

    public static boolean available() {
        return instance != null;
    }

    public static void setTime(float seconds) {
        uniform(UNIFORM_TIME).set(seconds);
    }

    /**
     * 裂口加宽量，单位是贴图 UV。
     */
    public static void setWiden(float width) {
        uniform(UNIFORM_WIDEN).set(width);
    }

    public static void setStars(float strength) {
        uniform(UNIFORM_STARS).set(strength);
    }

    /**
     * 相机世界坐标。星空各层靠它做反向位移：近层系数大（跟着相机跑），远层系数小（几乎不动），
     * 平面因此读出纵深，而不是一张贴在刀痕上的星空图。
     */
    public static void setCamera(net.minecraft.world.phys.Vec3 camera) {
        uniform(UNIFORM_CAMERA).set((float) camera.x, (float) camera.y, (float) camera.z);
    }

    private static AbstractUniform uniform(String name) {
        ShaderInstance shader = instance;
        return shader == null ? nullUniform() : shader.safeGetUniform(name);
    }

    /**
     * 没有 shader 时也要能安全地吞掉 uniform 写入。
     */
    private static AbstractUniform nullUniform() {
        AbstractUniform uniform = nullUniform;
        if (uniform == null){
            uniform = new AbstractUniform();
            nullUniform = uniform;
        }
        return uniform;
    }

    private IncursusRiftShaders() {
    }
}
