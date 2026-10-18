package org.brahypno.maledict.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sammy.malum.client.RenderUtils;
import com.sammy.malum.registry.client.MalumRenderTypeTokens;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.common.entity.SpiritArrowEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypes;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;

import java.awt.Color;

public class SpiritArrowRenderer extends ArrowRenderer<SpiritArrowEntity> {
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/projectiles/arrow.png");
    private Color renderColor = Color.WHITE;

    public SpiritArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritArrowEntity entity) {
        return ARROW_TEXTURE;
    }

    @Override
    public void render(SpiritArrowEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        var spiritType = entity.getArrowType().getSpiritType();
        var trailRenderType = LodestoneRenderTypes.ADDITIVE_TEXTURE_TRIANGLE
                .apply(MalumRenderTypeTokens.CONCENTRATED_TRAIL).getRenderType();
        var trailBuilder = VFXBuilders.createWorld().setRenderType(trailRenderType);
        float visualScale = Math.min(1.0F, entity.tickCount / 5.0F);
        RenderUtils.renderEntityTrail(
                poseStack, trailBuilder, entity.trailPointBuilder, entity,
                spiritType.getPrimaryColor(), spiritType.getSecondaryColor(),
                visualScale, partialTick);
        RenderUtils.renderEntityTrail(
                poseStack, trailBuilder, entity.spinningTrailPointBuilder, entity,
                spiritType.getPrimaryColor(), spiritType.getSecondaryColor(),
                visualScale, partialTick);

        renderColor = spiritType.getItemColor();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void vertex(PoseStack.Pose pose, VertexConsumer consumer,
                       int x, int y, int z, float u, float v,
                       int normalX, int normalZ, int normalY, int packedLight) {
        consumer.addVertex(pose, x, y, z)
                .setColor(renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ)
                ;
    }
}
