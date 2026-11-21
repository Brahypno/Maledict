package org.brahypno.maledict.common.item.weapon.baron;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.brahypno.maledict.Maledict;

/**
 * 礼杖的移速加成，主手、副手各一枚独立 id。
 *
 * <p>两把礼杖同时拿在手里时，同一个 id 会被判成重复，所以必须分开；两枚都挂在同一件物品上、
 * 但按 {@link EquipmentSlotGroup} 区分槽位，因此只在对应的那只手上生效。
 * 1.20.1 走的是 {@code getAttributeModifiers(EquipmentSlot, ItemStack)} 覆写，1.21 改成属性组件 +
 * 事件：配置要延迟读取，烤进注册期组件就晚了。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class BaronStaffAttributes {
    private static final ResourceLocation MAINHAND_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "baron_staff/speed_mainhand");
    private static final ResourceLocation OFFHAND_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "baron_staff/speed_offhand");

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (!(event.getItemStack().getItem() instanceof BaronStaffItem)) {
            return;
        }
        double bonus = BaronStaffItem.speedBonus();
        if (!Double.isFinite(bonus) || bonus == 0.0D) {
            return;
        }
        event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                MAINHAND_SPEED_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.MAINHAND);
        event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                OFFHAND_SPEED_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.OFFHAND);
    }

    private BaronStaffAttributes() {
    }
}
