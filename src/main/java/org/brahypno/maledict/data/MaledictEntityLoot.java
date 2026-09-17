package org.brahypno.maledict.data;

import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.brahypno.maledict.common.curio.EnlightenmentLevel;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.BiConsumer;

/**
 * 无常常规掉落：四档难度各一张表，掉一件启蒙之年（难度越高等级越高）加两个深岩珍金块。
 *
 * <p>表 ID 归 {@link BossDifficulty} 管，实体按当前难度选表（见
 * {@code FirstVicissitudeBossEntity#getDefaultLootTable}）；这里只负责把内容填进每张表，
 * 所以「哪档难度掉哪一级」在下面 {@link #enlightenmentLevel} 一处就能读全。
 *
 * <p>奖励是必掉的，没有击杀者条件也不吃抢夺：死亡事务本身只走一次（见
 * {@code VicissitudeBossEntity#dropAllDeathLoot}），这张表由原版死亡流程调用一次。
 *
 * <p>没有继承 {@code EntityLootSubProvider}：那个基类会按注册表逐个实体类型要求存在默认表，
 * 其他模组的生物也会被算进来，而本模组只有这一个实体。这里的四张表也不全是各自的默认表 ID，
 * 直接实现 {@link LootTableSubProvider} 更合适。
 */
public final class MaledictEntityLoot implements LootTableSubProvider {

    /** 每档难度都掉两个深岩珍金块，与启蒙之年的等级无关。 */
    private static final float CTHONIC_GOLD_BLOCKS = 2.0F;

    @Override
    public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
        for (BossDifficulty difficulty : BossDifficulty.values()) {
            output.accept(difficulty.lootTable(), table(enlightenmentLevel(difficulty)));
        }
    }

    private static LootTable.Builder table(int enlightenmentLevel) {
        CompoundTag level = new CompoundTag();
        // 键名归 EnlightenmentLevel 管，掉落表不自己抄一份字面量。
        level.putInt(EnlightenmentLevel.TAG, enlightenmentLevel);
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(MaledictItems.AGE_OF_ENLIGHTENMENT.get())
                                .apply(SetNbtFunction.setTag(level))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ItemRegistry.BLOCK_OF_CTHONIC_GOLD.get())
                                .apply(SetItemCountFunction.setCount(
                                        ConstantValue.exactly(CTHONIC_GOLD_BLOCKS)))));
    }

    /**
     * 每档难度的启蒙之年等级。
     *
     * <p>NBT 上写的是 amplifier，0 就是游戏里显示的一级（见 {@link EnlightenmentLevel}），
     * 于是四档由易到难正好是 I–IV 级，四个等级都能从这只 Boss 身上拿全。
     */
    private static int enlightenmentLevel(BossDifficulty difficulty) {
        return switch (difficulty) {
            case SIMPLE -> 0;
            case DIFFICULT -> 1;
            case COMPLETE -> 2;
            case EXTREME -> 3;
        };
    }
}
