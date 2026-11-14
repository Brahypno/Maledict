package org.brahypno.maledict.client.equipment.lightward;

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
import org.brahypno.maledict.Maledict;
import team.lodestar.lodestone.systems.model.LodestoneArmorModel;

import java.io.IOException;

public final class LightwardArmorModel extends LodestoneArmorModel {
    public static final ModelLayerLocation HELMET_LAYER = layer("aufheben_lightward_helmet");
    public static final ModelLayerLocation CHESTPLATE_LAYER = layer("aufheben_lightward_chestplate");
    public static final ModelLayerLocation LOWER_ARMOR_LAYER = layer("aufheben_lightward_lower_armor");
    private static final ResourceLocation HELMET_GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "models/armor/aufheben_lightward_helmet.geometry.json");

    public LightwardArmorModel(ModelPart root) {
        super(root);
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, name), "main");
    }

    public static LayerDefinition createHelmetLayer(ResourceManager resources) {
        var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        PartDefinition head = createHumanoidAlias(mesh).getChild("head");
        try (var reader = resources.getResourceOrThrow(HELMET_GEOMETRY).openAsReader()) {
            var parts = JsonParser.parseReader(reader).getAsJsonObject();
            for (var entry : parts.entrySet()) {
                var part = entry.getValue().getAsJsonObject();
                var cubes = CubeListBuilder.create();
                for (var element : part.getAsJsonArray("cubes")) {
                    var cube = element.getAsJsonObject();
                    var uv = cube.getAsJsonArray("uv");
                    var pos = cube.getAsJsonArray("pos");
                    var size = cube.getAsJsonArray("size");
                    cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                            .mirror(cube.has("mirror") && cube.get("mirror").getAsBoolean())
                            .addBox(axis(pos, 0), axis(pos, 1), axis(pos, 2),
                                    axis(size, 0), axis(size, 1), axis(size, 2));
                }
                var offset = part.getAsJsonArray("offset");
                var rotation = part.getAsJsonArray("rotation");
                head.addOrReplaceChild(entry.getKey(), cubes, PartPose.offsetAndRotation(
                        axis(offset, 0), axis(offset, 1), axis(offset, 2),
                        axis(rotation, 0), axis(rotation, 1), axis(rotation, 2)));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load " + HELMET_GEOMETRY, exception);
        }
        return LayerDefinition.create(mesh, 128, 128);
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
    public void copyFromDefault(HumanoidModel original) {
        // Lodestone copies these inherited leg poses into its armor parts at render time.
        leftLeg.copyFrom(original.leftLeg);
        rightLeg.copyFrom(original.rightLeg);
        super.copyFromDefault(original);
    }
}
