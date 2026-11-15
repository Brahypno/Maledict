package org.brahypno.maledict.client.weapon.incursus.rift;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * 裂隙花专用 core shader 的注册与 uniform 下发。
 *
 * <p>Forge 的该事件只在 MOD 总线上触发，而且资源重载时会重新触发一次并换掉实例，
 * 所以这里不缓存构造出来时的对象，只保存加载回调交付的那一个。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IncursusRiftShaders {
    public static final ResourceLocation RIFT_SHADER =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "incursus_rift");

    private static final String UNIFORM_TIME = "RiftTime";
    private static final String UNIFORM_PASS = "RiftPass";
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
            ShaderInstance shader = new ShaderInstance(
                    event.getResourceProvider(), RIFT_SHADER, DefaultVertexFormat.POSITION_COLOR_TEX);
            // GLSL 编译失败时 ShaderInstance 不抛异常，而是把对应 Program 留成 null；
            // 这种半成品注册进去只会在链接/绘制阶段炸掉，所以这里提前判掉。
            if (shader.getVertexProgram() == null || shader.getFragmentProgram() == null) {
                instance = null;
                failed = true;
                LOGGER.error("Incursus rift shader {} did not compile; see the shader log above. "
                             + "Falling back to the plain textured path.", RIFT_SHADER);
                shader.close();
                return;
            }
            event.registerShader(shader, loaded -> {
                if (failed) {
                    LOGGER.info("Incursus rift shader {} loaded after an earlier failure.", loaded.getName());
                }
                instance = loaded;
                failed = false;
                // 换过实例后旧 RenderType 里缓存的 ShaderStateShard 必须重算。
                IncursusRiftRenderTypes.invalidate();
            });
        } catch (IOException exception) {
            // Shader 资源坏了不该拖垮整个客户端：标记失败后由调用方退回原版 position_tex_color。
            instance = null;
            failed = true;
            LOGGER.error("Incursus rift shader {} failed to load; falling back to the plain textured path.",
                    RIFT_SHADER, exception);
        }
    }

    /** 当前可用的 shader；未加载或加载失败时为 null。 */
    public static ShaderInstance shader() {
        return instance;
    }

    public static boolean available() {
        return instance != null;
    }

    public static void setTime(float seconds) {
        uniform(UNIFORM_TIME).set(seconds);
    }

    /** {@code rim} 为真时切到边缘通道，为假时切到内核通道。 */
    public static void setPass(boolean rim) {
        uniform(UNIFORM_PASS).set(rim ? 1.0F : 0.0F);
    }

    /** 内核加宽量，单位是贴图 UV。 */
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

    /** 没有 shader 时也要能安全地吞掉 uniform 写入。 */
    private static AbstractUniform nullUniform() {
        AbstractUniform uniform = nullUniform;
        if (uniform == null) {
            uniform = new AbstractUniform();
            nullUniform = uniform;
        }
        return uniform;
    }

    private IncursusRiftShaders() {
    }
}
