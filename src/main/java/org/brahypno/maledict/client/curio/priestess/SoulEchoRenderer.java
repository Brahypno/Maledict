package org.brahypno.maledict.client.curio.priestess;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.maledict.common.curio.priestess.SoulEchoEntity;

import java.util.IdentityHashMap;
import java.util.Map;

/** Reuses the actual body's model, textures, scale, and layers at the stationary echo anchor. */
public final class SoulEchoRenderer extends EntityRenderer<SoulEchoEntity> {
    private final Map<RenderType, RenderType> translucentTypes = new IdentityHashMap<>();

    public SoulEchoRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(SoulEchoEntity echo, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        LivingEntity body = echo.visualBody();
        if (body == null) return;

        float alpha = Math.min(1, echo.remainingTicks() / 10.0F) * 0.65F;
        MultiBufferSource spectralBuffers = type -> new SoulEchoVertexConsumer(
                buffers.getBuffer(translucentType(type)), alpha);
        EntityRenderer<? super LivingEntity> renderer = entityRenderDispatcher.getRenderer(body);
        pose.pushPose();
        // Living renderers read the body's rotation themselves; compensate so the anchor keeps its facing.
        float bodyYaw = Mth.rotLerp(partialTick, body.yBodyRotO, body.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(bodyYaw - echo.getYRot()));
        renderer.render(body, yaw, partialTick, pose, spectralBuffers, 0xF000F0);
        pose.popPose();
    }

    private RenderType translucentType(RenderType original) {
        if (!original.format().equals(DefaultVertexFormat.NEW_ENTITY)) return original;
        return translucentTypes.computeIfAbsent(original, type -> new RenderType(
                "maledict_soul_echo", type.format(), type.mode(), type.bufferSize(), false, true,
                () -> {
                    // Preserve each layer's own texture and texture transform, then replace its surface shader.
                    type.setupRenderState();
                    RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentEmissiveShader);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableCull();
                    RenderSystem.depthMask(false);
                },
                () -> {
                    RenderSystem.depthMask(true);
                    RenderSystem.enableCull();
                    RenderSystem.disableBlend();
                    type.clearRenderState();
                }) {});
    }

    @Override
    public ResourceLocation getTextureLocation(SoulEchoEntity echo) {
        LivingEntity body = echo.visualBody();
        return body == null ? TextureAtlas.LOCATION_BLOCKS
                : entityRenderDispatcher.getRenderer(body).getTextureLocation(body);
    }
}
