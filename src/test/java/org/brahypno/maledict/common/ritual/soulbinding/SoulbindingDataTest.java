package org.brahypno.maledict.common.ritual.soulbinding;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.21 把拘魂记录从物品根 NBT 搬到了 {@code minecraft:custom_data} 组件，这组用例钉住搬运后的
 * 读写语义：空记录、已上膛、清空后组件整块消失、以及未知版本一律读成 {@code EMPTY}。
 */
class SoulbindingDataTest {
    @Test
    void emptyToolArmsAndClearsThroughTheCustomDataComponent() {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        assertEquals(SoulbindingData.State.EMPTY, SoulbindingData.state(stack));
        assertNull(SoulbindingData.snapshot(stack));

        SoulbindingData.arm(stack);
        assertEquals(SoulbindingData.State.ARMED, SoulbindingData.state(stack));
        assertTrue(SoulbindingData.isArmed(stack));
        // 只有 BOUND 才有快照；ARMED 只是「下一次击杀会记下精魂」。
        assertNull(SoulbindingData.snapshot(stack));

        SoulbindingData.clear(stack);
        assertEquals(SoulbindingData.State.EMPTY, SoulbindingData.state(stack));
        // 记录是 custom_data 里唯一的载荷，清空后整个组件应当消失，而不是留一个空壳。
        assertNull(stack.get(DataComponents.CUSTOM_DATA));
    }

    @Test
    void unknownVersionReadsAsEmpty() {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        CompoundTag root = new CompoundTag();
        root.putInt("Version", SoulbindingData.VERSION + 1);
        root.putString("State", "BOUND");
        CompoundTag custom = new CompoundTag();
        custom.put(SoulbindingData.ROOT, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));

        assertEquals(SoulbindingData.State.EMPTY, SoulbindingData.state(stack));
        assertNull(SoulbindingData.snapshot(stack));
    }

    @Test
    void borrowedAttributesHaveStableDistinctModifierIds() {
        long distinct = SoulbindingData.BONUSES.stream()
                                              .map(SoulbindingData.Bonus::id)
                                              .distinct()
                                              .count();
        assertEquals(SoulbindingData.BONUSES.size(), distinct);
        assertTrue(SoulbindingData.BONUSES.stream().allMatch(bonus -> bonus.ratio() > 0.0D));
        assertTrue(SoulbindingData.BONUSES.stream()
                                          .allMatch(bonus -> bonus.id().getNamespace().equals("maledict")));
    }
}
