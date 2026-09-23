package org.brahypno.maledict.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;
import org.brahypno.maledict.rig.VicissitudeFeatherShed;
import com.mojang.math.Axis;

import java.io.IOException;
import java.util.*;

/**
 * Rigid Blender mesh attached to vanilla ModelPart joints, also used by the emissive pass.
 */
final class VicissitudeBlenderMesh {
    private static final ResourceLocation RESOURCE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/entity/first_vicissitude.mesh.json");
    private final List<MeshPart> parts = new ArrayList<>();
    private record MeshPart(Joint joint, List<Triangle> triangles, float delay, float deployScale, Vertex center) {}

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
                List<Triangle> target = new ArrayList<>();
                for (var face : part.getAsJsonArray("triangles")) {
                    var triangle = face.getAsJsonObject();
                    var normal = triangle.getAsJsonArray("n");
                    var corners = triangle.getAsJsonArray("v");
                    target.add(new Triangle(vertex(corners.get(0).getAsJsonArray()),
                                            vertex(corners.get(1).getAsJsonArray()), vertex(corners.get(2).getAsJsonArray()),
                                            normal.get(0).getAsFloat(), normal.get(1).getAsFloat(), normal.get(2).getAsFloat()));
                }
                float x=0, y=0, z=0;
                for (Triangle t : target) { x+=t.a.x+t.b.x+t.c.x; y+=t.a.y+t.b.y+t.c.y; z+=t.a.z+t.b.z+t.c.z; }
                float count=target.size()*3F;
                parts.add(new MeshPart(joint, target, part.has("shed_delay") ? part.get("shed_delay").getAsFloat() : -1,
                        part.has("deploy_scale") ? part.get("deploy_scale").getAsFloat() : 1,
                        new Vertex(x/count,y/count,z/count,0,0)));
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
        for (var part : parts) {
            float elapsed = part.delay < 0 ? 0 : VicissitudeFeatherShed.elapsed(model.sheddingTicks(),part.delay);
            if (elapsed >= VicissitudeFeatherShed.FALL_TICKS) continue;
            stack.pushPose();
            if (elapsed > 0) {
                float side=part.joint.name().startsWith("WING_LEFT") ? 1 : -1;
                stack.translate(side*.014F*elapsed, VicissitudeFeatherShed.drop(elapsed)/16F,
                        .06F*(float)(Math.sin(elapsed*.3F+part.delay)-Math.sin(part.delay)));
                model.poseStackToRelease(part.joint,stack,part.delay,elapsed);
                stack.translate(part.center.x,part.center.y,part.center.z);
                stack.mulPose(Axis.ZP.rotationDegrees(side*elapsed*5));
                stack.mulPose(Axis.XP.rotationDegrees(elapsed*8));
                float scale=VicissitudeFeatherShed.scale(elapsed);
                stack.scale(scale,scale,scale);
                stack.translate(-part.center.x,-part.center.y,-part.center.z);
            } else model.poseStackTo(part.joint, stack);
            if (part.deployScale < 1) {
                float progress = Math.max(0, Math.min(1, (model.sheddingTicks()-8)/36F));
                progress = progress*progress*(3-2*progress);
                float scale = part.deployScale+(1-part.deployScale)*progress;
                stack.scale(scale,scale,scale);
            }
            var pose = stack.last();
            for (Triangle triangle : part.triangles) {
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
