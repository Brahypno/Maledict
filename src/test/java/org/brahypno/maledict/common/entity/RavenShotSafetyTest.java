package org.brahypno.maledict.common.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenShotSafetyTest {
    private static final AABB COW = new AABB(3, 0, -0.5D, 4, 1.5D, 0.5D);
    private static final Vec3 ENEMY = new Vec3(8, 1, 0);

    @Test
    void feathersCanRetaliateAgainstAnAttackerButCannotHurtUnrelatedAnimalsOrFriends() {
        assertTrue(RavenShotSafety.mayHit(true, false, false, false));
        assertTrue(RavenShotSafety.mayHit(false, true, false, false));
        assertFalse(RavenShotSafety.mayHit(false, false, false, false));
        assertFalse(RavenShotSafety.mayHit(false, true, true, false));
        assertFalse(RavenShotSafety.mayHit(false, true, false, true));
    }

    @Test
    void cowBetweenRavenAndZombieBlocksShotUntilRavenFliesAsideOrAbove() {
        assertTrue(RavenShotSafety.crossesBody(COW, new Vec3(0, 1, 0), ENEMY));
        assertFalse(RavenShotSafety.crossesBody(COW, new Vec3(0, 1, 3), ENEMY));
        assertFalse(RavenShotSafety.crossesBody(COW, new Vec3(0, 4, 0), ENEMY));
    }

    @Test
    void checkIncludesTheProjectilesCollisionMargin() {
        assertTrue(RavenShotSafety.crossesBody(COW, new Vec3(0, 1, 0.75D), new Vec3(8, 1, 0.75D)));
        assertFalse(RavenShotSafety.crossesBody(COW, new Vec3(0, 1, 0.81D), new Vec3(8, 1, 0.81D)));
    }

    @Test
    void overlappingBystanderBlocksButABystanderBeyondTheTargetDoesNot() {
        assertTrue(RavenShotSafety.crossesBody(COW, new Vec3(3.5D, 1, 0), ENEMY));
        assertFalse(RavenShotSafety.crossesBody(new AABB(9, 0, -0.5D, 10, 1.5D, 0.5D),
                new Vec3(0, 1, 0), ENEMY));
    }
}
