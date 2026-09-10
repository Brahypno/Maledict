package org.brahypno.maledict.common.entity;

/** Immutable committed state shared by the three copies. No display values live here. */
record VicissitudeVitality(float current, float maximum, long nextHit, int killAttempts, long lastKillTick) {
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
        return new VicissitudeVitality(value, maximum, nextHit, killAttempts, lastKillTick);
    }

    VicissitudeVitality afterDamage(float amount, long now, int interval) {
        return new VicissitudeVitality(Math.max(0.0F, current - amount), maximum,
                now + Math.max(1, interval), killAttempts, lastKillTick);
    }

    VicissitudeVitality killAttempt(long now, int window) {
        if (current <= 0.0F || now == lastKillTick) {
            return this;
        }
        int attempts = lastKillTick == Long.MIN_VALUE || now < lastKillTick
                || now - lastKillTick > Math.max(1, window) ? 1 : killAttempts + 1;
        return new VicissitudeVitality(attempts >= 3 ? 0.0F : current, maximum, nextHit, attempts, now);
    }
}
