package org.brahypno.maledict.common.entity;

/**
 * Immutable committed state shared by the three copies. No display values live here.
 *
 * <p>{@code gateOpensAt} 是我们那扇门：下一刀最早什么时候算数。它和原版的
 * {@code invulnerableTime} 是<b>串联</b>的两道判定——原版可以先拦，原版放行之后还要它放行，
 * 两条都过才真的扣血（见 {@code VicissitudeBossEntity#hurt}）。把开门时刻存在账本里，
 * 是为了命中节奏跟着存档走，而不是重新载入就又白送一刀。
 */
record VicissitudeVitality(float current, float maximum, long gateOpensAt,
                           int killAttempts, long lastKillTick) {
    VicissitudeVitality {
        if (!Float.isFinite(current) || !Float.isFinite(maximum) || maximum <= 0.0F
                || current < 0.0F || current > maximum || killAttempts < 0 || killAttempts > 3
                || (killAttempts == 3 && current != 0.0F)) {
            throw new IllegalArgumentException("Invalid Vicissitude vitality");
        }
    }

    static VicissitudeVitality initial(float maximum) {
        return new VicissitudeVitality(maximum, maximum, Long.MIN_VALUE, 0, Long.MIN_VALUE);
    }

    VicissitudeVitality withCurrent(float value) {
        return new VicissitudeVitality(value, maximum, gateOpensAt, killAttempts, lastKillTick);
    }

    /** 扣血：只动血量，门不归它管——门只由"算数的命中"关上，见 {@link #afterAcceptedHit}。 */
    VicissitudeVitality afterDamage(float amount) {
        return new VicissitudeVitality(Math.max(0.0F, current - amount), maximum,
                gateOpensAt, killAttempts, lastKillTick);
    }

    /** 记下这一刀：门关上 {@code interval} tick。 */
    VicissitudeVitality afterAcceptedHit(long now, int interval) {
        return new VicissitudeVitality(current, maximum, now + Math.max(1, interval),
                killAttempts, lastKillTick);
    }

    /** 门还关着吗？关着的时候这一刀不算数。 */
    boolean gateClosed(long now) {
        return now < gateOpensAt;
    }

    VicissitudeVitality killAttempt(long now, int window) {
        if (current <= 0.0F || now == lastKillTick) {
            return this;
        }
        int attempts = lastKillTick == Long.MIN_VALUE || now < lastKillTick
                || now - lastKillTick > Math.max(1, window) ? 1 : killAttempts + 1;
        return new VicissitudeVitality(attempts >= 3 ? 0.0F : current, maximum,
                gateOpensAt, attempts, now);
    }
}
