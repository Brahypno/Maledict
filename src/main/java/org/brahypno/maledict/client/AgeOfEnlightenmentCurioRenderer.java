package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.model.AgeOfEnlightenmentModel;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * 启蒙之年的饰品渲染器：回调发生在玩家模型的坐标空间里，故不必自己摆位置，把头部姿态复制给面具模型即可。
 */
public final class AgeOfEnlightenmentCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/curio/age_of_enlightenment.png");

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
                                                                         PoseStack poseStack,
                                                                         RenderLayerParent<T, M> renderLayerParent,
                                                                         MultiBufferSource bufferSource, int light,
                                                                         float limbSwing, float limbSwingAmount,
                                                                         float partialTicks, float ageInTicks,
                                                                         float netHeadYaw, float headPitch) {
        AgeOfEnlightenmentModel model = MaledictEntityRenderers.ageOfEnlightenmentModel();
        if (model == null) {
            // 图层还没烘焙（极端加载顺序下才会发生），跳过这一帧好过抛异常。
            return;
        }
        ICurioRenderer.followHeadRotations(slotContext.entity(), model.mask());
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
