package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.brahypno.maledict.client.model.FirstVicissitudeBossModel;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;
import org.joml.Quaternionf;

/**
 * Vanilla double glass shell with a gold block core, attached to the chest cavity.
 */
final class FirstVicissitudeChestCrystalLayer
        extends RenderLayer<FirstVicissitudeBossEntity, FirstVicissitudeBossModel> {
    // Model units: the outer cube's rotation sphere is 4 * sqrt(3) * SCALE = 9.35.
    static final float SCALE = 0.875F;
    static final float DEPTH = 1.0F;
    static final float VERTICAL_OFFSET = 1.0F;
    private static final RenderType GLASS = RenderType.entityCutoutNoCull(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/end_crystal/end_crystal.png"));
    private final ModelPart glass;

    FirstVicissitudeChestCrystalLayer(
            FirstVicissitudeBossRenderer renderer,
            EntityRendererProvider.Context context) {
        super(renderer);
        glass = context.bakeLayer(ModelLayers.END_CRYSTAL).getChild("glass");
    }

    @Override
    public void render(
            PoseStack stack, MultiBufferSource buffers, int light,
            FirstVicissitudeBossEntity entity, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible())
            return;
        stack.pushPose();
        getParentModel().poseStackTo(Joint.TORSO, stack);
        stack.translate(VicissitudeRig.CHEST_RING_HUB_X / 16F,
                        (VicissitudeRig.CHEST_RING_HUB_Y + VERTICAL_OFFSET) / 16F, DEPTH / 16F);
        stack.scale(SCALE, SCALE, SCALE);
        float turn = ageInTicks * 3F;
        var tilt = new Quaternionf().setAngleAxis((float) Math.PI / 3F,
                                                  (float) Math.sqrt(0.5), 0, (float) Math.sqrt(0.5));
        stack.mulPose(Axis.YP.rotationDegrees(turn));
        stack.mulPose(tilt);
        glass.render(stack, buffers.getBuffer(GLASS), light, OverlayTexture.NO_OVERLAY);
        stack.scale(.875F, .875F, .875F);
        stack.mulPose(tilt);
        stack.mulPose(Axis.YP.rotationDegrees(turn));
        glass.render(stack, buffers.getBuffer(GLASS), light, OverlayTexture.NO_OVERLAY);
        stack.scale(.875F, .875F, .875F);
        stack.mulPose(tilt);
        stack.mulPose(Axis.YP.rotationDegrees(turn));
        // A block model is 16 units wide; the original crystal core is 8 units wide.
        stack.scale(.5F, .5F, .5F);
        stack.translate(-.5, -.5, -.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Block.byItem(ItemRegistry.BLOCK_OF_CTHONIC_GOLD.get()).defaultBlockState(),
                stack, buffers, light, OverlayTexture.NO_OVERLAY);
        stack.popPose();
    }
}
