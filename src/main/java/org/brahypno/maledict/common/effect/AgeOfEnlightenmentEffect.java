package org.brahypno.maledict.common.effect;

import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 启蒙之年：佩戴启蒙之年的玩家击杀敌人后获得的增益，持续期间每次出手都必定暴击，每级额外 +1 法杖暂存弹数。
 * 与 Malum 的 {@code grim_certainty} 不同，这里命中后不消耗效果；{@code addAttributeModifiers} 结算时会自乘 {@code (amplifier + 1)}。
 */
public final class AgeOfEnlightenmentEffect extends MobEffect {
    public static final int COLOR = 0xFFC64B;

    /** 每级额外给的法杖暂存弹数，等级乘数由原版补上。 */
    public static final double RESERVE_STAFF_CHARGES_PER_LEVEL = 1.0D;

    /** UUID 必须固定：原版移除属性修饰符时按 UUID 认，否则效果到期摘不干净。 */
    private static final UUID RESERVE_STAFF_CHARGES_MODIFIER_ID = UUID.nameUUIDFromBytes(
            "maledict:age_of_enlightenment/reserve_staff_charges".getBytes(StandardCharsets.UTF_8));

    public AgeOfEnlightenmentEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
        addAttributeModifier(AttributeRegistry.RESERVE_STAFF_CHARGES.get(),
                RESERVE_STAFF_CHARGES_MODIFIER_ID.toString(),
                RESERVE_STAFF_CHARGES_PER_LEVEL,
                AttributeModifier.Operation.ADDITION);
    }
}
