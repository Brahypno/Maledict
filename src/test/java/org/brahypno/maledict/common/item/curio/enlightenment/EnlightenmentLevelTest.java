package org.brahypno.maledict.common.item.curio.enlightenment;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 护符等级的两个入口都得把负数夹成 0：药水等级的乘数是 {@code amplifier + 1}，负等级会把黑暗年代
 * 这个减益翻成增益；NBT 缺失或非数字也走 0。
 */
class EnlightenmentLevelTest {

    @Test
    void missingOrNegativeLevelsReadAsZeroAndPositivesPassThrough() {
        assertEquals(0, EnlightenmentLevel.fromTag(null));

        CompoundTag empty = new CompoundTag();
        assertEquals(0, EnlightenmentLevel.fromTag(empty));

        CompoundTag negative = new CompoundTag();
        negative.putInt(EnlightenmentLevel.TAG, -2);
        assertEquals(0, EnlightenmentLevel.fromTag(negative));

        CompoundTag positive = new CompoundTag();
        positive.putInt(EnlightenmentLevel.TAG, 3);
        assertEquals(3, EnlightenmentLevel.fromTag(positive));

        assertEquals(0, EnlightenmentLevel.fromAmplifier(-1));
        assertEquals(0, EnlightenmentLevel.fromAmplifier(0));
        assertEquals(2, EnlightenmentLevel.fromAmplifier(2));
    }
}
