package org.brahypno.maledict.common.ritual.wisdom;

/**
 * 「献祭」的时间表：标准九天九夜共 180 次，每次固定 6 点。
 * 实际时长缩短或倒计时加速只改变出手频率，不改变单次伤害，也不统计实际受伤量。
 *
 * <p>渡鸦取眼会提前收账：把还没落下的次数折成一次结算，见 {@link #remainingDamage(int)}。
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
     * 固定单次伤害，6 点即三颗心；等级不参与结算，仪式只发 I 级。
     */
    public static final float DAMAGE = 6.0F;

    /**
     * 完整仪式的触发次数，时长变化只压缩这些触发之间的间隔。
     */
    public static final int PULSES = DURATION_TICKS / INTERVAL_TICKS;

    /**
     * 还没落下的次数。已触发数超了总数（加速后连跳）或有负数时按边界兜底，不会算出负伤害。
     */
    public static int remainingPulses(int pulses) {
        return Math.max(0, Math.min(PULSES, PULSES - pulses));
    }

    /**
     * 把剩下的次数合成一次结算的伤害：剩余次数 × 固定单次伤害，标准仪式是 1080 点。
     *
     * <p>渡鸦啄眼用这个数；它不改变单次伤害，只是把「还要挨多少下」折成一笔。
     */
    public static float remainingDamage(int pulses) {
        return remainingPulses(pulses) * DAMAGE;
    }

    /**
     * 当前效果进度应触发的累计次数。第一 tick 就出手，之后将其余触发均匀分布在实际时长中。
     * 使用整数进度，不依赖剩余时长恰好命中整除节点；跳过的节点也能补上。
     */
    public static int targetPulses(int initialDuration, int remainingDuration) {
        if (initialDuration <= 0){
            return 0;
        }
        long elapsed = Math.max(0L, (long) initialDuration - Math.max(0, remainingDuration));
        return (int) Math.min(PULSES, 1L + elapsed * PULSES / initialDuration);
    }

    private SacrificeSchedule() {
    }
}
