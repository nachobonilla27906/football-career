package footballcareer;

import footballcareer.ui.UiTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UiThemeResourcesTest {
    @Test
    void packagedThemeContainsFontsAndThreeOrderedStylesheets() {
        for (String file : new String[] {"Barlow-Regular.ttf", "Barlow-Medium.ttf",
                "BarlowCondensed-SemiBold.ttf", "BarlowCondensed-Black.ttf", "OFL-Barlow.txt"})
            assertNotNull(getClass().getResource("/assets/fonts/" + file), file);
        assertEquals(3, UiTheme.stylesheets().size());
        assertEquals(true, UiTheme.stylesheets().getFirst().endsWith("tokens.css"));
        assertEquals(true, UiTheme.stylesheets().get(1).endsWith("screens.css"));
        assertEquals(true, UiTheme.stylesheets().getLast().endsWith("foundation.css"));
    }
}
