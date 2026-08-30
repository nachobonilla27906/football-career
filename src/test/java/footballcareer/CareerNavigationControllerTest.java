package footballcareer;

import footballcareer.ui.CareerNavigationController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CareerNavigationControllerTest {
    @Test
    void mapsAreasAndSelectsTransferTabsWithoutJavaFx() {
        CareerNavigationController navigation = new CareerNavigationController();
        assertEquals("squad", navigation.areaFor("medical"));
        assertEquals("transfers", navigation.areaFor("history"));
        assertEquals("central", navigation.areaFor("calendar"));
        assertTrue(navigation.isSelected("incoming", "market", 3));
        assertFalse(navigation.isSelected("sales", "market", 3));
        assertTrue(navigation.isSelected("office", "office", 0));
    }

    @Test
    void matchReportReturnsToItsRealOriginAndRejectsTransientScreens() {
        CareerNavigationController navigation = new CareerNavigationController();
        assertEquals("results", navigation.reportReturnSection("results"));
        assertEquals("RESULTADOS", navigation.sectionLabel("results"));
        assertEquals("dashboard", navigation.reportReturnSection("dashboard"));
        assertEquals("calendar", navigation.reportReturnSection("lineup"));
        assertEquals("CALENDARIO", navigation.sectionLabel("lineup"));
    }
}
