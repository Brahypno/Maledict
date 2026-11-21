package org.brahypno.maledict.client.entity.weepingWither;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.WitherSkeleton;
import org.brahypno.maledict.Maledict;

/**
 * 1.21.1 的 {@code WitherSkeletonRenderer} 已经是 {@code SkeletonRenderer<WitherSkeleton>}，
 * 匿名图层必须跟着收窄到 {@code WitherSkeleton} / {@code SkeletonModel<WitherSkeleton>}。
 */
public final class WeepingWitherSkeletonRenderer extends WitherSkeletonRenderer {
    private static final ResourceLocation BODY = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/weeping_wither_skeleton.png");
    private static final RenderType SEEP_LIGHT = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/weeping_wither_skeleton_emissive.png"));

    public WeepingWitherSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new RenderLayer<WitherSkeleton, SkeletonModel<WitherSkeleton>>(this) {
            @Override
            public void render(
                    PoseStack pose, MultiBufferSource buffers, int light, WitherSkeleton skeleton,
                    float swing, float amount, float partialTick, float age, float yaw, float pitch) {
                // Match the Weeping Wither's normal fissure intensity.
                getParentModel().renderToBuffer(pose, buffers.getBuffer(SEEP_LIGHT),
                                                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                                                net.minecraft.util.FastColor.ARGB32.colorFromFloat(1.0F, 0.50F, 0.50F, 0.50F));
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(WitherSkeleton skeleton) {
        return BODY;
    }
}
