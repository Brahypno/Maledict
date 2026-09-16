package org.brahypno.maledict.common.curio;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * 启蒙之年护符给出的等级，读的是饰品自己的 NBT。
 *
 * <p>一枚护符只写一个等级，击杀时给出的<b>启蒙之年</b>（给佩戴者）与<b>黑暗年代</b>
 * （给附近最近的一名敌人）都用它当 amplifier——效果等级乘数由原版
 * {@code MobEffectInstance} 自己按 {@code amplifier + 1} 结算，所以这里的数字就是
 * {@code getAmplifier()} 能读到的那个值，0 即游戏里显示的效果等级 I。
 *
 * <p>读不到就是 {@link #FALLBACK}（0 级）。普通途径拿到的护符身上没有这个键，
 * 于是行为与加等级之前一模一样；等级只能从 {@code give} 命令、战利品表、
 * 合成脚本这类外部来源写进来。
 *
 * <p>不依赖任何 Minecraft 世界的类型，只碰 {@link CompoundTag}，方便直接单测——
 * 与 {@link HalfHealth} 同一个理由。
 */
public final class EnlightenmentLevel {

    /** 饰品 NBT 上记等级的键。 */
    public static final String TAG = "EnlightenmentLevel";

    /** 键不存在（或写了个不是数字的值）时的等级：0 级。 */
    public static final int FALLBACK = 0;

    /**
     * @param tag 饰品栈的 NBT，可以是 {@code null}（没有 NBT 的栈就是这种）
     * @return 等级，缺失或为负都是 {@link #FALLBACK}
     */
    public static int fromTag(@Nullable CompoundTag tag) {
        if (tag == null) {
            return FALLBACK;
        }
        // 键不存在、或者类型不是数字时 getInt 给 0，正好就是退路；只有负数需要单独夹一次。
        return Math.max(FALLBACK, tag.getInt(TAG));
    }

    private EnlightenmentLevel() {
    }
}
