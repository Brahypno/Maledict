package org.brahypno.maledict.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.malum.registry.client.ParticleRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;
import team.lodestar.lodestone.systems.particle.world.behaviors.components.DirectionalBehaviorComponent;

import java.awt.Color;
import java.util.List;

/**
 * First Vicissitude boss 的全部客户端表现：地面预警、翼部蓄力、释放火花与死亡崩塌。
 */
public final class FirstVicissitudeEffects {
    private static final Color COLD_WHITE = new Color(0xE6EDF5);
    private static final Color COLD_BONE = new Color(0xB8B8C4);
    private static final Color DARK_VIOLET = new Color(0x51436D);
    private static final Color DEEP_BLACK = new Color(0x19151F);
    /**
     * 地面法阵画成黑色；这一点必须配合 {@link #spawnRune} 里的 lumitransparent 渲染类型，
     * 该组合下 alpha 取自贴图亮度、颜色取自这里，黑色才看得见而不是淡成一片。
     */
    private static final Color SIGIL_BLACK = new Color(0x000000);
    private static final Vec3 GROUND_UP = new Vec3(0.0D, 1.0D, 0.0D);
    /** 每 tick 转过的角度；所有符文共用一个相位，见 {@link #runeSpin}。 */
    private static final float SIGIL_SPIN_PER_TICK = 1.4F;
    /** 符文悬在锁定表面上方这么高，免得与地板平面打架。 */
    private static final double SIGIL_HEIGHT = 0.05D;
    private static final int SIGIL_LIFETIME = 10;
    /** 光晕带上的碎片数；每 tick 重生一个。 */
    private static final int HALO_FRAGMENT_COUNT = 12;
    /** 比槽位数长，转起来时断环才不会露出缺口。 */
    private static final int HALO_FRAGMENT_LIFETIME = 14;
    private static final float TWO_PI = (float) (Math.PI * 2.0D);


    private FirstVicissitudeEffects() {
    }

    /** 由渲染层调用，此时本帧的模型已经摆好姿势。 */
    public static void render(FirstVicissitudeBossEntity entity, float partialTick,
                              PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        spawnWingCharge(entity, partialTick);
        if (entity.getDeathTicks() >= 0.0F) {
            renderDeathCollapse(entity, partialTick);
        }
    }

    private static float markerAlpha(float progress) {
        return progress < 0.8F ? 0.5F + 0.4F * progress : 0.9F * (1.0F - (progress - 0.8F) / 0.2F);
    }

    /** 判定区收拢到多紧：锁定时 0，释放时 1。 */
    private static float closingFactor(float progress) {
        return 0.15F + 0.85F * Math.min(1.0F, progress * 1.25F);
    }

    /**
     * 地面预警的符文层：每客户端 tick 从同步来的标记状态生成一次，开销与帧率无关；符文按判定所用的同一份锁定几何摆放，且强制生成，玩家调低粒子设置也读得出来。
     */
    public static void tickGroundMarkers(List<FirstVicissitudeBossEntity> markers) {
        for (FirstVicissitudeBossEntity entity : markers) {
            if (entity.isRemoved() || !entity.isGroundMarkerActive()) {
                continue;
            }
            Level level = entity.level();
            if (!level.isClientSide) {
                continue;
            }
            if (entity.getGroundMarkerInnerRadius() > 0.0D) {
                tickRingRunes(level, entity);
            } else {
                tickDiscRunes(level, entity);
            }
        }
    }

    private static void tickDiscRunes(Level level, FirstVicissitudeBossEntity entity) {
        double outer = entity.getGroundMarkerOuterRadius();
        if (outer <= 0.0D) {
            return;
        }
        float progress = entity.getGroundMarkerProgress(0.0F);
        float alpha = markerAlpha(progress);
        if (alpha <= 0.0F) {
            return;
        }
        Vec3 center = entity.getGroundMarkerCenter();
        RandomSource random = level.random;
        long tick = level.getGameTime();
        float closing = closingFactor(progress);
        if (tick % 5L == 0L) {
            // 圆盘内部没有安全区，任何尺寸画整圈都诚实；半径留在判定半径以内，只有符文的软边碰到边缘。
            spawnRune(level, center, (float) (outer * (0.40D + 0.45D * closing)), alpha * 0.85F,
                    runeSpin(tick), SIGIL_LIFETIME);
        }
        if (tick % 2L == 0L) {
            double radius = tick % 4L == 0L ? outer * closing : outer;
            spawnBoundaryMote(level, center, radius, random.nextDouble() * 360.0D,
                    alpha * 0.8F, random);
        }
    }

    private static void tickRingRunes(Level level, FirstVicissitudeBossEntity entity) {
        double inner = entity.getGroundMarkerInnerRadius();
        double outer = entity.getGroundMarkerOuterRadius();
        if (outer <= inner) {
            return;
        }
        float progress = entity.getGroundMarkerProgress(0.0F);
        float alpha = markerAlpha(progress);
        if (alpha <= 0.0F) {
            return;
        }
        float gapCenter = entity.getGroundMarkerGapCenter();
        float gapWidth = entity.getGroundMarkerGapWidth();
        Vec3 center = entity.getGroundMarkerCenter();
        RandomSource random = level.random;
        long tick = level.getGameTime();

        // 每 tick 轮转一个碎片：槽数固定且全落在危险弧上，角度与碎片尺寸都做了内缩，符文一旦溢出就会占住安全地面、让预警说谎。
        int slot = (int) Math.floorMod(tick, (long) HALO_FRAGMENT_COUNT);
        float fragmentScale = (float) ((outer - inner) * (0.24D + 0.09D * progress));
        double radius = inner + (outer - inner) * (0.35D + 0.15D * (slot % 3));
        double inset = Math.toDegrees(Math.atan2(fragmentScale, radius));
        double arcStart = gapWidth * 0.5D + inset;
        double arc = 360.0D - gapWidth - inset * 2.0D;
        double angle = gapCenter + arcStart + arc * ((slot + 0.5D) / HALO_FRAGMENT_COUNT);
        // 波峰只改亮度与大小，绝不动判定区域。
        float wave = 0.5F + 0.5F * Mth.sin(slot / (float) HALO_FRAGMENT_COUNT * TWO_PI - tick * 0.4F);
        spawnRuneAt(level, center, angle, radius, fragmentScale, alpha * (0.45F + 0.40F * wave),
                runeSpin(tick), HALO_FRAGMENT_LIFETIME);

        if (tick % 2L == 0L) {
            boolean innerRim = random.nextBoolean();
            double rim = innerRim ? inner + (outer - inner) * 0.08D : outer - (outer - inner) * 0.08D;
            spawnBoundaryMote(level, center, rim, dangerousAngle(random, gapCenter, gapWidth, 8.0D),
                    alpha * 0.75F, random);
        }
    }

    /**
     * 所有符文共用一个相位：spin offset 抵消粒子自身累积的自转，后几 tick 重生的环才会对齐，而不是叠成各自转过角度的副本。
     */
    private static SpinParticleData runeSpin(long tick) {
        return SpinParticleData.create(SIGIL_SPIN_PER_TICK)
                .setSpinOffset(SIGIL_SPIN_PER_TICK * tick)
                .build();
    }

    private static void spawnRuneAt(Level level, Vec3 center, double angleDegrees, double radius,
                                    float scale, float alpha, SpinParticleData spin, int lifetime) {
        double radians = Math.toRadians(angleDegrees);
        Vec3 position = new Vec3(center.x + Math.cos(radians) * radius, center.y,
                center.z + Math.sin(radians) * radius);
        spawnRune(level, position, scale, alpha, spin, lifetime);
    }

    /**
     * 符文画成黑色必须搭配 lumitransparent：这些贴图是不透明白字黑底，走普通透明通道会盖出黑方块，从颜色推 alpha 又会把黑色整个抹掉。
     * 该 shader 先用贴图自身的亮度替换 alpha、再乘顶点色，于是白笔画不透明、黑底隐形、符文本身呈黑色。
     */
    private static void spawnRune(Level level, Vec3 position, float scale, float alpha,
                                  SpinParticleData spin, int lifetime) {
        WorldParticleBuilder.create(ParticleRegistry.RITUAL_CIRCLE,
                        new DirectionalBehaviorComponent(GROUND_UP))
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .setColorData(ColorParticleData.create(SIGIL_BLACK, SIGIL_BLACK).build())
                .setTransparencyData(GenericParticleData.create(alpha * 0.3F, alpha, 0.0F)
                        .setEasing(Easing.SINE_IN).build())
                .setScaleData(GenericParticleData.create(scale).build())
                .setSpinData(spin)
                .setLifetime(lifetime)
                .enableNoClip()
                .enableForcedSpawn()
                .spawn(level, position.x, position.y + SIGIL_HEIGHT, position.z);
    }

    private static void spawnBoundaryMote(Level level, Vec3 center, double radius, double angleDegrees,
                                          float alpha, RandomSource random) {
        double radians = Math.toRadians(angleDegrees);
        Vec3 position = new Vec3(center.x + Math.cos(radians) * radius, center.y,
                center.z + Math.sin(radians) * radius);
        WorldParticleBuilder.create(LodestoneParticleRegistry.SPARKLE_PARTICLE)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .setColorData(ColorParticleData.create(COLD_WHITE, COLD_BONE).build())
                .setTransparencyData(GenericParticleData.create(alpha, 0.0F)
                        .setEasing(Easing.SINE_IN).build())
                .setScaleData(GenericParticleData.create(0.10F + 0.03F * (float) radius).build())
                .setLifetime(8 + random.nextInt(5))
                .setMotion(0.0D, 0.02D, 0.0D)
                .enableNoClip()
                .enableForcedSpawn()
                .spawn(level, position.x, position.y + SIGIL_HEIGHT + 0.04D, position.z);
    }

    /**
     * 判定弧上的随机角，因此永远不会落进光晕判定放过的缺口；{@code insetDegrees} 让光点自身的角宽也避开安全区。
     */
    private static double dangerousAngle(RandomSource random, float gapCenter, float gapWidth,
                                         double insetDegrees) {
        double arc = 360.0D - gapWidth - insetDegrees * 2.0D;
        if (arc <= 0.0D) {
            return gapCenter + 180.0D;
        }
        return gapCenter + gapWidth * 0.5D + insetDegrees + random.nextDouble() * arc;
    }

    private static void spawnWingCharge(FirstVicissitudeBossEntity entity, float partialTick) {
        VicissitudeRig.Action action = entity.getRenderAction();
        if (!entity.isChargingRanged()) {
            return;
        }
        float ticks = entity.getActionTicks(partialTick);
        if (ticks < 0.0F || ticks > action.releaseTick()) {
            return;
        }
        Level level = entity.level();
        if (!level.isClientSide) {
            return;
        }
        float build = Mth.clamp(ticks / Math.max(1.0F, action.releaseTick()), 0.0F, 1.0F);
        boolean left = entity.isActionLeft();
        VicissitudeRigData.Joint anchor = left
                ? VicissitudeRigData.Joint.WING_LEFT_ATTACK_ANCHOR
                : VicissitudeRigData.Joint.WING_RIGHT_ATTACK_ANCHOR;
        Vec3 point = entity.anchorWorldPosition(anchor, partialTick);
        WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .setColorData(ColorParticleData.create(COLD_WHITE, DARK_VIOLET).build())
                .setTransparencyData(GenericParticleData.create(0.65F, 0.0F).build())
                .setScaleData(GenericParticleData.create(0.18F + build * 0.5F, 0.0F).build())
                .setLifetime(14)
                .setRandomOffset(0.18D)
                .spawn(level, point.x, point.y, point.z);
        if (build > 0.75F) {
            Vec3 tip = entity.anchorWorldPosition(left
                    ? VicissitudeRigData.Joint.WING_LEFT_TIP
                    : VicissitudeRigData.Joint.WING_RIGHT_TIP, partialTick);
            WorldParticleBuilder.create(LodestoneParticleRegistry.STAR_PARTICLE)
                    .setColorData(ColorParticleData.create(COLD_WHITE, COLD_BONE).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0.0F).build())
                    .setScaleData(GenericParticleData.create(0.12F, 0.0F).build())
                    .setLifetime(10)
                    .spawn(level, tip.x, tip.y, tip.z);
        }
    }

    /** 在服务端报来的锚点上做一次性爆发；每个事件调一次，绝不按帧调。 */
    public static void spawnReleaseBurst(Level level, Vec3 position, boolean phaseTwo) {
        Color bright = phaseTwo ? COLD_BONE : COLD_WHITE;
        for (int i = 0; i < 8; i++) {
            WorldParticleBuilder.create(LodestoneParticleRegistry.SPARKLE_PARTICLE)
                    .setColorData(ColorParticleData.create(bright, DARK_VIOLET).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0.0F).build())
                    .setScaleData(GenericParticleData.create(0.2F, 0.0F).build())
                    .setLifetime(12 + i)
                    .setRandomOffset(0.35D)
                    .setRandomMotion(0.12D, 0.12D, 0.12D)
                    .spawn(level, position.x, position.y, position.z);
        }
    }

    public static void spawnGatherFlash(Level level, Vec3 position, int count) {
        for (int i = 0; i < count; i++) {
            WorldParticleBuilder.create(LodestoneParticleRegistry.TWINKLE_PARTICLE)
                    .setColorData(ColorParticleData.create(COLD_WHITE, DEEP_BLACK).build())
                    .setTransparencyData(GenericParticleData.create(0.85F, 0.0F).build())
                    .setScaleData(GenericParticleData.create(0.25F, 0.0F).build())
                    .setLifetime(16)
                    .setRandomOffset(0.6D)
                    .spawn(level, position.x, position.y, position.z);
        }
    }

    private static void renderDeathCollapse(FirstVicissitudeBossEntity entity, float partialTick) {
        float ticks = entity.getDeathTicks();
        Level level = entity.level();
        if (ticks < 36.0F) {
            return;
        }
        float fade = Mth.clamp((ticks - 36.0F) / 44.0F, 0.0F, 1.0F);
        float scale = Math.max(0.02F, 0.5F * (1.0F - fade));
        Vec3 core = entity.anchorWorldPosition(VicissitudeRigData.Joint.HEAD_EFFECT_ANCHOR, partialTick);
        WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .setColorData(ColorParticleData.create(COLD_WHITE, DEEP_BLACK).build())
                .setTransparencyData(GenericParticleData.create(0.7F * (1.0F - fade), 0.0F).build())
                .setScaleData(GenericParticleData.create(scale, 0.0F).build())
                .setLifetime(20)
                .setRandomOffset(0.3D)
                .setMotion(0.0D, 0.02D, 0.0D)
                .spawn(level, core.x, core.y, core.z);
        Vec3 chest = entity.anchorWorldPosition(VicissitudeRigData.Joint.CHEST_EFFECT_ANCHOR, partialTick);
        WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                .setColorData(ColorParticleData.create(DARK_VIOLET, DEEP_BLACK).build())
                .setTransparencyData(GenericParticleData.create(0.5F * (1.0F - fade), 0.0F).build())
                .setScaleData(GenericParticleData.create(scale * 1.4F, 0.0F).build())
                .setLifetime(24)
                .setRandomOffset(0.4D)
                .spawn(level, chest.x, chest.y, chest.z);
    }

    public static void spawnCoreGlimmer(Level level, Vec3 position) {
        var particles = com.sammy.malum.visual_effects.SpiritLightSpecs.spiritLightSpecs(
                level, position, SpiritTypeRegistry.ELDRITCH_SPIRIT);
        particles.spawnParticles();
    }
}
