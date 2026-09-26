package org.brahypno.maledict.common.vitals;

/**
 * 延迟池在血条上盖住哪一段半心，以及某一颗心格被盖住多少。
 *
 * <p>纯数学、不碰客户端类型：半心的差一错误最容易出在这里 —— 区间**末尾**落在奇数半心上是
 * 「左半心」，区间**开头**落在奇数半心上是「右半心」，两种都要认。
 */
public final class DelayedVitalsHearts {

    /** 一颗心格被盖住多少。 */
    public enum Coverage {
        NONE,
        /** 只有左半心。 */
        LEFT_HALF,
        /** 只有右半心。 */
        RIGHT_HALF,
        FULL
    }

    private final int from;
    private final int to;

    private DelayedVitalsHearts(int from, int to) {
        this.from = from;
        this.to = to;
    }

    /** 待扣除：从「血量减掉待扣除」到当前血量，这一段正在离开血条。 */
    public static DelayedVitalsHearts pendingDamage(int healthHalfHearts, float pending) {
        return new DelayedVitalsHearts(
                Math.max(0, healthHalfHearts - halfHearts(pending)), healthHalfHearts);
    }

    /**
     * 待治疗：从当前血量往上，最多到血条顶端。
     *
     * <p>吸收心（黄心）画在**整条生命条之后**，不是紧接当前血量，所以这个上界天然不会盖到它们。
     */
    public static DelayedVitalsHearts pendingHeal(int healthHalfHearts, float pending, int barHalfHearts) {
        int from = Math.min(healthHalfHearts, barHalfHearts);
        return new DelayedVitalsHearts(from, Math.min(from + halfHearts(pending), barHalfHearts));
    }

    /** 不足一个半心的零头也画出来，否则池子快见底时那一段会突然消失。 */
    private static int halfHearts(float pending) {
        return pending <= 0.0F ? 0 : (int) Math.ceil(pending);
    }

    public boolean isEmpty() {
        return from >= to;
    }

    public int from() {
        return from;
    }

    public int to() {
        return to;
    }

    /** 下标为 {@code heart} 的心格被区间 {@code [from, to)} 盖住多少。 */
    public Coverage coverage(int heart) {
        int low = heart * 2;
        boolean left = low >= from && low < to;
        boolean right = low + 1 >= from && low + 1 < to;
        if (left && right) {
            return Coverage.FULL;
        }
        if (left) {
            return Coverage.LEFT_HALF;
        }
        return right ? Coverage.RIGHT_HALF : Coverage.NONE;
    }
}
