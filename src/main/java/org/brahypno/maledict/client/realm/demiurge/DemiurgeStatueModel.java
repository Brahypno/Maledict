package org.brahypno.maledict.client.realm.demiurge;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeAwakening;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeBodyChain;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeSlither;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeStatueEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** The Blender export's surviving quads, with shared UV patches and outward winding. */
final class DemiurgeStatueModel {
    private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/entity/demiurge_statue.mesh.json");
    private final List<List<Quad>> bones = new ArrayList<>();
    private final BonePose[][] frames;
    private final BonePose[][] slither;
    private final BonePose[][] transition;
    private final Vector3f[] positions;
    private final Quaternionf[] rotations;
    private final DemiurgeBodyChain bodyChain;

    private record Vertex(float x, float y, float z, float u, float v) {}
    private record Quad(float nx, float ny, float nz, List<Vertex> vertices) {}
    private record BonePose(Vector3f position, Quaternionf rotation) {}

    DemiurgeStatueModel() {
        try (var reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).openAsReader()) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("format").getAsInt() != 2) {
                throw new IllegalStateException("Unsupported Demiurge statue mesh format");
            }
            var animation = root.getAsJsonObject("animation");
            if (animation.get("duration").getAsInt() != DemiurgeAwakening.DURATION
                    || animation.get("roar_tick").getAsInt() != DemiurgeAwakening.ROAR_TICK) {
                throw new IllegalStateException("Demiurge awakening asset and server timeline differ");
            }
            int count = animation.getAsJsonArray("bones").size();
            positions = new Vector3f[count];
            rotations = new Quaternionf[count];
            for (int i = 0; i < count; i++) {
                bones.add(new ArrayList<>());
                positions[i] = new Vector3f();
                rotations[i] = new Quaternionf();
            }
            List<DemiurgeBodyChain.Joint> joints = new ArrayList<>();
            for (var value : animation.getAsJsonArray("body_chain")) {
                var joint = value.getAsJsonObject();
                var front = joint.getAsJsonArray("front");
                var back = joint.getAsJsonArray("back");
                List<Vector3f> corners = new ArrayList<>();
                for (var valueCorner : joint.getAsJsonArray("corners")) {
                    var corner = valueCorner.getAsJsonArray();
                    corners.add(new Vector3f(corner.get(0).getAsFloat(), corner.get(1).getAsFloat(), corner.get(2).getAsFloat()));
                }
                joints.add(new DemiurgeBodyChain.Joint(joint.get("bone").getAsInt(),
                        new Vector3f(front.get(0).getAsFloat(), front.get(1).getAsFloat(), front.get(2).getAsFloat()),
                        new Vector3f(back.get(0).getAsFloat(), back.get(1).getAsFloat(), back.get(2).getAsFloat()), corners));
            }
            bodyChain = new DemiurgeBodyChain(joints);
            frames = new BonePose[DemiurgeAwakening.DURATION + 1][count];
            var poses = animation.getAsJsonArray("frames");
            for (int tick = 0; tick < frames.length; tick++) {
                var frame = poses.get(tick).getAsJsonArray();
                for (int bone = 0; bone < count; bone++) {
                    var transform = frame.get(bone).getAsJsonObject();
                    var p = transform.getAsJsonArray("p");
                    var q = transform.getAsJsonArray("q");
                    frames[tick][bone] = new BonePose(new Vector3f(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat()),
                            new Quaternionf(q.get(0).getAsFloat(), q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat()));
                }
            }
            if (animation.get("slither_duration").getAsInt() != DemiurgeSlither.CYCLE_TICKS
                    || animation.get("ground_transition").getAsInt() != DemiurgeSlither.TRANSITION_TICKS) {
                throw new IllegalStateException("Demiurge ground locomotion asset and server timeline differ");
            }
            slither = new BonePose[DemiurgeSlither.CYCLE_TICKS + 1][count];
            var groundFrames = animation.getAsJsonArray("slither_frames");
            for (int tick = 0; tick < slither.length; tick++) {
                var frame = groundFrames.get(tick).getAsJsonArray();
                for (int bone = 0; bone < count; bone++) {
                    var transform = frame.get(bone).getAsJsonObject();
                    var p = transform.getAsJsonArray("p");
                    var q = transform.getAsJsonArray("q");
                    slither[tick][bone] = new BonePose(new Vector3f(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat()),
                            new Quaternionf(q.get(0).getAsFloat(), q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat()));
                }
            }
            transition = new BonePose[DemiurgeSlither.TRANSITION_TICKS + 1][count];
            var transitions = animation.getAsJsonArray("ground_transition_frames");
            for (int tick = 0; tick < transition.length; tick++) {
                var frame = transitions.get(tick).getAsJsonArray();
                for (int bone = 0; bone < count; bone++) {
                    var transform = frame.get(bone).getAsJsonObject();
                    var p = transform.getAsJsonArray("p");
                    var q = transform.getAsJsonArray("q");
                    transition[tick][bone] = new BonePose(new Vector3f(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat()),
                            new Quaternionf(q.get(0).getAsFloat(), q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat()));
                }
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
                bones.get(quad.get("bone").getAsInt()).add(new Quad(normal.get(0).getAsFloat(), normal.get(1).getAsFloat(),
                        normal.get(2).getAsFloat(), List.copyOf(vertices)));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Demiurge statue model", exception);
        }
    }

    void render(PoseStack stack, VertexConsumer buffer, int light, DemiurgeStatueEntity entity, float partialTick, float yaw) {
        float age = entity.awakeningAge(partialTick);
        float time = Math.max(0, Math.min(DemiurgeAwakening.DURATION, age));
        int tick = (int) time;
        int next = Math.min(DemiurgeAwakening.DURATION, tick + 1);
        float stance = DemiurgeSlither.groundBlend(age);
        float clock = entity.slitherClock(partialTick) % DemiurgeSlither.CYCLE_TICKS;
        int groundTick = (int) clock;
        Vec3 origin = new Vec3(entity.xo + (entity.getX() - entity.xo) * partialTick,
                entity.yo + (entity.getY() - entity.yo) * partialTick,
                entity.zo + (entity.getZ() - entity.zo) * partialTick);
        for (int bone = 0; bone < bones.size(); bone++) {
            Vector3f position = positions[bone];
            Quaternionf rotation = rotations[bone];
            BonePose a = frames[tick][bone], b = frames[next][bone];
            position.set(a.position).lerp(b.position, time - tick);
            rotation.set(a.rotation).slerp(b.rotation, time - tick);
            if (stance > 0) {
                if (age < DemiurgeAwakening.DURATION + DemiurgeSlither.TRANSITION_TICKS) {
                    float lowering = age - DemiurgeAwakening.DURATION;
                    int key = (int) lowering;
                    BonePose c = transition[key][bone], d = transition[key + 1][bone];
                    position.set(c.position).lerp(d.position, lowering - key);
                    rotation.set(c.rotation).slerp(d.rotation, lowering - key);
                } else {
                    BonePose c = slither[groundTick][bone], d = slither[groundTick + 1][bone];
                    position.set(c.position).lerp(d.position, clock - groundTick);
                    rotation.set(c.rotation).slerp(d.rotation, clock - groundTick);
                }
            }
        }
        if (stance > 0) bodyChain.follow(positions, rotations, origin, yaw, stance, entity::trailPoint);
        for (int bone = 0; bone < bones.size(); bone++) {
            Vector3f position = positions[bone];
            Quaternionf rotation = rotations[bone];
            stack.pushPose();
            stack.translate(position.x, position.y, position.z);
            stack.mulPose(rotation);
            var pose = stack.last();
            for (Quad quad : bones.get(bone)) {
                for (Vertex vertex : quad.vertices) {
                    buffer.vertex(pose.pose(), vertex.x, vertex.y, vertex.z)
                            .color(255, 255, 255, 255).uv(vertex.u, vertex.v)
                            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                            .normal(pose.normal(), quad.nx, quad.ny, quad.nz).endVertex();
                }
            }
            stack.popPose();
        }
    }
}
