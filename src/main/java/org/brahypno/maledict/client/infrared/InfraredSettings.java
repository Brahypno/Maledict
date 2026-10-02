package org.brahypno.maledict.client.infrared;

import net.minecraftforge.common.ForgeConfigSpec;
import org.brahypno.maledict.config.MaledictConfig;

/**
 * 微光视觉的四个客户端可调项，见 {@code config/maledict-client.toml} 的 {@code [infrared]} 段。
 *
 * <p>配置还没加载时 {@code ForgeConfigSpec.ConfigValue#get()} 会抛（不是返回默认值），
 * 而这几项在菜单、读盘、甚至数据生成里都可能被问到，所以这里每一条都带写死的退路——
 * 和 {@code AgeOfEnlightenmentEvents} 里读冷却倍率是同一个理由。
 */
final class InfraredSettings {

    /** 淡入之后能到的强度上限，1.0 即"作者调好的样子"。 */
    static final float DEFAULT_INTENSITY = 1.0F;

    /** 暗到这个亮度以下完全转灰；见着色器里的 smoothstep。 */
    static final float DEFAULT_GRAYSCALE_THRESHOLD = 0.30F;

    /** 阈值之上多宽的过渡带里把颜色还回来。 */
    static final float DEFAULT_GRAYSCALE_SOFTNESS = 0.30F;

    /** 关掉就等于整台设备不存在：不淡入、不推雾，眼槽那格照旧只是个槽位。 */
    static boolean enabled() {
        return read(MaledictConfig.INFRARED_ENABLED, true);
    }

    static float intensity() {
        return read(MaledictConfig.INFRARED_INTENSITY, DEFAULT_INTENSITY);
    }

    static float grayscaleThreshold() {
        return read(MaledictConfig.INFRARED_GRAYSCALE_THRESHOLD, DEFAULT_GRAYSCALE_THRESHOLD);
    }

    static float grayscaleSoftness() {
        return read(MaledictConfig.INFRARED_GRAYSCALE_SOFTNESS, DEFAULT_GRAYSCALE_SOFTNESS);
    }

    private static boolean read(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static float read(ForgeConfigSpec.DoubleValue value, float fallback) {
        try {
            return value.get().floatValue();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private InfraredSettings() {
    }
}
