package org.brahypno.maledict.common.refulgence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * 反照储能落在<b>那件武器自己的 ItemStack</b> 上，不是 {@code Map<玩家, 数值>}：
 * 换手、丢地上再捡回来、放箱子、跨维度、重启都跟着物品走，不会把能量留在另一个人身上。
 *
 * <p>NBT 布局（规范第 5.1 节）：
 * <pre>
 * maledict:refulgence: { charge: float, lastChargeTick: long, version: int }
 * </pre>
 * 只有服务端会写；客户端那侧看到的只是物品数据，读不读随它。
 */
public final class RefulgenceStore {
    private static final String ROOT = "maledict:refulgence";
    private static final String CHARGE = "charge";
    private static final String LAST_CHARGE_TICK = "lastChargeTick";
    private static final String VERSION = "version";

    public static RefulgenceCharge read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND)) {
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
        stack.getOrCreateTag().put(ROOT, data);
    }

    /** 到期、释放完、或者读出来的数不对时清干净，不把空壳留在工具上。 */
    public static void clear(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(ROOT);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    private RefulgenceStore() {
    }
}
