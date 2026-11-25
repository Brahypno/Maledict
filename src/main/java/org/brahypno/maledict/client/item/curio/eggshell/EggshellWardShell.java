package org.brahypno.maledict.client.item.curio.eggshell;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.joml.Matrix4f;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;

/** Three fragments cut from one egg-shaped surface, with light moving along that same surface. */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EggshellWardShell {
    static final int BONE = 0xD8C9AE;
    static final int SHADOW = 0x302A32;
    static final int GOLD = 0xB98A53;
    static final int IVORY = 0xF2DFC0;
    static final int BRONZE = 0x775838;
    private static final int HEIGHT_STEPS = 20;
    private static final int[] PLATE_TONES = {0x62483F, 0x967454, 0xB49572, 0xCAB08F, 0xDECAAA, 0xC4A27B, 0x846044};

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EggshellWardClient.pulses().isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        var poses = event.getPoseStack();
        var camera = event.getCamera();
        var buffers = minecraft.renderBuffers().bufferSource();
        long tick = minecraft.level.getGameTime();
        float partialTick = event.getPartialTick();
        VertexConsumer vertices = null;

        for (var entry : EggshellWardClient.pulses().entrySet()) {
            Player player = entry.getKey();
            if (player.isRemoved() || player.level() != minecraft.level || player.isInvisibleTo(minecraft.player)
                || (player == camera.getEntity() && !camera.isDetached())
                || !event.getFrustum().isVisible(player.getBoundingBox().inflate(1.25))) {
                continue;
            }
            var pulse = entry.getValue();
            float full = pulse.fullAlpha(tick, partialTick);
            float accent = pulse.accentAlpha(tick, partialTick);
            if (full < 0.01F && accent < 0.01F) {
                continue;
            }
            Vec3 offset = player.getPosition(partialTick).subtract(camera.getPosition());
            float height = pulse.height(partialTick);
            float glide = player.isFallFlying() ? Math.min(1.0F, (player.getFallFlyingTicks() + partialTick) / 5.0F) : 0.0F;
            float recline = Math.max(glide, player.getSwimAmount(partialTick));
            float closure = pulse.closure(tick, partialTick);
            Surface surface = Surface.forWearer(pulse.width(partialTick), height, recline, closure);
            float distance = (float) offset.add(0, surface.bottom() + surface.height() * 0.5F, 0).length();
            // Fade the plate out before a close third-person camera moves through its surface.
            float clearance = surface.radius() * surface.depthScale() + 0.08F;
            float near = Mth.clamp((distance - clearance) / 0.35F, 0.0F, 1.0F);
            if (near <= 0.0F) {
                continue;
            }
            if (vertices == null) {
                vertices = buffers.getBuffer(ShellRenderState.TYPE);
            }
            float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
            poses.pushPose();
            poses.translate(offset.x, offset.y, offset.z);
            poses.mulPose(Axis.YP.rotation(-yaw));
            drawShell(vertices, poses.last().pose(), surface, pulse, yaw, closure, full * near, accent * near,
                      pulse.accentAge(tick, partialTick));
            poses.popPose();
        }
        if (vertices != null) {
            buffers.endBatch(ShellRenderState.TYPE);
        }
    }

    static void drawShell(VertexConsumer vertices, Matrix4f matrix, Surface surface, EggshellWardClient.Pulse pulse,
                          float yaw, float closure, float full, float accent, float age) {
        var mesh = new ShellMesh(vertices, matrix, surface);
        if (full >= 0.01F) {
            float bearing = pulse.shellBearing();
            float side = bearing > 0.0F ? 1.0F : -1.0F;
            float mainAngle = Float.isNaN(bearing) ? 67.0F : Mth.clamp(Math.abs(bearing), 52.0F, 96.0F);
            float open = 1.0F - closure;
            float plate = Math.min(0.96F, full * 1.5F);
            mesh.fragment(new Fragment(0.046F, 0.98F, 0.59F, side * (mainAngle + 12 * open),
                                       side * 0.23F, 0.42F), closure, plate);
            mesh.fragment(new Fragment(0.065F, 0.94F, 0.53F, -side * (76 + 9 * open),
                                       -side * 0.18F, 2.37F), closure, plate * 0.91F);
            mesh.fragment(new Fragment(0.078F, 0.97F, 0.57F, 175 + side * 10 * open,
                                       -side * 0.22F, 4.29F), closure, plate * 0.92F);
        }
        if (accent >= 0.01F) {
            Vec3 direction = pulse.direction();
            if (direction.lengthSqr() == 0) {
                mesh.impact(-67 * Mth.DEG_TO_RAD, age, pulse.accentStrength(), accent * 0.54F);
                mesh.impact(67 * Mth.DEG_TO_RAD, age, pulse.accentStrength(), accent * 0.54F);
            } else {
                mesh.impact((float) Mth.atan2(-direction.x, direction.z) - yaw, age, pulse.accentStrength(), accent);
            }
        }
    }

    record Surface(float radius, float bottom, float height, float depthScale) {
        static Surface forWearer(float width, float height, float recline, float closure) {
            return new Surface(width * 0.5F + 0.42F + 0.15F * (1.0F - closure),
                               -0.14F, height + 0.46F + 0.16F * recline, 1.0F + recline * 0.78F);
        }

        float radiusAt(float t) {
            float oval = Math.max(0.0F, Mth.sin(Mth.PI * t));
            return radius * (float) Math.pow(oval, 0.78) * (1.0F - 0.25F * (t - 0.5F));
        }
    }

    private record Fragment(float bottom, float top, float halfWidth, float bearing, float bend, float variation) {
        private Row row(Surface surface, float progress, float closure) {
            float width = halfWidth * (float) Math.pow(Math.max(0.0F, Mth.sin(Mth.PI * progress)), 0.72);
            float turn = bend * (progress - 0.38F + 0.86F * closure * progress * progress);
            float center = bearing * Mth.DEG_TO_RAD + turn;
            float left = center - width * (1.0F + 0.16F * Mth.sin(17 * progress + variation));
            float right = center + width * (0.85F + 0.17F * Mth.sin(22 * progress + 2 * variation));
            float t = Mth.lerp(progress, bottom, top);
            return new Row(t, surface.radiusAt(t), left, right);
        }
    }

    private record Row(float t, float radius, float left, float right) {
        float angle(float across) {
            return Mth.lerp(across, left, right);
        }
    }

    private record ShellMesh(VertexConsumer vertices, Matrix4f matrix, Surface surface) {
        void fragment(Fragment fragment, float closure, float alpha) {
            Row low = fragment.row(surface, 0, closure);
            for (int row = 0; row < HEIGHT_STEPS; row++) {
                Row high = fragment.row(surface, (row + 1.0F) / HEIGHT_STEPS, closure);
                int acrossSteps = PLATE_TONES.length - 1;
                for (int step = 0; step < acrossSteps; step++) {
                    float a = step / (float) acrossSteps;
                    float b = (step + 1.0F) / acrossSteps;
                    face(low, high, a, b, PLATE_TONES[step], PLATE_TONES[step + 1], alpha);
                }
                float edge = alpha * (0.67F + 0.16F * Mth.sin(row * 0.52F + fragment.variation()));
                band(low, high, 0.0F, 0.08F, 0.003F, BRONZE, edge);
                band(low, high, 0.92F, 1.0F, 0.003F, row % 7 == 1 ? BRONZE : GOLD, edge);
                if (row >= 5 && row <= 15) {
                    band(low, high, 0.73F, 0.76F, 0.004F, BONE, alpha * 0.24F);
                }
                low = high;
            }
        }

        private void face(Row low, Row high, float from, float to, int dark, int light, float alpha) {
            vertexAtRadius(low.t(), low.angle(from), low.radius(), shade(dark, low.t()), alpha);
            vertexAtRadius(low.t(), low.angle(to), low.radius(), shade(light, low.t()), alpha);
            vertexAtRadius(high.t(), high.angle(to), high.radius(), shade(light, high.t()), alpha);
            vertexAtRadius(high.t(), high.angle(from), high.radius(), shade(dark, high.t()), alpha);
        }

        private static int shade(int color, float height) {
            float light = 0.72F + 0.28F * Mth.sin(Mth.PI * height);
            int red = (int) ((color >> 16 & 0xFF) * light);
            int green = (int) ((color >> 8 & 0xFF) * light);
            int blue = (int) ((color & 0xFF) * light);
            return red << 16 | green << 8 | blue;
        }

        private void band(Row low, Row high, float from, float to, float lift, int color, float alpha) {
            vertexAtRadius(low.t(), low.angle(from), low.radius() + lift, color, alpha);
            vertexAtRadius(low.t(), low.angle(to), low.radius() + lift, color, alpha);
            vertexAtRadius(high.t(), high.angle(to), high.radius() + lift, color, alpha);
            vertexAtRadius(high.t(), high.angle(from), high.radius() + lift, color, alpha);
        }

        void impact(float angle, float age, float strength, float alpha) {
            float focus = 0.64F;
            float flash = alpha * Mth.clamp(1.0F - age / 1.65F, 0.0F, 1.0F);
            float halfAngle = Mth.lerp(strength, 0.025F, 0.085F);
            if (flash >= 0.01F) {
                vertex(focus - 0.021F, angle, 0.014F, GOLD, flash * 0.4F);
                vertex(focus, angle + halfAngle, 0.014F, IVORY, flash);
                vertex(focus + 0.021F, angle, 0.014F, IVORY, flash);
                vertex(focus, angle - halfAngle, 0.014F, GOLD, flash * 0.4F);
            }
            float phase = Mth.clamp(age / 3.7F, 0.0F, 1.0F);
            float travel = phase * (2.0F - phase);
            float width = Mth.lerp(strength, 0.011F, 0.019F) * (1.0F - 0.55F * phase);
            flow(angle, focus, Mth.lerp(strength, 0.21F, 0.56F), travel, phase, width, alpha, 1);
            flow(angle, focus, Mth.lerp(strength, 0.16F, 0.43F), travel, phase, width, alpha * 0.8F, -1);
        }

        private void flow(float angle, float focus, float reach, float travel, float phase, float width, float alpha, int side) {
            float head = 0.046F + reach * travel;
            float tail = Math.max(0.0F, head - reach * (0.62F - 0.38F * phase));
            for (int i = 0; i < 7; i++) {
                float a = i / 7.0F;
                float b = (i + 1.0F) / 7.0F;
                float distA = Mth.lerp(a, tail, head);
                float distB = Mth.lerp(b, tail, head);
                float low = flowHeight(focus, distA, phase, side);
                float high = flowHeight(focus, distB, phase, side);
                float thetaA = angle + side * distA;
                float thetaB = angle + side * distB;
                vertex(low - width * a, thetaA, 0.012F, a > 0.70F ? IVORY : GOLD, alpha * a);
                vertex(low + width * a, thetaA, 0.012F, a > 0.70F ? IVORY : GOLD, alpha * a);
                vertex(high + width * b, thetaB, 0.012F, b > 0.70F ? IVORY : GOLD, alpha * b);
                vertex(high - width * b, thetaB, 0.012F, b > 0.70F ? IVORY : GOLD, alpha * b);
            }
        }

        private static float flowHeight(float focus, float distance, float phase, int side) {
            return focus + side * 0.13F * Mth.sin(distance * 3.1F) - 0.052F * phase * phase;
        }

        private void vertex(float t, float angle, float lift, int color, float alpha) {
            vertexAtRadius(t, angle, surface.radiusAt(t) + lift, color, alpha);
        }

        private void vertexAtRadius(float t, float angle, float radius, int color, float alpha) {
            vertices.vertex(matrix, -Mth.sin(angle) * radius, surface.bottom() + t * surface.height(),
                            Mth.cos(angle) * radius * surface.depthScale())
                    .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, (int) (alpha * 255)).endVertex();
        }
    }

    private static final class ShellRenderState extends RenderStateShard {
        private static final RenderType TYPE = LodestoneRenderTypeRegistry.createGenericRenderType(
                "maledict_eggshell_ward", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                LodestoneRenderTypeRegistry.builder()
                                           .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                                           .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                           .setCullState(NO_CULL)
                                           .setDepthTestState(LEQUAL_DEPTH_TEST)
                                           .setWriteMaskState(COLOR_WRITE));

        private ShellRenderState() {
            super("maledict_eggshell_ward", () -> {}, () -> {});
        }
    }

    private EggshellWardShell() {
    }
}
