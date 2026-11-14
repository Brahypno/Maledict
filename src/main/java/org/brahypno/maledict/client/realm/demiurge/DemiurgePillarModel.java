package org.brahypno.maledict.client.realm.demiurge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Half-block end pieces and one-block shaft modules share the statue's pixel atlas. */
final class DemiurgePillarModel {
    private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/entity/demiurge_pillar.mesh.json");
    private static final float END_HEIGHT = .5F;

    private final List<Quad> base;
    private final List<Quad> shaft;
    private final List<Quad> crown;

    DemiurgePillarModel() {
        try (var reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).openAsReader()) {
            JsonObject mesh = JsonParser.parseReader(reader).getAsJsonObject();
            base = readSection(mesh.getAsJsonArray("base"));
            shaft = readSection(mesh.getAsJsonArray("shaft"));
            crown = readSection(mesh.getAsJsonArray("crown"));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Demiurge pillar model", exception);
        }
    }

    void render(PoseStack stack, VertexConsumer buffer, int light, int height, int brightness, boolean emissive) {
        renderSection(base, stack.last(), buffer, light, brightness, emissive);
        stack.pushPose();
        stack.translate(0, END_HEIGHT, 0);
        for (int segment = 0; segment < height - 1; segment++) {
            renderSection(shaft, stack.last(), buffer, light, brightness, emissive);
            stack.translate(0, 1, 0);
            stack.mulPose(Axis.YP.rotationDegrees(90));
        }
        stack.popPose();
        stack.pushPose();
        stack.translate(0, height - END_HEIGHT, 0);
        renderSection(crown, stack.last(), buffer, light, brightness, emissive);
        stack.popPose();
    }

    private static void renderSection(List<Quad> section, PoseStack.Pose transform, VertexConsumer buffer,
                                      int light, int brightness, boolean emissive) {
        for (Quad quad : section) {
            if (emissive && !quad.glow) continue;
            for (Vertex vertex : quad.vertices) {
                buffer.vertex(transform.pose(), vertex.x, vertex.y, vertex.z)
                        .color(brightness, brightness, brightness, 255).uv(vertex.u, vertex.v)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                        .normal(transform.normal(), quad.nx, quad.ny, quad.nz).endVertex();
            }
        }
    }

    private static List<Quad> readSection(JsonArray section) {
        List<Quad> quads = new ArrayList<>(section.size());
        for (var element : section) {
            JsonObject quad = element.getAsJsonObject();
            JsonArray normal = quad.getAsJsonArray("n");
            List<Vertex> vertices = new ArrayList<>(4);
            for (var corner : quad.getAsJsonArray("v")) {
                JsonArray v = corner.getAsJsonArray();
                vertices.add(new Vertex(v.get(0).getAsFloat(), v.get(1).getAsFloat(), v.get(2).getAsFloat(),
                        v.get(3).getAsFloat(), v.get(4).getAsFloat()));
            }
            quads.add(new Quad(normal.get(0).getAsFloat(), normal.get(1).getAsFloat(), normal.get(2).getAsFloat(),
                    quad.get("glow").getAsBoolean(), List.copyOf(vertices)));
        }
        return List.copyOf(quads);
    }

    private record Vertex(float x, float y, float z, float u, float v) {}
    private record Quad(float nx, float ny, float nz, boolean glow, List<Vertex> vertices) {}
}
