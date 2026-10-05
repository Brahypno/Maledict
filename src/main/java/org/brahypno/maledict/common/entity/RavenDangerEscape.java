package org.brahypno.maledict.common.entity;

/** Leave environmental danger once per ignition; lingering burn ticks do not prevent normal AI forever. */
public final class RavenDangerEscape {
    private boolean requested;
    private boolean handledFire;
    private boolean escaping;

    public void updateBurning(boolean burning) {
        if (!burning) {
            handledFire = false;
        }
    }

    public void onDamage(boolean environmental, boolean burning) {
        if (!escaping && environmental && (!burning || !handledFire)) {
            requested = true;
        }
    }

    public boolean shouldEscape(boolean touchingDanger, boolean burning) {
        return touchingDanger || requested || (burning && !handledFire);
    }

    public void begin(boolean burning) {
        escaping = true;
        requested = false;
        whileEscaping(burning);
    }

    public void whileEscaping(boolean burning) {
        handledFire |= burning;
    }

    public void finish() {
        escaping = false;
    }

    public static boolean reachedSafety(boolean touchingDanger, double distanceFromDangerSquared) {
        return !touchingDanger && distanceFromDangerSquared >= 9.0D;
    }
}
