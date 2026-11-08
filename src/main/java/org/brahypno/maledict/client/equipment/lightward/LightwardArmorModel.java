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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.brahypno.maledict.Maledict;
import team.lodestar.lodestone.systems.model.LodestoneArmorModel;

import java.util.List;

/** Original Malum geometry with gray cheek extensions carrying one-pixel tears below the pupils. */
public final class LightwardArmorModel extends LodestoneArmorModel {
    public static final ModelLayerLocation HELMET_LAYER = layer("aufheben_lightward_helmet");
    public static final ModelLayerLocation CHESTPLATE_LAYER = layer("aufheben_lightward_chestplate");
    public static final ModelLayerLocation TEARS_LAYER = layer("aufheben_lightward_tears");
    private final ModelPart tears;

    public LightwardArmorModel(ModelPart root) {
        this(root, null);
    }

    public LightwardArmorModel(ModelPart root, ModelPart tears) {
        super(root);
        this.tears = tears;
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

    public static LayerDefinition createTearsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("tears", CubeListBuilder.create()
                .texOffs(84, 34).addBox(-3.0F, -3.0F, -5.25F, 2.0F, 3.0F, 1.0F)
                .mirror().texOffs(84, 34).addBox(1.0F, -3.0F, -5.25F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        if (tears == null) {
            return super.headParts();
        }
        return slot == EquipmentSlot.HEAD ? List.of(head, tears) : List.of();
    }

    @Override
    public void copyFromDefault(HumanoidModel original) {
        // Lodestone copies these inherited leg poses into its armor parts at render time.
        leftLeg.copyFrom(original.leftLeg);
        rightLeg.copyFrom(original.rightLeg);
        super.copyFromDefault(original);
        if (tears != null) {
            tears.copyFrom(head);
        }
    }
}
