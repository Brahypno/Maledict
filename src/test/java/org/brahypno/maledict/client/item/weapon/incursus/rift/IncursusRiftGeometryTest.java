package org.brahypno.maledict.client.item.weapon.incursus.rift;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 裂隙数学层是纯函数，这组用例钉住三件事：同一挥砍在所有客户端形状一致（种子稳定）、
 * 外扩位移走原版粒子的摩擦积分而不是线性、以及刀痕的合法性校验与寿命求值。
 */
class IncursusRiftGeometryTest {
    @Test
    void petalShapeIsStableAndInsideTheAuthoredRanges() {
        long seed = IncursusRiftGeometry.petalSeed(42L, 3);
        assertEquals(seed, IncursusRiftGeometry.petalSeed(42L, 3));
        for (int salt = 1; salt <= 8; salt++) {
            float value = IncursusRiftGeometry.hash01(seed, salt);
            assertTrue(value >= 0.0F && value < 1.0F, "hash01 out of range: " + value);
        }
        IncursusRiftPetal petal = IncursusRiftPetal.derive(42L, 3, IncursusRiftGeometry.DEFAULT_PETAL_COUNT);
        IncursusRiftPetal again = IncursusRiftPetal.derive(42L, 3, IncursusRiftGeometry.DEFAULT_PETAL_COUNT);
        assertEquals(petal, again);
        // 旧效果：尺寸 0.8–1.2 倍、速度 0.857–1.143 倍、自旋恒 0。
        assertTrue(petal.sizeScale() >= 0.8F && petal.sizeScale() <= 1.2F);
        assertTrue(petal.speedScale() >= 0.857F && petal.speedScale() <= 1.143F);
        assertEquals(0.0F, petal.spin());
        // 八瓣严格 45° 等分。
        assertEquals(Math.PI / 4.0 * 3.0, petal.angle(), 1.0E-9);
    }

    @Test
    void radialDistanceFollowsTheParticleFrictionIntegral() {
        float speed = 0.35F;
        // 12 tick 后走完 v0 · (1 - 0.98^12) / 0.02，而不是线性或 v0 × 12。
        double expected = speed * (1.0 - Math.pow(0.98, 12)) / (1.0 - 0.98);
        assertEquals((float) expected, IncursusRiftGeometry.radialDistance(1.0F, 12, speed), 1.0E-4F);
        assertTrue(IncursusRiftGeometry.radialDistance(0.5F, 12, speed)
                   < IncursusRiftGeometry.radialDistance(1.0F, 12, speed));
        assertEquals(0.0F, IncursusRiftGeometry.radialDistance(0.0F, 12, speed), 1.0E-6F);
        // 生命进度被夹在 0..1，传入越界值也不放大。
        assertEquals(IncursusRiftGeometry.radialDistance(1.0F, 12, speed),
                     IncursusRiftGeometry.radialDistance(4.0F, 12, speed), 1.0E-6F);

        // 腰环的几何口径：末帧花瓣中心必须正好落在服务端的伤害半径上（画出来的圈 == 打得到的圈），
        // 而蓄力只准改薄厚与收尾，不许碰半径。
        double radius = 4.25;
        // 每瓣自己的漂移倍率只影响中途的快慢：末帧终点与 speedScale 无关。
        for (float speedScale : new float[]{0.857F, 1.0F, 1.143F}) {
            assertEquals(radius,
                         IncursusRiftGeometry.petalDistance(1.0F, 12, speed, speedScale, radius), 1.0E-6);
        }
        assertTrue(IncursusRiftGeometry.petalDistance(0.5F, 12, speed, 1.0F, radius) < radius,
                   "halfway through the life the petal is still inside the damage radius");
        // 半径 0（reach 被写成非有限值或负数）时整朵花不画。
        assertEquals(0.0, IncursusRiftGeometry.petalDistance(1.0F, 12, speed, 1.0F, 0.0), 0.0);
        assertEquals(0.0F, IncursusRiftGeometry.halfExtentForRadius(0.0, 2.5F), 0.0F);
        // 刀痕半边按「本次半径 / 基准半径」等比缩放。
        assertEquals((float) (2.5F * (radius / IncursusRiftGeometry.AUTHORED_RING_RADIUS)),
                     IncursusRiftGeometry.halfExtentForRadius(radius, 2.5F), 1.0E-6F);
        // 蓄力 → 薄厚：轻点 MIN_OPACITY，满蓄力 1，坏包按满蓄力（不该让效果凭空变淡）。
        assertEquals(IncursusRiftGeometry.MIN_OPACITY, IncursusRiftGeometry.opacityForCharge(0.0F), 1.0E-6F);
        assertEquals(1.0F, IncursusRiftGeometry.opacityForCharge(1.0F), 1.0E-6F);
        assertEquals(1.0F, IncursusRiftGeometry.opacityForCharge(Float.NaN), 1.0E-6F);
        // 蓄力 → 寿命：轻点更短，但不低于下限；配置本身已经更短时不会被再砍一刀。
        int shortened = IncursusRiftGeometry.lifetimeForCharge(12, 0.5F);
        assertTrue(shortened < 12 && shortened >= IncursusRiftGeometry.MIN_LIFETIME_TICKS,
                   "a half charged swing must end sooner, got " + shortened);
        assertEquals(12, IncursusRiftGeometry.lifetimeForCharge(12, 1.0F));
        assertEquals(2, IncursusRiftGeometry.lifetimeForCharge(2, 0.0F));
    }

    @Test
    void alphaAndFrameSelectionRespectTheFadeStartAndLifetime() {
        assertEquals(1.0F, IncursusRiftGeometry.alpha(0.5F, 0.95F));
        assertEquals(1.0F, IncursusRiftGeometry.alpha(0.95F, 0.95F));
        assertEquals(0.0F, IncursusRiftGeometry.alpha(1.0F, 0.95F), 1.0E-6F);
        assertEquals(0, IncursusRiftGeometry.maskFrame(0.0F, IncursusRiftGeometry.MASK_FRAMES));
        assertEquals(IncursusRiftGeometry.MASK_FRAMES - 1,
                     IncursusRiftGeometry.maskFrame(1.0F, IncursusRiftGeometry.MASK_FRAMES));
        assertEquals(IncursusRiftGeometry.MASK_FRAMES - 1,
                     IncursusRiftGeometry.maskFrame(9.0F, IncursusRiftGeometry.MASK_FRAMES));

        Vec3 axis = new Vec3(1.0, 0.0, 0.0);
        Vec3 axisV = new Vec3(0.0, 1.0, 0.0);
        RiftStroke stroke = new RiftStroke(Vec3.ZERO, axis, axisV, new Vec3(0.1, 0.0, 0.0),
                                           1.0F, 2.0F, 10.0F, 0.95F, 0.5F, 0.0F);
        // localAge 已经减掉出生延迟，所以可见区间从 0 开始；上界是 lifetime + 1（最后一帧）。
        assertFalse(stroke.visibleAt(-0.1F));
        assertTrue(stroke.visibleAt(0.0F));
        // 原版粒子在 age == lifetime 时还能画一帧，所以可见区间是 [0, lifetime + 1)。
        assertTrue(stroke.visibleAt(10.9F));
        assertFalse(stroke.visibleAt(11.0F));
        assertFalse(stroke.visibleAt(11.9F));
        assertEquals(12.0F, stroke.endTick());
        assertEquals(0.0, stroke.travel(1.0F).lengthSqr(), 1.0E-9);
        assertEquals(0.5, stroke.travel(6.0F).x, 1.0E-9);
        assertEquals(0.0F, RiftStroke.variant(3, 1));
        assertEquals(1.0F, RiftStroke.variant(3, 4), 1.0E-6F);
        // 退化基向量必须被挡住，否则画出来的四边形是零面积。
        assertThrows(IllegalArgumentException.class, () -> new RiftStroke(
                Vec3.ZERO, Vec3.ZERO, axisV, Vec3.ZERO, 1.0F, 0.0F, 10.0F, 0.95F, 0.0F, 0.0F));
    }
}
