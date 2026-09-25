package org.brahypno.maledict.data;

import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;

import java.util.function.BiConsumer;

/**
 * 无常常规掉落表：四档难度各一张 ID、内容相同；SIMPLE 用的就是实体默认路径
 * {@code maledict:entities/first_vicissitude}，其余三档已被存档、{@code /loot} 与文档引用，不能合并。
 */
public final class MaledictEntityLoot implements LootTableSubProvider {

    /** 深岩珍金块的块数：两块即 18 个珍金锭。 */
    private static final float CTHONIC_GOLD_BLOCKS = 2.0F;

    private static final float NULL_SLATE = 6.0F;

    private static final float VOID_SALTS = 6.0F;

    @Override
    public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
        for (BossDifficulty difficulty : BossDifficulty.values()) {
            output.accept(difficulty.lootTable(), table());
        }
    }

    private static LootTable.Builder table() {
        return LootTable.lootTable()
                .withPool(pool(ItemRegistry.BLOCK_OF_CTHONIC_GOLD.get(), CTHONIC_GOLD_BLOCKS))
                .withPool(pool(ItemRegistry.NULL_SLATE.get(), NULL_SLATE))
                .withPool(pool(ItemRegistry.VOID_SALTS.get(), VOID_SALTS));
    }

    private static LootPool.Builder pool(Item item, float count) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(count))));
    }
}
