package org.brahypno.maledict.client.entity.weepingWither;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.wither.OssuaryFangs;

/**
 * Splintered bone spikes animated by the vanilla fang progress.
 */
public final class OssuaryFangsRenderer extends EntityRenderer<OssuaryFangs> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/ossuary_fangs.png");
    private static final int WHITE = FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F);
    /**
     * The splintered spikes read smaller than the vanilla fang model at the same pose scale, so the
     * whole pose is enlarged; the hit box stays the vanilla fang's.
     */
    private static final float SIZE_MULTIPLIER = 1.5F;
    private final OssuaryFangsModel model;

    public OssuaryFangsRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new OssuaryFangsModel();
    }

    @Override
    public void render(
            OssuaryFangs entity, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        float progress = entity.getAnimationProgress(partialTick);
        if (progress == 0.0F)
            return;
        float scale = 2.0F * SIZE_MULTIPLIER;
        if (progress > 0.9F)
            scale *= (1.0F - progress) / 0.1F;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90.0F - entity.getYRot()));
        pose.scale(-scale, -scale, scale);
        pose.translate(0.0F, -0.626F, 0.0F);
        pose.scale(0.5F, 0.5F, 0.5F);
        model.setupAnim(entity, progress, 0.0F, 0.0F, entity.getYRot(), entity.getXRot());
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, WHITE);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(OssuaryFangs entity) {
        return TEXTURE;
    }
}
