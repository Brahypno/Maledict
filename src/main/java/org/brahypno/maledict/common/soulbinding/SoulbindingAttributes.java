package org.brahypno.maledict.common.soulbinding;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

/**
 * Exposes a {@code BOUND} tool's snapshot as main-hand attribute modifiers.
 *
 * <p>Only {@link EquipmentSlot#MAINHAND} is considered, so moving the tool to the offhand, the
 * inventory or a curio slot drops every bonus. Modifiers are plain {@code ADDITION} with fixed
 * UUIDs and are only ever added, never used to replace another mod's modifiers. The values come
 * from the item stack alone, which is also what the tooltip reads.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class SoulbindingAttributes {
    private static final String MODIFIER_NAME = "maledict:soulbinding";

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }
        SoulbindingData.Snapshot snapshot = SoulbindingData.snapshot(event.getItemStack());
        if (snapshot == null) {
            return;
        }
        for (SoulbindingData.Bonus bonus : SoulbindingData.BONUSES) {
            Double base = snapshot.baseValue(bonus.attribute());
            if (base == null) {
                continue;
            }
            double amount = bonus.amount(base);
            if (!Double.isFinite(amount) || amount == 0.0D) {
                continue;
            }
            event.addModifier(bonus.attribute(), new AttributeModifier(
                    bonus.uuid(), MODIFIER_NAME, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private SoulbindingAttributes() {
    }
}
