package org.brahypno.maledict.client.item.weapon.incursus.rift;

import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftGeometry;
import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftPetal;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusSwingReach;
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
    void petalCentresEndOnTheSwingRadius() {
        float radius = 3.5F;
        float speed = 0.35F;
        // 末帧的花瓣中心圆就是伤害球的赤道：开局贴着花心，收尾落在半径上。
        assertEquals(radius, IncursusRiftGeometry.petalDistance(1.0F, 12, speed, 1.0F, radius), 1.0E-4,
                     "the last frame must put the petal centre on the damage radius");
        double opening = IncursusRiftGeometry.petalDistance(0.0F, 12, speed, 1.0F, radius);
        assertTrue(opening > 0.0 && opening < radius * 0.2,
                   "the bloom must start near the centre, got " + opening);
        assertTrue(IncursusRiftGeometry.petalDistance(0.5F, 12, speed, 1.0F, radius)
                   < IncursusRiftGeometry.petalDistance(0.9F, 12, speed, 1.0F, radius),
                   "the ring must keep opening outward");
        // 每瓣自己的漂移倍率只影响中途，终点一律严格落在伤害半径上。
        for (float speedScale : new float[]{0.857F, 1.0F, 1.143F}) {
            assertEquals(radius, IncursusRiftGeometry.petalDistance(1.0F, 12, speed, speedScale, radius),
                         1.0E-4, "every petal centre must end exactly on the damage radius");
        }
    }

    @Test
    void degenerateRingInputsCollapseInsteadOfExploding() {
        // 漂移被调成 0 时环不会缩进花心，而是从头到尾贴在伤害边界上。
        assertEquals(3.5, IncursusRiftGeometry.petalDistance(0.5F, 12, 0.0F, 1.0F, 3.5), 1.0E-4,
                     "a still ring must sit on the radius");
        // 半径 0 / 非有限（reach 被写坏）：整朵花不画，也不产出 NaN 顶点。
        assertEquals(0.0, IncursusRiftGeometry.petalDistance(1.0F, 12, 0.35F, 1.0F, 0.0), 0.0);
        assertEquals(0.0, IncursusRiftGeometry.petalDistance(1.0F, 12, 0.35F, 1.0F, Double.NaN), 0.0);
        assertEquals(0.0, IncursusRiftGeometry.petalDistance(1.0F, 12, 0.35F, 1.0F, Double.POSITIVE_INFINITY), 0.0);
    }

    @Test
    void strokeSizeScalesWithTheSwingRadius() {
        float baseHalf = 3.75F;
        // 默认半径下退回这套贴图当初定尺寸时的大小。
        assertEquals(baseHalf,
                     IncursusRiftGeometry.halfExtentForRadius(IncursusRiftGeometry.AUTHORED_RING_RADIUS, baseHalf),
                     1.0E-4, "the authored radius must reproduce the authored stroke size");
        assertEquals(baseHalf * 2.0F, IncursusRiftGeometry.halfExtentForRadius(
                IncursusRiftGeometry.AUTHORED_RING_RADIUS * 2.0, baseHalf), 1.0E-3,
                     "a wider ring must widen the strokes with it");
        assertEquals(0.0F, IncursusRiftGeometry.halfExtentForRadius(0.0, baseHalf), 0.0F,
                     "no radius means no stroke");
        // 6 格上限下的环仍然足够密：八道刀痕之间的弧长小于四边形宽度。
        double capped = IncursusSwingReach.MAX_RADIUS;
        double arc = Math.PI * 2.0 * capped / IncursusRiftGeometry.DEFAULT_PETAL_COUNT;
        assertTrue(arc < IncursusRiftGeometry.halfExtentForRadius(capped, baseHalf) * 2.0,
                   "eight strokes must still overlap at the reach cap, arc=" + arc);
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
