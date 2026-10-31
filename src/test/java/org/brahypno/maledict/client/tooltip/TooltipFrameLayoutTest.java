package org.brahypno.maledict.client.tooltip;

import org.junit.jupiter.api.Test;
import org.brahypno.maledict.client.tooltip.TooltipFrameLayout.Insets;
import org.brahypno.maledict.client.tooltip.TooltipFrameLayout.Slice;

import static org.junit.jupiter.api.Assertions.*;

class TooltipFrameLayoutTest {

    @Test
    void tinyFramesShrinkCornersWithoutNegativeOrOverlappingSlices() {
        var frame = new TooltipFrameLayout(0, 0, 7, 5);
        var slices = frame.slices(24, 24, Insets.all(8), true);
        assertEquals(35, slices.stream().mapToInt(slice -> slice.width() * slice.height()).sum());
        for (Slice slice : slices) {
            assertTrue(slice.width() > 0 && slice.height() > 0);
            assertTrue(slice.x() >= 0 && slice.x() + slice.width() <= 7);
            assertTrue(slice.y() >= 0 && slice.y() + slice.height() <= 5);
        }
    }
}
