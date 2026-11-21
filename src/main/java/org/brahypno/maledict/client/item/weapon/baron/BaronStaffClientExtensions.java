package org.brahypno.maledict.client.item.weapon.baron;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class BaronStaffClientExtensions implements IClientItemExtensions {
    public static final BaronStaffClientExtensions INSTANCE = new BaronStaffClientExtensions();

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return BaronStaffArmPose.PROXY.getValue();
    }

    private BaronStaffClientExtensions() {
    }
}
