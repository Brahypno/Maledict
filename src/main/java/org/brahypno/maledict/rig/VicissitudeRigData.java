package org.brahypno.maledict.rig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Authoring data for the First Vicissitude skeleton: joints, cubes, the deterministic UV
 * packing and the material palette. This class intentionally has no Minecraft imports so the
 * offline art tooling in {@code art/first-vicissitude} can compile it standalone and produce a
 * texture that matches the runtime model exactly.
 *
 * <p>Model space conventions (identical to vanilla entity models):
 * <ul>
 *   <li>+x points to the entity's left, +y points down, +z points to the entity's back.</li>
 *   <li>Model y = {@link #MODEL_ORIGIN_Y} is the entity origin (feet), so a point at height
 *       {@code h} blocks has {@code y = MODEL_ORIGIN_Y - 16h}.</li>
 * </ul>
 */
public final class VicissitudeRigData {
    public static final float UNITS_PER_BLOCK = 16.0F;
    /** Model-space y that matches the entity origin; matches the vanilla model offset convention. */
    public static final float MODEL_ORIGIN_Y = 24.0F;
    public static final int TEXTURE_SIZE = 256;
    public static final int UV_MARGIN = 1;

    /**
     * Animated joints. Constructor arguments are offsets relative to the parent pivot; the
     * enum resolves them into absolute model-space pivots so both the blockbench project and
     * the runtime forward kinematics can use one value. Order is parent-before-child.
     */
    public enum Joint {
        ROOT(null, 0.0F, 0.0F, 0.0F),
        BODY(ROOT, 0.0F, 0.0F, 0.0F),
        TORSO(BODY, 0.0F, -10.0F, 0.0F),
        CHEST_SHELL_LEFT(TORSO, 4.0F, 0.0F, -1.0F),
        CHEST_SHELL_RIGHT(TORSO, -4.0F, 0.0F, -1.0F),
        CHEST_SHELL_BACK(TORSO, 0.0F, 0.0F, 5.0F),
        CHEST_RING_LEFT(TORSO, 10.0F, 5.0F, 0.0F),
        CHEST_RING_RIGHT(TORSO, -10.0F, 5.0F, 0.0F),
        CHEST_RING_BOTTOM(TORSO, 0.0F, 9.0F, 0.0F),
        HEAD_ROOT(BODY, 0.0F, -24.0F, 0.0F),
        HEAD_CORE(HEAD_ROOT, 0.0F, -6.0F, 0.0F),
        HEAD_SHELL_LEFT(HEAD_ROOT, 4.0F, -8.0F, 0.0F),
        HEAD_SHELL_RIGHT(HEAD_ROOT, -4.0F, -8.0F, 0.0F),
        HEAD_SHELL_TOP(HEAD_ROOT, 0.0F, -13.0F, 0.0F),
        HALO_ROOT(HEAD_ROOT, 0.0F, -4.0F, 9.0F),
        HALO_FRAGMENT_1(HALO_ROOT, -7.0F, -12.0F, 0.0F),
        HALO_FRAGMENT_2(HALO_ROOT, 12.0F, 2.0F, 0.0F),
        HALO_FRAGMENT_3(HALO_ROOT, 4.0F, 13.0F, 0.0F),
        HALO_FRAGMENT_4(HALO_ROOT, -12.0F, 4.0F, 0.0F),
        ARM_LEFT(BODY, 10.0F, -15.0F, 0.0F),
        UPPER_ARM_LEFT(ARM_LEFT, 0.0F, 0.0F, 0.0F),
        FOREARM_LEFT(UPPER_ARM_LEFT, 0.0F, 12.0F, 0.0F),
        HAND_LEFT(FOREARM_LEFT, 0.0F, 10.0F, 0.0F),
        LEFT_HAND_EFFECT_ANCHOR(HAND_LEFT, 0.0F, 3.0F, 0.0F),
        ARM_RIGHT(BODY, -10.0F, -15.0F, 0.0F),
        UPPER_ARM_RIGHT(ARM_RIGHT, 0.0F, 0.0F, 0.0F),
        FOREARM_RIGHT(UPPER_ARM_RIGHT, 0.0F, 12.0F, 0.0F),
        HAND_RIGHT(FOREARM_RIGHT, 0.0F, 10.0F, 0.0F),
        SCYTHE_HAND_ANCHOR(HAND_RIGHT, 0.0F, 2.0F, 0.0F),
        WING_LEFT_ROOT(BODY, 9.0F, -18.0F, 9.0F),
        WING_LEFT_UPPER(WING_LEFT_ROOT, 0.0F, 0.0F, 0.0F),
        WING_LEFT_OUTER(WING_LEFT_UPPER, 18.0F, -2.0F, 2.0F),
        WING_LEFT_LOWER(WING_LEFT_OUTER, 14.0F, -2.0F, 1.0F),
        WING_LEFT_FEATHERS(WING_LEFT_LOWER, 4.0F, 0.0F, 0.0F),
        WING_LEFT_FEATHER_1(WING_LEFT_FEATHERS, 0.0F, 0.0F, 0.0F),
        WING_LEFT_FEATHER_2(WING_LEFT_FEATHERS, 4.0F, 0.0F, 0.0F),
        WING_LEFT_FEATHER_3(WING_LEFT_FEATHERS, 7.0F, 0.0F, 0.0F),
        WING_LEFT_FEATHER_4(WING_LEFT_FEATHERS, 10.0F, 0.0F, 0.0F),
        WING_LEFT_BROKEN_1(WING_LEFT_FEATHERS, 5.0F, 0.0F, 0.0F),
        WING_LEFT_BROKEN_2(WING_LEFT_FEATHERS, 9.0F, 0.0F, 0.0F),
        WING_LEFT_ATTACK_ANCHOR(WING_LEFT_LOWER, 10.0F, 0.0F, 0.0F),
        WING_LEFT_TIP(WING_LEFT_LOWER, 9.0F, 0.0F, 0.0F),
        WING_RIGHT_ROOT(BODY, -9.0F, -18.0F, 9.0F),
        WING_RIGHT_UPPER(WING_RIGHT_ROOT, 0.0F, 0.0F, 0.0F),
        WING_RIGHT_OUTER(WING_RIGHT_UPPER, -18.0F, -2.0F, 2.0F),
        WING_RIGHT_LOWER(WING_RIGHT_OUTER, -14.0F, -2.0F, 1.0F),
        WING_RIGHT_FEATHERS(WING_RIGHT_LOWER, -4.0F, 0.0F, 0.0F),
        WING_RIGHT_FEATHER_1(WING_RIGHT_FEATHERS, 0.0F, 0.0F, 0.0F),
        WING_RIGHT_FEATHER_2(WING_RIGHT_FEATHERS, -4.0F, 0.0F, 0.0F),
        WING_RIGHT_FEATHER_3(WING_RIGHT_FEATHERS, -7.0F, 0.0F, 0.0F),
        WING_RIGHT_FEATHER_4(WING_RIGHT_FEATHERS, -10.0F, 0.0F, 0.0F),
        WING_RIGHT_BROKEN_1(WING_RIGHT_FEATHERS, -5.0F, 0.0F, 0.0F),
        WING_RIGHT_BROKEN_2(WING_RIGHT_FEATHERS, -9.0F, 0.0F, 0.0F),
        WING_RIGHT_ATTACK_ANCHOR(WING_RIGHT_LOWER, -10.0F, 0.0F, 0.0F),
        WING_RIGHT_TIP(WING_RIGHT_LOWER, -9.0F, 0.0F, 0.0F),
        LOWER_ROOT(BODY, 0.0F, 0.0F, 0.0F),
        SPINE_TAIL_1(LOWER_ROOT, 0.0F, 2.0F, 0.0F),
        SPINE_TAIL_2(SPINE_TAIL_1, 0.0F, 9.0F, 0.0F),
        SPINE_TAIL_3(SPINE_TAIL_2, 0.0F, 8.0F, 0.0F),
        LOWER_FRAGMENT_LEFT(LOWER_ROOT, 5.0F, 3.0F, 0.0F),
        LOWER_FRAGMENT_RIGHT(LOWER_ROOT, -5.0F, 3.0F, 0.0F),
        CLOTH_FRAGMENT_LEFT(LOWER_ROOT, 7.0F, 9.0F, 2.0F),
        CLOTH_FRAGMENT_RIGHT(LOWER_ROOT, -7.0F, 9.0F, 2.0F),
        CHEST_EFFECT_ANCHOR(TORSO, 0.0F, -2.0F, -3.0F),
        HEAD_EFFECT_ANCHOR(HEAD_ROOT, 0.0F, -8.0F, -3.0F);

        private final Joint parent;
        private final float localX;
        private final float localY;
        private final float localZ;
        private final float pivotX;
        private final float pivotY;
        private final float pivotZ;

        Joint(Joint parent, float offsetX, float offsetY, float offsetZ) {
            this.parent = parent;
            this.localX = offsetX;
            this.localY = offsetY;
            this.localZ = offsetZ;
            this.pivotX = parent == null ? offsetX : parent.pivotX + offsetX;
            this.pivotY = parent == null ? offsetY : parent.pivotY + offsetY;
            this.pivotZ = parent == null ? offsetZ : parent.pivotZ + offsetZ;
        }

        public Joint parent() {
            return parent;
        }

        /** Absolute pivot in authoring space; also the bind-pose joint origin. */
        public float pivotX() {
            return pivotX;
        }

        public float pivotY() {
            return pivotY;
        }

        public float pivotZ() {
            return pivotZ;
        }

        public int index() {
            return ordinal();
        }

        /** Offset relative to the parent pivot, which is what the model hierarchy expects. */
        public float localX() {
            return localX;
        }

        public float localY() {
            return localY;
        }

        public float localZ() {
            return localZ;
        }

        public String partName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean isAnchor() {
            return name().endsWith("_ANCHOR");
        }
    }

    /** Texture material families; the offline tool paints faces from these. */
    public enum Material {
        BONE(0xB8B8C4, 0x9A9AA8, 0.0F),
        BONE_DARK(0x6E6678, 0x534C60, 0.0F),
        SHELL(0x49404F, 0x352F3C, 0.0F),
        SHELL_DARK(0x2A2431, 0x1D1922, 0.0F),
        RING(0x9AA0B4, 0x6C7183, 0.35F),
        CORE(0xE6EDF5, 0xC3CBDD, 1.0F),
        ENERGY(0x51436D, 0x3A304F, 0.8F),
        CLOTH(0x19151F, 0x120F17, 0.0F),
        FEATHER(0xC6C8D6, 0x8F93A6, 0.0F);

        private final int primary;
        private final int secondary;
        private final float emissive;

        Material(int primary, int secondary, float emissive) {
            this.primary = primary;
            this.secondary = secondary;
            this.emissive = emissive;
        }

        public int primary() {
            return primary;
        }

        public int secondary() {
            return secondary;
        }

        public float emissive() {
            return emissive;
        }
    }

    /** One authored cube. Centre based so the layout stays readable; sizes are integral. */
    public record CubeSpec(Joint joint, Material material, float centerX, float centerY, float centerZ,
                           int width, int height, int depth, boolean mirror) {
        public int sizeX() {
            return width;
        }

        public int sizeY() {
            return height;
        }

        public int sizeZ() {
            return depth;
        }

        /** Box-UV sheet size for this cube: (2d + 2w) x (d + h). */
        public int uvWidth() {
            return 2 * depth + 2 * width;
        }

        public int uvHeight() {
            return depth + height;
        }
    }

    /** A cube with its assigned UV origin and absolute box bounds. */
    public record PlacedCube(CubeSpec spec, int u, int v) {
        public Joint joint() {
            return spec.joint();
        }

        public Material material() {
            return spec.material();
        }

        /** Box minimum corner in absolute model space. */
        public float minX() {
            return spec.joint().pivotX() + spec.centerX() - spec.width() / 2.0F;
        }

        public float minY() {
            return spec.joint().pivotY() + spec.centerY() - spec.height() / 2.0F;
        }

        public float minZ() {
            return spec.joint().pivotZ() + spec.centerZ() - spec.depth() / 2.0F;
        }

        public float maxX() {
            return minX() + spec.width();
        }

        public float maxY() {
            return minY() + spec.height();
        }

        public float maxZ() {
            return minZ() + spec.depth();
        }
    }

    private static final List<CubeSpec> CUBES = buildCubeSpecs();
    private static final List<PlacedCube> PLACED_CUBES = packUvs(CUBES);

    private VicissitudeRigData() {
    }

    public static List<CubeSpec> cubes() {
        return CUBES;
    }

    public static List<PlacedCube> placedCubes() {
        return PLACED_CUBES;
    }

    public static List<PlacedCube> placedCubes(Joint joint) {
        List<PlacedCube> result = new ArrayList<>();
        for (PlacedCube cube : PLACED_CUBES) {
            if (cube.joint() == joint) {
                result.add(cube);
            }
        }
        return result;
    }

    private static List<CubeSpec> buildCubeSpecs() {
        List<CubeSpec> cubes = new ArrayList<>();
        // Torso: three separated shell fragments around a genuinely hollow chest.
        cubes.add(new CubeSpec(Joint.CHEST_SHELL_LEFT, Material.SHELL, 0.0F, 0.0F, 0.0F, 3, 15, 9, false));
        cubes.add(new CubeSpec(Joint.CHEST_SHELL_RIGHT, Material.SHELL, 0.0F, 0.0F, 0.0F, 3, 15, 9, true));
        cubes.add(new CubeSpec(Joint.CHEST_SHELL_BACK, Material.SHELL_DARK, 0.0F, 0.0F, 0.0F, 9, 15, 2, false));
        cubes.add(new CubeSpec(Joint.TORSO, Material.BONE_DARK, 0.0F, -7.0F, -2.0F, 8, 2, 4, false));
        // Chest remains of the ring; the broken arcs never close.
        cubes.add(new CubeSpec(Joint.CHEST_RING_LEFT, Material.RING, 0.0F, 0.0F, 0.0F, 2, 6, 2, false));
        cubes.add(new CubeSpec(Joint.CHEST_RING_RIGHT, Material.RING, 0.0F, 0.0F, 0.0F, 2, 6, 2, true));
        cubes.add(new CubeSpec(Joint.CHEST_RING_BOTTOM, Material.RING, 0.0F, 0.0F, 0.0F, 8, 2, 3, false));
        // Head: core is mostly hidden by the shell fragments until death reveals the assembly.
        cubes.add(new CubeSpec(Joint.HEAD_CORE, Material.CORE, 0.0F, 0.0F, 0.0F, 6, 7, 6, false));
        cubes.add(new CubeSpec(Joint.HEAD_SHELL_LEFT, Material.SHELL, 0.0F, 0.0F, 0.0F, 3, 8, 8, false));
        cubes.add(new CubeSpec(Joint.HEAD_SHELL_RIGHT, Material.SHELL, 0.0F, 0.0F, 0.0F, 3, 8, 8, true));
        cubes.add(new CubeSpec(Joint.HEAD_SHELL_TOP, Material.SHELL_DARK, 0.0F, 1.0F, 0.0F, 7, 3, 8, false));
        // The halo is a permanently broken ring: four unequally long fragments that never close.
        cubes.add(new CubeSpec(Joint.HALO_FRAGMENT_1, Material.RING, 0.0F, 0.0F, 0.0F, 13, 2, 2, false));
        cubes.add(new CubeSpec(Joint.HALO_FRAGMENT_1, Material.SHELL_DARK, 5.0F, -2.0F, 0.0F, 3, 3, 2, false));
        cubes.add(new CubeSpec(Joint.HALO_FRAGMENT_2, Material.RING, 0.0F, 0.0F, 0.0F, 9, 2, 2, false));
        cubes.add(new CubeSpec(Joint.HALO_FRAGMENT_2, Material.SHELL_DARK, -3.0F, 2.0F, 0.0F, 3, 3, 2, false));
        cubes.add(new CubeSpec(Joint.HALO_FRAGMENT_3, Material.RING, 0.0F, 0.0F, 0.0F, 7, 2, 2, false));
        cubes.add(new CubeSpec(Joint.HALO_FRAGMENT_4, Material.RING, 0.0F, 0.0F, 0.0F, 6, 2, 2, false));
        // Arms.
        cubes.add(new CubeSpec(Joint.UPPER_ARM_LEFT, Material.BONE, 0.0F, 5.0F, 0.0F, 3, 11, 3, false));
        cubes.add(new CubeSpec(Joint.FOREARM_LEFT, Material.BONE, 0.0F, 4.0F, 0.0F, 3, 10, 3, false));
        cubes.add(new CubeSpec(Joint.HAND_LEFT, Material.BONE_DARK, 0.0F, 1.0F, 0.0F, 3, 4, 3, false));
        cubes.add(new CubeSpec(Joint.UPPER_ARM_RIGHT, Material.BONE, 0.0F, 5.0F, 0.0F, 3, 11, 3, true));
        cubes.add(new CubeSpec(Joint.FOREARM_RIGHT, Material.BONE, 0.0F, 4.0F, 0.0F, 3, 10, 3, true));
        cubes.add(new CubeSpec(Joint.HAND_RIGHT, Material.BONE_DARK, 0.0F, 1.0F, 0.0F, 3, 4, 3, true));
        addWing(cubes, true);
        addWing(cubes, false);
        // Lower body: three spine sections plus loose fragments, no legs.
        cubes.add(new CubeSpec(Joint.SPINE_TAIL_1, Material.BONE_DARK, 0.0F, 4.0F, 0.0F, 4, 8, 4, false));
        cubes.add(new CubeSpec(Joint.SPINE_TAIL_2, Material.BONE_DARK, 0.0F, 4.0F, 0.0F, 3, 8, 3, false));
        cubes.add(new CubeSpec(Joint.SPINE_TAIL_3, Material.BONE, 0.0F, 3.0F, 0.0F, 2, 7, 2, false));
        cubes.add(new CubeSpec(Joint.LOWER_FRAGMENT_LEFT, Material.SHELL, 0.0F, 2.0F, 0.0F, 3, 8, 5, false));
        cubes.add(new CubeSpec(Joint.LOWER_FRAGMENT_RIGHT, Material.SHELL, 0.0F, 2.0F, 0.0F, 3, 8, 5, true));
        cubes.add(new CubeSpec(Joint.CLOTH_FRAGMENT_LEFT, Material.CLOTH, 0.0F, 5.0F, 0.0F, 4, 12, 2, false));
        cubes.add(new CubeSpec(Joint.CLOTH_FRAGMENT_RIGHT, Material.CLOTH, 0.0F, 5.0F, 0.0F, 4, 12, 2, true));
        return Collections.unmodifiableList(cubes);
    }

    private static void addWing(List<CubeSpec> cubes, boolean left) {
        boolean mirror = !left;
        float sign = left ? 1.0F : -1.0F;
        cubes.add(new CubeSpec(left ? Joint.WING_LEFT_UPPER : Joint.WING_RIGHT_UPPER,
                Material.BONE, sign * 9.0F, 0.0F, 0.0F, 18, 4, 5, mirror));
        cubes.add(new CubeSpec(left ? Joint.WING_LEFT_OUTER : Joint.WING_RIGHT_OUTER,
                Material.BONE_DARK, sign * 7.0F, 0.0F, 0.0F, 14, 3, 4, mirror));
        cubes.add(new CubeSpec(left ? Joint.WING_LEFT_LOWER : Joint.WING_RIGHT_LOWER,
                Material.FEATHER, sign * 6.0F, 0.0F, 0.0F, 12, 3, 3, mirror));
        for (int i = 1; i <= 4; i++) {
            Joint feather = Joint.valueOf("WING_" + (left ? "LEFT" : "RIGHT") + "_FEATHER_" + i);
            // Main feathers sweep back and down from the wing bone.
            cubes.add(new CubeSpec(feather, Material.FEATHER,
                    0.0F, 5.0F, 5.0F + i * 4.0F, 5, 3, 15, mirror));
        }
        for (int i = 1; i <= 2; i++) {
            Joint broken = Joint.valueOf("WING_" + (left ? "LEFT" : "RIGHT") + "_BROKEN_" + i);
            // Broken quills: exposed bone without the feather blade.
            cubes.add(new CubeSpec(broken, Material.BONE_DARK,
                    0.0F, 3.0F, 3.0F + i * 5.0F, 3, 2, 9, mirror));
        }
        Joint tip = left ? Joint.WING_LEFT_TIP : Joint.WING_RIGHT_TIP;
        cubes.add(new CubeSpec(tip, Material.BONE, sign * 3.0F, 0.0F, 0.0F, 6, 2, 2, mirror));
        cubes.add(new CubeSpec(tip, Material.BONE, sign * 6.0F, 2.0F, 1.0F, 5, 2, 2, mirror));
    }

    /**
     * Deterministic shelf packing of every cube's box-UV rectangle. Both the runtime model and
     * the offline texture generator call this, so their layouts can never drift apart.
     */
    private static List<PlacedCube> packUvs(List<CubeSpec> cubes) {
        List<PlacedCube> placed = new ArrayList<>(cubes.size());
        int shelfY = UV_MARGIN;
        int shelfHeight = 0;
        int cursorX = UV_MARGIN;
        for (CubeSpec cube : cubes) {
            int width = cube.uvWidth();
            int height = cube.uvHeight();
            if (width + 2 * UV_MARGIN > TEXTURE_SIZE || height + 2 * UV_MARGIN > TEXTURE_SIZE) {
                throw new IllegalStateException("Cube UV does not fit the sheet: " + cube);
            }
            if (cursorX + width + UV_MARGIN > TEXTURE_SIZE) {
                shelfY += shelfHeight + UV_MARGIN;
                shelfHeight = 0;
                cursorX = UV_MARGIN;
            }
            if (shelfY + height + UV_MARGIN > TEXTURE_SIZE) {
                throw new IllegalStateException("Texture sheet exhausted; reduce cube sizes: " + cube);
            }
            placed.add(new PlacedCube(cube, cursorX, shelfY));
            cursorX += width + UV_MARGIN;
            shelfHeight = Math.max(shelfHeight, height);
        }
        return Collections.unmodifiableList(placed);
    }
}
