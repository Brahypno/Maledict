package org.brahypno.maledict.common.realm.demiurge;

/** Shared attack beats for AI, exported animation and client presentation. */
public enum DemiurgeAttack {
    NONE(0, 0, 0),
    CHARGE(64, 20, 44),
    LASER(100, 24, 84),
    ROAR(110, 20, 90);

    public static final int COOLDOWN = 40;
    public static final int SILENCE_TICKS = 100;
    public static final int LASER_INTERVAL = 10;
    public static final double LASER_RANGE = 40;
    public static final double RAIN_RADIUS = 12;
    public final int duration;
    public final int release;
    public final int recovery;

    DemiurgeAttack(int duration, int release, int recovery) {
        this.duration = duration;
        this.release = release;
        this.recovery = recovery;
    }

    public boolean active(float age) {
        return this != NONE && age >= release && age < recovery;
    }

    public float blend(float age) {
        if (this == NONE || age < 0 || age >= duration) return 0;
        float x = Math.min(1, Math.min(age / 8, (duration - age) / 8));
        return x * x * (3 - 2 * x);
    }
}
