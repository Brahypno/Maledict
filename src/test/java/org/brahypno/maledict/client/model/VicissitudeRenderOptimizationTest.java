package org.brahypno.maledict.client.model;

import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import static org.junit.jupiter.api.Assertions.*;

class VicissitudeRenderOptimizationTest {
    @Test void darkFacesAreSkippedButFilteringAndWrappedEdgesAreKept() {
        int[] pixels = new int[20 * 20];
        java.util.Arrays.fill(pixels, 0xff000000);
        var dark = new VicissitudeEmissiveMask(20, 20, pixels);
        assertFalse(dark.touches(.3F, .3F, .6F, .6F));
        pixels[10 * 20 + 10] = 1;
        var glow = new VicissitudeEmissiveMask(20, 20, pixels);
        assertTrue(glow.touches(.5F, .5F, .55F, .55F));
        assertTrue(glow.touches(.4F, .4F, .45F, .45F));
        assertFalse(glow.touches(.15F, .15F, .25F, .25F));
        assertTrue(glow.touches(-.1F, .4F, .1F, .5F));
        assertTrue(glow.touches(.9F, .9F, 1, 1));
        assertFalse(new VicissitudeEmissiveMask(40, 40, new int[1600]).touches(.3F, .3F, .6F, .6F));
    }

    @Test void cachedTransformsMatchOriginalAncestorWalkAcrossActionsAndBothLayers() {
        var cache = new VicissitudePoseMatrices();
        var pose = VicissitudeRig.newPose();
        var incoming = new Matrix4f().translation(3, -2, 7).rotateY(.71F).scale(-1, -1, 1);
        for (var action : VicissitudeRig.Action.values()) {
            for (float phase : new float[]{0, .5F, 1}) {
                for (int tick : new int[]{0, 13, 37}) {
                    VicissitudeRig.compute(pose, phase == 1, phase, action, tick, true, 0, 127.5F, .4F, -1);
                    cache.update(pose);
                    for (Joint joint : Joint.values()) {
                        var chain = new ArrayDeque<Joint>();
                        for (Joint j = joint; j != null; j = j.parent()) chain.push(j);
                        var expected = new Matrix4f(incoming);
                        var expectedNormal = new Matrix3f(incoming);
                        for (Joint j : chain) {
                            var rotation = new Quaternionf().rotationZYX((float) Math.toRadians(pose.rotZ(j)),
                                    (float) Math.toRadians(pose.rotY(j)), (float) Math.toRadians(pose.rotX(j)));
                            expected.translate((j.localX() + pose.offX(j)) / 16F,
                                    (j.localY() + pose.offY(j)) / 16F, (j.localZ() + pose.offZ(j)) / 16F).rotate(rotation);
                            expectedNormal.rotate(rotation);
                        }
                        for (int layer = 0; layer < 2; layer++) {
                            var actual = new Matrix4f(incoming).mul(cache.position(joint));
                            var actualNormal = new Matrix3f(incoming).mul(cache.normal(joint));
                            assertTrue(expected.equals(actual, .00005F), action + "/" + joint);
                            assertTrue(expectedNormal.equals(actualNormal, .00005F), action + "/normal/" + joint);
                        }
                    }
                }
            }
        }
    }
}
