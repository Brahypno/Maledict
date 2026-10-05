package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import org.brahypno.maledict.client.model.RavenModel;

/** Native player shoulder NBT, drawn with our folded raven model at a shoulder-only scale. */
public final class RavenShoulderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final RavenModel model;

    public RavenShoulderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                              EntityModelSet models) {
        super(parent);
        model = new RavenModel(models.bakeLayer(RavenModel.LAYER));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float age,
                       float headYaw, float headPitch) {
        renderBird(pose, buffers, light, player, player.getShoulderEntityLeft(), true, headYaw, headPitch);
        renderBird(pose, buffers, light, player, player.getShoulderEntityRight(), false, headYaw, headPitch);
    }

    private void renderBird(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            CompoundTag bird, boolean left, float headYaw, float headPitch) {
        if (!"maledict:raven".equals(bird.getString("id"))) {
            return;
        }
        pose.pushPose();
        pose.translate(left ? 0.4D : -0.4D, player.isCrouching() ? 0.15D : -0.05D, 0.0D);
        float scale = bird.getInt("Age") < 0 ? 0.3F : 0.4F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0D, -1.5D, 0.0D);
        model.setupShoulderPose(headYaw, headPitch);
        model.renderToBuffer(pose, buffers.getBuffer(model.renderType(RavenRenderer.TEXTURE)),
                light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        pose.popPose();
    }
}
