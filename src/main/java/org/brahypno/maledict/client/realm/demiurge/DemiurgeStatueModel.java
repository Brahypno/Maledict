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
    private final StoneUv stoneUv;
    private record Vertex(float x, float y, float z, float u, float v) {}
    private record Quad(float nx, float ny, float nz, List<Vertex> vertices) {}
    private record StoneUv(float u0, float v0, float u1, float v1) {}

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
        stoneUv = sampleStoneUv();
    }

    /** A real body-quad UV region, so the procedural joints share the statue's stone grain. */
    private StoneUv sampleStoneUv() {
        int bone = DemiurgeRig.INSTANCE.jointBone(0);
        if (bone < 0 || bone >= bones.size() || bones.get(bone).isEmpty()) {
            return new StoneUv(0.0F, 0.0F, 1.0F, 1.0F);
        }
        float u0 = 1.0F, v0 = 1.0F, u1 = 0.0F, v1 = 0.0F;
        for (var vertex : bones.get(bone).get(0).vertices()) {
            u0 = Math.min(u0, vertex.u());
            v0 = Math.min(v0, vertex.v());
            u1 = Math.max(u1, vertex.u());
            v1 = Math.max(v1, vertex.v());
        }
        return new StoneUv(u0, v0, u1, v1);
    }

    void animate(DemiurgeStatueEntity entity, float partialTick, float yaw, Vec3 origin) {
        pose.sample(entity.awakeningAge(partialTick), entity.slitherClock(partialTick),
                entity.attack(), entity.attackAge(partialTick), origin, yaw, entity::trailPoint,
                entity.bodyCut(), entity.beamDirection(), entity.sweepState(partialTick));
    }

    Vec3 eye(int side, Vec3 origin, float yaw) { return pose.eye(side, origin, yaw); }
    Vec3 headForward(float yaw) { return pose.headForward(yaw); }

    /**
     * Base pass. {@code tint} scales the carved stone towards black and {@code dissolve} drops whole
     * quads in a stable hash order, so the dying statue flakes away instead of blinking out.
     */
    void render(PoseStack stack, VertexConsumer buffer, int light, float tint, float dissolve) {
        int brightness = (int) (255 * Math.max(0.0F, Math.min(1.0F, tint)));
        for (int bone = 0; bone < bones.size(); bone++) {
            if (pose.isHidden(bone)) continue;
            var position = pose.positions[bone];
            stack.pushPose();
            stack.translate(position.x, position.y, position.z);
            stack.mulPose(pose.rotations[bone]);
            var transform = stack.last();
            List<Quad> quads = bones.get(bone);
            for (int index = 0; index < quads.size(); index++) {
                if (dissolve > dissolveThreshold(bone, index)) continue;
                Quad quad = quads.get(index);
                for (Vertex vertex : quad.vertices) {
                    buffer.vertex(transform.pose(), vertex.x, vertex.y, vertex.z)
                            .color(brightness, brightness, brightness, 255).uv(vertex.u, vertex.v)
                            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                            .normal(transform.normal(), quad.nx, quad.ny, quad.nz).endVertex();
                }
            }
            stack.popPose();
        }
    }

    /**
     * Fills the gap between two rigid sections with a short stone sleeve. The authored body is a
     * chain of separate carved blocks, so a one-block terrain step leaves the end faces vertically
     * apart; the sleeve is only drawn when the joint actually separates, keeping flat ground clean.
     */
    void renderJoints(PoseStack stack, VertexConsumer buffer, int light, float tint, float dissolve) {
        if (dissolve > .6F || !pose.grounded()) return;
        int brightness = (int) (255 * Math.max(0.0F, Math.min(1.0F, tint)));
        var transform = stack.last();
        int count = DemiurgeRig.INSTANCE.jointCount();
        for (int index = 0; index + 1 < count; index++) {
            int bone = DemiurgeRig.INSTANCE.jointBone(index);
            int nextBone = DemiurgeRig.INSTANCE.jointBone(index + 1);
            if (pose.isHidden(bone) || pose.isHidden(nextBone)) continue;
            Vec3 start = pose.jointEnd(index, false);
            Vec3 end = pose.jointEnd(index + 1, true);
            double gap = start.distanceTo(end);
            if (gap < .08 || gap > 2.0) continue;
            double radius = Math.min(DemiurgeRig.INSTANCE.jointRadius(index),
                    DemiurgeRig.INSTANCE.jointRadius(index + 1));
            if (radius < .05) continue;
            renderSleeve(transform, buffer, start, end, radius, light, brightness);
        }
    }

    private void renderSleeve(PoseStack.Pose transform, VertexConsumer buffer, Vec3 startPoint, Vec3 endPoint,
                              double radius, int light, int brightness) {
        Vec3 axis = endPoint.subtract(startPoint);
        double gap = axis.length();
        if (gap < 1.0E-4) return;
        Vec3 direction = axis.scale(1.0 / gap);
        Vec3 helper = Math.abs(direction.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = direction.cross(helper).normalize();
        Vec3 v = direction.cross(u).normalize();
        double extension = Math.max(.06, radius * .35);
        Vec3 start = startPoint.subtract(direction.scale(extension));
        Vec3 end = endPoint.add(direction.scale(extension));
        int sides = 8;
        Vec3[] startRing = new Vec3[sides];
        Vec3[] endRing = new Vec3[sides];
        for (int side = 0; side < sides; side++) {
            double angle = Math.PI * 2 * side / sides;
            Vec3 offset = u.scale(Math.cos(angle) * radius).add(v.scale(Math.sin(angle) * radius));
            startRing[side] = start.add(offset);
            endRing[side] = end.add(offset);
        }
        for (int side = 0; side < sides; side++) {
            int next = (side + 1) % sides;
            double middle = Math.PI * 2 * (side + .5) / sides;
            Vec3 normal = u.scale(Math.cos(middle)).add(v.scale(Math.sin(middle))).normalize();
            float ua = stoneUv.u0 + (stoneUv.u1 - stoneUv.u0) * side / (float) sides;
            float ub = stoneUv.u0 + (stoneUv.u1 - stoneUv.u0) * (side + 1) / (float) sides;
            vertex(buffer, transform, startRing[side], ua, stoneUv.v0, normal, light, brightness);
            vertex(buffer, transform, startRing[next], ub, stoneUv.v0, normal, light, brightness);
            vertex(buffer, transform, endRing[next], ub, stoneUv.v1, normal, light, brightness);
            vertex(buffer, transform, endRing[side], ua, stoneUv.v1, normal, light, brightness);
        }
        cap(buffer, transform, start, startRing, direction.scale(-1), light, brightness);
        cap(buffer, transform, end, endRing, direction, light, brightness);
    }

    private void cap(VertexConsumer buffer, PoseStack.Pose transform, Vec3 center, Vec3[] ring, Vec3 normal,
                     int light, int brightness) {
        float u = (stoneUv.u0 + stoneUv.u1) * .5F;
        float v = (stoneUv.v0 + stoneUv.v1) * .5F;
        for (int side = 0; side < ring.length; side++) {
            int next = (side + 1) % ring.length;
            vertex(buffer, transform, center, u, v, normal, light, brightness);
            vertex(buffer, transform, ring[side], u, v, normal, light, brightness);
            vertex(buffer, transform, ring[next], u, v, normal, light, brightness);
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose transform, Vec3 point, float u, float v,
                               Vec3 normal, int light, int brightness) {
        buffer.vertex(transform.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(brightness, brightness, brightness, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(transform.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }

    /** Emissive pass; the remaining glow fades before the stone starts to crumble. */
    void renderEmissive(PoseStack stack, VertexConsumer buffer, int light, float glow, float time) {
        for (int bone = 0; bone < bones.size(); bone++) {
            if (pose.isHidden(bone)) continue;
            int brightness = (int) (255 * DemiurgeGlow.bone(glow, time, bone));
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

    /** Stable per-quad phase in [0, 1), so the dissolve order cannot change between frames. */
    private static float dissolveThreshold(int bone, int quad) {
        int hash = bone * 0x9E3779B9 + (quad + 1) * 0x85EBCA6B;
        hash ^= hash >>> 16;
        return (hash & 0xFFFF) / 65536.0F;
    }
}
