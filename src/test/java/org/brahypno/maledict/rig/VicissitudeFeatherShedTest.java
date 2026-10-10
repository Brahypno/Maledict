package org.brahypno.maledict.rig;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;
import static org.junit.jupiter.api.Assertions.*;

class VicissitudeFeatherShedTest {
    @Test void boneBladesStayHiddenUntilTheTransitionAndThenFullyDeploy() {
        assertEquals(0, VicissitudeFeatherShed.deployment(0));
        assertEquals(0, VicissitudeFeatherShed.deployment(8));
        float previous = 0;
        for (float tick = 8; tick < 44; tick += .25F) {
            float scale = VicissitudeFeatherShed.deployment(tick);
            assertTrue(scale >= previous && scale <= 1);
            previous = scale;
        }
        assertEquals(1, VicissitudeFeatherShed.deployment(44));
        assertEquals(1, VicissitudeFeatherShed.deployment(60));
    }

    /** Collision bounds must describe the visible feathered mesh, excluding concealed bone blades. */
    @Test void phaseOneBoundsMatchOnlyItsVisibleMesh() throws Exception {
        var meshPath = Path.of("../../src/main/resources/assets/maledict/models/entity/first_vicissitude.mesh.json");
        var expected = new EnumMap<Joint, double[]>(Joint.class);
        try (var reader = Files.newBufferedReader(meshPath)) {
            for (var element : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("parts")) {
                var part = element.getAsJsonObject();
                String name = part.get("joint").getAsString();
                if (!name.startsWith("wing_") || part.has("deploy_scale")) continue;
                Joint joint = Joint.valueOf(name.toUpperCase(java.util.Locale.ROOT));
                double[] bounds = expected.computeIfAbsent(joint, ignored -> new double[]{
                        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                        Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY});
                for (var triangle : part.getAsJsonArray("triangles")) {
                    for (var corner : triangle.getAsJsonObject().getAsJsonArray("v")) {
                        for (int axis = 0; axis < 3; axis++) {
                            double value = corner.getAsJsonArray().get(axis).getAsDouble();
                            bounds[axis] = Math.min(bounds[axis], value);
                            bounds[axis + 3] = Math.max(bounds[axis + 3], value);
                        }
                    }
                }
            }
        }
        assertEquals(expected.size(), VicissitudeMeshGeometry.WINGS.size());
        for (var actual : VicissitudeMeshGeometry.WINGS) {
            assertArrayEquals(expected.get(actual.joint()), new double[]{actual.minX(), actual.minY(), actual.minZ(),
                    actual.maxX(), actual.maxY(), actual.maxZ()}, .0001, actual.joint().name());
        }
    }
    @Test void releaseIsContinuousAndStaggered() {
        assertEquals(0, VicissitudeFeatherShed.elapsed(12, 15));
        assertEquals(0, VicissitudeFeatherShed.drop(0));
        assertEquals(1, VicissitudeFeatherShed.scale(0));
        assertTrue(VicissitudeFeatherShed.elapsed(20, 12) > VicissitudeFeatherShed.elapsed(20, 19));
    }
    /** 落羽在 24 tick 内落完：最后一 tick 还有余量，到点归零，不会拖进二阶段。 */
    @Test void theFallFinishesWithinTwentyFourTicks() {
        assertTrue(VicissitudeFeatherShed.scale(23.0F) > 0.0F);
        assertEquals(0.0F, VicissitudeFeatherShed.scale(24.0F));
        assertEquals(0.0F, VicissitudeFeatherShed.scale(VicissitudeFeatherShed.elapsed(60, 28)));
    }
    @Test void phaseTwoDropsFeatherOnlyCollisionGroups() {
        assertEquals(8, VicissitudeMeshGeometry.WINGS.stream()
                .filter(b -> b.joint().name().contains("FEATHER_")).count());
        assertTrue(VicissitudeMeshGeometry.BONE_WINGS.stream().noneMatch(b -> b.joint().name().contains("FEATHER_")));
        var pose=VicissitudeRig.newPose();
        VicissitudeRig.compute(pose,true,1,VicissitudeRig.Action.NONE,0,false,0,0,0,-1);
        assertEquals(2+VicissitudeMeshGeometry.BONE_WINGS.size(),VicissitudeRig.segmentVolumes(pose,0,0,0,0).size());
    }
}
