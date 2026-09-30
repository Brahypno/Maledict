package org.brahypno.maledict.common.effect;

/**
 * 「献祭」的时间表：九天九夜与每分钟一次的算术。单独放一个不碰 Minecraft 的类，是为了能单测钉住它。
 *
 * <p>原版 {@code MobEffectInstance#tick} 是「先按当前剩余时长判定、再递减」，所以时长 216000 的当 tick 就
 * 判定第一次，此后每 1200 tick 一次，到剩余 1200 为止共 {@link #pulses()} 次；最后 1200 tick 不再出手，
 * 只把时长耗完，归零那一刻由 Forge 的 {@code MobEffectEvent.Expired} 发成就。
 */
public final class SacrificeSchedule {
    /**
     * 一游戏日 24000 tick。
     */
    public static final int TICKS_PER_DAY = 24000;

    /**
     * 九天九夜。
     */
    public static final int DAYS = 9;

    /**
     * 效果本身的总时长，216000 tick；走完就是「度过九天九夜」。
     */
    public static final int DURATION_TICKS = DAYS * TICKS_PER_DAY;

    /**
     * 出手间隔：一分钟。
     */
    public static final int INTERVAL_TICKS = 1200;

    /**
     * 每次抽走的生命，6 点即三颗心；等级不参与结算，仪式只发 I 级。
     */
    public static final float DAMAGE = 6.0F;

    /**
     * 剩余时长为 {@code duration} 的这 tick 是否出手；原版按 {@code duration % n == 0} 判定。
     */
    public static boolean isPulse(int duration) {
        return duration > 0 && duration % INTERVAL_TICKS == 0;
    }

    /**
     * 走完九天九夜一共抽几次：一天 20 分钟、一分钟一下。
     */
    public static int pulses() {
        return DURATION_TICKS / INTERVAL_TICKS;
    }

    private SacrificeSchedule() {
    }
}
