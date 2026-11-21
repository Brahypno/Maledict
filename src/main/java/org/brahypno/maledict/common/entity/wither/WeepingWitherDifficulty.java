package org.brahypno.maledict.common.entity.wither;

import net.minecraft.world.Difficulty;

/**
 * Health ladder for the weeping wither. The pool is picked once, when the entity first joins the
 * world, and stored on the entity the way the demiurge keeps its own difficulty; the per-hit cap
 * keeps reading the live maximum, so a bigger pool also raises the cap.
 */
public final class WeepingWitherDifficulty {
    public static float maxHealth(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL, EASY -> 300;
            case NORMAL -> 400;
            case HARD -> 500;
        };
    }

    private WeepingWitherDifficulty() {
    }
}
