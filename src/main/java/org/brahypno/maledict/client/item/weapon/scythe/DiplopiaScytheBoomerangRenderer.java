package org.brahypno.maledict.client.item.weapon.scythe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sammy.malum.client.RenderUtils;
import com.sammy.malum.client.renderer.entity.scythe.ScytheBoomerangEntityRenderer;
import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.registry.client.MalumRenderTypeTokens;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.enchantment.DiplopiaThrows;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypes;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;
import team.lodestar.lodestone.systems.rendering.rendeertype.LodestoneRenderTypeBuilder;
import team.lodestar.lodestone.systems.rendering.rendeertype.ShaderUniformHandler;

import java.awt.*;

/**
 * Malum 回旋镰刀渲染器，只多一件事：复视的幻影画成半透明灰，免得被当成真镰刀。
 *
 * <p><b>为什么没有自己的实体类型。</b>1.8.2 的 {@code ScytheBoomerangEntity} 两个构造器都把类型
 * 写死成 {@code MalumEntities.SCYTHE_BOOMERANG}（javap 实读），另注册一个 Maledict 的类型只会得到
 * 「字段里仍是 Malum 类型」的怪物体，还得覆写 {@code getType()} 才能让渲染表命中。1.20.1 的做法更省：
 * 幻影标记写在被同步的物品堆叠上（{@link DiplopiaThrows#isEcho}），于是 Malum 的类型 + 这条渲染器
 * 就够，服务端与客户端都不需要额外的实体类型或同步字段。这条注册会顶替 Malum 原来给
 * {@code scythe_boomerang} 的渲染器，未标记的镰刀原样转调 {@code super.render}。
 *
 * <p><b>1.8.2 渲染 API 的取舍</b>（都用 javap 核对过）：
 * <ul>
 *   <li>原版的「强化」在弹体上是 {@code setNarrow/isNarrow}，不是 1.20.1 的 {@code isEnhanced}。</li>
 *   <li>拖尾换成 {@code LodestoneRenderTypes.TRANSPARENT_TWO_SIDED_TEXTURE_TRIANGLE}（{@code RenderTypeProvider}）
 *       + {@code .apply(token)} + {@code .withUniformHandler(ShaderUniformHandler.LUMITRANSPARENT)}，
 *       1.20.1 的 {@code LodestoneRenderTypeRegistry.applyAndCache} 在 1.8.2 没有对应物。</li>
 *   <li>{@code RenderUtils.renderEntityTrail} 仍然保留 {@code (scalar, alpha, partialTicks)} 这个九参重载，
 *       所以 1.20.1 的拖尾透明度原样保留（八参重载会把 alpha 固定成 scalar，那是上游自己的用法）。</li>
 *   <li>物品根 NBT 没了，去光效不再删 {@code "Enchantments"} 标签；1.21 读
 *       {@code enchantment_glint_override} 组件，置 false 即可让 {@code ItemStack#hasFoil()} 为假。</li>
 * </ul>
 */
public final class DiplopiaScytheBoomerangRenderer extends ScytheBoomerangEntityRenderer {
    private static final float ECHO_GRAY = 0.55f;
    private static final float ECHO_ALPHA = 0.45f;
    private static final float ECHO_TRAIL_ALPHA = 0.55f;
    private static final Color ECHO_TRAIL_PRIMARY = new Color(0.62f, 0.62f, 0.62f);
    private static final Color ECHO_TRAIL_SECONDARY = new Color(0.36f, 0.36f, 0.36f);

    public DiplopiaScytheBoomerangRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            ScytheBoomerangEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (!DiplopiaThrows.isEcho(entity)){
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        renderEcho(entity, partialTick, poseStack, buffer, packedLight);
    }

    private void renderEcho(
            ScytheBoomerangEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        poseStack.pushPose();
        ItemStack stack = renderStack(entity.getItem());
        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, 1);
        if (entity.isNarrow()){
            Vec3 direction = entity.getDeltaMovement().normalize();
            float yRot = (float) Mth.atan2(direction.x, direction.z);
            poseStack.mulPose(Axis.YP.rotation(yRot + 1.57f));
        }else {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        }
        poseStack.mulPose(Axis.ZP.rotation((entity.age + partialTick) * 0.9f));
        poseStack.scale(2.0f, 2.0f, 1.5f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(ECHO_GRAY, ECHO_GRAY, ECHO_GRAY, ECHO_ALPHA);
        try {
            itemRenderer.render(
                    stack,
                    stack.getItem() instanceof MalumScytheItem
                    ? ItemDisplayContext.NONE
                    : ItemDisplayContext.FIXED,
                    false,
                    poseStack,
                    translucent(buffer),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    model);
        }
        finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            poseStack.popPose();
        }

        LodestoneRenderTypeBuilder trailType = LodestoneRenderTypes.TRANSPARENT_TWO_SIDED_TEXTURE_TRIANGLE
                .apply(MalumRenderTypeTokens.CONCENTRATED_TRAIL)
                .withUniformHandler(ShaderUniformHandler.LUMITRANSPARENT);
        VFXBuilders.WorldVFXBuilder builder = VFXBuilders.createWorld().setRenderType(trailType);
        float scalar = Math.min(entity.age / 20.0f, 1.0f);
        RenderUtils.renderEntityTrail(
                poseStack, builder, entity.theFormer, entity,
                ECHO_TRAIL_PRIMARY, ECHO_TRAIL_SECONDARY, scalar, ECHO_TRAIL_ALPHA, partialTick);
        RenderUtils.renderEntityTrail(
                poseStack, builder, entity.theLatter, entity,
                ECHO_TRAIL_PRIMARY, ECHO_TRAIL_SECONDARY, scalar, ECHO_TRAIL_ALPHA, partialTick);
    }

    /**
     * 幻影本身不该发光：只改渲染用的副本，靠 1.21 的 {@code enchantment_glint_override} 组件关掉
     * {@code hasFoil()}，而不是去删附魔数据。
     */
    private static ItemStack renderStack(ItemStack stack) {
        if (!stack.hasFoil()){
            return stack;
        }
        ItemStack copy = stack.copy();
        copy.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        return copy;
    }

    /**
     * 即使模型要的是 cutout/solid 类型，也按半透明画物品四边形。
     */
    private static MultiBufferSource translucent(MultiBufferSource delegate) {
        RenderType type = RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
        return requested -> delegate.getBuffer(type);
    }
}
