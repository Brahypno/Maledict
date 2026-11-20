package org.brahypno.maledict.client.item.weapon.incursus.rift;

import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.client.item.weapon.incursus.rift.patterns.AscensionSpinPattern;
import org.brahypno.maledict.client.item.weapon.incursus.rift.patterns.AscensionUppercutPattern;
import org.brahypno.maledict.client.item.weapon.incursus.rift.patterns.HiddenBladeFlurryPattern;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一裂隙渲染的纯几何层：锁住那些肉眼无法逐顶点复核、改错就会让形状、数量或节奏跑偏的性质。
 *
 * <p>四种布局的数值来自 Malum 1.20.1 的粒子公式，是兼容合同而不是可调参数，
 * 所以这里逐条把它们钉成断言。
 */
class RiftStrokeGeometryTest {

    private static final float FADE_START = 0.95F;

    @Test
    void orientationMatchesMalumPointyDirectionalQuad() {
        // 水平朝 +Z、无自旋：U 就是 +Z，V 落在水平垂线上——旧裂隙花正是这个特例。
        Vec3 u = RiftOrientation.axisU(new Vec3(0.0, 0.0, 2.0));
        Vec3 v = RiftOrientation.axisV(new Vec3(0.0, 0.0, 2.0), 0.0);
        assertEquals(0.0, u.x, 1.0E-9);
        assertEquals(1.0, u.z, 1.0E-9, "the U axis is the normalized direction");
        assertEquals(-1.0, v.x, 1.0E-9);
        assertEquals(0.0, v.z, 1.0E-9);

        // spinOffset 是绕四边形局部 X 轴的转动：+90° 把水平刀痕立起来。
        Vec3 vertical = RiftOrientation.axisV(new Vec3(0.0, 0.0, 2.0), Math.PI * 0.5);
        assertEquals(0.0, vertical.x, 1.0E-9);
        assertEquals(-1.0, vertical.y, 1.0E-9);

        Vec3[] directions = {
                new Vec3(1.0, 0.0, 0.0), new Vec3(0.6, 0.8, 0.0),
                new Vec3(0.3, -0.5, 0.8), new Vec3(0.0, 1.0, 0.0)
        };
        double[] spins = {0.0, 0.5, 1.57, -2.2, 3.14};
        for (Vec3 direction : directions) {
            for (double spin : spins) {
                Vec3 axisU = RiftOrientation.axisU(direction);
                Vec3 axisV = RiftOrientation.axisV(direction, spin);
                assertEquals(1.0, axisU.length(), 1.0E-9, "U must be a unit vector");
                assertEquals(1.0, axisV.length(), 1.0E-9, "V must be a unit vector");
                assertEquals(0.0, axisU.dot(axisV), 1.0E-9, "U and V must stay perpendicular");
                double recovered = RiftOrientation.spinOf(axisU, axisV);
                assertEquals(0.0, Math.atan2(Math.sin(recovered - spin), Math.cos(recovered - spin)), 1.0E-9,
                             "spinOf must invert axisV exactly");
            }
        }
    }

    @Test
    void malumLayoutsKeepTheOriginalStrokeCounts() {
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);

        List<RiftStroke> spin = new AscensionSpinPattern(1L, forward, 0.0F, false, FADE_START).generate();
        assertEquals(48, spin.size(), "ascension spin is 3 layers of 16, never fewer");
        assertEquals(0.0F, spin.get(0).startTick());
        assertEquals(3.0F, spin.get(0).lifetimeTicks());
        assertEquals(1.75, spin.get(0).offset().z, 1.0E-6, "the first stroke of the first layer points forward");
        assertEquals(1.75, spin.get(0).offset().length(), 1.0E-6, "the innermost ring sits 1.75 blocks out");
        RiftStroke spinTail = spin.get(spin.size() - 1);
        assertEquals(2 + 15 / 4, spinTail.startTick(), 1.0E-6, "lifeDelay is i + j/4 with integer division");
        assertEquals(5.0F, spinTail.lifetimeTicks());

        List<RiftStroke> uppercut = new AscensionUppercutPattern(1L, forward, 1.57F, true, FADE_START).generate();
        assertEquals(12, uppercut.size(), "the enhanced uppercut is 6 layers of 2");
        Vec3 first = uppercut.get(0).offset();
        assertEquals(2.0, first.z, 1.0E-6);
        assertEquals(0.0, first.y, 1.0E-6);
        Vec3 last = uppercut.get(uppercut.size() - 1).offset();
        assertEquals(-1.0, last.z, 1.0E-6, "slashOffset walks from 2 towards -1");
        assertEquals(2.0, last.y, 1.0E-6, "upwardsOffset climbs 0.4 per layer");
        assertEquals(2.0F, uppercut.get(uppercut.size() - 1).startTick(), 1.0E-6, "lifeDelay is i/2");

        RiftStroke leadingCut = uppercut.get(0);
        assertEquals(Vec3.ZERO, leadingCut.travel(0.75F), "the front cut holds its spawn position on birth");
        assertEquals(0.4, leadingCut.travel(1.5F).y, 1.0E-6, "interpolation starts after the first move");
        assertEquals(1.6, leadingCut.travel(3.0F).y, 1.0E-6, "Lodestone's slash velocity has no drag");
        assertEquals(0, leadingCut.maskFrame(1.75F, IncursusRiftGeometry.MASK_FRAMES));
        assertEquals(1, leadingCut.maskFrame(2.0F, IncursusRiftGeometry.MASK_FRAMES));
        assertTrue(leadingCut.visibleAt(3.5F), "the last interpolated movement is still visible");
        assertFalse(leadingCut.visibleAt(4.0F));

        List<RiftStroke> flurry = new HiddenBladeFlurryPattern(1L, forward, 3.14F, true, FADE_START).generate();
        assertEquals(300, flurry.size(), "the hidden blade flurry is 3 waves of 100, never reduced");
        float lastEnd = 0.0F;
        for (RiftStroke stroke : flurry) {
            lastEnd = Math.max(lastEnd, stroke.endTick());
        }
        assertEquals(24.0F, lastEnd, 1.0E-6, "the staggered flurry ends around tick 24, not 30");
    }

    @Test
    void patternsAreDeterministicForOneSeed() {
        Vec3 forward = new Vec3(0.2, 0.0, 1.0);
        assertEquals(new AscensionSpinPattern(42L, forward, 0.0F, false, FADE_START).generate(),
                     new AscensionSpinPattern(42L, forward, 0.0F, false, FADE_START).generate(),
                     "the same seed must rebuild the same strokes on every client");
        assertEquals(new HiddenBladeFlurryPattern(7L, forward, 1.0F, true, FADE_START).generate(),
                     new HiddenBladeFlurryPattern(7L, forward, 1.0F, true, FADE_START).generate());
        assertNotEquals(new AscensionSpinPattern(1L, forward, 0.0F, false, FADE_START).generate(),
                        new AscensionSpinPattern(2L, forward, 0.0F, false, FADE_START).generate(),
                        "a different seed must not reproduce the same sampling");
    }
}
