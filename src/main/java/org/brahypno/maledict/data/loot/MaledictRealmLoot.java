package org.brahypno.maledict.data.loot;

import com.sammy.malum.registry.common.item.MalumItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.BlightedRemnantFeature;
import org.brahypno.maledict.common.realm.AbandonedWellStationFeature;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.List;

public final class MaledictRealmLoot implements LootTableSubProvider {
    /** 倒置圣物匣的箱子战利品表；模板生成器（{@code MaledictReliquaryTemplates}）用同一个键写进箱子 NBT。 */
    public static final ResourceKey<LootTable> RELIQUARY = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "chests/inverted_reliquary"));

    private final HolderLookup.Provider registries;

    public MaledictRealmLoot(HolderLookup.Provider registries) {
        // 1.21 的附魔函数要现取附魔注册表，构造期留一份 provider。
        this.registries = registries;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(AbandonedWellStationFeature.LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(stack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get(), 3, 7)))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 4))
                        .add(stack(MalumItems.ARCANE_SPIRIT.get(), 1, 3))
                        .add(stack(MalumItems.EARTHEN_SPIRIT.get(), 1, 3))
                        .add(stack(MaledictItems.FALLEN_SOULWOOD_BRANCHES.get(), 2, 5))
                        .add(stack(MaledictItems.WITHERLIGHT_MUSHROOM.get(), 1, 3)))
                .withPool(notes("station", 4)));
        output.accept(BlightedRemnantFeature.RITE_LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 4))
                        .add(stack(MalumItems.ARCANE_SPIRIT.get(), 1, 3))
                        .add(stack(MalumItems.EARTHEN_SPIRIT.get(), 1, 3))
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
                        .add(stack(MalumItems.MALIGNANT_LEAD.get(), 1, 3).setWeight(25))
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
                        .add(enchantedArmor(Items.DIAMOND_BOOTS)))
                // TODO: 与 1.20.1 一致的临时拘魂卷轴来源，等它有了正经出处再撤。
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(MaledictItems.SOULBINDING_SCROLL.get()))));
    }

    private LootItem.Builder<?> enchantedArmor(Item item) {
        return LootItem.lootTableItem(item)
                .apply(EnchantWithLevelsFunction.enchantWithLevels(registries, UniformGenerator.between(30, 40)));
    }

    private static LootItem.Builder<?> stack(Item item, int min, int max) {
        return LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }

    private static LootPool.Builder notes(String kind) {
        return notes(kind, 2);
    }

    private static LootPool.Builder notes(String kind, int pageCount) {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int page = 1; page <= pageCount; page++) {
            pages.add(Filterable.passThrough(Component.translatable("book.maledict.realm." + kind + "." + page)));
        }
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(Items.WRITTEN_BOOK)
                        .apply(SetComponentsFunction.setComponent(DataComponents.WRITTEN_BOOK_CONTENT,
                                new WrittenBookContent(Filterable.passThrough("Field Notes"), "?", 0, pages, false)))
                        .apply(SetComponentsFunction.setComponent(DataComponents.CUSTOM_NAME,
                                Component.translatable("book.maledict.realm." + kind))));
    }

}
