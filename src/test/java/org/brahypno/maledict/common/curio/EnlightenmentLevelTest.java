package org.brahypno.maledict.common.curio;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 护符 NBT 上那个等级键怎么读：没有、写错类型或写成负数都退回 0 级。 */
class EnlightenmentLevelTest {

    @Test
    void anExplicitLevelIsRead() {
        assertEquals(1, EnlightenmentLevel.fromTag(level(1)));
        assertEquals(2, EnlightenmentLevel.fromTag(level(2)));
        assertEquals(9, EnlightenmentLevel.fromTag(level(9)));
        assertEquals(0, EnlightenmentLevel.fromTag(level(0)));
        assertEquals(0, EnlightenmentLevel.fromTag(new CompoundTag()));
    }

    @Test
    void aStackWithoutNbtFallsBackToZeroLevel() {
        assertEquals(0, EnlightenmentLevel.fromTag(null));
    }

    @Test
    void aNegativeLevelFallsBackToZero() {
        assertEquals(0, EnlightenmentLevel.fromTag(level(-1)));
        assertEquals(0, EnlightenmentLevel.fromTag(level(-100)));
    }

    private static CompoundTag level(int value) {
        CompoundTag tag = new CompoundTag();
        // 字面键名：外部来源（战利品表、give 命令）按这个名字写，改名就是改存档格式。
        tag.putInt("Maldict:EnlightenmentLevel", value);
        return tag;
    }
}
