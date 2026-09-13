package org.brahypno.maledict.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sammy.malum.registry.client.MalumRenderTypeTokens;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;

import java.awt.Color;
import java.util.List;

/**
 * All client-side presentation for the First Vicissitude boss: ground warnings, wing charge,
 * release sparks and death collapse. One entry point per entity, no shared static state, and
 * the readable model geometry never depends on particles being enabled.
 */
public final class FirstVicissitudeEffects {
    private static final Color COLD_WHITE = new Color(0xE6EDF5);
    private static final Color COLD_BONE = new Color(0xB8B8C4);
    private static final Color DARK_VIOLET = new Color(0x51436D);
    private static final Color DEEP_BLACK = new Color(0x19151F);
    private static final int GROUND_SEGMENTS = 32;


    private FirstVicissitudeEffects() {
    }

    /** Called from the render layer with the model already posed for this frame. */
    public static void render(FirstVicissitudeBossEntity entity, float partialTick,
                              PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        spawnWingCharge(entity, partialTick);
        if (entity.getDeathTicks() >= 0.0F) {
            renderDeathCollapse(entity, partialTick);
        }
    }

    // ------------------------------------------------------------------ ground warnings

    /**
     * Ground warnings are drawn in a world space pass, not in the entity layer: that keeps the
     * coordinates trivial and, more importantly, keeps them visible when the boss itself is
     * outside the frustum.
     */
    public static void renderGroundMarkers(List<FirstVicissitudeBossEntity> markers,
                                           float partialTick, PoseStack poseStack,
                                           MultiBufferSource buffers, Vec3 camera) {
        if (markers.isEmpty()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        for (FirstVicissitudeBossEntity entity : markers) {
            if (entity.isRemoved() || !entity.isGroundMarkerActive()) {
                continue;
            }
            renderGroundMarker(entity, partialTick, poseStack, consumer, camera);
        }
    }

    private static void renderGroundMarker(FirstVicissitudeBossEntity entity, float partialTick,
                                           PoseStack poseStack, VertexConsumer consumer,
                                           Vec3 camera) {
        Vec3 center = entity.getGroundMarkerCenter();
        double inner = entity.getGroundMarkerInnerRadius();
        double outer = entity.getGroundMarkerOuterRadius();
        float gapCenter = entity.getGroundMarkerGapCenter();
        float gapWidth = entity.getGroundMarkerGapWidth();
        float progress = entity.getGroundMarkerProgress(partialTick);
        // The warning closes in on the judged area and fades right after the release.
        float alpha = progress < 0.8F ? 0.5F + 0.4F * progress : 0.9F * (1.0F - (progress - 0.8F) / 0.2F);
        float edge = (float) (inner + (outer - inner) * (0.15F + 0.85F * Math.min(1.0F, progress * 1.25F)));
        poseStack.pushPose();
        // A hair above the surface, which is exactly the locked block top.
        poseStack.translate(center.x - camera.x, center.y - camera.y + 0.03D, center.z - camera.z);
        renderRing(consumer, poseStack, edge, outer, gapCenter, gapWidth, COLD_WHITE,
                Math.max(0.0F, alpha));
        renderRing(consumer, poseStack, outer * 0.96D, outer * 1.04D, gapCenter, gapWidth,
                COLD_BONE, Math.max(0.0F, alpha * 0.9F));
        if (inner > 0.0D) {
            renderRing(consumer, poseStack, inner, inner * 1.12D, gapCenter, gapWidth,
                    DARK_VIOLET, Math.max(0.0F, alpha * 0.7F));
        }
        poseStack.popPose();
    }

    private static void renderRing(VertexConsumer consumer, PoseStack poseStack, double inner,
                                   double outer, float gapCenter, float gapWidth, Color color,
                                   float alpha) {
        if (outer <= inner || alpha <= 0.0F) {
            return;
        }
        double mid = (inner + outer) * 0.5D;
        float radial = (float) Math.max(0.08D, outer - inner);
        float step = 360.0F / GROUND_SEGMENTS;
        for (int i = 0; i < GROUND_SEGMENTS; i++) {
            float angle = i * step;
            if (gapWidth > 0.0F && insideGap(angle, gapCenter, gapWidth)) {
                continue;
            }
            poseStack.pushPose();
            // World angle measured from +x towards +z, the same convention the judged area uses.
            poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
            poseStack.translate(mid, 0.0D, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            double arc = 2.0D * Math.PI * mid * (step / 360.0D) * 1.1D;
            flatQuad(consumer, poseStack, radial, (float) arc, color, alpha);
            poseStack.popPose();
        }
    }
    /** A quad in the local XY plane at z = 0, centred on the current pose stack origin. */
    private static void flatQuad(VertexConsumer consumer, PoseStack poseStack, float width,
                                 float height, Color color, float alpha) {
        org.joml.Matrix4f matrix = poseStack.last().pose();
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        float red = color.getRed() / 255.0F;
        float green = color.getGreen() / 255.0F;
        float blue = color.getBlue() / 255.0F;
        consumer.vertex(matrix, -halfWidth, -halfHeight, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, halfWidth, -halfHeight, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, halfWidth, halfHeight, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, -halfWidth, halfHeight, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    private static boolean insideGap(float angle, float gapCenter, float gapWidth) {
        float delta = Mth.wrapDegrees(angle - gapCenter);
        return Math.abs(delta) < gapWidth * 0.5F;
    }

    // ------------------------------------------------------------------ wing charge and release

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

    /** One shot burst at a server reported anchor; called once per event, never per frame. */
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

    /** Teleport and transition flash: a short inward collapse of cold light. */
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

    // ------------------------------------------------------------------ death

    private static void renderDeathCollapse(FirstVicissitudeBossEntity entity, float partialTick) {
        float ticks = entity.getDeathTicks();
        Level level = entity.level();
        if (ticks < 36.0F) {
            return;
        }
        // Light leaves the joints and shrinks out instead of exploding.
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

    /** Recreates the pneuma-style glimmer used by the tracking orb on the boss core. */
    public static void spawnCoreGlimmer(Level level, Vec3 position) {
        var particles = com.sammy.malum.visual_effects.SpiritLightSpecs.spiritLightSpecs(
                level, position, SpiritTypeRegistry.ELDRITCH_SPIRIT);
        particles.spawnParticles();
    }
}
