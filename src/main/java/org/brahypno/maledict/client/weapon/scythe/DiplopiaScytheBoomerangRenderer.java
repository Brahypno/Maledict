package org.brahypno.maledict.client.weapon.scythe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sammy.malum.client.RenderUtils;
import com.sammy.malum.client.renderer.entity.scythe.ScytheBoomerangEntityRenderer;
import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.registry.client.MalumRenderTypeTokens;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.enchantment.DiplopiaThrows;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;
import team.lodestar.lodestone.systems.rendering.rendeertype.ShaderUniformHandler;

import java.awt.Color;

/**
 * Malum's boomerang renderer with one addition: Diplopia phantoms are drawn as translucent gray
 * copies so they cannot be mistaken for the real scythe. The marker lives on the synced item stack,
 * so the base entity type is enough and no custom entity type or synced data is required. Every
 * unmarked scythe delegates to Malum's renderer unchanged.
 */
public final class DiplopiaScytheBoomerangRenderer extends ScytheBoomerangEntityRenderer {
    private static final float ECHO_GRAY = 0.55f;
    private static final float ECHO_ALPHA = 0.45f;
    private static final float ECHO_TRAIL_ALPHA = 0.55f;
    private static final Color ECHO_TRAIL_PRIMARY = new Color(0.62f, 0.62f, 0.62f);
    private static final Color ECHO_TRAIL_SECONDARY = new Color(0.36f, 0.36f, 0.36f);

    public DiplopiaScytheBoomerangRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            ScytheBoomerangEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferIn,
            int packedLightIn) {
        if (!DiplopiaThrows.isEcho(entity)){
            super.render(entity, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
            return;
        }
        renderEcho(entity, partialTicks, poseStack, bufferIn, packedLightIn);
    }

    private void renderEcho(
            ScytheBoomerangEntity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferIn,
            int packedLightIn) {
        poseStack.pushPose();
        ItemStack stack = renderStack(entity.getItem());
        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, 1);
        if (entity.isEnhanced()){
            Vec3 direction = entity.getDeltaMovement().normalize();
            float yRot = (float) Mth.atan2(direction.x, direction.z);
            poseStack.mulPose(Axis.YP.rotation(yRot + 1.57f));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        }
        poseStack.mulPose(Axis.ZP.rotation((entity.age + partialTicks) * 0.9f));
        poseStack.scale(2.0f, 2.0f, 1.5f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(ECHO_GRAY, ECHO_GRAY, ECHO_GRAY, ECHO_ALPHA);
        try {
            itemRenderer.render(
                    stack,
                    stack.getItem() instanceof MalumScytheItem
                    ? ItemDisplayContext.NONE
                    : ItemDisplayContext.FIXED,
                    false,
                    poseStack,
                    translucent(bufferIn),
                    packedLightIn,
                    OverlayTexture.NO_OVERLAY,
                    model);
        }
        finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            poseStack.popPose();
        }

        LodestoneRenderType trailType = LodestoneRenderTypeRegistry.TRANSPARENT_TWO_SIDED_TEXTURE_TRIANGLE
                .applyAndCache(MalumRenderTypeTokens.CONCENTRATED_TRAIL, ShaderUniformHandler.LUMITRANSPARENT);
        VFXBuilders.WorldVFXBuilder builder = VFXBuilders.createWorld().setRenderType(trailType);
        float scalar = Math.min(entity.age / 20.0f, 1.0f);
        RenderUtils.renderEntityTrail(
                poseStack, builder, entity.theFormer, entity,
                ECHO_TRAIL_PRIMARY, ECHO_TRAIL_SECONDARY, scalar, ECHO_TRAIL_ALPHA, partialTicks);
        RenderUtils.renderEntityTrail(
                poseStack, builder, entity.theLatter, entity,
                ECHO_TRAIL_PRIMARY, ECHO_TRAIL_SECONDARY, scalar, ECHO_TRAIL_ALPHA, partialTicks);
    }

    /** The phantom itself should not glint; strip the enchantment glint from the render copy only. */
    private static ItemStack renderStack(ItemStack stack) {
        if (!stack.hasFoil()){
            return stack;
        }
        ItemStack copy = stack.copy();
        copy.getOrCreateTag().remove("Enchantments");
        copy.getOrCreateTag().remove("StoredEnchantments");
        return copy;
    }

    /** Renders item quads with blending even when the model asks for a cutout/solid type. */
    private static MultiBufferSource translucent(MultiBufferSource delegate) {
        RenderType type = RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
        return requested -> delegate.getBuffer(type);
    }
}
