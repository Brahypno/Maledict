package org.brahypno.maledict.common.realm.demiurge;

/** Server timeline and collision envelope for the authored five-second awakening. */
public final class DemiurgeAwakening {
    public static final int DURATION = 100;
    public static final int ROAR_TICK = 64;
    public static final byte ROAR_EVENT = 61;

    public static float rise(float age) {
        float x = Math.max(0, Math.min(1, (age - 12) / 46));
        return x * x * x * (x * (x * 6 - 15) + 10);
    }

    public static float height(float age) {
        return 4.2F + 4.2F * rise(age);
    }

    private DemiurgeAwakening() {}
}
