package org.brahypno.maledict.common.vicissitude.rig;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;
import org.brahypno.maledict.common.vicissitude.rig.VicissitudeRigData.Joint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;

class VicissitudeFeatherShedTest {

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

}
