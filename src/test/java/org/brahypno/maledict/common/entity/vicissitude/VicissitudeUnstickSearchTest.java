package org.brahypno.maledict.common.entity.vicissitude;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VicissitudeUnstickSearchTest {
    @Test void findsTheOnlyExitOnTheFarPositiveSide() {
        var exit = new VicissitudeUnstickSearch.Offset(16, 0, 0);
        assertEquals(exit, VicissitudeUnstickSearch.find(exit::equals));
    }

    @Test void failedSearchStaysBoundedAndCoversAllRingsAndHeights() {
        List<VicissitudeUnstickSearch.Offset> visited = new ArrayList<>();
        assertNull(VicissitudeUnstickSearch.find(offset -> { visited.add(offset); return false; }));
        assertEquals(64, visited.size());
        assertEquals(64, visited.stream().distinct().count());
        for (int radius : new int[]{4, 8, 12, 16}) {
            for (int sign : new int[]{-1, 1}) {
                assertTrue(visited.contains(new VicissitudeUnstickSearch.Offset(sign * radius, 0, 0)));
                assertTrue(visited.contains(new VicissitudeUnstickSearch.Offset(0, 0, sign * radius)));
            }
        }
        for (int height : new int[]{-8, -6, -4, -2, 0, 2, 4, 6, 8})
            assertTrue(visited.stream().anyMatch(offset -> offset.y() == height));
    }
}
