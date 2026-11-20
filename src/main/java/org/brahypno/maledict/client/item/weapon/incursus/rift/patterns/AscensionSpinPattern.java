package org.brahypno.maledict.client.item.weapon.incursus.rift.patterns;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftGeometry;
import org.brahypno.maledict.client.item.weapon.incursus.rift.RiftOrientation;
import org.brahypno.maledict.client.item.weapon.incursus.rift.RiftStroke;
import org.brahypno.maledict.client.item.weapon.incursus.rift.RiftStrokePattern;
import team.lodestar.lodestone.helpers.RandomHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 飞腾普通旋斩：Malum {@code AscensionRadialSlashParticleEffect} 的 3 层 × 16 道 = 48 道。
 *
 * <p>数值全部照抄 Malum 1.20.1 对应源码，是兼容合同而不是可调参数：圆周角
 * {@code (i·0.33 + j) / 16 · 2π}、半径 1.75、尺寸 2.5–3、速度 0.2–0.4 加 0.8 上飘、
 * 寿命 3+i、延迟 {@code i + j/4}（整数除法）。{@code 0.33} 不能换成 1 或 3，
 * 否则三层会退成互相重合的圆。
 *
 * <p>随机流由报文种子在客户端重建，抽样范围、奇偶分支与调用顺序都按原版：
 * 先 spinOffset，再尺寸，最后速度。保证的是"公式与分布一致、同种子跨客户端一致"，
 * 而不是与某次 Malum 世界 RNG 逐点相同。
 */
public final class AscensionSpinPattern implements RiftStrokePattern {

    private static final int LAYERS = 3;
    private static final int PER_LAYER = 16;
    /**
     * 原版 {@code slashDirection.scale(1.75f)}：每一层都是半径 1.75 格的圆。
     */
    private static final float RING_RADIUS = 1.75F;

    private final long seed;
    private final Vec3 direction;
    private final float baseAngle;
    private final boolean mirrored;
    private final float fadeStart;

    public AscensionSpinPattern(
            long seed, Vec3 direction, float baseAngle, boolean mirrored, float fadeStart) {
        this.seed = seed;
        this.direction = RiftOrientation.motionDirection(direction);
        this.baseAngle = baseAngle;
        this.mirrored = mirrored;
        this.fadeStart = fadeStart;
    }

    public static int strokeCount() {
        return LAYERS * PER_LAYER;
    }

    @Override
    public List<RiftStroke> generate() {
        RandomSource random = RandomSource.create(seed);
        double yaw = Math.atan2(direction.x, direction.z);
        Vec3 left = new Vec3(-Math.cos(yaw), 0.0, Math.sin(yaw));
        int count = strokeCount();
        List<RiftStroke> strokes = new ArrayList<>(count);
        for (int i = 0; i < LAYERS; i++) {
            for (int j = 0; j < PER_LAYER; j++) {
                int index = strokes.size();
                float spinOffset = baseAngle + RandomHelper.randomBetween(random, -0.5F, 0.5F)
                                   + (mirrored ? 3.14F : 0.0F);
                float slashAngle = (i * 0.33F + j) / 16.0F * (float) (Math.PI * 2.0);
                // 原版先把左向量与方向按正弦余弦合成再归一化：方向长度不足 1 时环会略微收窄，
                // 这里保持同样的合成顺序，不做提前归一化。
                Vec3 slashDirection = RiftOrientation.normalizedOr(
                        left.scale(Math.sin(slashAngle)).add(direction.scale(Math.cos(slashAngle))), left);
                float size = RandomHelper.randomBetween(random, 2.5F, 3.0F);
                Vec3 velocity = slashDirection.scale(RandomHelper.randomBetween(random, 0.2F, 0.4F))
                                             .add(0.0, 0.8F, 0.0);
                strokes.add(new RiftStroke(
                        slashDirection.scale(RING_RADIUS),
                        RiftOrientation.axisU(slashDirection),
                        RiftOrientation.axisV(slashDirection, spinOffset),
                        velocity, size,
                        i + j / 4, 3 + i, fadeStart,
                        IncursusRiftGeometry.hash01(seed, index),
                        RiftStroke.variant(index, count)));
            }
        }
        return strokes;
    }
}
