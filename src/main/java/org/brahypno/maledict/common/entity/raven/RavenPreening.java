package org.brahypno.maledict.common.entity.raven;

/** One feather roll per gathering, after three uninterrupted seconds of grounded preening. */
final class RavenPreening {
    private static final int PREEN_TICKS = 60;
    private int ticks;
    private boolean rolled;

    boolean tick(boolean preening) {
        if (!preening) {
            ticks = 0;
            return false;
        }
        if (rolled || ++ticks < PREEN_TICKS) {
            return false;
        }
        rolled = true;
        return true;
    }

    void reset() {
        ticks = 0;
        rolled = false;
    }
}
