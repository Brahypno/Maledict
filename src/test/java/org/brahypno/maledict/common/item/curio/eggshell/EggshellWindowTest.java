package org.brahypno.maledict.common.item.curio.eggshell;

import org.brahypno.maledict.common.item.curio.eggshell.EggshellWindow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EggshellWindowTest {
    private static final int DURATION = 40;
    private static final float EPSILON = 1.0E-6F;

    @Test
    void openingBatchIsFullAndStartsWindow() {
        EggshellWindow window = new EggshellWindow();

        assertEquals(10.0F, window.apply(0, 10.0F, DURATION), EPSILON);
        window.land(0, DURATION);

        assertEquals(DURATION, window.windowUntil());
        assertTrue(window.isOpen(DURATION), "The gate reopens exactly when the window ends");
    }

    @Test
    void gradientScalesByElapsedOverDuration() {
        EggshellWindow window = new EggshellWindow();
        assertEquals(10.0F, window.apply(0, 10.0F, DURATION), EPSILON);
        window.land(0, DURATION);

        assertEquals(5.0F, window.apply(20, 10.0F, DURATION), EPSILON);
        assertEquals(7.5F, window.apply(30, 10.0F, DURATION), EPSILON);
        assertEquals(10.0F, window.apply(40, 10.0F, DURATION), EPSILON);
    }

    @Test
    void sameTickHitsUseTheLargestRawAmount() {
        EggshellWindow window = new EggshellWindow();
        assertEquals(10.0F, window.apply(0, 10.0F, DURATION), EPSILON);
        window.land(0, DURATION);

        // 10 + (20 - 10) = 20, not 30.
        assertEquals(10.0F, window.apply(0, 20.0F, DURATION), EPSILON);
    }

    @Test
    void windowBatchAlsoUsesTheLargestRawAmount() {
        EggshellWindow window = new EggshellWindow();
        assertEquals(10.0F, window.apply(0, 10.0F, DURATION), EPSILON);
        window.land(0, DURATION);

        // 10 * 0.5 + (20 - 10) * 0.5 = 10, which is max(10, 20) * 0.5.
        assertEquals(5.0F, window.apply(20, 10.0F, DURATION), EPSILON);
        assertEquals(5.0F, window.apply(20, 20.0F, DURATION), EPSILON);
    }

    @Test
    void smallerSameTickHitIsSuppressed() {
        EggshellWindow window = new EggshellWindow();
        assertEquals(20.0F, window.apply(0, 20.0F, DURATION), EPSILON);
        window.land(0, DURATION);

        assertEquals(0.0F, window.apply(0, 10.0F, DURATION), EPSILON);
    }

    @Test
    void windowHitsDoNotRefreshTheWindow() {
        EggshellWindow window = new EggshellWindow();
        assertEquals(10.0F, window.apply(0, 10.0F, DURATION), EPSILON);
        window.land(0, DURATION);

        assertEquals(5.0F, window.apply(20, 10.0F, DURATION), EPSILON);
        window.land(20, DURATION);

        assertEquals(DURATION, window.windowUntil(), "A window hit must not move the end");
        assertEquals(9.75F, window.apply(39, 10.0F, DURATION), 1.0E-5F);
    }

    @Test
    void aLaterOpeningStartsANewWindow() {
        EggshellWindow window = new EggshellWindow();
        assertEquals(10.0F, window.apply(0, 10.0F, DURATION), EPSILON);
        window.land(0, DURATION);

        assertEquals(10.0F, window.apply(40, 10.0F, DURATION), EPSILON);
        window.land(40, DURATION);

        assertEquals(2L * DURATION, window.windowUntil());
    }

    @Test
    void landFromAnotherTickCannotOpenAStaleBatch() {
        EggshellWindow window = new EggshellWindow();
        assertEquals(10.0F, window.apply(0, 10.0F, DURATION), EPSILON);

        window.land(1, DURATION);

        assertEquals(Long.MIN_VALUE, window.windowUntil());
    }
}
