package org.brahypno.maledict.common.attribute;

import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.brahypno.maledict.registry.MaledictAttributes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Grants Omen and reduces Malum's actual conversion attribute, without replacing Malum's handler. */
public final class TranscendOmenUtil {
    private static final UUID CANCELLATION = UUID.nameUUIDFromBytes(
            "maledict:transcend_omen/cancellation".getBytes(StandardCharsets.UTF_8));

    /** Set one source's transient contribution and reduce Malum conversion. Returns the reverse remainder.
     * Reuse the source UUID; amount zero removes it. Conversion results update on the next living tick. */
    public static double setAmount(LivingEntity entity, UUID source, double amount) {
        if (entity.level().isClientSide) return 0;
        AttributeInstance omen = entity.getAttribute(MaledictAttributes.TRANSCEND_OMEN.get());
        if (omen == null) return 0;
        omen.removeModifier(source);
        if (amount > 0) {
            omen.addTransientModifier(new AttributeModifier(source, "Transcend Omen", amount,
                    AttributeModifier.Operation.ADDITION));
        }
        return reduceMalignantConversion(entity);
    }

    /** Also called before Malum's tick, so equipment/effect changes on either side are included. */
    static double reduceMalignantConversion(LivingEntity entity) {
        AttributeInstance omen = entity.getAttribute(MaledictAttributes.TRANSCEND_OMEN.get());
        AttributeInstance malignant = entity.getAttribute(AttributeRegistry.MALIGNANT_CONVERSION.get());
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
        instance.addTransientModifier(new AttributeModifier(CANCELLATION, "Conversion cancellation",
                reductionModifier(instance, fraction), AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    static double reductionModifier(AttributeInstance instance, double fraction) {
        // AttributeInstance clamps AFTER all operations: raw resistance 2 (current 1) at 50%
        // must become .5, not remain 1. The attribute's original lower bound still applies.
        double base = instance.getBaseValue();
        for (var modifier : instance.getModifiers(AttributeModifier.Operation.ADDITION)) {
            base += modifier.getAmount();
        }
        double raw = base;
        for (var modifier : instance.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) {
            raw += base * modifier.getAmount();
        }
        for (var modifier : instance.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            raw *= 1 + modifier.getAmount();
        }
        double capped = instance.getValue();
        return raw > capped ? capped * (1 - fraction) / raw - 1 : -fraction;
    }
}
