import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Supplies Blender with the actual runtime hierarchy and evaluated animation channels. */
public final class RigPoseExporter {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        StringBuilder out = new StringBuilder("{\"joints\":[");
        for (Joint j : Joint.values()) {
            if (j.ordinal() > 0) out.append(',');
            out.append(String.format("{\"name\":\"%s\",\"parent\":\"%s\",\"pivot\":[%f,%f,%f]}",
                    j.partName(), j.parent() == null ? "" : j.parent().partName(),
                    j.pivotX(), j.pivotY(), j.pivotZ()));
        }
        out.append(String.format("],\"chest_hub\":[%f,%f],\"poses\":{",
                Joint.TORSO.pivotX() + VicissitudeRig.CHEST_RING_HUB_X,
                Joint.TORSO.pivotY() + VicissitudeRig.CHEST_RING_HUB_Y));
        String[] names = {"phase_one", "phase_two", "death_reveal"};
        for (int i = 0; i < names.length; i++) {
            if (i > 0) out.append(',');
            out.append('"').append(names[i]).append("\":{");
            var pose = VicissitudeRig.newPose();
            VicissitudeRig.compute(pose, i > 0, i > 0 ? 1 : 0, VicissitudeRig.Action.NONE,
                    0, false, 0, 0, 0, i == 2 ? 35 : -1);
            for (Joint j : Joint.values()) {
                if (j.ordinal() > 0) out.append(',');
                out.append(String.format("\"%s\":{\"rotation\":[%f,%f,%f],\"offset\":[%f,%f,%f]}",
                        j.partName(), pose.rotX(j), pose.rotY(j), pose.rotZ(j),
                        pose.offX(j), pose.offY(j), pose.offZ(j)));
            }
            out.append('}');
        }
        out.append("}}");
        Files.createDirectories(Path.of("build/rig-tool"));
        Files.writeString(Path.of("build/rig-tool/blender-rig.json"), out);
    }
}
