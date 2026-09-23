package org.brahypno.maledict.rig;

import java.util.List;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;

/** Blender-exported wing bounds; regenerate with art/first-vicissitude/tools/build_blender.py. */
public final class VicissitudeMeshGeometry {
    public record Bounds(Joint joint, float minX, float minY, float minZ,
                         float maxX, float maxY, float maxZ) {}
    public static final List<Bounds> WINGS = List.of(
            new Bounds(Joint.WING_LEFT_FEATHER_1, -5.37416F, -1.75395F, -2.00000F, 11.41091F, 31.10183F, 4.65000F),
            new Bounds(Joint.WING_LEFT_FEATHER_2, -5.93865F, -2.83826F, -2.00000F, 14.39080F, 33.12624F, 5.30000F),
            new Bounds(Joint.WING_LEFT_FEATHER_3, -6.90341F, -3.96633F, -2.00000F, 15.59846F, 27.19744F, 5.30000F),
            new Bounds(Joint.WING_LEFT_FEATHER_4, -4.86451F, -5.05004F, -2.00000F, 12.06657F, 14.28055F, 4.65000F),
            new Bounds(Joint.WING_RIGHT_FEATHER_1, -11.41091F, -1.75395F, -2.00000F, 5.37416F, 31.10183F, 4.65000F),
            new Bounds(Joint.WING_RIGHT_FEATHER_2, -14.39080F, -2.83826F, -2.00000F, 6.47401F, 31.16188F, 4.33344F),
            new Bounds(Joint.WING_RIGHT_FEATHER_3, -11.97753F, -3.94630F, -2.00000F, 6.90341F, 27.19744F, 4.65000F),
            new Bounds(Joint.WING_RIGHT_FEATHER_4, -12.06657F, -5.05004F, -2.00000F, 4.86451F, 14.28055F, 4.65000F),
            new Bounds(Joint.WING_LEFT_UPPER, -1.14366F, -33.00303F, -1.98235F, 75.00356F, 45.00636F, 7.68133F),
            new Bounds(Joint.WING_RIGHT_UPPER, -75.00356F, -33.00303F, -1.98235F, 1.14366F, 45.00636F, 7.68133F),
            new Bounds(Joint.WING_LEFT_OUTER, -7.05332F, 1.81233F, 3.00000F, 4.81119F, 27.04182F, 5.00000F),
            new Bounds(Joint.WING_RIGHT_OUTER, -4.81119F, 1.81233F, 3.00000F, 7.05332F, 27.04182F, 5.00000F));
    public static final List<Bounds> BONE_WINGS = List.of(
            new Bounds(Joint.WING_LEFT_UPPER, -1.14366F, -33.00303F, -1.98235F, 75.00356F, 45.00636F, 7.68133F),
            new Bounds(Joint.WING_RIGHT_UPPER, -75.00356F, -33.00303F, -1.98235F, 1.14366F, 45.00636F, 7.68133F));
    private VicissitudeMeshGeometry() {}
}
