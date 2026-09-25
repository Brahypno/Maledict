package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sammy.malum.client.RenderUtils;
import com.sammy.malum.client.SpiritBasedWorldVFXBuilder;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.client.MalumRenderTypeTokens;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.brahypno.maledict.common.entity.VicissitudeScytheProjectileEntity;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;

/**
 * 掷出的镰刀本体：自带旋转与一条渐隐尾迹；镰刀从不藏进 Boss 模型里，飞行过程始终看得见。
 */
public final class VicissitudeScytheRenderer
        extends EntityRenderer<VicissitudeScytheProjectileEntity> {
    private static final LodestoneRenderType TRAIL_TYPE = LodestoneRenderTypeRegistry
            .ADDITIVE_TEXTURE_TRIANGLE.applyAndCache(MalumRenderTypeTokens.CONCENTRATED_TRAIL);

    public VicissitudeScytheRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
        shadowStrength = 0.0F;
    }

    @Override
    public void render(VicissitudeScytheProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        float spin = entity.tickCount + partialTick;
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw + spin * 28.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - Math.min(45.0F, spin * 3.0F)));
        poseStack.scale(1.6F, 1.6F, 1.6F);
        Minecraft.getInstance().getItemRenderer().renderStatic(entity.getItem(),
                ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack,
                buffers, entity.level(), entity.getId());
        poseStack.popPose();

        MalumSpiritType spirit = SpiritTypeRegistry.UMBRAL_SPIRIT;
        VFXBuilders.WorldVFXBuilder trail = SpiritBasedWorldVFXBuilder.create(spirit)
                .setRenderType(TRAIL_TYPE);
        RenderUtils.renderEntityTrail(poseStack, trail, entity.trail, entity,
                spirit.getPrimaryColor(), spirit.getSecondaryColor(),
                entity.isReturning() ? 0.5F : 0.9F, partialTick);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VicissitudeScytheProjectileEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
