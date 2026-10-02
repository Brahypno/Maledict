package org.brahypno.maledict.client.infrared;

/**
 * 微光视觉的两段纯算术：亮度迟滞与强度推进。
 *
 * <p>抽成不碰 Minecraft 类的纯函数，是为了让阈值语义与过渡时长能被 {@code InfraredCurveTest}
 * 直接断言——读相机、读槽位、写 uniform 的部分在 {@link InfraredClientHandler} 与
 * {@link InfraredPostProcessor}，它们只能在游戏里验。
 */
final class InfraredCurve {

    /** 亮度 <= 6 时允许进入微光模式。 */
    static final int ENABLE_LIGHT = 6;

    /** 亮度 >= 8 时退出；中间的 7 保持现状，这就是迟滞区。 */
    static final int DISABLE_LIGHT = 8;

    /** 强度过渡的时间常数（秒）：约 0.36 秒走完 95%。 */
    static final float TRANSITION_TAU_SECONDS = 0.12F;

    /** 强度低于这个值就当作已经关掉：后处理要等到淡出彻底看不见之后才停。 */
    static final float ACTIVE_EPSILON = 0.001F;

    /** 迟滞：进来要够暗，出去要够亮，中间那格保持现状，免得站在阈值上闪。 */
    static boolean darknessAfter(boolean active, int brightness) {
        return active ? brightness < DISABLE_LIGHT : brightness <= ENABLE_LIGHT;
    }

    /**
     * 指数逼近目标：{@code alpha = 1 - e^(-dt/τ)}。按真实帧时长算，掉帧时不会比高帧率时淡得慢，
     * 而且逐帧叠出来的结果与整段一次性算出来的完全一致（指数是可乘的）。
     *
     * <p>淡出不会真的到 0，所以低于 {@link #ACTIVE_EPSILON} 时直接归零。
     */
    static float approach(float current, float target, float deltaSeconds) {
        float step = Math.max(0.0F, deltaSeconds);
        float alpha = (float) (1.0D - Math.exp(-step / TRANSITION_TAU_SECONDS));
        float next = current + (target - current) * alpha;
        if (target <= 0.0F && next < ACTIVE_EPSILON) {
            return 0.0F;
        }
        return next;
    }

    private InfraredCurve() {
    }
}
