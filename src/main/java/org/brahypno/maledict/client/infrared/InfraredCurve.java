package org.brahypno.maledict.client.infrared;

/**
 * 微光视觉的亮度判决：进入/退出阈值与「要不要提亮」的合成条件。
 *
 * <p>抽成不碰 Minecraft 类的纯函数，是为了让阈值语义能被 {@code InfraredCurveTest} 直接断言。
 * 读相机、读眼槽、写请求的部分在 {@link InfraredClientHandler}，只能在游戏里验。
 *
 * <p><b>这里没有强度平滑。</b>视觉系统 V2.5 起，淡入淡出的唯一所有者是 ChangeLib 的统一调度器
 * （本地效果用 {@code tau = 0.12s} 的指数逼近）；本类再叠一层会让同一段过渡被平滑两次。
 */
final class InfraredCurve {

    /** 亮度 <= 6 时允许进入微光模式。 */
    static final int ENABLE_LIGHT = 6;

    /** 亮度 >= 8 时退出；中间的 7 保持现状，这就是迟滞区。 */
    static final int DISABLE_LIGHT = 8;

    /** 低于这个值就当作没有效果：配置把 intensity 调到这么小时，整层画面处理不该跑。 */
    static final float ACTIVE_EPSILON = 0.001F;

    /** 迟滞：进来要够暗，出去要够亮，中间那格保持现状，免得站在阈值上闪。 */
    static boolean darknessAfter(boolean active, int brightness) {
        return active ? brightness < DISABLE_LIGHT : brightness <= ENABLE_LIGHT;
    }

    /** 环境暗，且原版夜视或亮度增强没有提供正常亮度时，才需要后处理提亮。 */
    static boolean needsEnhancement(boolean dark, boolean nightVision, boolean gammaOverridden) {
        return dark && !nightVision && !gammaOverridden;
    }

    private InfraredCurve() {
    }
}
