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
 * 强化飞腾上挑：Malum {@code AscensionUppercutParticleEffect} 的 6 层 × 2 道 = 12 道。
 *
 * <p>纵向效果完全来自原版那四个量：{@code up = left × direction}、逐层前移 {@code 2 - i·0.6}、
 * 逐层升高 {@code i·0.4}、以及 {@code spinOffset = 1.57 ± 0.25 + 3.14}（竖直刀面再翻转半圈）。
 * 不额外加倾斜角、左右偏移或逐道错峰——那些都会改变原版的观感。
 *
 * <p>{@code direction} 保留报文里的原始长度：{@code left × direction} 的长度跟着它走，
 * 服务端下发的是 {@code getLookAngle().multiply(1,0,1)}，归一化会把纵向间距一起缩掉。
 */
public final class AscensionUppercutPattern implements RiftStrokePattern {

    private static final int LAYERS = 6;
    private static final int PER_LAYER = 2;
    private final long seed;
    private final Vec3 direction;
    private final float baseAngle;
    private final boolean mirrored;
    private final float fadeStart;

    public AscensionUppercutPattern(
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
        Vec3 up = left.cross(direction);
        int count = strokeCount();
        List<RiftStroke> strokes = new ArrayList<>(count);
        for (int i = 0; i < LAYERS; i++) {
            float upwardsOffset = i * 0.4F;
            float slashOffset = 2.0F - i * 0.6F;
            for (int j = 0; j < PER_LAYER; j++) {
                int index = strokes.size();
                float spinOffset = baseAngle + RandomHelper.randomBetween(random, -0.25F, 0.25F)
                                   + (mirrored ? 3.14F : 0.0F);
                Vec3 offset = direction.scale(slashOffset).add(up.scale(upwardsOffset));
                float size = RandomHelper.randomBetween(random, 2.5F, 3.0F);
                Vec3 velocity = direction.scale(RandomHelper.randomBetween(random, 0.2F, 0.4F))
                                         .add(0.0, 0.8F, 0.0);
                strokes.add(new RiftStroke(
                        offset,
                        RiftOrientation.axisU(direction),
                        RiftOrientation.axisV(direction, spinOffset),
                        velocity, size,
                        i / 2, 3 + i, fadeStart,
                        IncursusRiftGeometry.hash01(seed, index),
                        RiftStroke.variant(index, count)));
            }
        }
        return strokes;
    }
}
