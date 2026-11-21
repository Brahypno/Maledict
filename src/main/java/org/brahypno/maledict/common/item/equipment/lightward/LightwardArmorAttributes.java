package org.brahypno.maledict.common.item.equipment.lightward;

import com.sammy.malum.registry.common.MalumAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.brahypno.maledict.Maledict;

/**
 * 灵魂护盾三项不是 {@code ArmorMaterial} 的字段，只能逐件挂在物品属性上。
 *
 * <p>同一属性上的修饰符按 id 去重，所以四件必须各用各的 id（见 {@link #modifierId}），
 * 否则后穿的那件会把先穿的顶掉。1.21 的 {@code AttributeModifier} 用 {@link ResourceLocation} 认身份，
 * 槽位在 {@code addModifier} 上按 {@link EquipmentSlotGroup} 给出，属性句柄是 {@code Holder<Attribute>}。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class LightwardArmorAttributes {
    /**
     * 灵魂护盾三项每件加成：容量 +7，恢复速率 +20%，稳固度 +10%。
     */
    private static final double SOUL_WARD_CAP_PER_PIECE = 7.0D;
    private static final double SOUL_WARD_RECOVERY_RATE_PER_PIECE = 0.2D;
    private static final double SOUL_WARD_INTEGRITY_PER_PIECE = 0.1D;

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (!(event.getItemStack().getItem() instanceof LightwardArmorItem armor)) {
            return;
        }
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(armor.getType().getSlot());
        Holder<Attribute> cap = MalumAttributes.SOUL_WARD_CAPACITY;
        Holder<Attribute> recovery = MalumAttributes.SOUL_WARD_RECOVERY_RATE;
        Holder<Attribute> integrity = MalumAttributes.SOUL_WARD_INTEGRITY;
        event.addModifier(cap, new AttributeModifier(
                modifierId(armor, "cap"), SOUL_WARD_CAP_PER_PIECE, AttributeModifier.Operation.ADD_VALUE), slot);
        event.addModifier(recovery, new AttributeModifier(
                modifierId(armor, "recovery_rate"), SOUL_WARD_RECOVERY_RATE_PER_PIECE,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), slot);
        event.addModifier(integrity, new AttributeModifier(
                modifierId(armor, "integrity"), SOUL_WARD_INTEGRITY_PER_PIECE,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), slot);
    }

    private static ResourceLocation modifierId(LightwardArmorItem armor, String attribute) {
        return ResourceLocation.fromNamespaceAndPath(
                Maledict.MODID, "aufheben_lightward/" + armor.getType().getName() + "/soul_ward_" + attribute);
    }

    private LightwardArmorAttributes() {
    }
}
