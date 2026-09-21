package org.brahypno.maledict.rig;

import java.util.List;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;

/** Blender-exported wing bounds; regenerate with art/first-vicissitude/tools/build_blender.py. */
public final class VicissitudeMeshGeometry {
    public record Bounds(Joint joint, float minX, float minY, float minZ,
                         float maxX, float maxY, float maxZ) {}
    public static final List<Bounds> WINGS = List.of(
            new Bounds(Joint.WING_LEFT_LOWER, -1.50000F, -5.24847F, -2.50000F, 20.02761F, 20.07285F, 4.15775F),
            new Bounds(Joint.WING_LEFT_OUTER, -7.05332F, -7.53442F, -1.88892F, 14.64498F, 27.04182F, 5.15809F),
            new Bounds(Joint.WING_LEFT_UPPER, -1.26267F, -7.08716F, -2.58782F, 18.98776F, 22.04742F, 7.00000F),
            new Bounds(Joint.WING_RIGHT_LOWER, -20.02761F, -5.24847F, -2.50000F, 1.50000F, 20.07285F, 4.15775F),
            new Bounds(Joint.WING_RIGHT_OUTER, -14.64498F, -7.53442F, -1.88892F, 7.05332F, 27.04182F, 5.15809F),
            new Bounds(Joint.WING_RIGHT_UPPER, -18.98776F, -7.08716F, -2.58782F, 1.26267F, 22.04742F, 7.00000F),
            new Bounds(Joint.WING_LEFT_FEATHER_1, -5.37416F, -1.75395F, -2.00000F, 11.41091F, 31.10183F, 4.65000F),
            new Bounds(Joint.WING_LEFT_FEATHER_2, -5.93865F, -2.83826F, -2.00000F, 14.39080F, 33.12624F, 5.30000F),
            new Bounds(Joint.WING_LEFT_FEATHER_3, -6.90341F, -3.96633F, -2.00000F, 15.59846F, 27.19744F, 5.30000F),
            new Bounds(Joint.WING_LEFT_FEATHER_4, -4.86451F, -5.05004F, -2.00000F, 12.06657F, 14.28055F, 4.65000F),
            new Bounds(Joint.WING_RIGHT_FEATHER_1, -11.41091F, -1.75395F, -2.00000F, 5.37416F, 31.10183F, 4.65000F),
            new Bounds(Joint.WING_RIGHT_FEATHER_2, -14.39080F, -2.83826F, -2.00000F, 6.47401F, 31.16188F, 4.33344F),
            new Bounds(Joint.WING_RIGHT_FEATHER_3, -11.97753F, -3.94630F, -2.00000F, 6.90341F, 27.19744F, 4.65000F),
            new Bounds(Joint.WING_RIGHT_FEATHER_4, -12.06657F, -5.05004F, -2.00000F, 4.86451F, 14.28055F, 4.65000F),
            new Bounds(Joint.WING_LEFT_BROKEN_1, -9.66814F, -1.20879F, 0.30124F, -3.88546F, 15.03579F, 2.11979F),
            new Bounds(Joint.WING_LEFT_BROKEN_2, -3.64615F, -1.26923F, 0.30206F, 2.11077F, 11.04615F, 2.11965F),
            new Bounds(Joint.WING_RIGHT_BROKEN_1, 1.88565F, -1.21224F, 0.30066F, 9.66705F, 21.03638F, 2.11989F),
            new Bounds(Joint.WING_RIGHT_BROKEN_2, -4.11184F, -1.25371F, 0.30094F, 3.65240F, 17.04349F, 2.11984F));
    private VicissitudeMeshGeometry() {}
}
