package org.brahypno.maledict.client.curio.priestess;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.common.curio.priestess.SoulEchoEntity;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;
import team.lodestar.lodestone.systems.rendering.rendeertype.RenderTypeToken;

/** One batched billboard per echo, using Malum's spirit-flame frames rather than copying a mob's render layers. */
public final class SoulEchoRenderer extends EntityRenderer<SoulEchoEntity> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[20];
    private static final LodestoneRenderType[] TYPES = new LodestoneRenderType[20];
    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath("malum",
                    "textures/particle/spirit_flame/entrance/spirit_flame_entrance_" + String.format("%02d", i) + ".png");
            TYPES[i] = LodestoneRenderTypeRegistry.ADDITIVE_TEXTURE.applyAndCache(RenderTypeToken.createCachedToken(TEXTURES[i]));
        }
    }
    private final VFXBuilders.WorldVFXBuilder builder = VFXBuilders.createWorld().setFormat(TYPES[0].format());

    public SoulEchoRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(SoulEchoEntity echo, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        int remaining = echo.remainingTicks();
        int frame = Math.min(19, (SoulEchoEntity.LIFETIME_TICKS - Math.min(remaining, SoulEchoEntity.LIFETIME_TICKS)) / 3);
        float alpha = Math.min(1, remaining / 10.0F) * 0.7F;
        pose.pushPose();
        pose.translate(0, echo.getBbHeight() * 0.5F, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(echo.getBbWidth() * 0.75F, echo.getBbHeight() * 0.55F, 1);
        builder.setVertexConsumer(buffers.getBuffer(TYPES[frame])).setUV(0, 0, 1, 1)
                .setLight(0xF000F0).setColor(0.55F, 0.95F, 0.9F, alpha).renderQuad(pose, 1);
        pose.popPose();
    }

    @Override public ResourceLocation getTextureLocation(SoulEchoEntity echo) { return TEXTURES[0]; }
}
