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
 * All client-side presentation for the First Vicissitude boss: ground warnings, wing charge,
 * release sparks and death collapse. One entry point per entity, no shared static state, and
 * the readable model geometry never depends on particles being enabled.
 */
public final class FirstVicissitudeEffects {
    private static final Color COLD_WHITE = new Color(0xE6EDF5);
    private static final Color COLD_BONE = new Color(0xB8B8C4);
    private static final Color DARK_VIOLET = new Color(0x51436D);
    private static final Color DEEP_BLACK = new Color(0x19151F);
    /**
     * The ground sigil is drawn black. This only works together with the lumitransparent render
     * type, see {@link #spawnRune}: the glyph keeps its alpha from the texture's own brightness
     * while its colour comes from this value, so black stays visible instead of fading to nothing.
     */
    private static final Color SIGIL_BLACK = new Color(0x000000);
    /** Every ground rune lies flat on the floor, so its directional quad faces straight up. */
    private static final Vec3 GROUND_UP = new Vec3(0.0D, 1.0D, 0.0D);
    /** Degrees a rune turns per tick. All runes share one phase, see {@link #runeSpin}. */
    private static final float SIGIL_SPIN_PER_TICK = 1.4F;
    /** Runes hover this far above the locked surface so they never fight the floor plane. */
    private static final double SIGIL_HEIGHT = 0.05D;
    private static final int SIGIL_LIFETIME = 10;
    /** Rune fragments spread over the halo band; one is respawned per tick. */
    private static final int HALO_FRAGMENT_COUNT = 12;
    /** Longer than the slot count, so the broken ring never opens up while it turns. */
    private static final int HALO_FRAGMENT_LIFETIME = 14;
    private static final float TWO_PI = (float) (Math.PI * 2.0D);


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

    // The warnings used to be drawn as world space quad rings with RenderType.lightning() on top of
    // the particles. That render type is the lightning/weather one, so the rings never matched the
    // boss' palette and read as washed out yellow white instead. They are gone: the rune layer
    // below is now the whole ground telegraph, and because every rune is force spawned it also
    // survives the minimal particle setting the geometry used to cover for.

    /** Warning brightness over the marker lifetime; every rune reads the same curve. */
    private static float markerAlpha(float progress) {
        return progress < 0.8F ? 0.5F + 0.4F * progress : 0.9F * (1.0F - (progress - 0.8F) / 0.2F);
    }

    /** How far the flag has closed in on the judged area, 0 at lock time and 1 at release. */
    private static float closingFactor(float progress) {
        return 0.15F + 0.85F * Math.min(1.0F, progress * 1.25F);
    }

    // ------------------------------------------------------------------ ground warning runes

    /**
     * Rune layer for the ground warnings, spawned once per client tick from the synced marker
     * state. Ticking instead of rendering keeps the cost independent of the frame rate, and every
     * rune is placed from the same locked geometry the judgement uses: the chest mark judges its
     * whole disc and gets a full circle, the halo verdict spares a wedge and is therefore only
     * ever decorated inside its band. Every rune is force spawned, so the judged area stays
     * readable even when the player turns the particle setting down.
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

    /** Chest mark: a sigil that grows to fill the disc, plus motes riding the closing flag. */
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
            // The disc has no safe interior, so a full rune circle is honest at any size. It is
            // kept just inside the judged radius: only the soft tips of the glyph reach the rim.
            spawnRune(level, center, (float) (outer * (0.40D + 0.45D * closing)), alpha * 0.85F,
                    runeSpin(tick), SIGIL_LIFETIME);
        }
        if (tick % 2L == 0L) {
            // Motes alternate between the closing flag and the judged rim, which are the two edges
            // this mark is read by now that no geometry is drawn over the ground.
            double radius = tick % 4L == 0L ? outer * closing : outer;
            spawnBoundaryMote(level, center, radius, random.nextDouble() * 360.0D,
                    alpha * 0.8F, random);
        }
    }

    /** Halo verdict: a broken rune ring inside the band, never on the spared wedge. */
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

        // One fragment per tick in round robin. The slot count is fixed and every slot sits on the
        // dangerous arc, so the ring is always broken in exactly the wedge the verdict spares.
        // Both the slot angles and the fragment size are inset: a rune allowed to spill would
        // claim safe ground and make the telegraph lie about the judged area.
        int slot = (int) Math.floorMod(tick, (long) HALO_FRAGMENT_COUNT);
        float fragmentScale = (float) ((outer - inner) * (0.24D + 0.09D * progress));
        double radius = inner + (outer - inner) * (0.35D + 0.15D * (slot % 3));
        double inset = Math.toDegrees(Math.atan2(fragmentScale, radius));
        double arcStart = gapWidth * 0.5D + inset;
        double arc = 360.0D - gapWidth - inset * 2.0D;
        double angle = gapCenter + arcStart + arc * ((slot + 0.5D) / HALO_FRAGMENT_COUNT);
        // A crest travels around the ring so the countdown reads as a turning verdict. It only
        // changes brightness and size, never the judged area.
        float wave = 0.5F + 0.5F * Mth.sin(slot / (float) HALO_FRAGMENT_COUNT * TWO_PI - tick * 0.4F);
        spawnRuneAt(level, center, angle, radius, fragmentScale, alpha * (0.45F + 0.40F * wave),
                runeSpin(tick), HALO_FRAGMENT_LIFETIME);

        if (tick % 2L == 0L) {
            // Rim motes sharpen both judged rims; the safe centre and the wedge stay empty.
            boolean innerRim = random.nextBoolean();
            double rim = innerRim ? inner + (outer - inner) * 0.08D : outer - (outer - inner) * 0.08D;
            spawnBoundaryMote(level, center, rim, dangerousAngle(random, gapCenter, gapWidth, 8.0D),
                    alpha * 0.75F, random);
        }
    }

    /**
     * Every rune shares one phase. The spin offset cancels the roll a particle accumulates over
     * its own life, so rings respawned on later ticks line up instead of stacking as separately
     * rotated copies.
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
     * Runes are drawn black with the lumitransparent render type, which is the only combination
     * that works with these textures: they are fully opaque white-on-black, so a plain transparent
     * pass would stamp a black square, and deriving alpha from the colour would erase black
     * entirely. The shader replaces the texture alpha with the texture's own brightness first and
     * only then multiplies by the vertex colour, so the white glyph strokes stay opaque, the black
     * background stays invisible and the glyph itself comes out black. Malum's Erosion Scepter
     * draws its near black circle particle the same way.
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

    /**
     * Boundary accent. Deliberately the one bright part of the warning: the sigil itself is black,
     * and a black glyph on dark ground needs this cold edge to stay readable.
     */
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
     * A random angle on the judged arc, so never inside the wedge the halo verdict spares.
     * {@code insetDegrees} keeps a mote of the given angular width off the safe wedge as well.
     */
    private static double dangerousAngle(RandomSource random, float gapCenter, float gapWidth,
                                         double insetDegrees) {
        double arc = 360.0D - gapWidth - insetDegrees * 2.0D;
        if (arc <= 0.0D) {
            return gapCenter + 180.0D;
        }
        return gapCenter + gapWidth * 0.5D + insetDegrees + random.nextDouble() * arc;
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
