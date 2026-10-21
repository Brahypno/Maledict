package org.brahypno.maledict.common.effect;

/**
 * 「献祭」共出手 180 次，每次固定 6 点。九天九夜内均匀安排出手，
 * 缩短时长或加速倒计时只会提高出手频率，不改变每次伤害。
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
     * 仪式请求的标准时长，216000 tick；实际施加时可以被其它机制缩短。
     */
    public static final int DURATION_TICKS = DAYS * TICKS_PER_DAY;

    /**
     * 出手间隔：一分钟。
     */
    public static final int INTERVAL_TICKS = 1200;

    /**
     * 标准时长下每次打出的原始伤害为 6 点；等级不参与结算，仪式只发 I 级。
     */
    public static final float DAMAGE = 6.0F;

    public static final int PULSE_COUNT = DURATION_TICKS / INTERVAL_TICKS;
    public static final float TOTAL_DAMAGE = DAMAGE * PULSE_COUNT;

    /**
     * 当前倒计时进度应已出手的次数。第一 tick 出手一次；半时长每 600 tick 一次，
     * 默认每 1200 tick 一次。跳过的出手节点仍逐次打出固定伤害。
     */
    public static int targetPulses(int initialDuration, int remainingDuration) {
        if (initialDuration <= 0) {
            return 0;
        }
        long elapsed = Math.max(0L, (long) initialDuration - Math.max(0, remainingDuration));
        return (int) Math.min(PULSE_COUNT, elapsed * PULSE_COUNT / initialDuration + 1);
    }

    private SacrificeSchedule() {
    }
}
