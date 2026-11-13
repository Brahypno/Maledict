package org.brahypno.maledict.compat.maid;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The two independent sword items. {@code transformed} is the only distinction the gameplay code
 * needs; the actual item registry decides whether a stack may be armed, released, or transformed.
 */
public final class MaidBondSwordItem extends SwordItem {
    private final boolean transformed;

    public MaidBondSwordItem(boolean transformed) {
        // The documented 6/12 are the item modifiers, not the final player panel value.
        // Netherite contributes four, so 2 + 4 = 6 and 8 + 4 = 12.
        super(Tiers.NETHERITE, transformed ? 8 : 2, transformed ? -2.4F : -2.0F,
              properties(transformed));
        this.transformed = transformed;
    }

    private static Properties properties(boolean transformed) {
        Properties properties = new Properties().stacksTo(1);
        return transformed ? properties.fireResistant() : properties;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (transformed)
            return;
        MaidBondData.Gift gift = MaidBondData.read(stack);
        if (gift == null)
            return;

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

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var attributes = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.MAINHAND)
            return attributes;
        return ImmutableMultimap.<Attribute, AttributeModifier>builder().putAll(attributes)
                                .put(LodestoneAttributeRegistry.MAGIC_DAMAGE.get(), new AttributeModifier(
                                        LodestoneAttributeRegistry.UUIDS.get(LodestoneAttributeRegistry.MAGIC_DAMAGE),
                                        "Maid bond magic damage", transformed ? 12.0D : 2.0D,
                                        AttributeModifier.Operation.ADDITION))
                                .build();
    }
}
