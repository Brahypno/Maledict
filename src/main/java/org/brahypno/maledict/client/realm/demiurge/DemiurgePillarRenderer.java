package org.brahypno.maledict.client.realm.demiurge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.brahypno.maledict.common.realm.demiurge.DemiurgePillarEntity;

public final class DemiurgePillarRenderer extends EntityRenderer<DemiurgePillarEntity> {
    private final DemiurgePillarModel model;

    public DemiurgePillarRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new DemiurgePillarModel();
        shadowRadius = .8F;
    }

    @Override
    public void render(DemiurgePillarEntity entity, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int light) {
        DemiurgePillarEntity.Phase phase = entity.phase();
        if (entity.isInvisible() || phase == DemiurgePillarEntity.Phase.EXPLODING
                || phase == DemiurgePillarEntity.Phase.DESTROYED) {
            super.render(entity, yaw, partialTick, stack, buffers, light);
            return;
        }
        float rise = Mth.clamp(entity.rise(), .03F, 1.0F);
        int height = Math.max(1, entity.pillarHeight());
        stack.pushPose();
        // The full shaft is always drawn, but it starts buried in the support
        // block and is translated up as it rises. Scaling the model vertically
        // made the shaft look compressed; translation gives a real emergence.
        stack.translate(0, -height * (1.0F - rise), 0);
        model.render(stack, buffers.getBuffer(RenderType.entityCutout(getTextureLocation(entity))),
                light, height, 255, false);
        if (phase == DemiurgePillarEntity.Phase.ARMED || phase == DemiurgePillarEntity.Phase.EMPOWERED
                || phase == DemiurgePillarEntity.Phase.LAUNCHED) {
            boolean bright = phase != DemiurgePillarEntity.Phase.ARMED;
            float pulse = .75F + .25F * Mth.sin((entity.tickCount + partialTick) * (bright ? 1.9F : .55F));
            model.render(stack, buffers.getBuffer(RenderType.eyes(DemiurgeStatueRenderer.GLOW)),
                    LightTexture.FULL_BRIGHT, height, (int) ((bright ? 255 : 145) * pulse), true);
        }
        stack.popPose();
        super.render(entity, yaw, partialTick, stack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DemiurgePillarEntity entity) {
        return DemiurgeStatueRenderer.TEXTURE;
    }
}
