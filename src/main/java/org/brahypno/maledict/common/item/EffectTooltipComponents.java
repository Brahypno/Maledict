package org.brahypno.maledict.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Effect descriptions without the leading space that wraps into an empty line before long Chinese text. */
public final class EffectTooltipComponents {
    private EffectTooltipComponents() {
    }

    public static Component positiveEffect(String suffix, Object... arguments) {
        return Component.translatable("malum.effect." + suffix, arguments).withStyle(ChatFormatting.DARK_GREEN);
    }

    public static Component negativeEffect(String suffix, Object... arguments) {
        return Component.translatable("malum.effect." + suffix, arguments).withStyle(ChatFormatting.RED);
    }
}
