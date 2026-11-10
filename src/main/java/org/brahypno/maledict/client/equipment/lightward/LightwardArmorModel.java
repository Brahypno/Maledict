package org.brahypno.maledict.client.equipment.lightward;

import com.sammy.malum.client.model.MalignantStrongholdArmorModel;
import com.sammy.malum.client.model.SoulStainedSteelArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.brahypno.maledict.Maledict;
import team.lodestar.lodestone.systems.model.LodestoneArmorModel;

import java.util.List;

/** Original Malum geometry with Lightward cheek inlays and a rear helmet guard. */
public final class LightwardArmorModel extends LodestoneArmorModel {
    public static final ModelLayerLocation HELMET_LAYER = layer("aufheben_lightward_helmet");
    public static final ModelLayerLocation CHESTPLATE_LAYER = layer("aufheben_lightward_chestplate");
    public static final ModelLayerLocation HELMET_DETAILS_LAYER = layer("aufheben_lightward_helmet_details");
    private final ModelPart helmetDetails;

    public LightwardArmorModel(ModelPart root) {
        this(root, null);
    }

    public LightwardArmorModel(ModelPart root, ModelPart helmetDetails) {
        super(root);
        this.helmetDetails = helmetDetails;
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, name), "main");
    }

    public static LayerDefinition createHelmetLayer() {
        return SoulStainedSteelArmorModel.createBodyLayer();
    }

    public static LayerDefinition createChestplateLayer() {
        return MalignantStrongholdArmorModel.createBodyLayer();
    }

    public static LayerDefinition createHelmetDetailsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition details = mesh.getRoot().addOrReplaceChild("helmet_details", CubeListBuilder.create()
                .texOffs(84, 34).addBox(-3.0F, -3.0F, -5.25F, 2.0F, 3.0F, 1.0F)
                .mirror().texOffs(84, 34).addBox(1.0F, -3.0F, -5.25F, 2.0F, 3.0F, 1.0F)
                .mirror(false).texOffs(92, 34).addBox(-4.5F, -8.5F, 4.5F, 9.0F, 9.0F, 1.0F)
                .texOffs(100, 46).addBox(-3.5F, -2.25F, 5.25F, 7.0F, 2.0F, 1.0F),
                PartPose.ZERO);
        details.addOrReplaceChild("left_rear_lame", CubeListBuilder.create()
                        .texOffs(92, 46).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(-2.65F, -4.6F, 5.5F, 0, 0, -0.22F));
        details.addOrReplaceChild("right_rear_lame", CubeListBuilder.create()
                        .texOffs(92, 46).mirror().addBox(-1.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(2.65F, -4.6F, 5.5F, 0, 0, 0.22F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        if (helmetDetails == null) {
            return super.headParts();
        }
        return slot == EquipmentSlot.HEAD ? List.of(head, helmetDetails) : List.of();
    }

    @Override
    public void copyFromDefault(HumanoidModel original) {
        // Lodestone copies these inherited leg poses into its armor parts at render time.
        leftLeg.copyFrom(original.leftLeg);
        rightLeg.copyFrom(original.rightLeg);
        super.copyFromDefault(original);
        if (helmetDetails != null) {
            helmetDetails.copyFrom(head);
        }
    }
}
