package org.brahypno.maledict.client.realm;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** A still, untextured sky dome; ordinary block and entity colors are untouched. */
public final class BlightedRealmEffects extends DimensionSpecialEffects {
    public BlightedRealmEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float daylight) {
        return color.scale(0.85D);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float time, float partialTick) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix,
                             Camera camera, Matrix4f projection, boolean foggy, Runnable setupFog) {
        setupFog.run();
        if (foggy || camera.getFluidInCamera() != FogType.NONE) return true;
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = modelViewMatrix;
        float[] elevations = {-(float) Math.PI / 2, 0, (float) Math.PI / 6, (float) Math.PI / 3, (float) Math.PI / 2};
        for (int band = 0; band < elevations.length - 1; band++) {
            for (int segment = 0; segment < 48; segment++) {
                float first = segment * (float) Math.PI * 2 / 48;
                float next = (segment + 1) * (float) Math.PI * 2 / 48;
                vertex(buffer, matrix, first, elevations[band]);
                vertex(buffer, matrix, next, elevations[band]);
                vertex(buffer, matrix, next, elevations[band + 1]);
                vertex(buffer, matrix, first, elevations[band + 1]);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        return true;
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float azimuth, float elevation) {
        float height = (float) Math.sin(elevation);
        float radius = (float) Math.cos(elevation) * 128;
        float blend = Math.max(0, height);
        int red = Math.round(0x55 + (0x16 - 0x55) * blend);
        int green = Math.round(0x42 + (0x12 - 0x42) * blend);
        int blue = Math.round(0x61 + (0x1E - 0x61) * blend);
        buffer.addVertex(matrix, (float) Math.cos(azimuth) * radius, height * 128,
                (float) Math.sin(azimuth) * radius).setColor(red, green, blue, 255);
    }
}
