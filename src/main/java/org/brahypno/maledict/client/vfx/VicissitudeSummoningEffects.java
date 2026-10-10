package org.brahypno.maledict.client.vfx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.rite.VicissitudeSummoning;
import org.joml.Matrix4f;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** An opaque suspended aperture, orbiting wisps and a distance-faded cinematic post effect. */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class VicissitudeSummoningEffects {
    private static final List<FirstVicissitudeBossEntity> SUMMONING = new ArrayList<>();
    private static final Color BONE = new Color(0xD8DBE2);
    private static final Color VIOLET = new Color(0x6D5A87);
    private static final int SEGMENTS = 96;
    private static final float RADIUS = 3.8F;

    private VicissitudeSummoningEffects() {
    }

    public static void tick(List<FirstVicissitudeBossEntity> bosses) {
        SUMMONING.clear();
        for (FirstVicissitudeBossEntity boss : bosses) {
            if (!boss.isRiteSummoning() || !boss.isAlive()) continue;
            SUMMONING.add(boss);
            spawnRim(boss);
        }
    }

    public static void clear() {
        SUMMONING.clear();
        VicissitudeSummoningPostProcessor.INSTANCE.setStrength(0, 0);
    }

    public static void consumeTotemLayer(Level level, Vec3 position) {
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2 / 24;
            double x = Math.cos(angle), z = Math.sin(angle);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setColorData(ColorParticleData.create(BONE, VIOLET).build())
                    .setTransparencyData(GenericParticleData.create(0.85F, 0).build())
                    .setScaleData(GenericParticleData.create(0.18F, 0).build())
                    .setLifetime(24).setMotion(-x * 0.025D, 0.13D, -z * 0.025D)
                    .enableNoClip().enableForcedSpawn()
                    .spawn(level, position.x + x * 0.55D, position.y + (i % 3 - 1) * 0.25D,
                            position.z + z * 0.55D);
        }
        for (int i = 0; i < 8; i++) {
            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .setColorData(ColorParticleData.create(Color.BLACK, VIOLET).build())
                    .setTransparencyData(GenericParticleData.create(0, 0.65F, 0).build())
                    .setScaleData(GenericParticleData.create(0.3F, 0.65F).build())
                    .setLifetime(28).setMotion(0, 0.09D, 0)
                    .enableNoClip().enableForcedSpawn()
                    .spawn(level, position.x + (level.random.nextDouble() - 0.5D) * 0.8D,
                            position.y, position.z + (level.random.nextDouble() - 0.5D) * 0.8D);
        }
    }

    private static void spawnRim(FirstVicissitudeBossEntity boss) {
        Level level = boss.level();
        float ticks = boss.getSummoningTicks(0);
        float radius = RADIUS * VicissitudeSummoning.aperture(ticks);
        if (radius < 0.1F) return;
        Vec3 portal = boss.getSummoningPortal();
        for (int i = 0; i < 6; i++) {
            double angle = ticks * 0.065D + Math.PI * 2 * i / 6;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setColorData(ColorParticleData.create(BONE, VIOLET).build())
                    .setTransparencyData(GenericParticleData.create(0.7F, 0).build())
                    .setScaleData(GenericParticleData.create(0.12F, 0.03F).build())
                    .setLifetime(18).setMotion(-z * 0.035D, -0.025D, x * 0.035D)
                    .enableNoClip().enableForcedSpawn()
                    .spawn(level, portal.x + x, portal.y - 0.04D, portal.z + z);
        }
        double angle = level.random.nextDouble() * Math.PI * 2;
        double x = Math.cos(angle) * radius * 0.85;
        double z = Math.sin(angle) * radius * 0.85;
        WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .setColorData(ColorParticleData.create(Color.BLACK, VIOLET).build())
                .setTransparencyData(GenericParticleData.create(0, 0.6F, 0).build())
                .setScaleData(GenericParticleData.create(0.3F, 0.8F).build())
                .setLifetime(30).setMotion(-x * 0.018D, -0.045D, -z * 0.018D)
                .enableNoClip().enableForcedSpawn()
                .spawn(level, portal.x + x, portal.y - 0.12D, portal.z + z);
        if (ticks >= VicissitudeSummoning.ARRIVAL_TICK && ((int) ticks) % 2 == 0) {
            float spread = (ticks - VicissitudeSummoning.ARRIVAL_TICK) * 0.22F;
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI * 2 / 12;
                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARKLE_PARTICLE)
                        .setColorData(ColorParticleData.create(BONE, VIOLET).build())
                        .setTransparencyData(GenericParticleData.create(0.6F, 0).setEasing(Easing.SINE_IN).build())
                        .setScaleData(GenericParticleData.create(0.16F, 0).build())
                        .setLifetime(14).setMotion(Math.cos(a) * 0.08D, 0.02D, Math.sin(a) * 0.08D)
                        .enableNoClip().enableForcedSpawn()
                        .spawn(level, boss.getX() + Math.cos(a) * spread,
                                boss.getY() + 0.15D, boss.getZ() + Math.sin(a) * spread);
            }
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            float strength = 0;
            float flash = 0;
            for (FirstVicissitudeBossEntity boss : SUMMONING) {
                if (boss.level() != mc.level || boss.isRemoved() || !boss.isAlive()) continue;
                float ticks = boss.getSummoningTicks(event.getPartialTick());
                float distance = (float) event.getCamera().getPosition().distanceTo(boss.getSummoningPortal());
                float proximity = 1.0F - Mth.clamp((distance - 12) / 36, 0, 1);
                strength = Math.max(strength, VicissitudeSummoning.grayscale(ticks) * proximity);
                flash = Math.max(flash, Math.max(0, 1 - Math.abs(ticks - VicissitudeSummoning.ARRIVAL_TICK) / 6)
                        * 0.16F * proximity);
            }
            VicissitudeSummoningPostProcessor.INSTANCE.setStrength(strength, flash);
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SUMMONING.isEmpty()) return;
        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(RiftRenderState.TYPE);
        for (FirstVicissitudeBossEntity boss : SUMMONING) {
            if (boss.level() != mc.level || boss.isRemoved() || !boss.isAlive()) continue;
            float ticks = boss.getSummoningTicks(event.getPartialTick());
            float radius = RADIUS * VicissitudeSummoning.aperture(ticks);
            if (radius < 0.01F) continue;
            Vec3 portal = boss.getSummoningPortal();
            poses.pushPose();
            poses.translate(portal.x - camera.x, portal.y - camera.y, portal.z - camera.z);
            Matrix4f matrix = poses.last().pose();
            for (int i = 0; i < SEGMENTS; i++) {
                double a = i * Math.PI * 2 / SEGMENTS;
                double b = (i + 1) * Math.PI * 2 / SEGMENTS;
                float r0 = radius * (1 + 0.025F * Mth.sin((float) a * 7 + ticks * 0.11F));
                float r1 = radius * (1 + 0.025F * Mth.sin((float) b * 7 + ticks * 0.11F));
                float x0 = (float) Math.cos(a) * r0, z0 = (float) Math.sin(a) * r0;
                float x1 = (float) Math.cos(b) * r1, z1 = (float) Math.sin(b) * r1;
                vertex(vertices, matrix, 0, 0, 0, 0, 1);
                vertex(vertices, matrix, x0, 0, z0, 0, 1);
                vertex(vertices, matrix, x1, 0, z1, 0, 1);
                // A narrow cold rim fades outwards without making the black centre additive.
                vertex(vertices, matrix, x0, 0, z0, 0.7F, 0.85F);
                vertex(vertices, matrix, x0 * 1.07F, 0, z0 * 1.07F, 0.45F, 0);
                vertex(vertices, matrix, x1 * 1.07F, 0, z1 * 1.07F, 0.45F, 0);
                vertex(vertices, matrix, x0, 0, z0, 0.7F, 0.85F);
                vertex(vertices, matrix, x1 * 1.07F, 0, z1 * 1.07F, 0.45F, 0);
                vertex(vertices, matrix, x1, 0, z1, 0.7F, 0.85F);
            }
            poses.popPose();
        }
        buffers.endBatch(RiftRenderState.TYPE);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix,
                               float x, float y, float z, float gray, float alpha) {
        vertices.vertex(matrix, x, y, z).color(gray, gray, gray, alpha).endVertex();
    }

    private static final class RiftRenderState extends RenderStateShard {
        private static final RenderType TYPE = LodestoneRenderTypeRegistry.createGenericRenderType(
                "maledict_summoning_rift", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES,
                LodestoneRenderTypeRegistry.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(LodestoneRenderTypeRegistry.NO_TEXTURE)
                        .setCullState(LodestoneRenderTypeRegistry.NO_CULL)
                        .setWriteMaskState(COLOR_DEPTH_WRITE));

        private RiftRenderState() {
            super("maledict_summoning_rift", () -> {}, () -> {});
        }
    }
}
