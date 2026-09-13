package org.brahypno.maledict.rig;

import java.util.List;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;

/** Blender-exported wing bounds; regenerate with art/first-vicissitude/tools/build_blender.py. */
public final class VicissitudeMeshGeometry {
    public record Bounds(Joint joint, float minX, float minY, float minZ,
                         float maxX, float maxY, float maxZ) {}
    public static final List<Bounds> WINGS = List.of(
            new Bounds(Joint.WING_LEFT_UPPER, -1.64639F, -11.00751F, -3.78163F, 29.61127F, 24.41298F, 10.60000F),
            new Bounds(Joint.WING_LEFT_OUTER, -2.99724F, -11.00751F, -4.86940F, 23.61642F, 29.21170F, 8.60000F),
            new Bounds(Joint.WING_RIGHT_UPPER, -29.61127F, -11.00751F, -3.78163F, 1.64639F, 24.41074F, 10.60000F),
            new Bounds(Joint.WING_RIGHT_OUTER, -23.61642F, -11.00751F, -4.86940F, 2.37061F, 29.20969F, 8.60000F),
            new Bounds(Joint.WING_LEFT_BROKEN_1, -10.93979F, 0.65826F, 0.00363F, -2.91425F, 17.05145F, 2.69886F),
            new Bounds(Joint.WING_LEFT_BROKEN_2, -4.93979F, 0.65826F, 0.00363F, 3.09192F, 19.03939F, 2.69906F),
            new Bounds(Joint.WING_RIGHT_BROKEN_1, 2.91425F, 0.65826F, 0.00363F, 10.93979F, 17.05145F, 2.69886F),
            new Bounds(Joint.WING_RIGHT_BROKEN_2, -3.09192F, 0.65826F, 0.00363F, 4.93979F, 19.03939F, 2.69906F),
            new Bounds(Joint.WING_LEFT_FEATHER_1, -5.37299F, -3.24960F, -4.71349F, 15.53397F, 30.01352F, 11.46600F),
            new Bounds(Joint.WING_LEFT_FEATHER_2, -5.68582F, -3.20462F, -4.70461F, 15.28409F, 33.01230F, 11.60900F),
            new Bounds(Joint.WING_LEFT_FEATHER_3, -5.67164F, -3.24960F, -4.71349F, 15.28354F, 30.01337F, 11.46600F),
            new Bounds(Joint.WING_LEFT_FEATHER_4, -2.67037F, -3.24455F, -3.66354F, 10.00898F, 21.01516F, 8.99900F),
            new Bounds(Joint.WING_RIGHT_FEATHER_1, -15.53397F, -3.28154F, -4.71349F, 5.31739F, 30.01352F, 11.46600F),
            new Bounds(Joint.WING_RIGHT_FEATHER_2, -15.28409F, -3.23368F, -4.70461F, 5.64717F, 33.01130F, 11.60900F),
            new Bounds(Joint.WING_RIGHT_FEATHER_3, -15.28354F, -3.24960F, -4.71349F, 5.62894F, 28.01253F, 11.46600F),
            new Bounds(Joint.WING_RIGHT_FEATHER_4, -10.00813F, -3.27592F, -3.66354F, 2.59713F, 17.86524F, 8.99900F),
            new Bounds(Joint.WING_LEFT_LOWER, -2.99724F, -9.00751F, -5.86940F, 18.81127F, 7.17947F, 6.12044F),
            new Bounds(Joint.WING_RIGHT_LOWER, -18.81127F, -9.00751F, -5.86940F, 2.37061F, 7.17947F, 6.12044F));
    private VicissitudeMeshGeometry() {}
}
