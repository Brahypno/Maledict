package org.brahypno.maledict.common.ritual.soulbinding;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.brahypno.maledict.Maledict;

/**
 * Exposes a {@code BOUND} tool's snapshot as main-hand attribute modifiers.
 *
 * <p>Only {@link EquipmentSlotGroup#MAINHAND} is considered, so moving the tool to the offhand, the
 * inventory or a curio slot drops every bonus. Modifiers are plain {@code ADD_VALUE} with fixed ids
 * and are only ever added, never used to replace another mod's modifiers. The values come from the
 * item stack alone, which is also what the tooltip reads.
 *
 * <p>1.21 的 {@code ItemAttributeModifierEvent} 不再有 {@code getSlotType()}：槽位改为在
 * {@code addModifier} 上按 {@link EquipmentSlotGroup} 指定，属性句柄是 {@code Holder<Attribute>}，
 * 加法运算枚举也从 {@code ADDITION} 改名为 {@code ADD_VALUE}。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class SoulbindingAttributes {
    private static final String MODIFIER_NAME = "maledict:soulbinding";

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        SoulbindingData.Snapshot snapshot = SoulbindingData.snapshot(event.getItemStack());
        if (snapshot == null){
            return;
        }
        for (SoulbindingData.Bonus bonus : SoulbindingData.BONUSES) {
            Holder<Attribute> attribute = bonus.attribute();
            Double base = snapshot.baseValue(attribute);
            if (base == null){
                continue;
            }
            double amount = bonus.amount(base);
            if (!Double.isFinite(amount) || amount == 0.0D){
                continue;
            }
            event.addModifier(attribute, new AttributeModifier(
                    bonus.id(), amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
    }

    private SoulbindingAttributes() {
    }
}
