package org.brahypno.maledict.common.entity;

/** Authored encounter stages; DORMANT is phase one with the timer not started, and DYING is terminal. */
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
