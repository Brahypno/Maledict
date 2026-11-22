package org.brahypno.maledict.compat.maid;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import team.lodestar.lodestone.registry.common.LodestoneAttributes;

import java.util.List;

/**
 * The two independent sword items. {@code transformed} is the only distinction the gameplay code
 * needs; the actual item registry decides whether a stack may be armed, released, or transformed.
 *
 * <p>1.21 的差异：{@code SwordItem(Tier, int, float, Properties)} 与
 * {@code getAttributeModifiers(EquipmentSlot, ItemStack)} 都没了——攻击力/攻速与附加属性全部烤进
 * {@code minecraft:attribute_modifiers} 组件，由 {@link Item.Properties#attributes} 在注册期给定，
 * 因此这里不需要覆写任何取属性的方法。灵性伤害改成
 * {@link LodestoneAttributes#MAGIC_DAMAGE} + {@link LodestoneAttributes#BASE_MAGIC_DAMAGE} 这一对
 * 1.8.2 的写法（与 {@code LoverAxeItem} 同款）。
 */
public final class MaidBondSwordItem extends SwordItem {
    private final boolean transformed;

    public MaidBondSwordItem(boolean transformed) {
        // The documented 6/12 are the item modifiers, not the final player panel value.
        // Netherite contributes four, so 2 + 4 = 6 and 8 + 4 = 12.
        super(Tiers.NETHERITE, properties(transformed));
        this.transformed = transformed;
    }

    private static Properties properties(boolean transformed) {
        ItemAttributeModifiers attributes =
                SwordItem.createAttributes(Tiers.NETHERITE, transformed ? 8 : 2, transformed ? -2.4F : -2.0F)
                        .withModifierAdded(LodestoneAttributes.MAGIC_DAMAGE,
                                new AttributeModifier(LodestoneAttributes.BASE_MAGIC_DAMAGE,
                                        transformed ? 12.0D : 2.0D, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND);
        Properties properties = new Properties().stacksTo(1).attributes(attributes);
        return transformed ? properties.fireResistant() : properties;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (transformed)
            return;
        MaidBondData.Gift gift = MaidBondData.read(stack);
        if (gift == null)
            return;

        // 1.20.1 直传 Level；1.21 传的是 TooltipContext，在线玩家要通过 level() 取（箱子里的
        // 预览没有世界，那时就退回记录里的名字）。
        Level level = context.level();
        Component owner = gift.originalOwnerName() != null
                          ? Component.literal(gift.originalOwnerName())
                          : Component.translatable("tooltip.maledict.maid_bond.unknown_owner");
        if (gift.originalOwnerName() == null && level != null){
            Player online = level.getPlayerByUUID(gift.originalOwner());
            if (online != null)
                owner = online.getDisplayName();
        }
        Component maid = gift.customName() != null
                         ? gift.customName()
                         : Component.translatable("tooltip.maledict.maid_bond.unknown_maid");
        tooltip.add(Component.translatable("tooltip.maledict.maid_bond.original", owner, maid)
                             .withStyle(ChatFormatting.GRAY));
    }
}
