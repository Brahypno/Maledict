package org.brahypno.maledict.common.effect;

/**
 * 「献祭」的伤害预算：完整仪式共 1080 点，按效果实际消耗的时长比例结算。
 * 初始时长越短，同样一段效果进度对应的伤害越高；倒计时跳减也不能漏账。
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
     * 标准时长下每次抽走的生命，6 点即三颗心；等级不参与结算，仪式只发 I 级。
     */
    public static final float DAMAGE = 6.0F;

    /**
     * 标准仪式的总伤害，时长变化不会减少这笔代价。
     */
    public static final float TOTAL_DAMAGE = DAMAGE * DURATION_TICKS / INTERVAL_TICKS;

    /**
     * 当前进度应累计结算的伤害。保留第一 tick 预付一分钟的旧节奏；之后按服务器分钟出手，
     * 每次补到当前进度，最后不足一分钟的部分在自然到期时结清。
     */
    public static double targetDamage(int initialDuration, int remainingDuration) {
        if (initialDuration <= 0) {
            return 0.0D;
        }
        long elapsed = Math.max(0L, (long) initialDuration - Math.max(0, remainingDuration));
        long chargedTicks = Math.min(initialDuration, elapsed + INTERVAL_TICKS);
        return TOTAL_DAMAGE * (double) chargedTicks / initialDuration;
    }

    /** 血量以 float 存储，允许一次结算中的浮点舍入误差。 */
    public static boolean isComplete(double paidDamage) {
        return paidDamage >= TOTAL_DAMAGE - 0.001D;
    }

    private SacrificeSchedule() {
    }
}
