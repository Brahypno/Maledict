package org.brahypno.maledict.data;

import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.brahypno.maledict.common.realm.BlightedRemnantFeature;
import org.brahypno.maledict.common.realm.AbandonedWellStationFeature;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.BiConsumer;

final class MaledictRealmLoot {
    static void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
        output.accept(AbandonedWellStationFeature.LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(stack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get(), 3, 7)))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 4))
                        .add(stack(ItemRegistry.ARCANE_SPIRIT.get(), 1, 3))
                        .add(stack(ItemRegistry.EARTHEN_SPIRIT.get(), 1, 3))
                        .add(stack(MaledictItems.FALLEN_SOULWOOD_BRANCHES.get(), 2, 5))
                        .add(stack(MaledictItems.WITHERLIGHT_MUSHROOM.get(), 1, 3)))
                .withPool(notes("station", 4)));
        output.accept(BlightedRemnantFeature.RITE_LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 4))
                        .add(stack(ItemRegistry.ARCANE_SPIRIT.get(), 1, 3))
                        .add(stack(ItemRegistry.EARTHEN_SPIRIT.get(), 1, 3))
                        .add(stack(Items.BONE_MEAL, 2, 5))
                        .add(stack(MaledictItems.WITHERLIGHT_MUSHROOM.get(), 1, 2)))
                .withPool(notes("rite")));
        output.accept(BlightedRemnantFeature.DIG_LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(stack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get(), 2, 6)))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 3))
                        .add(stack(Items.COAL, 2, 5)).add(stack(Items.IRON_NUGGET, 3, 8))
                        .add(stack(Items.STICK, 2, 6)))
                .withPool(notes("dig")));
    }

    private static LootItem.Builder<?> stack(Item item, int min, int max) {
        return LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }

    private static LootPool.Builder notes(String kind) {
        return notes(kind, 2);
    }

    private static LootPool.Builder notes(String kind, int pageCount) {
        CompoundTag book = new CompoundTag();
        book.putString("title", "Field Notes");
        book.putString("author", "?");
        ListTag pages = new ListTag();
        for (int page = 1; page <= pageCount; page++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.maledict.realm." + kind + "." + page))));
        }
        book.put("pages", pages);
        CompoundTag display = new CompoundTag();
        display.putString("Name", Component.Serializer.toJson(Component.translatable("book.maledict.realm." + kind)));
        book.put("display", display);
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(Items.WRITTEN_BOOK).apply(SetNbtFunction.setTag(book)));
    }

    private MaledictRealmLoot() {}
}
