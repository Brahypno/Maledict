package org.brahypno.maledict.client.item.equipment.lightward;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.FastColor;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 扬弃逐光靴子的一块连续曲面：顶点与 UV 由几何 JSON 给出，走正常的 {@link ModelPart} 姿态与
 * 护甲材质渲染，所以它跟别的部件一样跟随腿部、接受受伤闪红与隐身。
 *
 * <p>1.21 的两处签名差异：{@code ModelPart.Cube#compile} 的颜色从四个 float 收成一个打包 ARGB
 * {@code int}（用 {@link FastColor.ARGB32} 拆开）；{@code VertexConsumer} 换成流式接口
 * （{@code addVertex(...)} + {@code setColor/setUv/setNormal}，没有 {@code endVertex()}）。
 *
 * <p>面按 QUADS 提交：顶点数不足四个的面重复最后一个顶点把它补成四边形，与 1.20.1 同码。
 */
final class LightwardMeshCube extends ModelPart.Cube {
    private record Face(int[] vertices, float[][] uv, Vector3f normal) {}

    private final float[][] vertices;
    private final List<Face> faces;

    LightwardMeshCube(JsonObject mesh) {
        // 只用姿态与包围盒：真正的三角形在 compile 里逐顶点提交，盒子本身不画。
        super(0, 0, -4, 5, -4, 8, 9, 8, 0, 0, 0, false, 128, 128, Set.of());
        var points = mesh.getAsJsonArray("vertices");
        vertices = new float[points.size()][3];
        for (int i = 0; i < points.size(); i++) {
            var point = points.get(i).getAsJsonArray();
            for (int axis = 0; axis < 3; axis++) {
                vertices[i][axis] = point.get(axis).getAsFloat() / 16;
            }
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
    public void compile(PoseStack.Pose pose, VertexConsumer sink, int light, int overlay, int color) {
        float alpha = FastColor.ARGB32.alpha(color) / 255.0F;
        float red = FastColor.ARGB32.red(color) / 255.0F;
        float green = FastColor.ARGB32.green(color) / 255.0F;
        float blue = FastColor.ARGB32.blue(color) / 255.0F;
        var position = new Vector4f();
        var normal = new Vector3f();
        for (var face : faces) {
            normal.set(face.normal).mul(pose.normal());
            for (int corner = 0; corner < 4; corner++) {
                // 护甲走 QUADS：把最后一个顶点重复一遍，三角形才能闭合。
                int index = Math.min(corner, face.vertices.length - 1);
                var point = vertices[face.vertices[index]];
                position.set(point[0], point[1], point[2], 1).mul(pose.pose());
                sink.addVertex(position.x, position.y, position.z)
                    .setColor(red, green, blue, alpha)
                    .setUv(face.uv[index][0], face.uv[index][1])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(normal.x, normal.y, normal.z);
            }
        }
    }
}
