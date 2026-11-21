package org.brahypno.maledict.client.item.equipment.lightward;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Copies the renderer's resolved pose so crouching, swimming and held-item poses remain intact.
 *
 * <p>1.21 里 {@code Item#initializeClient} 没有了，客户端物品扩展要在 MOD 总线的
 * {@code RegisterClientExtensionsEvent} 上登记一个实例（见 {@code MaledictClientSetup}），
 * 模型在 {@code AddLayers} 阶段烘焙进静态字段。
 */
public final class LightwardArmorClientExtensions implements IClientItemExtensions {
    public static final LightwardArmorClientExtensions INSTANCE = new LightwardArmorClientExtensions();

    private static LightwardArmorModel soulStained;
    private static LightwardArmorModel helmet;
    private static LightwardArmorModel chestplate;
    private static LightwardArmorModel boots;

    public static void bake(EntityModelSet models) {
        helmet = new LightwardArmorModel(models.bakeLayer(LightwardArmorModel.HELMET_LAYER));
        soulStained = new LightwardArmorModel(models.bakeLayer(LightwardArmorModel.LOWER_ARMOR_LAYER));
        boots = LightwardArmorModel.createBootsModel(models.bakeLayer(LightwardArmorModel.BOOTS_LAYER),
                Minecraft.getInstance().getResourceManager());
        chestplate = new LightwardArmorModel(models.bakeLayer(LightwardArmorModel.CHESTPLATE_LAYER),
                models.bakeLayer(LightwardArmorModel.CHESTPLATE_DETAILS_LAYER));
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(
            LivingEntity entity, ItemStack stack,
            EquipmentSlot slot, HumanoidModel<?> original) {
        LightwardArmorModel model = switch (slot) {
            case HEAD -> helmet;
            case LEGS -> soulStained;
            case FEET -> boots;
            case CHEST -> chestplate;
            default -> null;
        };
        if (model == null) {
            return original;
        }
        model.slot = slot;
        model.copyFromDefault(original);
        return model;
    }

    private LightwardArmorClientExtensions() {
    }
}
