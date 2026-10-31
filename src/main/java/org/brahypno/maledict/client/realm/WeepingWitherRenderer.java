package org.brahypno.maledict.client.realm;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.brahypno.maledict.common.realm.wither.WeepingWitherEntity;

/** Vanilla-derived model, weathered texture and restrained local light. */
public final class WeepingWitherRenderer extends WitherBossRenderer {
    // Saturated vertex tint survives additive blending over the light, worn bone surfaces.
    public static final float ARMOR_RED = 0.65F;
    public static final float ARMOR_GREEN = 0.18F;
    public static final float ARMOR_BLUE = 0.95F;
    private static final ResourceLocation BODY = texture("weeping_wither");
    private static final ResourceLocation INVULNERABLE = texture("weeping_wither_invulnerable");
    private static final ResourceLocation REFORMING = texture("weeping_wither_reforming");
    private static final ResourceLocation EYES = texture("weeping_wither_eyes");
    private static final ResourceLocation FISSURE = texture("weeping_wither_fissure");
    private static final ResourceLocation ARMOR = texture("weeping_wither_armor");

    public WeepingWitherRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new WeepingWitherModel(context.bakeLayer(ModelLayers.WITHER));
        layers.clear();
        addLayer(new Armor(this, context));
        addLayer(new SeepLight(this));
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("maledict", "textures/entity/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(WitherBoss entity) {
        if (entity instanceof WeepingWitherEntity wither && wither.isReforming()) return REFORMING;
        int ticks = entity.getInvulnerableTicks();
        return ticks > 0 && (ticks > 80 || ticks / 5 % 2 != 1) ? INVULNERABLE : BODY;
    }

    @Override
    protected void scale(WitherBoss entity, PoseStack pose, float partialTick) {
        super.scale(entity, pose, partialTick);
        pose.scale(WeepingWitherEntity.SCALE, WeepingWitherEntity.SCALE, WeepingWitherEntity.SCALE);
    }

    private static final class Armor extends RenderLayer<WitherBoss, WitherBossModel<WitherBoss>> {
        private final WitherBossModel<WitherBoss> shell;

        Armor(WeepingWitherRenderer renderer, EntityRendererProvider.Context context) {
            super(renderer);
            shell = new WeepingWitherModel(context.bakeLayer(ModelLayers.WITHER_ARMOR));
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, WitherBoss entity,
                           float swing, float amount, float partialTick, float age, float yaw, float pitch) {
            if (!entity.isPowered() || entity.isInvisible()) return;
            shell.prepareMobModel(entity, swing, amount, partialTick);
            getParentModel().copyPropertiesTo(shell);
            shell.setupAnim(entity, swing, amount, age, yaw, pitch);
            float ticks = entity.tickCount + partialTick;
            shell.renderToBuffer(pose, buffers.getBuffer(RenderType.energySwirl(ARMOR,
                            Mth.cos(ticks * 0.02F) * 3.0F % 1.0F, ticks * 0.01F % 1.0F)),
                    0xF000F0, OverlayTexture.NO_OVERLAY, ARMOR_RED, ARMOR_GREEN, ARMOR_BLUE, 1.0F);
        }
    }

    private static final class SeepLight extends RenderLayer<WitherBoss, WitherBossModel<WitherBoss>> {
        SeepLight(WeepingWitherRenderer renderer) { super(renderer); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, WitherBoss entity,
                           float swing, float amount, float partialTick, float age, float yaw, float pitch) {
            if (entity.isInvisible()) return;
            boolean reforming = entity instanceof WeepingWitherEntity wither && wither.isReforming();
            if (!reforming) draw(pose, buffers, EYES, 0.55F);
            draw(pose, buffers, FISSURE, reforming ? 0.95F + Mth.sin(age * 0.25F) * 0.25F : 0.50F);
        }

        private void draw(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, float intensity) {
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(texture)),
                    0xF000F0, OverlayTexture.NO_OVERLAY, intensity, intensity, intensity, 1.0F);
        }
    }
}
