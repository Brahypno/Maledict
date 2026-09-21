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
 * 无常常规掉落：两个深岩珍金块、六块虚无板石、六份虚空盐。
 *
 * <p>表 ID 归 {@link BossDifficulty} 管，实体按当前难度选表（见
 * {@code FirstVicissitudeBossEntity#getDefaultLootTable}）；这里只负责把内容填进每张表。
 * 四张表的内容现在是一样的——难度差别在实体侧的启蒙之年与真生命上，见下——但四个 ID 仍然
 * 各自保留：SIMPLE 用的就是实体默认路径 {@code maledict:entities/first_vicissitude}，
 * 另外三档在存档、{@code /loot} 与文档里都被引用过，合并 ID 只会白白作废一批引用。
 *
 * <p>奖励是必掉的，没有击杀者条件也不吃抢夺：死亡事务本身只走一次（见
 * {@code VicissitudeBossEntity#dropAllDeathLoot}），这张表由原版死亡流程调用一次。
 * 启蒙之年<b>不在这里</b>：它是类似下界之星的专属额外掉落，只有玩家击杀才发，
 * 见 {@code FirstVicissitudeBossEntity#dropCustomDeathLoot}。
 *
 * <p>没有继承 {@code EntityLootSubProvider}：那个基类会按注册表逐个实体类型要求存在默认表，
 * 其他模组的生物也会被算进来，而本模组只有这一个实体。这里的四张表也不全是各自的默认表 ID，
 * 直接实现 {@link LootTableSubProvider} 更合适。
 */
public final class MaledictEntityLoot implements LootTableSubProvider {

    /** 深岩珍金块的块数：两块即 18 个珍金锭。 */
    private static final float CTHONIC_GOLD_BLOCKS = 2.0F;

    /** 虚无板石：Malum 穿过哭常之底后的灵魂石，本模组原本没有稳定来源。 */
    private static final float NULL_SLATE = 6.0F;

    /** 虚空盐：同上一条的灰烬版本，与虚无板石一起构成虚空线的基础材料。 */
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

    /** 每种奖励一个必掉池：rolls 1 次、固定数量，互相之间不抢概率。 */
    private static LootPool.Builder pool(Item item, float count) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(count))));
    }
}
