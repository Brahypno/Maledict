package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;

/** Temporary phase-one renderer: the standard slim player model and skin. */
public final class FirstVicissitudeBossRenderer
        extends MobRenderer<FirstVicissitudeBossEntity, PlayerModel<FirstVicissitudeBossEntity>> {
    public FirstVicissitudeBossRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(FirstVicissitudeBossEntity entity) {
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    protected void scale(FirstVicissitudeBossEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
