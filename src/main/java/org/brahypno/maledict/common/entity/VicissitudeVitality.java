package org.brahypno.maledict.common.entity;

/**
 * Immutable committed state shared by the three copies. No display values live here.
 *
 * <p>{@code gateOpensAt} is the second gate, in series with vanilla {@code invulnerableTime};
 * it is persisted so the hit rhythm follows the save.
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

    /** 扣血只动血量，不动门；门只由 {@link #afterAcceptedHit} 关。 */
    VicissitudeVitality afterDamage(float amount) {
        return new VicissitudeVitality(Math.max(0.0F, current - amount), maximum,
                gateOpensAt, killAttempts, lastKillTick);
    }

    VicissitudeVitality afterAcceptedHit(long now, int interval) {
        return new VicissitudeVitality(current, maximum, now + Math.max(1, interval),
                killAttempts, lastKillTick);
    }

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
