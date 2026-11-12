package org.brahypno.maledict.client.realm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EvokerFangsModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.brahypno.maledict.common.realm.wither.OssuaryFangs;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Splintered bone that follows the vanilla fang animation. */
public final class OssuaryFangsModel extends EvokerFangsModel<OssuaryFangs> {
    private static final int FACE_SIZE = 16;
    private static final int ATLAS_WIDTH = 4 * FACE_SIZE;
    private static final int ATLAS_HEIGHT = 2 * FACE_SIZE;

    public OssuaryFangsModel() {
        super(createRoot());
    }

    private static ModelPart createRoot() {
        ModelPart base = part(0, 0, Mth.PI,
                new Spike(0, -15, 0, 11, 9, 10, 2.7F, -0.8F, 0.5F, true),
                new Spike(-3.8F, -14, 2.3F, 5, 4, 9, 1.0F, -2.0F, 0.9F, true),
                new Spike(4.1F, -14, -1.8F, 4, 5, 8, 0.8F, 1.9F, -0.7F, true));
        ModelPart tallFang = part(1.8F, -2.6F, 0,
                new Spike(0, -8, 0, 4.7F, 5.4F, 25, 0.25F, -2.6F, 0.8F, false),
                new Spike(2, -7, 1.5F, 2.6F, 2.9F, 16, 0.4F, 2.8F, 1.2F, false));
        ModelPart shortFang = part(-1.6F, 2.7F, 0,
                new Spike(0, -8, 0, 4.4F, 4.9F, 21, 0.3F, -2.1F, -0.6F, false),
                new Spike(1.8F, -7, -1.7F, 2.3F, 2.5F, 13, 0.45F, 2.2F, -1.3F, false));
        shortFang.yRot = Mth.PI;
        return new ModelPart(List.of(), Map.of("base", base, "upper_jaw", tallFang, "lower_jaw", shortFang));
    }

    private static ModelPart part(float x, float z, float roll, ModelPart.Cube... cubes) {
        ModelPart part = new ModelPart(List.of(cubes), Map.of());
        part.setPos(x, 24.0F, z);
        part.zRot = roll;
        return part;
    }

    private static final class Spike extends ModelPart.Cube {
        private final Face[] faces = new Face[6];

        Spike(float x, float y, float z, float width, float depth, float height, float tipWidth,
              float leanX, float leanZ, boolean root) {
            super(0, 0, x - width / 2, y, z - depth / 2, width, height, depth,
                    0, 0, 0, false, ATLAS_WIDTH, ATLAS_HEIGHT, Set.of());
            Vector3f[] foot = ring(x, y, z, width, depth);
            Vector3f[] tip = ring(x + leanX, y + height, z + leanZ, tipWidth, tipWidth);
            // The upper row textures the bone shafts, the lower row textures the buried roots.
            int row = root ? FACE_SIZE : 0;
            float tipV = (row + 0.5F) / ATLAS_HEIGHT;
            float footV = (row + FACE_SIZE - 0.5F) / ATLAS_HEIGHT;
            for (int side = 0; side < 4; side++) {
                int next = (side + 1) % 4;
                float leftU = (side * FACE_SIZE + 0.5F) / ATLAS_WIDTH;
                float rightU = ((side + 1) * FACE_SIZE - 0.5F) / ATLAS_WIDTH;
                faces[side] = new Face(foot[side], tip[side], tip[next], foot[next], leftU, rightU, footV, tipV);
            }
            float leftU = 0.5F / ATLAS_WIDTH;
            float rightU = (FACE_SIZE - 0.5F) / ATLAS_WIDTH;
            faces[4] = new Face(tip[3], tip[2], tip[1], tip[0], leftU, rightU, tipV, tipV);
            faces[5] = new Face(foot[0], foot[1], foot[2], foot[3], leftU, rightU, footV, footV);
        }

        private static Vector3f[] ring(float x, float y, float z, float width, float depth) {
            float w = width / 2;
            float d = depth / 2;
            return new Vector3f[]{new Vector3f(x - w, y, z - d), new Vector3f(x + w, y, z - d),
                    new Vector3f(x + w, y, z + d), new Vector3f(x - w, y, z + d)};
        }

        @Override
        public void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                            float red, float green, float blue, float alpha) {
            for (Face face : faces) {
                Vector3f normal = face.normal;
                for (int i = 0; i < face.points.size(); i++) {
                    Vector3f point = face.points.get(i);
                    consumer.vertex(pose.pose(), point.x() / 16, point.y() / 16, point.z() / 16)
                            .color(red, green, blue, alpha)
                            .uv(i < 2 ? face.leftU : face.rightU, i == 0 || i == 3 ? face.edgeV : face.innerV)
                            .overlayCoords(overlay).uv2(light)
                            .normal(pose.normal(), normal.x(), normal.y(), normal.z()).endVertex();
                }
            }
        }
    }

    private record Face(List<Vector3f> points, Vector3f normal, float leftU, float rightU, float edgeV, float innerV) {
        Face(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float leftU, float rightU, float edgeV, float innerV) {
            this(List.of(a, b, c, d), new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize(),
                    leftU, rightU, edgeV, innerV);
        }
    }
}
