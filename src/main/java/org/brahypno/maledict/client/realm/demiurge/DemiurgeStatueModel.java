package org.brahypno.maledict.client.realm.demiurge;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeRig;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeStatueEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Visible quads share the exported poses used by body bounds and laser origins. */
final class DemiurgeStatueModel {
    private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/entity/demiurge_statue.mesh.json");
    private final List<List<Quad>> bones = new ArrayList<>();
    private final DemiurgeRig.Pose pose = DemiurgeRig.INSTANCE.pose();
    private record Vertex(float x, float y, float z, float u, float v) {}
    private record Quad(float nx, float ny, float nz, List<Vertex> vertices) {}

    DemiurgeStatueModel() {
        for (int i = 0; i < DemiurgeRig.INSTANCE.size; i++) bones.add(new ArrayList<>());
        try (var reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).openAsReader()) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var element : root.getAsJsonArray("quads")) {
                var quad = element.getAsJsonObject();
                JsonArray normal = quad.getAsJsonArray("n");
                List<Vertex> vertices = new ArrayList<>(4);
                for (var corner : quad.getAsJsonArray("v")) {
                    JsonArray v = corner.getAsJsonArray();
                    vertices.add(new Vertex(v.get(0).getAsFloat(), v.get(1).getAsFloat(),
                            v.get(2).getAsFloat(), v.get(3).getAsFloat(), v.get(4).getAsFloat()));
                }
                bones.get(quad.get("bone").getAsInt()).add(new Quad(normal.get(0).getAsFloat(), normal.get(1).getAsFloat(),
                        normal.get(2).getAsFloat(), List.copyOf(vertices)));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Demiurge statue model", exception);
        }
    }

    void animate(DemiurgeStatueEntity entity, float partialTick, float yaw, Vec3 origin) {
        pose.sample(entity.awakeningAge(partialTick), entity.slitherClock(partialTick),
                entity.attack(), entity.attackAge(partialTick), origin, yaw, entity::trailPoint, entity.beamDirection());
    }

    Vec3 eye(int side, Vec3 origin, float yaw) { return pose.eye(side, origin, yaw); }
    Vec3 headForward(float yaw) { return pose.headForward(yaw); }

    void render(PoseStack stack, VertexConsumer buffer, int light) {
        render(stack, buffer, light, -1, 0);
    }

    void render(PoseStack stack, VertexConsumer buffer, int light, float glow, float time) {
        for (int bone = 0; bone < bones.size(); bone++) {
            int brightness = glow < 0 ? 255 : (int) (255 * DemiurgeGlow.bone(glow, time, bone));
            var position = pose.positions[bone];
            stack.pushPose();
            stack.translate(position.x, position.y, position.z);
            stack.mulPose(pose.rotations[bone]);
            var transform = stack.last();
            for (Quad quad : bones.get(bone)) for (Vertex vertex : quad.vertices) {
                buffer.vertex(transform.pose(), vertex.x, vertex.y, vertex.z)
                        .color(brightness, brightness, brightness, 255).uv(vertex.u, vertex.v)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                        .normal(transform.normal(), quad.nx, quad.ny, quad.nz).endVertex();
            }
            stack.popPose();
        }
    }
}
