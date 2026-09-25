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
 * First Vicissitude boss 的渲染器：基础贴图、叠加自发光层、右手真武器与特效入口。
 */
public final class FirstVicissitudeBossRenderer
        extends MobRenderer<FirstVicissitudeBossEntity, FirstVicissitudeBossModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "first_vicissitude"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/first_vicissitude.png");
    public static final ResourceLocation EMISSIVE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/first_vicissitude_emissive.png");
    /** 视锥剔除的放宽量：最大翼展的一半加上环，免得大翅膀在屏幕边缘被剔掉。 */
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
     * 原版这里既加身体朝向（{@code Ry(180 - rotationYaw)}），也在 {@code deathTime} 转正后加一段侧倒；
     * 只保留朝向——手写的倒地动画本身是直立的，侧倒是唯一被跳过的部分。
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

    /** 冷核、裂纹与几块发光环甲板的叠加自发光层。 */
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
     * 难度武器，按右手链的绑定姿势绘制；物品取自实体同步的档位而非主手，免得被解除武装或换掉物品后 Boss 空着手。
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
            // 走完整条 肩→上臂→前臂→手 的链加握把锚点；只做一次 translateAndRotate 会让武器飘在模型原点。
            getParentModel().poseStackTo(VicissitudeRigData.Joint.SCYTHE_HAND_ANCHOR, poseStack);
            // 与原生 ItemInHandLayer 持械同一套基：Rx(-90) 再 Ry(180)；锚点已在手上，故不再补肩到手的平移。
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
