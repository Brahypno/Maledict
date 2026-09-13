package org.brahypno.maledict.common.entity;

/**
 * Authored encounter stages. DORMANT means "phase one silhouette, but the timer has not started
 * yet"; death can interrupt any stage and DYING never returns to a living stage.
 */
public enum VicissitudeBossStage {
    DORMANT,
    PHASE_ONE,
    TRANSITION,
    PHASE_TWO,
    DYING;

    private static final VicissitudeBossStage[] VALUES = values();

    public static VicissitudeBossStage byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : DORMANT;
    }
}
