package org.brahypno.maledict.registry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.brahypno.maledict.Maledict;

public final class MaledictEnchantments {
    public static final ResourceKey<Enchantment> ECTOPLASM = key("ectoplasm");
    public static final ResourceKey<Enchantment> REMINISCENCE = key("reminiscence");
    public static final ResourceKey<Enchantment> AFTERTASTE = key("aftertaste");

    public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        for (var entry : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            if (entry.getKey().is(key)) return stack.getEnchantmentLevel(entry.getKey());
        }
        return 0;
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, path));
    }

    private MaledictEnchantments() {}
}
