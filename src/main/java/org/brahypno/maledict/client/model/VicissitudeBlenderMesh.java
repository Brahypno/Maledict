package org.brahypno.maledict.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;

import java.io.IOException;
import java.util.*;

/**
 * Rigid Blender mesh attached to vanilla ModelPart joints, also used by the emissive pass.
 */
final class VicissitudeBlenderMesh {
    @SuppressWarnings({"removal"})
    private static final ResourceLocation RESOURCE = new ResourceLocation(
            Maledict.MODID, "models/entity/first_vicissitude.mesh.json");
    private final Map<Joint, List<Triangle>> triangles = new EnumMap<>(Joint.class);

    private record Vertex(float x, float y, float z, float u, float v) {}

    private record Triangle(Vertex a, Vertex b, Vertex c, float nx, float ny, float nz) {}

    VicissitudeBlenderMesh() {
        // Model instances are rebuilt on resource reload; never retain a static mesh across F3+T.
        try (var reader = Minecraft.getInstance().getResourceManager()
                                   .getResourceOrThrow(RESOURCE).openAsReader()) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("format").getAsInt() != 1){
                throw new IllegalStateException("Unsupported First Vicissitude mesh format");
            }
            for (var element : root.getAsJsonArray("parts")) {
                var part = element.getAsJsonObject();
                Joint joint = Joint.valueOf(part.get("joint").getAsString().toUpperCase(Locale.ROOT));
                var target = triangles.computeIfAbsent(joint, ignored -> new ArrayList<>());
                for (var face : part.getAsJsonArray("triangles")) {
                    var triangle = face.getAsJsonObject();
                    var normal = triangle.getAsJsonArray("n");
                    var corners = triangle.getAsJsonArray("v");
                    target.add(new Triangle(vertex(corners.get(0).getAsJsonArray()),
                                            vertex(corners.get(1).getAsJsonArray()), vertex(corners.get(2).getAsJsonArray()),
                                            normal.get(0).getAsFloat(), normal.get(1).getAsFloat(), normal.get(2).getAsFloat()));
                }
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException("Cannot load Blender First Vicissitude model", exception);
        }
    }

    private static Vertex vertex(JsonArray values) {
        return new Vertex(values.get(0).getAsFloat() / 16.0F,
                          values.get(1).getAsFloat() / 16.0F, values.get(2).getAsFloat() / 16.0F,
                          values.get(3).getAsFloat(), values.get(4).getAsFloat());
    }

    void render(
            FirstVicissitudeBossModel model, PoseStack stack, VertexConsumer buffer,
            int light, int overlay, float red, float green, float blue, float alpha) {
        for (var entry : triangles.entrySet()) {
            stack.pushPose();
            model.poseStackTo(entry.getKey(), stack);
            var pose = stack.last();
            for (Triangle triangle : entry.getValue()) {
                emit(triangle.a, triangle, pose, buffer, light, overlay, red, green, blue, alpha);
                emit(triangle.b, triangle, pose, buffer, light, overlay, red, green, blue, alpha);
                emit(triangle.c, triangle, pose, buffer, light, overlay, red, green, blue, alpha);
                // Entity RenderTypes use QUADS: duplicate the final corner for a triangle.
                emit(triangle.c, triangle, pose, buffer, light, overlay, red, green, blue, alpha);
            }
            stack.popPose();
        }
    }

    private static void emit(
            Vertex vertex, Triangle triangle, PoseStack.Pose pose, VertexConsumer buffer,
            int light, int overlay, float red, float green, float blue, float alpha) {
        buffer.vertex(pose.pose(), vertex.x, vertex.y, vertex.z).color(red, green, blue, alpha)
              .uv(vertex.u, vertex.v).overlayCoords(overlay).uv2(light)
              .normal(pose.normal(), triangle.nx, triangle.ny, triangle.nz).endVertex();
    }
}
