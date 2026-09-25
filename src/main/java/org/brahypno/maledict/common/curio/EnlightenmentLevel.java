package org.brahypno.maledict.common.curio;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/** 启蒙之年护符给出的等级，读的是饰品自己的 NBT；读不到就是 {@link #FALLBACK}（0 级）。 */
public final class EnlightenmentLevel {

    public static final String TAG = "Maldict:EnlightenmentLevel";

    public static final int FALLBACK = 0;

    public static int fromTag(@Nullable CompoundTag tag) {
        if (tag == null){
            return FALLBACK;
        }
        // getInt 对缺失或非数字都返回 0，正好是退路；只有负数要单独夹一次。
        return Math.max(FALLBACK, tag.getInt(TAG));
    }

    private EnlightenmentLevel() {
    }
}
