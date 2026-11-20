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
import java.util.List;

public final class LightwardArmorModel extends LodestoneArmorModel {
    public static final ModelLayerLocation HELMET_LAYER = layer("aufheben_lightward_helmet");
    public static final ModelLayerLocation CHESTPLATE_LAYER = layer("aufheben_lightward_chestplate");
    public static final ModelLayerLocation CHESTPLATE_DETAILS_LAYER = layer("aufheben_lightward_chestplate_details");
    public static final ModelLayerLocation LOWER_ARMOR_LAYER = layer("aufheben_lightward_lower_armor");
    private static final ResourceLocation HELMET_GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/armor/aufheben_lightward_helmet.geometry.json");
    private static final ResourceLocation CHESTPLATE_GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/armor/aufheben_lightward_chestplate.geometry.json");
    private static final ResourceLocation ARMS_GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/armor/aufheben_lightward_arms.geometry.json");
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
                    var deformation = cube.has("inflate")
                            ? new CubeDeformation(cube.get("inflate").getAsFloat()) : CubeDeformation.NONE;
                    cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                         .mirror(cube.has("mirror") && cube.get("mirror").getAsBoolean())
                         .addBox(axis(pos, 0), axis(pos, 1), axis(pos, 2),
                                 axis(size, 0), axis(size, 1), axis(size, 2), deformation);
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

    public static LayerDefinition createChestplateLayer() {
        return MalignantStrongholdArmorModel.createBodyLayer();
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return slot == EquipmentSlot.CHEST && !chestParts.isEmpty() ? chestParts : super.bodyParts();
    }

    @Override
    public void copyFromDefault(HumanoidModel original) {
        // Lodestone copies these inherited leg poses into its armor parts at render time.
        leftLeg.copyFrom(original.leftLeg);
        rightLeg.copyFrom(original.rightLeg);
        super.copyFromDefault(original);
        if (chestOverlay != null) {
            chestOverlay.copyFrom(body);
            leftArmOverlay.copyFrom(leftArm);
            rightArmOverlay.copyFrom(rightArm);
        }
    }
}
