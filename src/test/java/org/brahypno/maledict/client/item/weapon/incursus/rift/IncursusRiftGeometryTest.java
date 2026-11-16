package org.brahypno.maledict.client.item.weapon.incursus.rift;

import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftGeometry;
import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftPetal;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 裂隙花的纯数学层：只锁那些肉眼难以复核、但改错就会让形状或节奏跑偏的性质。
 */
class IncursusRiftGeometryTest {

    @Test
    void petalSeedsAreStableAndDistinct() {
        long swing = 0x5DEECE66DL;
        Set<Long> seeds = new HashSet<>();
        for (int i = 0; i < IncursusRiftGeometry.DEFAULT_PETAL_COUNT; i++) {
            long seed = IncursusRiftGeometry.petalSeed(swing, i);
            assertEquals(seed, IncursusRiftGeometry.petalSeed(swing, i), "same swing and index must repeat");
            seeds.add(seed);
        }
        assertEquals(IncursusRiftGeometry.DEFAULT_PETAL_COUNT, seeds.size(), "eight petals need eight seeds");
        assertNotEquals(IncursusRiftGeometry.petalSeed(swing, 0), IncursusRiftGeometry.petalSeed(swing + 1, 0),
                        "a different swing must not reuse the same petals");
    }

    @Test
    void hashedValuesStayInRange() {
        for (int salt = 0; salt < 16; salt++) {
            float value = IncursusRiftGeometry.hash01(0x1234ABCDL, salt);
            assertTrue(value >= 0.0F && value < 1.0F, "hash01 must stay in [0,1), got " + value);
        }
    }

    @Test
    void petalsSpreadOverFullCircleWithTinyStagger() {
        int petals = IncursusRiftGeometry.DEFAULT_PETAL_COUNT;
        double expected = Math.PI * 2.0 / petals;
        for (int i = 0; i < petals; i++) {
            IncursusRiftPetal petal = IncursusRiftPetal.derive(0x77L, i, petals);
            double plain = IncursusRiftGeometry.petalAngle(i, petals, 0.0F);
            assertEquals(i * expected, plain, 1.0E-9, "plain angle must be exactly 45 degrees apart");
            assertTrue(Math.abs(petal.angle() - plain) <= IncursusRiftPetal.MAX_PHASE_OFFSET + 1.0E-6,
                       "phase stagger must stay subtle, got " + (petal.angle() - plain));
        }
    }

    @Test
    void alphaHoldsThenFades() {
        float fadeStart = 0.95F;
        assertEquals(1.0F, IncursusRiftGeometry.alpha(0.0F, fadeStart), 1.0E-6,
                     "the old particles were fully opaque from their first tick");
        assertEquals(1.0F, IncursusRiftGeometry.alpha(0.55F, fadeStart), 1.0E-6,
                     "nothing fades before fadeStart");
        assertEquals(1.0F, IncursusRiftGeometry.alpha(fadeStart, fadeStart), 1.0E-6);
        assertEquals(0.0F, IncursusRiftGeometry.alpha(1.0F, fadeStart), 1.0E-6);
        assertTrue(IncursusRiftGeometry.alpha(0.975F, fadeStart) < 1.0F,
                   "past the fade start the rift must actually fade");
    }

    @Test
    void maskFramesSplitTheLifetimeEvenly() {
        assertEquals(0, IncursusRiftGeometry.maskFrame(0.0F, 5));
        assertEquals(0, IncursusRiftGeometry.maskFrame(0.19F, 5));
        assertEquals(1, IncursusRiftGeometry.maskFrame(0.20F, 5));
        assertEquals(4, IncursusRiftGeometry.maskFrame(0.99F, 5));
        assertEquals(4, IncursusRiftGeometry.maskFrame(1.0F, 5), "the last frame must hold at the end");
    }

    @Test
    void radialTravelMatchesTheVanillaFrictionIntegral() {
        // 旧粒子就是原版 Particle.tick()：每 tick pos += v，随后 v *= 0.98。
        float speed = 0.35F;
        float total = IncursusRiftGeometry.radialDistance(1.0F, 12, speed);
        float expected = (float) (speed * (1.0 - Math.pow(0.98, 12)) / (1.0 - 0.98));
        assertEquals(expected, total, 1.0E-5, "the whole life must match the damped integral");
        assertEquals(0.0F, IncursusRiftGeometry.radialDistance(0.0F, 12, speed), 1.0E-6);
        float halfway = IncursusRiftGeometry.radialDistance(0.5F, 12, speed);
        assertTrue(halfway > total * 0.5F,
                   "damping means the first half of the life already covers more than half the distance");
        assertTrue(total < speed * 12,
                   "without damping the petal would travel speed times lifetime, which the old one never did");
    }

    @Test
    void openEnvelopeAndHalfExtentStayConstant() {
        assertEquals(1.0F, IncursusRiftGeometry.openEnvelope(0.0F), 1.0E-6);
        assertEquals(1.0F, IncursusRiftGeometry.openEnvelope(1.0F), 1.0E-6);
        assertEquals(3.75F, IncursusRiftGeometry.halfExtent(0.6F, 3.75F), 1.0E-6,
                     "the old quadSize never grew or shrank over the lifetime");
    }

    @Test
    void petalVariationCoversTheOldRandomRanges() {
        float minSize = Float.MAX_VALUE;
        float maxSize = -Float.MAX_VALUE;
        float minSpeed = Float.MAX_VALUE;
        float maxSpeed = -Float.MAX_VALUE;
        for (int index = 0; index < 64; index++) {
            IncursusRiftPetal petal = IncursusRiftPetal.derive(0x5EEDL + index, index, 8);
            minSize = Math.min(minSize, petal.sizeScale());
            maxSize = Math.max(maxSize, petal.sizeScale());
            minSpeed = Math.min(minSpeed, petal.speedScale());
            maxSpeed = Math.max(maxSpeed, petal.speedScale());
        }
        assertTrue(minSize >= 0.8F && maxSize <= 1.2F, "size must stay inside 0.8x-1.2x of the base");
        assertTrue(minSpeed >= 0.857F - 1.0E-4F && maxSpeed <= 1.143F + 1.0E-4F,
                   "speed must stay inside 0.3-0.4 blocks per tick at the 0.35 base");
        assertTrue(maxSize - minSize > 0.2F, "the petals must not all come out the same size");
    }
}
