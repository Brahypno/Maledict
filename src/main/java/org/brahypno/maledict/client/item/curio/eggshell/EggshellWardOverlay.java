package org.brahypno.maledict.client.item.curio.eggshell;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Matrix4f;

import static org.brahypno.maledict.client.item.curio.eggshell.EggshellWardShell.BONE;
import static org.brahypno.maledict.client.item.curio.eggshell.EggshellWardShell.BRONZE;
import static org.brahypno.maledict.client.item.curio.eggshell.EggshellWardShell.GOLD;
import static org.brahypno.maledict.client.item.curio.eggshell.EggshellWardShell.IVORY;
import static org.brahypno.maledict.client.item.curio.eggshell.EggshellWardShell.SHADOW;

/** Two curved sections of the ward as seen from inside the shell. */
public final class EggshellWardOverlay {
    private static final int RIM_STEPS = 36;
    private static final int SHADED_BONE = 0x8A6E5D;
    private static final int WARM_BONE = 0xC5AB8E;
    private static final float WALL_WIDTH = 0.89F;
    private static final float WALL_FACE_OPACITY = 0.95F;
    private static final float SPARK_LIFETIME = 3.7F;
    private static final float NODE_LIFETIME = 2.6F;
    private static final Dust[] DUST = {
        new Dust(0.27F, 0.8F, 6.5F, 0.072F, 0.46F),
        new Dust(0.46F, 1.8F, 5.8F, -0.065F, 0.65F),
        new Dust(0.65F, 1.2F, 7.4F, 0.059F, 0.43F),
        new Dust(0.81F, 2.7F, 5.3F, -0.081F, 0.53F)
    };
    private static final Spark[] SPARKS = {
        new Spark(-0.032F, 0.05F, 0.09F, 0.52F),
        new Spark(0.018F, 0.18F, -0.074F, 0.58F),
        new Spark(-0.064F, 0.33F, -0.086F, 0.39F),
        new Spark(0.058F, 0.44F, 0.073F, 0.47F),
        new Spark(0.008F, 0.64F, 0.081F, 0.38F)
    };

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = gui.getMinecraft();
        var player = minecraft.player;
        var camera = minecraft.gameRenderer.getMainCamera();
        if (minecraft.options.hideGui || player == null || camera.getEntity() != player || camera.isDetached()) {
            return;
        }
        var pulse = EggshellWardClient.pulses().get(player);
        if (pulse == null) {
            return;
        }
        long tick = player.level().getGameTime();
        float full = pulse.fullAlpha(tick, partialTick);
        float accent = pulse.accentAlpha(tick, partialTick);
        if (full < 0.01F && accent < 0.01F) {
            return;
        }

        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        var vertices = Tesselator.getInstance().getBuilder();
        vertices.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        drawRims(vertices, graphics.pose().last().pose(), width, height, pulse, tick, partialTick, camera.getYRot());
        var mesh = vertices.endOrDiscardIfEmpty();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    static void drawRims(VertexConsumer vertices, Matrix4f matrix, int width, int height,
                         EggshellWardClient.Pulse pulse, long tick, float partialTick, float cameraYaw) {
        Vec3 direction = pulse.direction();
        Vec3 forward = Vec3.directionFromRotation(0, cameraYaw);
        float side = (float) (forward.x * direction.z - forward.z * direction.x);
        float front = (float) forward.dot(direction);
        boolean hasDirection = direction.lengthSqr() > 0;
        float full = pulse.fullAlpha(tick, partialTick);
        float accent = pulse.accentAlpha(tick, partialTick);
        float strength = pulse.accentStrength();
        float closure = pulse.closure(tick, partialTick);
        // Put a physical limit on the inset as well as a relative one, for ultrawide displays.
        float span = Math.min(width * 0.165F, height * 0.33F);
        var left = new Rim(vertices, matrix, 0, 1, height * 0.96F, height * 0.08F, span, closure, 0.15F, 12);
        var right = new Rim(vertices, matrix, width, -1, height * 0.91F, height * 0.15F, span, closure, 1.35F, 21);
        float fullAge = pulse.fullAge(tick, partialTick);
        float age = pulse.accentAge(tick, partialTick);
        float focus = 0.56F + front * 0.23F;
        left.draw(full, fullAge, Impact.project(focus, age, strength, accent, -side, hasDirection));
        right.draw(full * 0.9F, fullAge, Impact.project(focus, age, strength, accent, side, hasDirection));
    }

    private record Dust(float at, float delay, float life, float travel, float size) {}

    private record Spark(float at, float delay, float travel, float size) {}

    private record Impact(float center, float reach, float alpha, float age, float strength,
                          float side, boolean hasDirection) {
        static Impact project(float focus, float age, float strength, float accent, float side, boolean hasDirection) {
            float phase = Mth.clamp(age / 4.5F, 0.0F, 1.0F);
            float center = Mth.lerp(phase * (2.0F - phase) * 0.56F, focus, 0.49F);
            float reach = Mth.lerp(strength, 0.17F, 0.31F) * (1.0F - 0.24F * phase);
            float weight = hasDirection ? 0.58F + side * 0.42F : 0.68F;
            return new Impact(center, reach, accent * weight, age, strength, side, hasDirection);
        }

        float lightAt(float t) {
            float weight = Math.max(0.0F, 1.0F - Math.abs(t - center) / reach);
            return alpha * weight * weight * (3.0F - 2.0F * weight);
        }
    }

    private record Rim(VertexConsumer vertices, Matrix4f matrix, float edge, int inward,
                       float bottom, float top, float span, float closure, float variation, int missingGold) {
        void draw(float full, float age, Impact impact) {
            if (full < 0.01F && impact.alpha() < 0.01F) {
                return;
            }
            shell(full, impact);
            if (full >= 0.01F) {
                dust(age, full);
            }
            if (impact.alpha() >= 0.01F) {
                sparks(impact);
                if (impact.strength() >= 0.56F && impact.age() >= 0.0F && impact.age() < NODE_LIFETIME) {
                    float etch = (missingGold + 6.5F) / RIM_STEPS;
                    gleam(etch, -0.347F, 0.58F, 0.81F, IVORY,
                          impact.lightAt(etch) * 0.63F * (1.0F - impact.age() / NODE_LIFETIME));
                }
            }
        }

        private void shell(float full, Impact impact) {
            for (int i = 0; i < RIM_STEPS; i++) {
                float a = i / (float) RIM_STEPS;
                float b = (i + 1.0F) / RIM_STEPS;
                float lightA = impact.lightAt(a) * silhouette(a);
                float lightB = impact.lightAt(b) * silhouette(b);
                // A late impact reveals a part of the same wall and illuminates it in this pass.
                float opacityA = Math.max(full * silhouette(a), lightA * 0.64F);
                float opacityB = Math.max(full * silhouette(b), lightB * 0.64F);
                if (Math.max(opacityA, opacityB) < 0.01F) {
                    continue;
                }
                strip(a, b, -1.0F, -0.76F, SHADOW, SHADED_BONE, opacityA, opacityB, 0.31F, 0.76F);
                strip(a, b, -0.76F, -0.46F, SHADED_BONE, WARM_BONE, opacityA, opacityB, 0.76F, 0.88F);
                strip(a, b, -0.46F, -0.15F, WARM_BONE, BONE, opacityA, opacityB, 0.88F, WALL_FACE_OPACITY);
                strip(a, b, -0.15F, 0.0F, BONE, BRONZE, opacityA, opacityB, WALL_FACE_OPACITY, 0.65F);
                if (Math.max(lightA, lightB) >= 0.01F) {
                    strip(a, b, -0.62F, -0.13F, GOLD, BONE, lightA, lightB, 0.06F, 0.32F);
                    strip(a, b, -0.13F, 0.014F, BONE, GOLD, lightA, lightB, 0.32F, 0.68F);
                }
                if (i < missingGold || i > missingGold + 2) {
                    int metal = i % 9 < 3 ? BRONZE : GOLD;
                    strip(a, b, -0.04F, 0.0F, metal, metal, opacityA, opacityB, 0.26F, 0.76F);
                }
                if (i >= missingGold + 4 && i <= missingGold + 8) {
                    strip(a, b, -0.39F, -0.33F, SHADED_BONE, BONE, opacityA, opacityB, 0.29F, 0.53F);
                }
                if (i >= 6 && i < missingGold && i % 6 != 5) {
                    strip(a, b, -0.68F, -0.65F, SHADOW, SHADED_BONE, opacityA, opacityB, 0.25F, 0.42F);
                }
            }
        }

        private void dust(float age, float alpha) {
            int count = inward > 0 ? DUST.length : DUST.length - 1;
            for (int i = 0; i < count; i++) {
                Dust dust = DUST[i];
                float life = (age - dust.delay() - (inward > 0 ? 0.0F : 0.45F)) / dust.life();
                if (life <= 0.0F || life >= 1.0F) {
                    continue;
                }
                float t = dust.at() + dust.travel() * life + (inward > 0 ? 0.0F : 0.047F);
                float offset = -0.28F - 0.24F * Mth.sin(Mth.PI * life);
                gleam(t, offset, dust.size(), i % 2 == 0 ? 1.15F : 0.72F,
                      i % 2 == 0 ? BONE : GOLD, alpha * 0.40F * Mth.sin(Mth.PI * life));
            }
        }

        private void sparks(Impact impact) {
            int count = impact.hasDirection() ? SPARKS.length : 2;
            for (int i = 0; i < count; i++) {
                Spark spark = SPARKS[i];
                float life = (impact.age() - spark.delay()) / (SPARK_LIFETIME - 0.29F * (i % 3));
                if (life <= 0.0F || life >= 1.0F) {
                    continue;
                }
                float weight = impact.hasDirection() ? Mth.clamp(1.0F + impact.side() - 0.31F * i, 0.0F, 1.0F) : 0.62F;
                float travel = life * (2.0F - life) * Mth.lerp(impact.strength(), 0.62F, 1.0F);
                float t = Mth.clamp(impact.center() + spark.at() + spark.travel() * travel, 0.15F, 0.90F);
                float offset = -0.13F - (0.18F + 0.043F * i) * Mth.sin(Mth.PI * life);
                float brightness = impact.lightAt(t) * weight * Math.min(1.0F, 8.0F * life) * (1.0F - life);
                gleam(t, offset, spark.size() * 1.21F, 1.26F + 0.24F * (i % 3),
                      life < 0.48F && i < 2 ? IVORY : GOLD, brightness);
            }
        }

        private void gleam(float t, float offset, float halfWidth, float halfLength, int color, float alpha) {
            if (alpha < 0.015F) {
                return;
            }
            float x = xAt(t, offset);
            float y = yAt(t);
            float dx = xAt(t + 0.005F, offset) - xAt(t - 0.005F, offset);
            float dy = (top - bottom) * 0.01F;
            float unit = Mth.clamp((bottom - top) / 165.0F, 0.73F, 1.8F);
            float inverseLength = unit / Mth.sqrt(dx * dx + dy * dy);
            float tx = dx * inverseLength;
            float ty = dy * inverseLength;
            vertex(x - tx * halfLength, y - ty * halfLength, color, alpha * 0.22F);
            vertex(x - ty * halfWidth, y + tx * halfWidth, color, alpha);
            vertex(x + tx * halfLength, y + ty * halfLength, color, alpha * 0.70F);
            vertex(x + ty * halfWidth, y - tx * halfWidth, color, alpha);
        }

        private static float silhouette(float t) {
            return Math.min(1.0F, 4.2F * Math.min(t, 1.0F - t));
        }

        private float inset(float t) {
            float oval = Mth.sin(Mth.PI * t);
            float swell = oval * (0.70F + 0.07F * Mth.sin(6.0F * t + variation) + 0.065F * closure);
            float ridge = 0.043F * Mth.sin(14.0F * t + variation) * oval;
            return Math.min(1.0F, 0.19F + 0.11F * t + swell + ridge);
        }

        private void strip(float a, float b, float outer, float inner, int outerColor, int innerColor,
                           float alphaA, float alphaB, float outsideOpacity, float insideOpacity) {
            if (inward > 0) {
                point(a, outer, outerColor, alphaA * outsideOpacity);
                point(a, inner, innerColor, alphaA * insideOpacity);
                point(b, inner, innerColor, alphaB * insideOpacity);
                point(b, outer, outerColor, alphaB * outsideOpacity);
            } else {
                point(a, inner, innerColor, alphaA * insideOpacity);
                point(a, outer, outerColor, alphaA * outsideOpacity);
                point(b, outer, outerColor, alphaB * outsideOpacity);
                point(b, inner, innerColor, alphaB * insideOpacity);
            }
        }

        private void point(float t, float offset, int color, float alpha) {
            vertex(xAt(t, offset), yAt(t), color, alpha);
        }

        private float xAt(float t, float offset) {
            if (offset == -1.0F) {
                return edge;
            }
            float lip = inset(t);
            float wallWidth = Math.min(lip * 0.96F,
                    (WALL_WIDTH + 0.13F * Mth.sin(5.0F * t + variation)) * silhouette(t));
            return edge + inward * (lip + offset * wallWidth) * span;
        }

        private float yAt(float t) {
            return Mth.lerp(t, bottom, top);
        }

        private void vertex(float x, float y, int color, float alpha) {
            vertices.vertex(matrix, x, y, 0)
                    .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, (int) (alpha * 255)).endVertex();
        }
    }

    private EggshellWardOverlay() {
    }
}
