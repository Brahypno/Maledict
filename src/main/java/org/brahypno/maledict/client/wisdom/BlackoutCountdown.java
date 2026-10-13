package org.brahypno.maledict.client.wisdom;

/** 与微光视觉和原版效果独立的黑幕倒计时；最后一秒按帧插值淡出。 */
final class BlackoutCountdown {
    private static final int FADE_OUT_TICKS = 20;
    private int remainingTicks;

    void start(int ticks) {
        remainingTicks = ticks;
    }

    void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    boolean isActive() {
        return remainingTicks > 0;
    }

    /** 六十 tick 总时长：前四十 tick 全黑，余下二十 tick 从不透明线性淡到透明。 */
    float opacity(float partialTick) {
        float remaining = remainingTicks - Math.max(0.0F, Math.min(1.0F, partialTick));
        return Math.max(0.0F, Math.min(1.0F, remaining / FADE_OUT_TICKS));
    }
}
