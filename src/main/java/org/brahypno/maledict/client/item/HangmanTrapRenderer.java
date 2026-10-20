package org.brahypno.maledict.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.common.entity.projectile.HangmanTrapEntity;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypes;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;
import team.lodestar.lodestone.systems.rendering.rendeertype.RenderTypeToken;

/** The staff's directional HEXAGON visual, with two batched quads instead of continuously emitted particles. */
public final class HangmanTrapRenderer extends EntityRenderer<HangmanTrapEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("malum", "textures/particle/hexagon.png");
    private static final LodestoneRenderType RENDER_TYPE = LodestoneRenderTypes.ADDITIVE_TEXTURE
            .apply(RenderTypeToken.createToken(TEXTURE)).getRenderType();
    // setFormat also initializes the vertex supplier required by renderQuad.
    private final VFXBuilders.WorldVFXBuilder builder = VFXBuilders.createWorld().setFormat(RENDER_TYPE.format());

    public HangmanTrapRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(HangmanTrapEntity trap, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(trap.face().getRotation());
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        float angle = (trap.tickCount + partialTick) * 1.5F;
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        builder.setVertexConsumer(buffers.getBuffer(RENDER_TYPE)).setUV(0, 0, 1, 1)
                .setLight(0xF000F0).setColor(MalumSpiritTypes.WICKED_SPIRIT.getPrimaryColor(), 0.6F)
                .renderQuad(pose, 1.0F);
        pose.translate(0, 0, 0.002D);
        pose.mulPose(Axis.ZP.rotationDegrees(-angle * 2.0F));
        builder.setColor(MalumSpiritTypes.WICKED_SPIRIT.getSecondaryColor(), 0.35F)
                .renderQuad(pose, 0.75F);
        pose.popPose();
        super.render(trap, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(HangmanTrapEntity trap) {
        return TEXTURE;
    }
}
