package org.brahypno.maledict.common.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** A bounded search that reaches every ring and direction before spending trials on elevation. */
final class VicissitudeUnstickSearch {
    record Offset(int x, int y, int z) {}
    private static final int[] RADII = {4, 8, 12, 16};
    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1},
            {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
    private static final int[] HEIGHTS = {2, -2, 4, -4, 6, -6, 8, -8};
    private static final List<Offset> CANDIDATES = candidates();

    private VicissitudeUnstickSearch() {}

    private static List<Offset> candidates() {
        List<Offset> offsets = new ArrayList<>(64);
        for (int pass = 0; pass < 2; pass++) {
            for (int radius : RADII) {
                for (int direction = 0; direction < DIRECTIONS.length; direction++) {
                    offsets.add(new Offset(radius * DIRECTIONS[direction][0],
                            pass == 0 ? 0 : HEIGHTS[direction], radius * DIRECTIONS[direction][1]));
                }
            }
        }
        return List.copyOf(offsets);
    }

    static Offset find(Predicate<Offset> usable) {
        for (Offset offset : CANDIDATES) if (usable.test(offset)) return offset;
        return null;
    }
}
