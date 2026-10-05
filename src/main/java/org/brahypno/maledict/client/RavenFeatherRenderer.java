package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.RavenFeatherEntity;

/** Uses the supplied transparent pixel feather directly, with no additional item registration. */
public final class RavenFeatherRenderer extends EntityRenderer<RavenFeatherEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/raven_feather.png");

    public RavenFeatherRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RavenFeatherEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(RavenFeatherEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose transform = pose.last();
        vertex(vertices, transform, -0.25F, -0.25F, 0, 1, light);
        vertex(vertices, transform, 0.25F, -0.25F, 1, 1, light);
        vertex(vertices, transform, 0.25F, 0.25F, 1, 0, light);
        vertex(vertices, transform, -0.25F, 0.25F, 0, 0, light);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               float x, float y, float u, float v, int light) {
        vertices.vertex(pose.pose(), x, y, 0).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 0, 1).endVertex();
    }
}
