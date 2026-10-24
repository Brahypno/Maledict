package org.brahypno.maledict.common.invocation;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
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
        stack.getOrCreateTagElement("Invocation").putString("Id", definition.toString());
        return stack;
    }

    @Nullable
    public static ResourceLocation definitionId(ItemStack stack) {
        if (!(stack.getItem() instanceof InvocationSigilItem) || !stack.hasTag()
                || !stack.getTag().contains("Invocation", Tag.TAG_COMPOUND)) return null;
        return ResourceLocation.tryParse(stack.getTagElement("Invocation").getString("Id"));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        InvocationDefinition definition = InvocationDefinition.find(level, stack);
        if (definition == null || !InvocationSpawns.supports(definition)) {
            tooltip.add(Component.translatable("message.maledict.invocation.invalid").withStyle(ChatFormatting.RED));
            return;
        }
        var type = ForgeRegistries.ENTITY_TYPES.getValue(definition.entity());
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
