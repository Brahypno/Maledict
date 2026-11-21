package org.brahypno.maledict.client.item.weapon.baron;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class BaronStaffClientExtensions implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose HOLD_STAFF = HumanoidModel.ArmPose.create(
            "MALEDICT_HOLD_BARON_STAFF", false, (model, entity, arm) -> {
                ModelPart holdingArm = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                // The third-person item transform compensates for this resting pitch.
                holdingArm.xRot = holdingArm.xRot * 0.15F - 75.0F * Mth.DEG_TO_RAD;
            });

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HOLD_STAFF;
    }
}
