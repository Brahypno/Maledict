package org.brahypno.maledict.client.realm.demiurge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeStatueEntity;

/** One opaque, backface-culled pass; particle and awakening effects remain undecided. */
public final class DemiurgeStatueRenderer extends EntityRenderer<DemiurgeStatueEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/demiurge_statue.png");
    private final DemiurgeStatueModel model;

    public DemiurgeStatueRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new DemiurgeStatueModel();
        shadowRadius = 2.3F;
    }

    @Override
    public void render(DemiurgeStatueEntity entity, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int light) {
        if (!entity.isInvisible()) {
            stack.pushPose();
            stack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
            // entityCutout retains default CULL; no duplicate reverse faces or full glow pass.
            model.render(stack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light);
            stack.popPose();
        }
        super.render(entity, yaw, partialTick, stack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DemiurgeStatueEntity entity) {
        return TEXTURE;
    }
}
