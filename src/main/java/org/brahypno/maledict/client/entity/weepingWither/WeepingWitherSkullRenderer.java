package org.brahypno.maledict.client.entity.weepingWither;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.brahypno.maledict.common.entity.wither.WeepingWitherSkull;

/**
 * Vanilla projectile geometry with the boss's weathered skull, wraparound cracks and local light.
 */
public final class WeepingWitherSkullRenderer extends EntityRenderer<WeepingWitherSkull> {
    private static final ResourceLocation BODY = texture("weeping_wither_skull");
    private static final ResourceLocation CHARGED = texture("weeping_wither_skull_charged");
    private static final ResourceLocation LIGHT = texture("weeping_wither_skull_emissive");
    private static final int WHITE = FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F);
    private final SkullModel model;

    public WeepingWitherSkullRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new SkullModel(context.bakeLayer(ModelLayers.WITHER_SKULL));
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("maledict", "textures/entity/" + name + ".png");
    }

    private static int glow(float intensity) {
        return FastColor.ARGB32.colorFromFloat(1.0F, intensity, intensity, intensity);
    }

    @Override
    protected int getBlockLightLevel(WeepingWitherSkull entity, BlockPos pos) {return 15;}

    @Override
    public ResourceLocation getTextureLocation(WeepingWitherSkull entity) {
        return entity.isDangerous() ? CHARGED : BODY;
    }

    @Override
    public void render(
            WeepingWitherSkull entity, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.scale(-1.0F, -1.0F, 1.0F);
        model.setupAnim(0.0F, Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()),
                        Mth.lerp(partialTick, entity.xRotO, entity.getXRot()));
        model.renderToBuffer(pose, buffers.getBuffer(model.renderType(getTextureLocation(entity))),
                             light, OverlayTexture.NO_OVERLAY, WHITE);
        float glow = entity.isDangerous() ? 0.95F : 0.55F;
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(LIGHT)),
                             0xF000F0, OverlayTexture.NO_OVERLAY, glow(glow));
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }
}
