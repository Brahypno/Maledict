package org.brahypno.maledict.common.effect;

import net.minecraft.core.Holder;
import com.sammy.malum.registry.common.MalumAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import team.lodestar.lodestone.registry.common.LodestoneAttributes;

import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;

/**
 * 黑暗时代：佩戴启蒙之年击杀敌人时，附近一名敌人获得的减益，刻意标成 {@link MobEffectCategory#BENEFICIAL} 走 HUD 正面边框。
 * {@code addAttributeModifiers} 结算时会自乘 {@code (amplifier + 1)}，故 {@code -0.2} 每级即 -20%；{@code MULTIPLY_TOTAL} 是比例削减，属性下限 0 会夹住结果。
 */
public final class AgeOfDarknessEffect extends MobEffect {
    /** Malum 邪恶精魂紫 {@code #792CEC}。 */
    public static final int COLOR = 0x792CEC;

    /** 每级削减的比例，{@code 0.2} 即 20%。 */
    public static final double REDUCTION_PER_LEVEL = 0.2D;

    /** ResourceLocation 必须固定：原版添加与移除属性修饰符都是按 ResourceLocation 匹配的，否则摘不干净。 */
    private static final ResourceLocation MAGIC_RESISTANCE_MODIFIER_ID = modifierId("magic_resistance");
    private static final ResourceLocation SOUL_WARD_CAP_MODIFIER_ID = modifierId("soul_ward_cap");
    private static final ResourceLocation SOUL_WARD_INTEGRITY_MODIFIER_ID = modifierId("soul_ward_integrity");

    public AgeOfDarknessEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
        strip(LodestoneAttributes.MAGIC_RESISTANCE, MAGIC_RESISTANCE_MODIFIER_ID);
        strip(MalumAttributes.SOUL_WARD_CAPACITY, SOUL_WARD_CAP_MODIFIER_ID);
        strip(MalumAttributes.SOUL_WARD_INTEGRITY, SOUL_WARD_INTEGRITY_MODIFIER_ID);
    }

    private void strip(Holder<Attribute> attribute, ResourceLocation modifierId) {
        addAttributeModifier(attribute, modifierId, -REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static ResourceLocation modifierId(String attribute) {
        return ResourceLocation.parse("maledict:age_of_darkness/" + attribute);
    }
}
