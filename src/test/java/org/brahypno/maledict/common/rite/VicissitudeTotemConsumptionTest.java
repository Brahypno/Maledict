package org.brahypno.maledict.common.rite;

import org.junit.jupiter.api.Test;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public class VicissitudeTotemConsumptionTest {
    @Test
    void upperLogSelectionStopsAtFirstAir() {
        String[] column = {"pole", "soulwood", "runewood", "air", "soulwood"};
        List<Integer> logs = scan(column);
        assertEquals(List.of(1, 2), logs);
    }

    @Test
    void otherBlocksAreLeftOutWhileTheSearchContinuesUntilAir() {
        assertEquals(List.of(1, 3), scan(new String[]{"pole", "soulwood", "stone", "runewood", "air"}));
    }

    @Test
    void airImmediatelyAboveTheTopLeavesNothingExtraToConsume() {
        assertEquals(List.of(), scan(new String[]{"pole", "air", "runewood"}));
    }

    @Test
    void aColumnWithoutAirStopsAtTheWorldHeight() {
        assertEquals(List.of(1, 2), scan(new String[]{"pole", "runewood", "soulwood"}));
    }

    private static List<Integer> scan(String[] column) {
        return VicissitudeTotemConsumption.findUpperLogs(0, column.length,
                y -> column[y].equals("air"),
                y -> column[y].equals("soulwood") || column[y].equals("runewood"));
    }

    @GameTest(template = "test_empty")
    public static void loadedConsumptionRemovesRealBlocksOnceAndPreservesReplacements(GameTestHelper helper) {
        ListTag layers = new ListTag();
        for (int y = 4; y >= 1; y--) {
            BlockPos position = helper.absolutePos(new BlockPos(2, y, 2));
            helper.getLevel().setBlockAndUpdate(position, Blocks.STONE.defaultBlockState());
            CompoundTag entry = new CompoundTag();
            entry.put("Position", NbtUtils.writeBlockPos(position));
            entry.put("State", NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
            layers.add(entry);
        }
        CompoundTag saved = new CompoundTag(); saved.put("Layers", layers);
        VicissitudeTotemConsumption consumption = VicissitudeTotemConsumption.load(helper.getLevel(), saved);
        java.util.ArrayList<Integer> removed = new java.util.ArrayList<>();
        consumption.tick(helper.getLevel(), 10, (index, point) -> removed.add(index));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 4, 2));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 3, 2));
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(2, 3, 2)), Blocks.DIAMOND_BLOCK.defaultBlockState());
        VicissitudeTotemConsumption loaded = VicissitudeTotemConsumption.load(helper.getLevel(), consumption.save());
        loaded.tick(helper.getLevel(), 90, (index, point) -> removed.add(index));
        loaded.tick(helper.getLevel(), 160, (index, point) -> removed.add(index));
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, new BlockPos(2, 3, 2));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 2));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 2));
        helper.assertTrue(removed.equals(List.of(0, 2, 3)), "Loaded progress must skip changed blocks and never consume a layer twice");
        helper.succeed();
    }
}
