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
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.brahypno.maledict.common.realm.BlightedRemnantFeature;
import org.brahypno.maledict.common.realm.AbandonedWellStationFeature;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.BiConsumer;

final class MaledictRealmLoot {
    static final ResourceLocation RELIQUARY = ResourceLocation.fromNamespaceAndPath("maledict", "chests/inverted_reliquary");

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
        output.accept(RELIQUARY, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 5))
                        .add(stack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get(), 3, 8).setWeight(60))
                        .add(stack(ItemRegistry.MALIGNANT_LEAD.get(), 1, 3).setWeight(25))
                        .add(stack(MaledictItems.CRYING_TWISTED_ROCK.get(), 1, 3).setWeight(25))
                        .add(stack(MaledictItems.CRYING_TAINTED_ROCK.get(), 1, 3).setWeight(25))
                        .add(stack(Items.NETHERITE_SCRAP, 1, 2).setWeight(15)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.05F))
                        .add(LootItem.lootTableItem(Items.NETHERITE_INGOT)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.01F))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(enchantedArmor(Items.DIAMOND_HELMET))
                        .add(enchantedArmor(Items.DIAMOND_CHESTPLATE))
                        .add(enchantedArmor(Items.DIAMOND_LEGGINGS))
                        .add(enchantedArmor(Items.DIAMOND_BOOTS))));
    }

    private static LootItem.Builder<?> enchantedArmor(Item item) {
        return LootItem.lootTableItem(item)
                .apply(EnchantWithLevelsFunction.enchantWithLevels(UniformGenerator.between(30, 40)));
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
