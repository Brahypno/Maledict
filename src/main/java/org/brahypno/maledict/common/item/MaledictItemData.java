package org.brahypno.maledict.common.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/** Shared copy-on-write access to the item's custom data component. */
public final class MaledictItemData {
    public static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> update) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, update);
    }

    private MaledictItemData() {}
}
