package org.brahypno.maledict.client.item.equipment.lightward;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A continuous armor surface rendered through the normal ModelPart pose and armor material. */
final class LightwardMeshCube extends ModelPart.Cube {
    private record Face(int[] vertices, float[][] uv, Vector3f normal) {}
    private final float[][] vertices;
    private final List<Face> faces;

    LightwardMeshCube(JsonObject mesh) {
        super(0, 0, -4, 5, -4, 8, 9, 8, 0, 0, 0, false, 128, 128, Set.of());
        var points = mesh.getAsJsonArray("vertices");
        vertices = new float[points.size()][3];
        for (int i = 0; i < points.size(); i++) {
            var point = points.get(i).getAsJsonArray();
            for (int axis = 0; axis < 3; axis++) vertices[i][axis] = point.get(axis).getAsFloat() / 16;
        }
        faces = new ArrayList<>();
        for (var value : mesh.getAsJsonArray("faces")) {
            var face = value.getAsJsonObject();
            var indices = face.getAsJsonArray("vertices");
            var coordinates = face.getAsJsonArray("uv");
            int[] corners = new int[indices.size()];
            float[][] uv = new float[indices.size()][2];
            for (int i = 0; i < corners.length; i++) {
                corners[i] = indices.get(i).getAsInt();
                var pair = coordinates.get(i).getAsJsonArray();
                uv[i][0] = pair.get(0).getAsFloat() / 128;
                uv[i][1] = pair.get(1).getAsFloat() / 128;
            }
            var normal = face.getAsJsonArray("normal");
            faces.add(new Face(corners, uv, new Vector3f(normal.get(0).getAsFloat(),
                    normal.get(1).getAsFloat(), normal.get(2).getAsFloat())));
        }
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer sink, int light, int overlay,
                        float red, float green, float blue, float alpha) {
        var position = new Vector4f();
        var normal = new Vector3f();
        for (var face : faces) {
            normal.set(face.normal).mul(pose.normal());
            for (int corner = 0; corner < 4; corner++) {
                // Armor uses QUADS; duplicating the last corner closes a triangle.
                int index = Math.min(corner, face.vertices.length - 1);
                var point = vertices[face.vertices[index]];
                position.set(point[0], point[1], point[2], 1).mul(pose.pose());
                sink.vertex(position.x, position.y, position.z, red, green, blue, alpha,
                        face.uv[index][0], face.uv[index][1], overlay, light, normal.x, normal.y, normal.z);
            }
        }
    }
}
