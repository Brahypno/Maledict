package org.brahypno.maledict.common.rite;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VicissitudeTotemConsumptionTest {
    @Test
    void mixedLogsAboveTheTopAreCollectedTogetherButAirEndsTheSearch() {
        String[] column = {"pole", "soulwood", "runewood", "air", "soulwood"};
        List<Integer> logs = scan(column);
        assertEquals(List.of(1, 2), logs);
        // The extra logs join the first removal; they never add delayed totem layers.
        assertEquals(0, VicissitudeSummoning.totemLayersConsumed(9, 4));
        assertEquals(1, VicissitudeSummoning.totemLayersConsumed(10, 4));
        assertEquals(2, VicissitudeSummoning.totemLayersConsumed(25, 4));
        assertEquals(4, VicissitudeSummoning.totemLayersConsumed(55, 4));
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
}
