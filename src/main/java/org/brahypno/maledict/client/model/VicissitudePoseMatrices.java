package org.brahypno.maledict.client.model;

import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Rigid model-space transforms, rebuilt for each setupAnim, shared by all render layers. */
final class VicissitudePoseMatrices {
    private static final Joint[] JOINTS = Joint.values();
    private final Matrix4f[] positions = new Matrix4f[JOINTS.length];
    private final Matrix3f[] normals = new Matrix3f[JOINTS.length];

    VicissitudePoseMatrices() {
        for (Joint joint : JOINTS) {
            positions[joint.index()] = new Matrix4f();
            normals[joint.index()] = new Matrix3f();
        }
    }

    void update(VicissitudeRig.Pose pose) {
        for (Joint joint : JOINTS) {
            Matrix4f local = positions[joint.index()];
            local.translation((joint.localX() + pose.offX(joint)) / 16F,
                    (joint.localY() + pose.offY(joint)) / 16F,
                    (joint.localZ() + pose.offZ(joint)) / 16F);
            local.rotateZYX((float) Math.toRadians(pose.rotZ(joint)),
                    (float) Math.toRadians(pose.rotY(joint)), (float) Math.toRadians(pose.rotX(joint)));
            if (joint.parent() != null) positions[joint.parent().index()].mul(local, local);
            normals[joint.index()].set(local);
        }
    }

    Matrix4f position(Joint joint) { return positions[joint.index()]; }
    Matrix3f normal(Joint joint) { return normals[joint.index()]; }
}
