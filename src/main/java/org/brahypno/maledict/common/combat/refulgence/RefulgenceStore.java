package org.brahypno.maledict.common.combat.refulgence;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 反照储能落在<b>那件武器自己的 ItemStack</b> 上，不是 {@code Map<玩家, 数值>}：
 * 换手、丢地上再捡回来、放箱子、跨维度、重启都跟着物品走，不会把能量留在另一个人身上。
 *
 * <p>NBT 布局：
 * <pre>
 * maledict:refulgence: { charge: float, lastChargeTick: long, version: int }
 * </pre>
 * 1.21 没有 {@code ItemStack#getTag()} 了，储物槽换成 {@code minecraft:custom_data} 这个数据组件，
 * 里面还是同一份 NBT。只有服务端会写；客户端那侧看到的只是物品数据，读不读随它。
 */
public final class RefulgenceStore {
    private static final String ROOT = "maledict:refulgence";
    private static final String CHARGE = "charge";
    private static final String LAST_CHARGE_TICK = "lastChargeTick";
    private static final String VERSION = "version";

    public static RefulgenceCharge read(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return RefulgenceCharge.empty();
        }
        CompoundTag tag = custom.copyTag();
        if (!tag.contains(ROOT, Tag.TAG_COMPOUND)) {
            return RefulgenceCharge.empty();
        }
        CompoundTag data = tag.getCompound(ROOT);
        return new RefulgenceCharge(data.getFloat(CHARGE), data.getLong(LAST_CHARGE_TICK));
    }

    public static void write(ItemStack stack, RefulgenceCharge charge) {
        if (charge.isEmpty()) {
            clear(stack);
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putFloat(CHARGE, charge.charge());
        data.putLong(LAST_CHARGE_TICK, charge.lastChargeTick());
        data.putInt(VERSION, RefulgenceCharge.VERSION);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(ROOT, data));
    }

    /**
     * 到期、释放完、或者读出来的数不对时清干净，不把空壳留在工具上。
     */
    public static void clear(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return;
        }
        CompoundTag tag = custom.copyTag();
        tag.remove(ROOT);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private RefulgenceStore() {
    }
}
