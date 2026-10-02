package org.brahypno.maledict.client.infrared;

/**
 * 微光视觉（红外）的客户端状态。整个特性只活在客户端：不同步、不落盘、不改世界光照。
 *
 * <p>三层分工，别混在一起：
 * <ul>
 *     <li>{@link #setEyeSlotEnabled} 只表达「眼槽这台设备开着」——第一格视界槽在，且它的渲染开关（Curios
 *     界面里那只眼睛）是亮的；槽空着也算开着，见 {@link InfraredEyeSlot}。</li>
 *     <li>{@link #setRequested} 是渲染系统算出来的结论：「这一刻效果该不该可见」。
 *     环境亮度、夜视、伽马值都在那里判，眼槽自己不管这些。</li>
 *     <li>{@link #setStrength} 是 0..1 的实际强度，淡入淡出的唯一去处；
 *     着色器只认它，不认识上面任何一个布尔。</li>
 * </ul>
 *
 * <p>没有 {@code previousStrength}：强度每帧推一次，而读写都在同一帧的渲染阶段里，
 * 中间不存在需要插值的时刻。
 */
public final class InfraredClientState {

    /** 「设备开着」：视界槽在，且那一格的渲染开关是亮的。 */
    private static boolean eyeSlotEnabled;

    /** 迟滞判决后的「环境够暗」。 */
    private static boolean darknessActive;

    /** 三条件合起来的最终结论：设备开着 + 环境够暗 + 没有夜视/伽马覆盖。 */
    private static boolean requested;

    /** 0 = 完全关闭，1 = 完全生效。 */
    private static float strength;

    private InfraredClientState() {
    }

    public static void setEyeSlotEnabled(boolean enabled) {
        eyeSlotEnabled = enabled;
    }

    public static boolean isEyeSlotEnabled() {
        return eyeSlotEnabled;
    }

    public static void setDarknessActive(boolean active) {
        darknessActive = active;
    }

    public static boolean isDarknessActive() {
        return darknessActive;
    }

    public static void setRequested(boolean value) {
        requested = value;
    }

    public static boolean isRequested() {
        return requested;
    }

    /** 覆盖当前强度。淡出到看不见时由驱动方归零，而不是让它无限逼近。 */
    public static void setStrength(float value) {
        strength = value;
    }

    public static float strength() {
        return strength;
    }

    /**
     * 退服、换世界时清账。旧世界的眼槽与亮度都不作数了，留着会让新世界凭空亮着；
     * 处理器本身是常驻的，只把强度归零。
     */
    public static void reset() {
        eyeSlotEnabled = false;
        darknessActive = false;
        requested = false;
        strength = 0.0F;
    }
}
