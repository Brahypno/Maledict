package org.brahypno.maledict.common.item;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.brahypno.maledict.Maledict;

@EventBusSubscriber(modid = Maledict.MODID)
public final class IncursusBladeAttributes {
    @SubscribeEvent
    public static void updateAttributes(ItemAttributeModifierEvent event) {
        if (event.getItemStack().getItem() instanceof IncursusBladeItem blade) {
            // Malum supplies an attribute component, so the fallback item override is not queried.
            // Replace only this weapon's base entries; unrelated modifiers remain available.
            for (var entry : blade.getDefaultAttributeModifiers(event.getItemStack()).modifiers()) {
                event.replaceModifier(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
    }

    private IncursusBladeAttributes() {}
}
