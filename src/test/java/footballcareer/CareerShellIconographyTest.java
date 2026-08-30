package footballcareer;

import footballcareer.ui.CareerShellView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CareerShellIconographyTest {
    @Test
    void navigationUsesConsistentIconsWithoutLosingCounters() {
        assertEquals("⌂  CENTRAL", CareerShellView.navigationLabel("CENTRAL"));
        assertEquals("⇄  TRASPASOS  //  3",
                CareerShellView.navigationLabel("TRASPASOS  //  3"));
        assertEquals("⚙  PERSONALIZAR",
                CareerShellView.navigationLabel("PERSONALIZAR"));
        assertTrue(CareerShellView.navigationLabel("CALENDARIO").endsWith("CALENDARIO"));
    }
}
