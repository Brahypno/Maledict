package org.brahypno.maledict.common.entity.vicissitude;

/**
 * Immutable committed state shared by the three copies. No display values live here.
 *
 * <p>The hit rhythm is deliberately absent: it is a ChangeLib time gate on the health binding, so it
 * is lived state rather than saved state. A reload therefore starts a fresh gate instead of
 * restoring a cadence nobody can observe while the entity is gone.
 */
record VicissitudeVitality(float current, float maximum, int killAttempts, long lastKillTick) {
    VicissitudeVitality {
        if (!Float.isFinite(current) || !Float.isFinite(maximum) || maximum <= 0.0F
            || current < 0.0F || current > maximum || killAttempts < 0 || killAttempts > 3
            || (killAttempts == 3 && current != 0.0F)){
            throw new IllegalArgumentException("Invalid Vicissitude vitality");
        }
    }

    static VicissitudeVitality initial(float maximum) {
        return new VicissitudeVitality(maximum, maximum, 0, Long.MIN_VALUE);
    }

    VicissitudeVitality withCurrent(float value) {
        return new VicissitudeVitality(value, maximum, killAttempts, lastKillTick);
    }

    VicissitudeVitality afterCanceledDeath() {
        return new VicissitudeVitality(Math.min(1.0F, maximum), maximum, 0, Long.MIN_VALUE);
    }

    /**
     * 扣血只动血量；这一击的节奏由绑定上的 ChangeLib 时序门在提交处自行记下。
     */
    VicissitudeVitality afterDamage(float amount) {
        return new VicissitudeVitality(Math.max(0.0F, current - amount), maximum,
                                       killAttempts, lastKillTick);
    }

    VicissitudeVitality killAttempt(long now, int window) {
        if (current <= 0.0F || now == lastKillTick){
            return this;
        }
        int attempts = lastKillTick == Long.MIN_VALUE || now < lastKillTick
                       || now - lastKillTick > Math.max(1, window) ? 1 : killAttempts + 1;
        return new VicissitudeVitality(attempts >= 3 ? 0.0F : current, maximum,
                                       attempts, now);
    }
}
