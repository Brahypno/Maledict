package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.malum.client.RenderUtils;
import com.sammy.malum.client.SpiritBasedWorldVFXBuilder;
import com.sammy.malum.client.renderer.entity.FloatingItemEntityRenderer;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.client.MalumRenderTypeTokens;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.common.entity.VicissitudeLightOrbEntity;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;

/** Mirrors the umbral/eldritch double glimmer and trail of Malum's pneuma void. */
public final class VicissitudeLightOrbRenderer extends EntityRenderer<VicissitudeLightOrbEntity> {
    public VicissitudeLightOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
        shadowStrength = 0.0F;
    }

    @Override
    public void render(VicissitudeLightOrbEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        MalumSpiritType umbral = SpiritTypeRegistry.UMBRAL_SPIRIT;
        MalumSpiritType eldritch = SpiritTypeRegistry.ELDRITCH_SPIRIT;
        LodestoneRenderType renderType = LodestoneRenderTypeRegistry.ADDITIVE_TEXTURE_TRIANGLE
                .applyAndCache(MalumRenderTypeTokens.CONCENTRATED_TRAIL);
        VFXBuilders.WorldVFXBuilder umbralTrail = SpiritBasedWorldVFXBuilder.create(umbral)
                .setRenderType(renderType);
        VFXBuilders.WorldVFXBuilder eldritchTrail = SpiritBasedWorldVFXBuilder.create(eldritch)
                .setRenderType(renderType);

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getYOffset(partialTick), 0.0D);
        FloatingItemEntityRenderer.renderSpiritGlimmer(poseStack, umbral, 0.85F, 4.0F, partialTick);
        FloatingItemEntityRenderer.renderSpiritGlimmer(poseStack, eldritch, 0.6F, 0.5F, partialTick);
        poseStack.popPose();

        RenderUtils.renderEntityTrail(poseStack, umbralTrail, entity.trail, entity,
                umbral.getPrimaryColor(), umbral.getSecondaryColor(), 1.0F, partialTick);
        RenderUtils.renderEntityTrail(poseStack, eldritchTrail, entity.trail, entity,
                eldritch.getPrimaryColor(), eldritch.getSecondaryColor(), 0.75F, 0.5F, partialTick);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VicissitudeLightOrbEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
