package org.brahypno.maledict.client.infrared;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import team.lodestar.lodestone.systems.postprocess.PostProcessHandler;
import team.lodestar.lodestone.systems.postprocess.PostProcessor;

/**
 * 微光视觉的后处理通道：把画面压成提亮过的灰度，用 {@code Strength} 做淡入淡出。
 *
 * <p>只建一个常驻实例（{@link #INSTANCE}），由 {@link InfraredPostProcessorSetup} 登记给
 * {@link PostProcessHandler}；开关靠 {@code setActive}，强弱靠 uniform，不反复创建销毁链。
 *
 * <p>着色器里的曲线参数都从这里传：调外观改下面几个常量即可，改完重进游戏就生效。
 * 具体曲线见 {@code assets/maledict/shaders/program/infrared.fsh}。
 */
public final class InfraredPostProcessor extends PostProcessor {

    public static final InfraredPostProcessor INSTANCE = new InfraredPostProcessor();

    /** {@code maledict:infrared} 指向 {@code assets/maledict/shaders/post/infrared.json}。 */
    private static final ResourceLocation POST_CHAIN_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "infrared");

    /** 整体增益：抬完暗部之后还嫌暗就加它。 */
    private static final float GAIN = 1.5F;

    /** 暗部抬升的幂次：越小越亮。0.55 让几乎全黑的像素也能看出轮廓。 */
    private static final float LIFT = 0.55F;

    /** 以中灰为轴的对比：灰度化之后靠它把形状重新分开。 */
    private static final float CONTRAST = 1.15F;

    /** 高光软拐点：超过它的部分不再线性增长。 */
    private static final float HIGHLIGHT_KNEE = 0.8F;

    /** 高光压缩的硬度：越大越不容易糊成纯白（渐近线约为 knee + 1/softness）。 */
    private static final float HIGHLIGHT_SOFTNESS = 6.0F;

    /** 传感器噪点强度，0 就是关着。第一版保持干净，要开也是极轻的一层。 */
    private static final float NOISE_STRENGTH = 0.0F;

    private InfraredPostProcessor() {
    }

    @Override
    public ResourceLocation getPostChainLocation() {
        return POST_CHAIN_LOCATION;
    }

    @Override
    public void beforeProcess(PoseStack viewModelStack) {
        setUniform("Strength", InfraredClientState.strength());
        setUniform("Gain", GAIN);
        setUniform("Lift", LIFT);
        setUniform("Contrast", CONTRAST);
        setUniform("HighlightKnee", HIGHLIGHT_KNEE);
        setUniform("HighlightSoftness", HIGHLIGHT_SOFTNESS);
        setUniform("NoiseStrength", NOISE_STRENGTH);
        // 动态灰度的两把尺子来自客户端配置：暗到哪一档完全转灰、往上多宽把颜色还回来。
        setUniform("GrayscaleThreshold", InfraredSettings.grayscaleThreshold());
        setUniform("GrayscaleSoftness", InfraredSettings.grayscaleSoftness());
    }

    @Override
    public void afterProcess() {
        // 没有自己绑的贴图要解，链里的采样器由 PostChain 自己管。
    }

    /**
     * 给链上每个 pass 里存在这个 uniform 的都写一遍：blit 那一道没有这些名字，
     * {@link EffectInstance#getUniform} 直接返回 null，跳过即可。
     */
    private void setUniform(String name, float value) {
        if (effects == null) {
            return;
        }
        for (EffectInstance effect : effects) {
            Uniform uniform = effect.getUniform(name);
            if (uniform != null) {
                uniform.set(value);
            }
        }
    }
}
