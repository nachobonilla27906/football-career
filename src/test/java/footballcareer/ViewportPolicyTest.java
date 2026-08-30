package footballcareer;

import footballcareer.ui.ViewportPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewportPolicyTest {
    @Test
    void commonLaptopAndWindowedSizesUseCompactChrome() {
        assertTrue(ViewportPolicy.compact(1280, 720));
        assertTrue(ViewportPolicy.compact(1180, 760));
        assertTrue(ViewportPolicy.contentHeight(720) >= 560);
        assertTrue(ViewportPolicy.contentHeight(600) >= 460);
        assertFalse(ViewportPolicy.compact(1920, 1080));
    }

    @Test
    void everyCenteredScreenAndOverlayFitsLaptopViewport() {
        assertEquals(1248, ViewportPolicy.centeredContentWidth(1280));
        assertTrue(ViewportPolicy.overlayWidth(1280) <= 620);
        assertTrue(ViewportPolicy.centeredContentWidth(800) <= 768);
        assertTrue(ViewportPolicy.overlayWidth(800)
                < ViewportPolicy.centeredContentWidth(800));
        assertEquals(4, ViewportPolicy.dashboardColumns(1280));
        assertEquals(2, ViewportPolicy.dashboardColumns(900));
    }
}
