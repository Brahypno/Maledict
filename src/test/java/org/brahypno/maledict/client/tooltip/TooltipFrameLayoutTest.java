package org.brahypno.maledict.client.tooltip;

import org.junit.jupiter.api.Test;
import org.brahypno.maledict.client.tooltip.TooltipFrameLayout.Insets;
import org.brahypno.maledict.client.tooltip.TooltipFrameLayout.Size;
import org.brahypno.maledict.client.tooltip.TooltipFrameLayout.Slice;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TooltipFrameLayoutTest {
    @Test
    void measuresWrappedTextAndImageComponentsUsingVanillaHeightRules() {
        assertEquals(new Size(50, 8), TooltipFrameLayout.measure(List.of(new Size(50, 10))));
        assertEquals(new Size(96, 50), TooltipFrameLayout.measure(
                List.of(new Size(50, 10), new Size(96, 30), new Size(45, 10))));
    }

    @Test
    void decorationsSurroundVanillaSilhouetteWithoutCoveringItsInterior() {
        var border = Insets.all(8);
        var frame = TooltipFrameLayout.around(100, 50, 80, 20, border);
        assertEquals(new TooltipFrameLayout(88, 38, 104, 44), frame);
        var slices = frame.slices(24, 24, border, false);
        assertEquals(8, slices.size());
        for (Slice slice : slices) {
            assertTrue(slice.x() + slice.width() <= 96 || slice.x() >= 184
                    || slice.y() + slice.height() <= 46 || slice.y() >= 74);
        }
    }

    @Test
    void replacementCoversWholeFrameAndUsesAsymmetricTextureRegions() {
        var border = new Insets(3, 4, 5, 6);
        var frame = TooltipFrameLayout.around(8, 9, 30, 10, border);
        var slices = frame.slices(20, 24, border, true);
        assertEquals(9, slices.size());
        assertEquals(new Slice(1, 1, 3, 4, 0, 0, 3, 4), slices.get(0));
        assertEquals(new Slice(4, 5, 38, 18, 3, 4, 12, 14), slices.get(4));
        assertEquals(new Slice(42, 23, 5, 6, 15, 18, 5, 6), slices.get(8));
        assertEquals(frame.width() * frame.height(),
                slices.stream().mapToInt(slice -> slice.width() * slice.height()).sum());
    }

    @Test
    void tiledEdgesKeepFeatherProportionsAndCropTheLastTile() {
        var frame = new TooltipFrameLayout(0, 0, 103, 83);
        var slices = frame.slices(64, 64, Insets.all(16), false, true);
        var top = slices.stream().filter(slice -> slice.y() == 0 && slice.u() == 16).toList();
        assertEquals(List.of(new Slice(16, 0, 32, 16, 16, 0, 32, 16),
                new Slice(48, 0, 32, 16, 16, 0, 32, 16),
                new Slice(80, 0, 7, 16, 16, 0, 7, 16)), top);
        var left = slices.stream().filter(slice -> slice.x() == 0 && slice.v() == 16).toList();
        assertEquals(List.of(new Slice(0, 16, 16, 32, 0, 16, 16, 32),
                new Slice(0, 48, 16, 19, 0, 16, 16, 19)), left);
        assertEquals(103 * 83 - 71 * 51,
                slices.stream().mapToInt(slice -> slice.width() * slice.height()).sum());
    }

    @Test
    void topCrestStaysCenteredAndTouchesBlackBackgroundWhenTooltipWidthChanges() {
        var outset = new Insets(12, 20, 8, 16);
        var narrow = TooltipFrameLayout.around(100, 80, 100, 40, outset);
        var wide = TooltipFrameLayout.around(100, 80, 200, 40, outset);
        assertEquals(new TooltipFrameLayout(114, 54, 72, 24), narrow.topDecoration(outset, 72, 24, 2));
        assertEquals(new TooltipFrameLayout(164, 54, 72, 24), wide.topDecoration(outset, 72, 24, 2));
    }

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
