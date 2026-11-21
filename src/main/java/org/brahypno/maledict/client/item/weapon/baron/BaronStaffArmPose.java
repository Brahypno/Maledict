package org.brahypno.maledict.client.item.weapon.baron;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * 伯爵礼杖的持杖姿势。NeoForge 用枚举扩展造新常量：{@code META-INF/enumextensions.json}
 * 声明构造参数取自 {@link #PROXY}，第 0 个参数是 {@code twoHanded}，第 1 个是这个变换器。
 *
 * <p>第三人称的物品变换已经补偿了这个抬起的俯角，所以这里只把手臂压下去。
 */
public final class BaronStaffArmPose {
    public static final EnumProxy<HumanoidModel.ArmPose> PROXY = new EnumProxy<>(
            HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
                ModelPart holdingArm = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                holdingArm.xRot = holdingArm.xRot * 0.15F - 75.0F * Mth.DEG_TO_RAD;
            });

    private BaronStaffArmPose() {
    }
}
