package org.brahypno.maledict.client.realm.demiurge;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** The Blender export's surviving quads, with shared UV patches and outward winding. */
final class DemiurgeStatueModel {
    private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/entity/demiurge_statue.mesh.json");
    private final List<Quad> quads = new ArrayList<>();

    private record Vertex(float x, float y, float z, float u, float v) {}
    private record Quad(float nx, float ny, float nz, List<Vertex> vertices) {}

    DemiurgeStatueModel() {
        try (var reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).openAsReader()) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("format").getAsInt() != 1) {
                throw new IllegalStateException("Unsupported Demiurge statue mesh format");
            }
            for (var element : root.getAsJsonArray("quads")) {
                var quad = element.getAsJsonObject();
                JsonArray normal = quad.getAsJsonArray("n");
                List<Vertex> vertices = new ArrayList<>(4);
                for (var corner : quad.getAsJsonArray("v")) {
                    JsonArray v = corner.getAsJsonArray();
                    vertices.add(new Vertex(v.get(0).getAsFloat(), v.get(1).getAsFloat(),
                            v.get(2).getAsFloat(), v.get(3).getAsFloat(), v.get(4).getAsFloat()));
                }
                if (vertices.size() != 4) {
                    throw new IllegalStateException("Demiurge statue mesh requires four vertices per quad");
                }
                quads.add(new Quad(normal.get(0).getAsFloat(), normal.get(1).getAsFloat(),
                        normal.get(2).getAsFloat(), List.copyOf(vertices)));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Demiurge statue model", exception);
        }
    }

    void render(PoseStack stack, VertexConsumer buffer, int light) {
        var pose = stack.last();
        for (Quad quad : quads) {
            for (Vertex vertex : quad.vertices) {
                buffer.vertex(pose.pose(), vertex.x, vertex.y, vertex.z)
                        .color(255, 255, 255, 255).uv(vertex.u, vertex.v)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                        .normal(pose.normal(), quad.nx, quad.ny, quad.nz).endVertex();
            }
        }
    }
}
