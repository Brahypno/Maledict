package org.brahypno.maledict.common.curio;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link EnlightenmentLevel} 的读表：护符 NBT 上那个 {@code EnlightenmentLevel} 键怎么读。
 *
 * <p>钉住的是那句「取决于饰品的 NBT，fallback 0 级」：普通途径拿到的护符身上没有这个键，
 * 读出来必须是 0 级，也就是与加等级之前完全一样的行为；等级只能由外部来源（{@code give}、
 * 战利品表）写进来。
 */
class EnlightenmentLevelTest {

    @Test
    void aStackWithoutNbtFallsBackToZeroLevel() {
        assertEquals(0, EnlightenmentLevel.fromTag(null));
    }

    @Test
    void aStackWithoutTheKeyFallsBackToZeroLevel() {
        assertEquals(0, EnlightenmentLevel.fromTag(new CompoundTag()));
    }

    /** 写进去几级就是几级：这个数直接当 {@code amplifier} 用。 */
    @Test
    void anExplicitLevelIsRead() {
        assertEquals(1, EnlightenmentLevel.fromTag(level(1)));
        assertEquals(2, EnlightenmentLevel.fromTag(level(2)));
        assertEquals(9, EnlightenmentLevel.fromTag(level(9)));
    }

    /** 显式写 0 与没有这个键是同一个结果，不需要区分。 */
    @Test
    void levelZeroReadsAsZero() {
        assertEquals(EnlightenmentLevel.FALLBACK, EnlightenmentLevel.fromTag(level(0)));
    }

    /** 负数会被夹回 0：等级乘数是 {@code amount * (amplifier + 1)}，负等级会把效果反过来。 */
    @Test
    void aNegativeLevelFallsBackToZero() {
        assertEquals(0, EnlightenmentLevel.fromTag(level(-1)));
        assertEquals(0, EnlightenmentLevel.fromTag(level(-100)));
    }

    /** 键写成了别的类型：当成没写，不抛异常。 */
    @Test
    void aKeyOfAnotherTypeFallsBackToZero() {
        CompoundTag tag = new CompoundTag();
        tag.putString(EnlightenmentLevel.TAG, "2");
        assertEquals(0, EnlightenmentLevel.fromTag(tag));
    }

    private static CompoundTag level(int value) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(EnlightenmentLevel.TAG, value);
        return tag;
    }
}
