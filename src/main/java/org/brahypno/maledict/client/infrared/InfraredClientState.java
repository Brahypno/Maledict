package org.brahypno.maledict.client.infrared;

/**
 * 微光视觉的客户端判断状态。整个特性只活在客户端：不同步、不落盘、不改世界光照。
 *
 * <p>两层分工，别混在一起：
 * <ul>
 *     <li>{@link #setEyeSlotEnabled} 只表达「眼槽这台设备开着」——第一格视界槽在，且它的渲染开关（Curios
 *     界面里那只眼睛）是亮的；槽空着也算开着，见 {@link InfraredEyeSlot}。</li>
 *     <li>{@link #setDarknessActive} 是亮度迟滞判决后的「环境够暗」。</li>
 * </ul>
 *
 * <p>这里<b>没有</b>强度字段。画面强度、淡入淡出与后处理链由 ChangeLib 的统一调度器持有，
 * 本模组只表达「要不要」，不表达「多强」——否则会出现两次平滑串联。
 */
public final class InfraredClientState {

    /** 「设备开着」：视界槽在，且那一格的渲染开关是亮的。 */
    private static boolean eyeSlotEnabled;

    /** 迟滞判决后的「环境够暗」。 */
    private static boolean darknessActive;

    /** 三条件合起来的最终结论：设备开着 + 环境够暗 + 没有夜视/伽马覆盖。只读快照，供调试与其他逻辑参考。 */
    private static boolean requested;

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

    /**
     * 退服、换世界时清账。旧世界的眼槽与亮度都不作数了，留着会让新世界凭空亮着。
     * 画面那一侧由 {@code ClientVisualEffects.remove(...)} 撤销，见 {@code InfraredClientHandler}。
     */
    public static void reset() {
        eyeSlotEnabled = false;
        darknessActive = false;
        requested = false;
    }
}
