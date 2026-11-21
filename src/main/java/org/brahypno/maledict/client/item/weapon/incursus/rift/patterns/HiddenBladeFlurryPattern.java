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
 * 隐藏锋刃项链连斩：Malum {@code HiddenBladeCounterParticleEffect} 的 3 波 × 100 道 = 300 道。
 *
 * <p>这是整套改造里唯一"数量大到值得质疑"的效果，但 300 就是原版：三波各 100 道，
 * 每一道单独抽样、单独带延迟。角度也不是 300 个独立随机方向，而是
 * <b>触发基础角</b>加上按 {@code j} 奇偶交替的正反偏转：
 * {@code spinOffset = baseAngle ± (随机 0.314–3.14 再乘 ±1)}。位置是
 * {@code direction} 三个分量各抽一个 −1.5…4.5 的偏移（原版就是逐分量相乘，不是球面均匀分布）。
 *
 * <p>错峰由 {@code lifeDelay = (j 偶数 ? 2 : 0) + i + j/6} 与 {@code lifetime = 2 + i} 给出，
 * 最后一道大约在 24 tick 后消失。把 300 压成 72、或把延迟改成人造波次，都会改变观感。
 */
public final class HiddenBladeFlurryPattern implements RiftStrokePattern {

    private static final int WAVES = 3;
    private static final int PER_WAVE = 100;
    private static final float MAX_BACKWARDS_OFFSET = 1.5F;
    private static final float MAX_FORWARDS_OFFSET = 4.5F;

    private final long seed;
    private final Vec3 direction;
    private final float baseAngle;
    private final boolean mirrored;
    private final float fadeStart;

    public HiddenBladeFlurryPattern(
            long seed, Vec3 direction, float baseAngle, boolean mirrored, float fadeStart) {
        this.seed = seed;
        this.direction = RiftOrientation.motionDirection(direction);
        this.baseAngle = baseAngle;
        this.mirrored = mirrored;
        this.fadeStart = fadeStart;
    }

    public static int strokeCount() {
        return WAVES * PER_WAVE;
    }

    @Override
    public List<RiftStroke> generate() {
        RandomSource random = RandomSource.create(seed);
        int count = strokeCount();
        List<RiftStroke> strokes = new ArrayList<>(count);
        for (int i = 0; i < WAVES; i++) {
            for (int j = 0; j < PER_WAVE; j++) {
                int index = strokes.size();
                // 原版抽样顺序：偏转角（含一次 nextBoolean）、尺寸、位置三分量、速度。
                float offsetBase = RandomHelper.randomBetween(random, 0.314F, 3.14F)
                                   * (random.nextBoolean() ? 1.0F : -1.0F)
                                   + (mirrored ? 3.14F : 0.0F);
                float spinOffset = baseAngle + (j % 2 == 0 ? 1.0F : -1.0F) * offsetBase;
                float size = RandomHelper.randomBetween(random, 2.0F, 6.0F);
                int lifeDelay = (j % 2 == 0 ? 2 : 0) + i + j / 6;
                Vec3 offset = direction.multiply(
                        RandomHelper.randomBetween(random, -MAX_BACKWARDS_OFFSET, MAX_FORWARDS_OFFSET),
                        RandomHelper.randomBetween(random, -MAX_BACKWARDS_OFFSET, MAX_FORWARDS_OFFSET),
                        RandomHelper.randomBetween(random, -MAX_BACKWARDS_OFFSET, MAX_FORWARDS_OFFSET));
                Vec3 velocity = direction.scale(RandomHelper.randomBetween(random, 0.3F, 0.5F));
                strokes.add(new RiftStroke(
                        offset,
                        RiftOrientation.axisU(direction),
                        RiftOrientation.axisV(direction, spinOffset),
                        velocity, size,
                        lifeDelay, 2 + i, fadeStart,
                        IncursusRiftGeometry.hash01(seed, index),
                        RiftStroke.variant(index, count)));
            }
        }
        return strokes;
    }
}
