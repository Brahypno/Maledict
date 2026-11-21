package org.brahypno.maledict.client.item.equipment.lightward;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.sammy.malum.client.model.MalignantStrongholdArmorModel;
import com.sammy.malum.client.model.SoulStainedSteelArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import org.brahypno.maledict.Maledict;
import team.lodestar.lodestone.systems.model.LodestoneArmorModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扬弃逐光护甲的自定义模型：头盔、胸甲与靴子读手工挑出来的 geometry JSON，腿直接复用 Malum 的灵魂锈钢
 * 层级。
 *
 * <p>1.21 侧两处差异：Lodestone 的腿部件字段叫 {@code leftLegging}/{@code rightLegging}（不是
 * {@code leftLeg}/{@code rightLeg}）；{@code copyFromDefault} 仍收 {@code HumanoidModel}。
 */
public final class LightwardArmorModel extends LodestoneArmorModel {
    public static final ModelLayerLocation HELMET_LAYER = layer("aufheben_lightward_helmet");
    public static final ModelLayerLocation CHESTPLATE_LAYER = layer("aufheben_lightward_chestplate");
    public static final ModelLayerLocation CHESTPLATE_DETAILS_LAYER = layer("aufheben_lightward_chestplate_details");
    public static final ModelLayerLocation LOWER_ARMOR_LAYER = layer("aufheben_lightward_lower_armor");
    public static final ModelLayerLocation BOOTS_LAYER = layer("aufheben_lightward_boots");
    private static final ResourceLocation HELMET_GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/armor/aufheben_lightward_helmet.geometry.json");
    private static final ResourceLocation CHESTPLATE_GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/armor/aufheben_lightward_chestplate.geometry.json");
    private static final ResourceLocation ARMS_GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/armor/aufheben_lightward_arms.geometry.json");
    private static final ResourceLocation BOOTS_GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/armor/aufheben_lightward_boots.geometry.json");
    private final ModelPart chestOverlay;
    private final ModelPart leftArmOverlay;
    private final ModelPart rightArmOverlay;
    private final List<ModelPart> chestParts;

    public LightwardArmorModel(ModelPart root) {
        this(root, null);
    }

    public LightwardArmorModel(ModelPart root, ModelPart chestDetails) {
        super(root);
        chestOverlay = chestDetails == null ? null : chestDetails.getChild("body");
        leftArmOverlay = chestDetails == null ? null : chestDetails.getChild("left_arm");
        rightArmOverlay = chestDetails == null ? null : chestDetails.getChild("right_arm");
        if (chestOverlay != null) {
            // The replacement torso has an open shrine; the solid Malum breastplate would cover its cavity.
            body.getChild("torso").visible = false;
            leftArm.getChild("left_shoulder").visible = false;
            rightArm.getChild("right_shoulder").visible = false;
        }
        chestParts = chestOverlay == null ? List.of()
                : List.of(body, leftArm, rightArm, chestOverlay, leftArmOverlay, rightArmOverlay);
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, name), "main");
    }

    public static LayerDefinition createHelmetLayer(ResourceManager resources) {
        return createGeometryLayer(resources, HELMET_GEOMETRY, "head");
    }

    public static LayerDefinition createChestplateDetailsLayer(ResourceManager resources) {
        var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        PartDefinition root = createHumanoidAlias(mesh);
        addGeometry(root, resources, CHESTPLATE_GEOMETRY, "body");
        addGeometry(root, resources, ARMS_GEOMETRY, "body");
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition createGeometryLayer(ResourceManager resources, ResourceLocation geometry, String bone) {
        var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        addGeometry(createHumanoidAlias(mesh), resources, geometry, bone);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addGeometry(PartDefinition root, ResourceManager resources,
                                    ResourceLocation geometry, String bone) {
        try (var reader = resources.getResourceOrThrow(geometry).openAsReader()) {
            var parts = JsonParser.parseReader(reader).getAsJsonObject();
            for (var entry : parts.entrySet()) {
                var part = entry.getValue().getAsJsonObject();
                var cubes = CubeListBuilder.create();
                for (var element : part.getAsJsonArray("cubes")) {
                    var cube = element.getAsJsonObject();
                    var uv = cube.getAsJsonArray("uv");
                    var pos = cube.getAsJsonArray("pos");
                    var size = cube.getAsJsonArray("size");
                    // inflate 可以是标量（老几何）或三轴数组（靴子的晶根）；uv_scale 决定贴图密度。
                    var inflate = cube.get("inflate");
                    var deformation = inflate == null ? CubeDeformation.NONE
                            : inflate.isJsonArray() ? new CubeDeformation(
                                    axis(inflate.getAsJsonArray(), 0), axis(inflate.getAsJsonArray(), 1),
                                    axis(inflate.getAsJsonArray(), 2))
                            : new CubeDeformation(inflate.getAsFloat());
                    float uvScale = cube.has("uv_scale") ? cube.get("uv_scale").getAsFloat() : 1;
                    cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                         .mirror(cube.has("mirror") && cube.get("mirror").getAsBoolean())
                         .addBox(axis(pos, 0), axis(pos, 1), axis(pos, 2),
                                 axis(size, 0), axis(size, 1), axis(size, 2), deformation, 1 / uvScale, 1 / uvScale);
                }
                var offset = part.getAsJsonArray("offset");
                var rotation = part.getAsJsonArray("rotation");
                PartDefinition parent = root.getChild(part.has("parent") ? part.get("parent").getAsString() : bone);
                parent.addOrReplaceChild(entry.getKey(), cubes, PartPose.offsetAndRotation(
                        axis(offset, 0), axis(offset, 1), axis(offset, 2),
                        axis(rotation, 0), axis(rotation, 1), axis(rotation, 2)));
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException("Cannot load " + geometry, exception);
        }
    }

    private static float axis(JsonArray vector, int index) {
        return vector == null ? 0 : vector.get(index).getAsFloat();
    }

    public static LayerDefinition createLowerArmorLayer() {
        return SoulStainedSteelArmorModel.createBodyLayer();
    }

    /**
     * 靴子的 layer：几何 JSON 里的 {@code cubes} 挂在 {@code left_foot} 上，只用来把那层烘焙出来
     * （{@code uv_scale} 与 {@code inflate} 就在这一步生效）；真正的曲面由 {@link #createBootsModel} 从
     * 同一份 JSON 的 {@code meshes} 重建。
     */
    public static LayerDefinition createBootsLayer(ResourceManager resources) {
        return createGeometryLayer(resources, BOOTS_GEOMETRY, "left_foot");
    }

    /**
     * 靴子模型：把烘焙层里除脚以外的部件原样搬过来，两只脚换成由 {@link LightwardMeshCube} 组成的
     * 连续曲面。
     *
     * <p>Lodestone 的 {@code copyFromDefault} 会把 {@code leftFoot}/{@code rightFoot} 的变换同步成腿的，
     * 所以曲面天然跟着腿走，不需要额外摆姿态。缺哪个部件就用空件兜底：Malum 的层级名一改，
     * 这里只会少画一件，不会在烘焙时抛 {@code NoSuchElementException}。
     */
    public static LightwardArmorModel createBootsModel(ModelPart baked, ResourceManager resources) {
        Map<String, ModelPart> children = new HashMap<>();
        for (String name : List.of("head", "hat", "body", "left_arm", "right_arm", "left_leg", "right_leg",
                "leggings", "left_legging", "right_legging")) {
            children.put(name, childOrEmpty(baked, name));
        }
        try (var reader = resources.getResourceOrThrow(BOOTS_GEOMETRY).openAsReader()) {
            var parts = JsonParser.parseReader(reader).getAsJsonObject();
            for (String side : List.of("left", "right")) {
                var meshes = parts.getAsJsonObject("boot_core_" + side).getAsJsonArray("meshes");
                List<ModelPart.Cube> cubes = new ArrayList<>();
                for (var mesh : meshes) {
                    cubes.add(new LightwardMeshCube(mesh.getAsJsonObject()));
                }
                var core = new ModelPart(cubes, Map.of());
                children.put(side + "_foot", new ModelPart(List.of(), Map.of("boot_core_" + side, core)));
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException("Cannot load " + BOOTS_GEOMETRY, exception);
        }
        return new LightwardArmorModel(new ModelPart(List.of(), children));
    }

    private static ModelPart childOrEmpty(ModelPart parent, String name) {
        return parent.hasChild(name) ? parent.getChild(name) : new ModelPart(List.of(), Map.of());
    }

    public static LayerDefinition createChestplateLayer() {
        return MalignantStrongholdArmorModel.createBodyLayer();
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return slot == EquipmentSlot.CHEST && !chestParts.isEmpty() ? chestParts : super.bodyParts();
    }

    @Override
    @SuppressWarnings("rawtypes") // LodestoneArmorModel declares the parameter raw; a wildcard would not override it.
    public void copyFromDefault(HumanoidModel original) {
        // Lodestone copies these inherited leg poses into its armor parts at render time.
        leftLegging.copyFrom(original.leftLeg);
        rightLegging.copyFrom(original.rightLeg);
        super.copyFromDefault(original);
        if (chestOverlay != null) {
            chestOverlay.copyFrom(body);
            leftArmOverlay.copyFrom(leftArm);
            rightArmOverlay.copyFrom(rightArm);
        }
    }
}
