package org.brahypno.maledict.common.effect;

import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 黑暗时代：佩戴启蒙之年击杀敌人时，附近一名敌人获得的减益，刻意标成 {@link MobEffectCategory#BENEFICIAL} 走 HUD 正面边框。
 * {@code addAttributeModifiers} 结算时会自乘 {@code (amplifier + 1)}，故 {@code -0.2} 每级即 -20%；{@code MULTIPLY_TOTAL} 是比例削减，属性下限 0 会夹住结果。
 */
public final class AgeOfDarknessEffect extends MobEffect {
    /** Malum 邪恶精魂紫 {@code #792CEC}。 */
    public static final int COLOR = 0x792CEC;

    /** 每级削减的比例，{@code 0.2} 即 20%。 */
    public static final double REDUCTION_PER_LEVEL = 0.2D;

    /** UUID 必须固定：原版添加与移除属性修饰符都是按 UUID 匹配的，否则摘不干净。 */
    private static final UUID MAGIC_RESISTANCE_MODIFIER_ID = modifierId("magic_resistance");
    private static final UUID SOUL_WARD_CAP_MODIFIER_ID = modifierId("soul_ward_cap");
    private static final UUID SOUL_WARD_INTEGRITY_MODIFIER_ID = modifierId("soul_ward_integrity");

    public AgeOfDarknessEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
        strip(LodestoneAttributeRegistry.MAGIC_RESISTANCE.get(), MAGIC_RESISTANCE_MODIFIER_ID);
        strip(AttributeRegistry.SOUL_WARD_CAP.get(), SOUL_WARD_CAP_MODIFIER_ID);
        strip(AttributeRegistry.SOUL_WARD_INTEGRITY.get(), SOUL_WARD_INTEGRITY_MODIFIER_ID);
    }

    private void strip(Attribute attribute, UUID modifierId) {
        addAttributeModifier(attribute, modifierId.toString(), -REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static UUID modifierId(String attribute) {
        return UUID.nameUUIDFromBytes(
                ("maledict:age_of_darkness/" + attribute).getBytes(StandardCharsets.UTF_8));
    }
}
