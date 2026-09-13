import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Offline art tool for the First Vicissitude boss.
 *
 * <p>Reads the same authoring data the runtime model uses ({@link VicissitudeRigData}), so the
 * generated base/emissive textures, the Blockbench project and the preview renders can never
 * drift from the in-game model. No Minecraft classes are involved, so it compiles standalone:
 *
 * <pre>
 * javac -d build/rig-tool -sourcepath src/main/java art/first-vicissitude/tools/RigArtGenerator.java
 * java -cp build/rig-tool RigArtGenerator
 * </pre>
 */
public final class RigArtGenerator {
    private static final int TEXTURE_SIZE = VicissitudeRigData.TEXTURE_SIZE;
    private static final Path TEXTURE_DIR = Path.of(
            "src/main/resources/assets/maledict/textures/entity");
    private static final Path ART_DIR = Path.of("art/first-vicissitude");
    private static final Path PREVIEW_DIR = ART_DIR.resolve("preview");

    public static void main(String[] args) throws IOException {
        Files.createDirectories(TEXTURE_DIR);
        Files.createDirectories(PREVIEW_DIR);
        writeTexture(TEXTURE_DIR.resolve("first_vicissitude.png"), false);
        writeTexture(TEXTURE_DIR.resolve("first_vicissitude_emissive.png"), true);
        writeBlockbenchProject(ART_DIR.resolve("first_vicissitude.bbmodel"));
        writePreviews();
        writeTokenIcon();
        System.out.println("cubes=" + VicissitudeRigData.placedCubes().size());
    }

    /** Claim token icon: a broken custody seal in the boss' cold violet palette. */
    private static void writeTokenIcon() throws IOException {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, 0x00000000);
            }
        }
        int bone = 0xFFB8B8C4;
        int cold = 0xFFE6EDF5;
        int violet = 0xFF51436D;
        int dark = 0xFF19151F;
        double center = 7.5D;
        for (int y = 1; y <= 14; y++) {
            for (int x = 1; x <= 14; x++) {
                double dx = x - center;
                double dy = y - center;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double angle = Math.toDegrees(Math.atan2(dy, dx));
                if (distance > 7.0D) {
                    continue;
                }
                int color = dark;
                if (distance > 5.6D) {
                    // Broken outer ring: a permanent gap on the lower right.
                    color = insideGap((float) angle, 45.0F, 70.0F) ? 0x00000000 : violet;
                } else if (distance > 3.4D) {
                    color = insideGap((float) angle, 225.0F, 55.0F) ? 0x00000000 : bone;
                } else if (distance > 1.6D) {
                    color = violet;
                } else {
                    color = cold;
                }
                if (color != 0x00000000) {
                    image.setRGB(x, y, color);
                }
            }
        }
        Path path = Path.of("src/main/resources/assets/maledict/textures/item/curio_return_token.png");
        Files.createDirectories(path.getParent());
        ImageIO.write(image, "PNG", path.toFile());
        System.out.println("wrote " + path);
    }

    // ---------------------------------------------------------------- textures

    private static void writeTexture(Path path, boolean emissive) throws IOException {
        BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        if (emissive) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                for (int x = 0; x < TEXTURE_SIZE; x++) {
                    image.setRGB(x, y, 0x00000000);
                }
            }
        } else {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                for (int x = 0; x < TEXTURE_SIZE; x++) {
                    image.setRGB(x, y, 0xFF000000 | VicissitudeRigData.Material.CLOTH.secondary());
                }
            }
        }

        for (VicissitudeRigData.PlacedCube cube : VicissitudeRigData.placedCubes()) {
            VicissitudeRigData.Material material = cube.material();
            if (emissive && material.emissive() <= 0.0F) {
                continue;
            }
            int u = cube.u();
            int v = cube.v();
            int w = cube.spec().width();
            int h = cube.spec().height();
            int d = cube.spec().depth();
            // Box UV rows: [east][north][west][south] below [top][bottom].
            paintFace(image, u, v + d, d, h, material, FaceShade.EAST, cube, emissive);
            paintFace(image, u + d, v + d, w, h, material, FaceShade.NORTH, cube, emissive);
            paintFace(image, u + d + w, v + d, d, h, material, FaceShade.WEST, cube, emissive);
            paintFace(image, u + d + w + d, v + d, w, h, material, FaceShade.SOUTH, cube, emissive);
            paintFace(image, u + d, v, w, d, material, FaceShade.TOP, cube, emissive);
            paintFace(image, u + d + w, v, w, d, material, FaceShade.BOTTOM, cube, emissive);
        }
        ImageIO.write(image, "PNG", path.toFile());
        System.out.println("wrote " + path);
    }

    private enum FaceShade {
        TOP(1.18F),
        BOTTOM(0.72F),
        EAST(0.94F),
        WEST(0.86F),
        NORTH(1.04F),
        SOUTH(0.80F);

        private final float factor;

        FaceShade(float factor) {
            this.factor = factor;
        }
    }

    private static void paintFace(BufferedImage image, int x0, int y0, int width, int height,
                                  VicissitudeRigData.Material material, FaceShade shade,
                                  VicissitudeRigData.PlacedCube cube, boolean emissive) {
        if (width <= 0 || height <= 0) {
            return;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int px = x0 + x;
                int py = y0 + y;
                if (px < 0 || py < 0 || px >= TEXTURE_SIZE || py >= TEXTURE_SIZE) {
                    continue;
                }
                float noise = hash(cube.u() * 31 + x, cube.v() * 17 + y, cube.joint().ordinal());
                int base = mix(material.primary(), material.secondary(), noise * 0.75F);
                float factor = shade.factor;
                // Edge darkening keeps individual plates readable at a distance.
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    factor *= 0.72F;
                } else if (x == 1 || y == 1 || x == width - 2 || y == height - 2) {
                    factor *= 0.9F;
                }
                if (isCrack(material, cube, x, y)) {
                    factor *= 0.55F;
                }
                if (emissive) {
                    factor = Math.min(1.6F, factor * (0.85F + material.emissive()));
                    int alpha = (int) (255 * Math.min(1.0F, 0.45F + material.emissive()));
                    image.setRGB(px, py, (alpha << 24) | scale(base, factor));
                } else {
                    image.setRGB(px, py, 0xFF000000 | scale(base, factor));
                }
            }
        }
    }

    private static boolean isCrack(VicissitudeRigData.Material material,
                                   VicissitudeRigData.PlacedCube cube, int x, int y) {
        if (material != VicissitudeRigData.Material.BONE
            && material != VicissitudeRigData.Material.BONE_DARK
            && material != VicissitudeRigData.Material.SHELL
            && material != VicissitudeRigData.Material.SHELL_DARK) {
            return false;
        }
        int seed = cube.joint().ordinal() * 7 + cube.u();
        return ((x * 3 + y * 5 + seed) % 23 == 0) || ((x + y * 2 + seed) % 41 == 0);
    }

    // ---------------------------------------------------------------- bbmodel

    private static void writeBlockbenchProject(Path path) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"meta\": {\"format_version\": \"4.5\", \"model_format\": \"modded_entity\", ")
                .append("\"box_uv\": false},\n");
        json.append("  \"name\": \"first_vicissitude\",\n");
        json.append("  \"maledict_rig_space\": \"model units, y up in this project; ")
                .append("java model y = -this y, entity origin at y = 0, 16 units = 1 block\",\n");
        json.append("  \"resolution\": {\"width\": ").append(TEXTURE_SIZE)
                .append(", \"height\": ").append(TEXTURE_SIZE).append("},\n");
        json.append("  \"textures\": [{\"path\": \"first_vicissitude.png\", \"name\": \"first_vicissitude\", ")
                .append("\"id\": \"0\", \"particle\": false}],\n");

        json.append("  \"elements\": [\n");
        List<VicissitudeRigData.PlacedCube> cubes = VicissitudeRigData.placedCubes();
        for (int i = 0; i < cubes.size(); i++) {
            VicissitudeRigData.PlacedCube cube = cubes.get(i);
            json.append(cubeJson(cube, i));
            json.append(i + 1 < cubes.size() ? ",\n" : "\n");
        }
        json.append("  ],\n");

        json.append("  \"outliner\": [\n");
        json.append(outlinerJson(VicissitudeRigData.Joint.ROOT, 4));
        json.append("\n  ]\n");
        json.append("}\n");

        Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
        System.out.println("wrote " + path);
    }

    private static String cubeJson(VicissitudeRigData.PlacedCube cube, int index) {
        int w = cube.spec().width();
        int h = cube.spec().height();
        int d = cube.spec().depth();
        float minX = cube.spec().centerX() - w / 2.0F;
        float maxX = minX + w;
        // Blockbench coordinates are y-up, so the model space y axis is negated.
        float minY = -(cube.spec().centerY() + h / 2.0F);
        float maxY = -(cube.spec().centerY() - h / 2.0F);
        float minZ = cube.spec().centerZ() - d / 2.0F;
        float maxZ = minZ + d;
        int u = cube.u();
        int v = cube.v();
        String material = cube.material().name().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        builder.append("    {\"name\": \"").append(cube.joint().partName()).append('_').append(index)
                .append("\", \"uuid\": \"").append(uuid(cube, index)).append("\", ")
                .append("\"material\": \"").append(material).append("\", ")
                .append("\"from\": [").append(number(minX)).append(", ").append(number(minY)).append(", ")
                .append(number(minZ)).append("], ")
                .append("\"to\": [").append(number(maxX)).append(", ").append(number(maxY)).append(", ")
                .append(number(maxZ)).append("], ")
                .append("\"origin\": [").append(number(cube.joint().pivotX())).append(", ")
                .append(number(-cube.joint().pivotY())).append(", ")
                .append(number(cube.joint().pivotZ())).append("], ")
                .append("\"faces\": {")
                .append(face("north", u + d, v + d, u + d + w, v + d + h))
                .append(", ").append(face("east", u, v + d, u + d, v + d + h))
                .append(", ").append(face("south", u + d + w + d, v + d, u + d + w + d + w, v + d + h))
                .append(", ").append(face("west", u + d + w, v + d, u + d + w + d, v + d + h))
                .append(", ").append(face("up", u + d, v, u + d + w, v + d))
                .append(", ").append(face("down", u + d + w, v, u + d + w + w, v + d))
                .append("}, \"type\": \"cube\"}");
        return builder.toString();
    }

    private static String face(String name, int u1, int v1, int u2, int v2) {
        return "\"" + name + "\": {\"uv\": [" + u1 + ", " + v1 + ", " + u2 + ", " + v2
               + "], \"texture\": 0}";
    }

    private static String outlinerJson(VicissitudeRigData.Joint joint, int indent) {
        String pad = " ".repeat(indent);
        StringBuilder builder = new StringBuilder();
        builder.append(pad).append("{\"name\": \"").append(joint.partName()).append("\", ")
                .append("\"origin\": [").append(number(joint.pivotX())).append(", ")
                .append(number(-joint.pivotY())).append(", ")
                .append(number(joint.pivotZ())).append("], \"uuid\": \"")
                .append(uuidForJoint(joint)).append('"');
        List<String> children = new ArrayList<>();
        for (VicissitudeRigData.PlacedCube cube : VicissitudeRigData.placedCubes(joint)) {
            children.add("\"" + uuid(cube, VicissitudeRigData.placedCubes().indexOf(cube)) + "\"");
        }
        for (VicissitudeRigData.Joint candidate : VicissitudeRigData.Joint.values()) {
            if (candidate.parent() == joint) {
                children.add(outlinerJson(candidate, indent + 2));
            }
        }
        if (children.isEmpty()) {
            builder.append('}');
            return builder.toString();
        }
        builder.append(", \"children\": [\n");
        for (int i = 0; i < children.size(); i++) {
            String child = children.get(i);
            if (child.startsWith("{")) {
                builder.append(pad).append("  ").append(child);
            } else {
                builder.append(pad).append("  ").append(child);
            }
            builder.append(i + 1 < children.size() ? ",\n" : "\n");
        }
        builder.append(pad).append("]}");
        return builder.toString();
    }

    private static String uuid(VicissitudeRigData.PlacedCube cube, int index) {
        return String.format("00000000-0000-4000-8000-%012d", index + 1);
    }

    private static String uuidForJoint(VicissitudeRigData.Joint joint) {
        return String.format("10000000-0000-4000-8000-%012d", joint.ordinal() + 1);
    }

    private static String number(float value) {
        if (value == Math.rint(value)) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // ---------------------------------------------------------------- previews

    /** Orthographic software render of the posed skeleton; stands in for in-game screenshots. */
    private static void writePreviews() throws IOException {
        for (boolean phaseTwo : new boolean[]{false, true}) {
            VicitudeView view = phaseTwo ? VicitudeView.PHASE_TWO : VicitudeView.PHASE_ONE;
            RenderPose pose = RenderPose.build(view, phaseTwo);
            writePreview(PREVIEW_DIR.resolve((phaseTwo ? "phase_two" : "phase_one") + "_front.png"),
                    pose, Camera.FRONT, view);
            writePreview(PREVIEW_DIR.resolve((phaseTwo ? "phase_two" : "phase_one") + "_side.png"),
                    pose, Camera.SIDE, view);
            writePreview(PREVIEW_DIR.resolve((phaseTwo ? "phase_two" : "phase_one") + "_back.png"),
                    pose, Camera.BACK, view);
        }
    }

    private enum VicitudeView {
        PHASE_ONE("phase one (high idol)"),
        PHASE_TWO("phase two (forward executor)");

        private final String label;

        VicitudeView(String label) {
            this.label = label;
        }
    }

    private enum Camera {
        FRONT(1.0F, 0.0F, 0.0F, 1.0F),
        SIDE(0.0F, -1.0F, 1.0F, 0.0F),
        BACK(-1.0F, 0.0F, 0.0F, -1.0F);

        /** Screen right axis in entity-local space. */
        private final float rightX;
        private final float rightZ;
        /** View depth axis; larger means closer to the camera. */
        private final float depthX;
        private final float depthZ;

        Camera(float rightX, float rightZ, float depthX, float depthZ) {
            this.rightX = rightX;
            this.rightZ = rightZ;
            this.depthX = depthX;
            this.depthZ = depthZ;
        }
    }

    /** Reuses the runtime rig so previews show exactly what the game model will pose. */
    private record RenderPose(VicissitudeRig.Pose pose) {
        static RenderPose build(VicitudeView view, boolean phaseTwo) {
            VicissitudeRig.Pose pose = VicissitudeRig.newPose();
            VicissitudeRig.compute(pose, phaseTwo, phaseTwo ? 1.0F : 0.0F, VicissitudeRig.Action.NONE,
                    0.0F, true, 0.0F, 0.0F, 0.0F, -1.0F);
            return new RenderPose(pose);
        }
    }

    private static void writePreview(Path path, RenderPose renderPose, Camera camera,
                                     VicitudeView view) throws IOException {
        int size = 640;
        // The rig spans roughly model y -46 (halo) .. +24 (entity origin).
        float scale = 6.4F;
        float centerY = -11.0F;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(0x101018));
        graphics.fillRect(0, 0, size, size);
        graphics.setStroke(new BasicStroke(1.0F));

        List<Face> faces = new ArrayList<>();
        for (VicissitudeRigData.PlacedCube cube : VicissitudeRigData.placedCubes()) {
            float minX = cube.spec().centerX() - cube.spec().width() / 2.0F;
            float minY = cube.spec().centerY() - cube.spec().height() / 2.0F;
            float minZ = cube.spec().centerZ() - cube.spec().depth() / 2.0F;
            float maxX = minX + cube.spec().width();
            float maxY = minY + cube.spec().height();
            float maxZ = minZ + cube.spec().depth();
            float[][] points = {
                    {minX, minY, minZ}, {maxX, minY, minZ}, {maxX, maxY, minZ}, {minX, maxY, minZ},
                    {minX, minY, maxZ}, {maxX, minY, maxZ}, {maxX, maxY, maxZ}, {minX, maxY, maxZ}};
            float[][] transformed = new float[8][3];
            for (int i = 0; i < 8; i++) {
                VicissitudeRig.V3 point = VicissitudeRig.transform(renderPose.pose(), cube.joint(),
                        points[i][0], points[i][1], points[i][2]);
                transformed[i] = new float[]{point.x(), point.y(), point.z()};
            }
            int[][] quads = {
                    {0, 3, 2, 1}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 7, 6, 2}, {1, 2, 6, 5}, {0, 4, 7, 3}};
            for (int[] quad : quads) {
                faces.add(new Face(transformed, quad, cube.material()));
            }
        }

        faces.sort(Comparator.comparingDouble(face -> -face.depth(camera)));
        for (Face face : faces) {
            int[] xs = new int[4];
            int[] ys = new int[4];
            for (int i = 0; i < 4; i++) {
                float[] point = face.point(i);
                xs[i] = Math.round(size / 2.0F + point[0] * camera.rightX * scale
                                   + point[2] * camera.rightZ * scale);
                ys[i] = Math.round(size / 2.0F + (point[1] - centerY) * scale);
            }
            graphics.setColor(face.color(camera));
            graphics.fillPolygon(xs, ys, 4);
            graphics.setColor(face.edgeColor(camera));
            graphics.drawPolygon(xs, ys, 4);
        }

        graphics.setColor(new Color(0xE6EDF5));
        graphics.drawString("first_vicissitude - " + view.label, 16, 24);
        graphics.dispose();
        ImageIO.write(image, "PNG", path.toFile());
        System.out.println("wrote " + path);
    }

    private record Face(float[][] points, int[] quad, VicissitudeRigData.Material material) {
        float[] point(int index) {
            return points[quad[index]];
        }

        float depth(Camera camera) {
            float sum = 0.0F;
            for (int index : quad) {
                sum += points[index][0] * camera.depthX + points[index][2] * camera.depthZ;
            }
            return sum / 4.0F;
        }

        Color color(Camera camera) {
            float shade = 0.55F + 0.45F * Math.abs(normalY());
            int base = material.primary();
            int secondary = material.secondary();
            int mixed = mix(base, secondary, 0.35F);
            float glow = 1.0F + material.emissive() * 0.6F;
            return new Color(clampChannel((int) (channel(mixed, 16) * shade * glow)),
                    clampChannel((int) (channel(mixed, 8) * shade * glow)),
                    clampChannel((int) (channel(mixed, 0) * shade * glow)));
        }

        Color edgeColor(Camera camera) {
            Color base = color(camera);
            return new Color(clampChannel((int) (base.getRed() * 0.6F)),
                    clampChannel((int) (base.getGreen() * 0.6F)),
                    clampChannel((int) (base.getBlue() * 0.6F)));
        }

        private float normalY() {
            float[] a = points[quad[0]];
            float[] b = points[quad[1]];
            float[] c = points[quad[2]];
            float ux = b[0] - a[0];
            float uy = b[1] - a[1];
            float uz = b[2] - a[2];
            float vx = c[0] - b[0];
            float vy = c[1] - b[1];
            float vz = c[2] - b[2];
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            return length == 0.0F ? 0.0F : ny / length;
        }
    }

    // ---------------------------------------------------------------- helpers

    private static boolean insideGap(float angle, float gapCenter, float gapWidth) {
        float delta = angle - gapCenter;
        while (delta <= -180.0F) {
            delta += 360.0F;
        }
        while (delta > 180.0F) {
            delta -= 360.0F;
        }
        return Math.abs(delta) < gapWidth * 0.5F;
    }

    private static int channel(int rgb, int shift) {
        return (rgb >> shift) & 0xFF;
    }

    private static int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int mix(int first, int second, float delta) {
        float amount = Math.max(0.0F, Math.min(1.0F, delta));
        int red = (int) (channel(first, 16) * (1 - amount) + channel(second, 16) * amount);
        int green = (int) (channel(first, 8) * (1 - amount) + channel(second, 8) * amount);
        int blue = (int) (channel(first, 0) * (1 - amount) + channel(second, 0) * amount);
        return (clampChannel(red) << 16) | (clampChannel(green) << 8) | clampChannel(blue);
    }

    private static int scale(int rgb, float factor) {
        return (clampChannel((int) (channel(rgb, 16) * factor)) << 16)
               | (clampChannel((int) (channel(rgb, 8) * factor)) << 8)
               | clampChannel((int) (channel(rgb, 0) * factor));
    }

    private static float hash(int x, int y, int seed) {
        int value = (int) (x * 374761393L + y * 668265263L + (long) seed * 2246822519L);
        value = (value ^ (value >>> 13)) * 1274126177;
        return ((value ^ (value >>> 16)) & 0xFFFF) / 65535.0F;
    }

    private RigArtGenerator() {
    }
}
