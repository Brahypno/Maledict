package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.model.RavenModel;
import org.brahypno.maledict.common.entity.RavenEntity;

public final class RavenRenderer extends MobRenderer<RavenEntity, RavenModel> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/raven.png");

    public RavenRenderer(EntityRendererProvider.Context context) {
        super(context, new RavenModel(context.bakeLayer(RavenModel.LAYER)), 0.55F);
    }

    @Override
    public ResourceLocation getTextureLocation(RavenEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(RavenEntity entity, PoseStack pose, float partialTick) {
        if (entity.isBaby()) {
            pose.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
