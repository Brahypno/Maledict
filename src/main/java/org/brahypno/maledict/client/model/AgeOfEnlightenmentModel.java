package org.brahypno.maledict.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.brahypno.maledict.Maledict;

/**
 * 启蒙之年的面具模型：一个盖住前半张脸的盒子；原点在眼睛高度，头占 x,z ∈ [-4,4]、y ∈ [-8,0]，-z 是脸朝向。
 * 盒界全取整数（比头每边大 1，既不与头共面 z-fighting，又让正脸 10x10 中间的 8x8 与原版脸 1:1），贴图 UV 是按这个算的，别随手改。
 */
public final class AgeOfEnlightenmentModel extends EntityModel<Player> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "age_of_enlightenment"), "main");

    /** 贴图画布尺寸，必须与 art/age-of-enlightenment/tools/MaskTextureBuilder.java 的 TEX 一致。 */
    public static final int TEXTURE_WIDTH = 64;
    public static final int TEXTURE_HEIGHT = 64;

    private final ModelPart mask;

    public AgeOfEnlightenmentModel(ModelPart root) {
        this.mask = root.getChild("mask");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("mask", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 10.0F, 5.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    public ModelPart mask() {
        return mask;
    }

    @Override
    public void setupAnim(Player entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // 故意留空：头部姿态每帧由 ICurioRenderer.followHeadRotations 复制过来，不在模型里做动画。
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        mask.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
