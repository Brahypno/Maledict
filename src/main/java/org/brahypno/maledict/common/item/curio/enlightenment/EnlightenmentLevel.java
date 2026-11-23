package org.brahypno.maledict.common.item.curio.enlightenment;

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
        return fromAmplifier(tag.getInt(TAG));
    }

    /**
     * 药水等级转护符等级：负等级会把减益翻成增益（等级乘数是 amplifier + 1），所以下限夹到
     * {@link #FALLBACK}。传染给下一个敌人的黑暗年代也走这一道闸。
     */
    public static int fromAmplifier(int amplifier) {
        return Math.max(FALLBACK, amplifier);
    }

    private EnlightenmentLevel() {
    }
}
