package org.brahypno.maledict.common.realm.demiurge;

/** Shared attack beats for AI, exported animation and client presentation. */
public enum DemiurgeAttack {
    NONE(0, 0, 0),
    CHARGE(64, 20, 44),
    LASER(100, 24, 84),
    ROAR(110, 20, 90),
    /** The mid/rear body swings sideways; 0-15 windup, 16-27 damage, 28-49 retract. */
    SWEEP(50, 16, 28),
    /** Raise an inner ring and an outer half-ring of stone pillars; each pillar is independent. */
    FALSE_CREATION(48, 14, 40);

    public static final int SILENCE_TICKS = 100;
    public static final int LASER_INTERVAL = 10;
    public static final double LASER_RANGE = 40;
    /** Shared by the roar's "target in range" check and the locked rain disk. */
    public static final double RAIN_RADIUS = 10;
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
        return smooth(age / release) * smooth((duration - age) / (duration - recovery));
    }

    public float chargeSpeed(float age) {
        if (this != CHARGE || age < release || age >= recovery + 10) return 0;
        return .8F * smooth((age - release + 1) / 5)
                * (1 - smooth((age - recovery) / 10));
    }

    private static float smooth(float x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * x * (x * (x * 6 - 15) + 10);
    }
}
