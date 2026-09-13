package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.model.FirstVicissitudeBossModel;
import org.brahypno.maledict.client.vfx.FirstVicissitudeEffects;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.rig.VicissitudeRigData;

/**
 * Renderer for the First Vicissitude boss: base texture, an additive emissive pass, the real
 * weapon in the right hand and the effect entry point. Frustum culling is widened to include
 * the full wing span so the wings never pop out at the edge of the screen.
 */
@SuppressWarnings({"removal"})
public final class FirstVicissitudeBossRenderer
        extends MobRenderer<FirstVicissitudeBossEntity, FirstVicissitudeBossModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(Maledict.MODID, "first_vicissitude"), "main");
    public static final ResourceLocation TEXTURE = new ResourceLocation(
            Maledict.MODID, "textures/entity/first_vicissitude.png");
    public static final ResourceLocation EMISSIVE = new ResourceLocation(
            Maledict.MODID, "textures/entity/first_vicissitude_emissive.png");
    /**
     * Half of the maximum wing span plus the ring, so large wings stay visible.
     */
    private static final double CULL_INFLATE = 4.5D;

    public FirstVicissitudeBossRenderer(EntityRendererProvider.Context context) {
        super(context, new FirstVicissitudeBossModel(context.bakeLayer(LAYER)), 1.6F);
        addLayer(new EmissiveLayer(this));
        addLayer(new WeaponLayer(this));
        addLayer(new EffectsLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(FirstVicissitudeBossEntity entity) {
        return TEXTURE;
    }

    /**
     * Vanilla applies two things here: the body yaw ({@code Ry(180 - rotationYaw)}) and, once
     * {@code deathTime} is positive, a roll that lays the corpse on its side.
     *
     * <p>Only the yaw is kept. Dropping the whole method earlier removed the yaw as well, which
     * froze the model's heading while the entity itself still turned; the authored eighty tick
     * collapse is meant to stay upright, so the death roll is the only part skipped.
     */
    @Override
    protected void setupRotations(FirstVicissitudeBossEntity entity, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - rotationYaw));
    }

    @Override
    protected RenderType getRenderType(
            FirstVicissitudeBossEntity entity, boolean visible,
            boolean translucent, boolean outline) {
        return RenderType.entityCutoutNoCull(TEXTURE);
    }

    @Override
    public boolean shouldRender(
            FirstVicissitudeBossEntity entity,
            net.minecraft.client.renderer.culling.Frustum frustum,
            double cameraX, double cameraY, double cameraZ) {
        return frustum.isVisible(entity.getBoundingBox().inflate(CULL_INFLATE));
    }

    /**
     * Additive pass for the cold core, the cracks and the few glowing ring plates.
     */
    private static final class EmissiveLayer
            extends RenderLayer<FirstVicissitudeBossEntity, FirstVicissitudeBossModel> {
        private EmissiveLayer(FirstVicissitudeBossRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(
                PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                FirstVicissitudeBossEntity entity, float limbSwing, float limbSwingAmount,
                float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible()){
                return;
            }
            float fade = entity.getCoreGlow();
            if (fade <= 0.0F){
                return;
            }
            getParentModel().renderToBuffer(poseStack,
                                            buffer.getBuffer(RenderType.eyes(EMISSIVE)), packedLight,
                                            OverlayTexture.NO_OVERLAY, fade, fade, fade, 1.0F);
        }
    }

    /**
     * The difficulty weapon, drawn in the bind pose of the right hand chain.
     *
     * <p>The stack comes from the entity's synced tier, not from the main hand: the encounter has
     * to stay visibly armed even when a disarm effect, an inventory swap or another mod empties
     * or replaces the real item.
     */
    private static final class WeaponLayer
            extends RenderLayer<FirstVicissitudeBossEntity, FirstVicissitudeBossModel> {
        private WeaponLayer(FirstVicissitudeBossRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(
                PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                FirstVicissitudeBossEntity entity, float limbSwing, float limbSwingAmount,
                float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!entity.shouldRenderHeldWeapon()){
                return;
            }
            ItemStack stack = entity.getDisplayWeapon();
            if (stack.isEmpty()){
                return;
            }
            poseStack.pushPose();
            // Walk the whole shoulder -> upper arm -> forearm -> hand chain plus the grip anchor;
            // a single translateAndRotate would leave the weapon floating at the model origin.
            getParentModel().poseStackTo(VicissitudeRigData.Joint.SCYTHE_HAND_ANCHOR, poseStack);
            // Exactly the basis vanilla's ItemInHandLayer uses for a held item on a mob arm:
            // Rx(-90) then Ry(180), which leaves the blade along the arm, tilted ~35 degrees
            // forward. The anchor already sits at the hand, so vanilla's shoulder-to-hand
            // translation is not repeated here.
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            float scale = weaponScale(stack);
            poseStack.scale(scale, scale, scale);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack,
                                                                   ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight,
                                                                   OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            poseStack.popPose();
        }

        private static float weaponScale(ItemStack stack) {
            if (stack.is(MaledictItems.INCURSUS_BLADE.get())
                || stack.is(ItemRegistry.SOUL_STAINED_STEEL_SCYTHE.get())){
                return 1.6F;
            }
            return 1.15F;
        }
    }

    private static final class EffectsLayer
            extends RenderLayer<FirstVicissitudeBossEntity, FirstVicissitudeBossModel> {
        private EffectsLayer(FirstVicissitudeBossRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(
                PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                FirstVicissitudeBossEntity entity, float limbSwing, float limbSwingAmount,
                float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            FirstVicissitudeEffects.render(entity, partialTick, poseStack, buffer, packedLight);
        }
    }
}
