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
import net.minecraft.util.Mth;
import org.brahypno.maledict.common.entity.VicissitudeSpiritBoltEntity;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;

/**
 * Straight phase-one/phase-two bolts: a single cold core with a short straight trail, visually
 * distinct from the double glimmer of the tracking orb.
 */
public final class VicissitudeSpiritBoltRenderer
        extends EntityRenderer<VicissitudeSpiritBoltEntity> {
    private static final LodestoneRenderType TRAIL_TYPE = LodestoneRenderTypeRegistry
            .ADDITIVE_TEXTURE_TRIANGLE.applyAndCache(MalumRenderTypeTokens.CONCENTRATED_TRAIL);

    public VicissitudeSpiritBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
        shadowStrength = 0.0F;
    }

    @Override
    public void render(VicissitudeSpiritBoltEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        // Phase one pressure bolts read as a big slow double core; the phase two damage volley is
        // a small fast flicker, so the two can never be confused in flight.
        boolean press = entity.pressesHealth();
        float phase = entity.tickCount + partialTick;
        MalumSpiritType spirit = press
                ? SpiritTypeRegistry.UMBRAL_SPIRIT : SpiritTypeRegistry.ELDRITCH_SPIRIT;
        float pulse = press
                ? 0.95F + 0.22F * Mth.sin(phase * 0.22F)
                : 0.5F + 0.16F * Mth.sin(phase * 0.95F);
        VFXBuilders.WorldVFXBuilder trail = SpiritBasedWorldVFXBuilder.create(spirit)
                .setRenderType(TRAIL_TYPE);
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        FloatingItemEntityRenderer.renderSpiritGlimmer(poseStack, spirit, pulse, 0.5F, partialTick);
        if (press) {
            FloatingItemEntityRenderer.renderSpiritGlimmer(poseStack,
                    SpiritTypeRegistry.ELDRITCH_SPIRIT, pulse * 0.7F, 3.0F, partialTick);
        }
        poseStack.popPose();
        RenderUtils.renderEntityTrail(poseStack, trail, entity.trail, entity,
                spirit.getPrimaryColor(), spirit.getSecondaryColor(),
                press ? 0.75F : 0.45F, partialTick);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VicissitudeSpiritBoltEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
