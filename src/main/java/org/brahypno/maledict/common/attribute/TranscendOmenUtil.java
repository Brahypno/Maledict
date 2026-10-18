package org.brahypno.maledict.common.attribute;

import com.sammy.malum.registry.common.MalumAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.brahypno.maledict.registry.MaledictAttributes;

import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;

/** Grants Omen and reduces Malum's actual conversion attribute, without replacing Malum's handler. */
public final class TranscendOmenUtil {
    private static final ResourceLocation CANCELLATION = ResourceLocation.parse("maledict:transcend_omen/cancellation");

    /** Set one source's transient contribution and reduce Malum conversion. Returns the reverse remainder.
     * Reuse the source ResourceLocation; amount zero removes it. Conversion results update on the next living tick. */
    public static double setAmount(LivingEntity entity, ResourceLocation source, double amount) {
        if (entity.level().isClientSide) return 0;
        AttributeInstance omen = entity.getAttribute(MaledictAttributes.TRANSCEND_OMEN);
        if (omen == null) return 0;
        omen.removeModifier(source);
        if (amount > 0) {
            omen.addTransientModifier(new AttributeModifier(source, amount,
                    AttributeModifier.Operation.ADD_VALUE));
        }
        return reduceMalignantConversion(entity);
    }

    /** Also called before Malum's tick, so equipment/effect changes on either side are included. */
    static double reduceMalignantConversion(LivingEntity entity) {
        AttributeInstance omen = entity.getAttribute(MaledictAttributes.TRANSCEND_OMEN);
        AttributeInstance malignant = entity.getAttribute(MalumAttributes.MALIGNANT_CONVERSION);
        if (omen == null) return 0;
        double reverse = omen.getValue();
        if (malignant == null) return reverse;
        malignant.removeModifier(CANCELLATION);
        double forward = malignant.getValue();
        double canceled = Math.min(reverse, forward);
        if (canceled > 0) {
            cancel(malignant, canceled / forward);
        }
        return Math.max(reverse - forward, 0);
    }

    private static void cancel(AttributeInstance instance, double fraction) {
        instance.addTransientModifier(new AttributeModifier(CANCELLATION, reductionModifier(instance, fraction), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    static double reductionModifier(AttributeInstance instance, double fraction) {
        // AttributeInstance clamps AFTER all operations: raw resistance 2 (current 1) at 50%
        // must become .5, not remain 1. The attribute's original lower bound still applies.
        double base = instance.getBaseValue();
        for (var modifier : instance.getModifiers().stream().filter(modifier -> modifier.operation() == AttributeModifier.Operation.ADD_VALUE).toList()) {
            base += modifier.amount();
        }
        double raw = base;
        for (var modifier : instance.getModifiers().stream().filter(modifier -> modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE).toList()) {
            raw += base * modifier.amount();
        }
        for (var modifier : instance.getModifiers().stream().filter(modifier -> modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL).toList()) {
            raw *= 1 + modifier.amount();
        }
        double capped = instance.getValue();
        return raw > capped ? capped * (1 - fraction) / raw - 1 : -fraction;
    }
}
