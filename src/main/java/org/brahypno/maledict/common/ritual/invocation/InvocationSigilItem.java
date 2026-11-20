package org.brahypno.maledict.common.ritual.invocation;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import org.brahypno.maledict.common.item.MaledictItemData;
import org.brahypno.maledict.registry.MaledictItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class InvocationSigilItem extends Item {
    public static final List<String> TARGETS = List.of("sniffer", "allay", "skeleton_horse",
            "brown_mooshroom", "blue_axolotl", "shulker");

    public InvocationSigilItem() {
        super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    public static ItemStack create(String target) {
        return create(ResourceLocation.fromNamespaceAndPath("maledict", "invocation/" + target));
    }

    public static ItemStack create(ResourceLocation definition) {
        ItemStack stack = new ItemStack(MaledictItems.INVOCATION_SIGIL.get());
        MaledictItemData.update(stack, tag -> {
            CompoundTag invocation = new CompoundTag();
            invocation.putString("Id", definition.toString());
            tag.put("Invocation", invocation);
        });
        return stack;
    }

    @Nullable
    public static ResourceLocation definitionId(ItemStack stack) {
        CompoundTag tag = MaledictItemData.read(stack);
        if (!(stack.getItem() instanceof InvocationSigilItem) || !tag.contains("Invocation", Tag.TAG_COMPOUND)) return null;
        return ResourceLocation.tryParse(tag.getCompound("Invocation").getString("Id"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        InvocationDefinition definition = InvocationDefinition.find(context.level(), stack);
        if (definition == null || !InvocationSpawns.supports(definition)) {
            tooltip.add(Component.translatable("message.maledict.invocation.invalid").withStyle(ChatFormatting.RED));
            return;
        }
        var type = BuiltInRegistries.ENTITY_TYPE.get(definition.entity());
        tooltip.add(Component.translatable("tooltip.maledict.invocation.target", type.getDescription())
                .withStyle(ChatFormatting.GOLD));
        if (!definition.variant().equals("default")) {
            tooltip.add(Component.translatable("tooltip.maledict.invocation.variant",
                    Component.translatable("invocation.maledict.variant." + definition.variant())));
        }
        tooltip.add(Component.translatable("tooltip.maledict.invocation." + (definition.soulwood() ? "soulwood" : "runewood"))
                .withStyle(definition.soulwood() ? ChatFormatting.RED : ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.maledict.invocation.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.maledict.invocation.consume").withStyle(ChatFormatting.GRAY));
    }
}
