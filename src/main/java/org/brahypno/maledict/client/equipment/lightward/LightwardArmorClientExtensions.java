package org.brahypno.maledict.client.equipment.lightward;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Copies the renderer's resolved pose so crouching, swimming and held-item poses remain intact. */
public final class LightwardArmorClientExtensions implements IClientItemExtensions {
    private static LightwardArmorModel soulStained;
    private static LightwardArmorModel chestplate;

    public static void bake(EntityModelSet models) {
        soulStained = new LightwardArmorModel(models.bakeLayer(LightwardArmorModel.HELMET_LAYER),
                models.bakeLayer(LightwardArmorModel.TEARS_LAYER).getChild("tears"));
        chestplate = new LightwardArmorModel(models.bakeLayer(LightwardArmorModel.CHESTPLATE_LAYER));
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                 EquipmentSlot slot, HumanoidModel<?> original) {
        LightwardArmorModel model = switch (slot) {
            case HEAD, LEGS, FEET -> soulStained;
            case CHEST -> chestplate;
            default -> null;
        };
        if (model == null) return original;
        model.slot = slot;
        model.copyFromDefault(original);
        return model;
    }
}
