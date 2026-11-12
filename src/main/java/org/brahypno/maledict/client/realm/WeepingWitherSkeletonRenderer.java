package org.brahypno.maledict.client.realm;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.brahypno.maledict.Maledict;

public final class WeepingWitherSkeletonRenderer extends WitherSkeletonRenderer {
    private static final ResourceLocation BODY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/weeping_wither_skeleton.png");
    private static final RenderType SEEP_LIGHT = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/weeping_wither_skeleton_emissive.png"));

    public WeepingWitherSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractSkeleton skeleton,
                               float swing, float amount, float partialTick, float age, float yaw, float pitch) {
                // Match the Weeping Wither's normal fissure intensity.
                getParentModel().renderToBuffer(pose, buffers.getBuffer(SEEP_LIGHT),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.50F, 0.50F, 0.50F, 1.0F);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSkeleton skeleton) {
        return BODY;
    }
}
