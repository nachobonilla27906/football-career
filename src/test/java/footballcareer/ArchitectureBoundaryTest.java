package footballcareer;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBoundaryTest {
    @Test
    void careerShellStaysOutsideMainApplicationClass() throws Exception {
        String main = Files.readString(Path.of("src/main/java/footballcareer/Main.java"));
        String shell = Files.readString(Path.of(
                "src/main/java/footballcareer/ui/CareerShellView.java"));
        String dashboard = Files.readString(Path.of(
                "src/main/java/footballcareer/ui/DashboardView.java"));

        assertTrue(main.contains("careerShellView.build("));
        assertFalse(main.contains("private Button areaButton("));
        assertTrue(shell.contains("class CareerShellView"));
        assertTrue(shell.contains("NavigationItem"));
        assertTrue(main.contains("dashboardView.build("));
        assertFalse(main.contains("VBox content = page(\"CENTRO DE MANDO\""));
        assertTrue(dashboard.contains("record Model"));
        String liveMatch = main.substring(main.indexOf("private void showLiveMatch"),
                main.indexOf("private void showSubstitutionOverlay"));
        assertTrue(liveMatch.contains("new IncrementalLiveMatchService().start"));
        assertFalse(liveMatch.contains("new MatchEventRepository"));
        assertFalse(liveMatch.contains("new MatchLineupRepository"));
        assertFalse(liveMatch.contains("new MatchTacticsRepository()"));
        assertTrue(main.contains("retainedScreens.put(\"standings\""));
        assertTrue(main.contains("retainedScreens.put(\"results\""));
        assertTrue(main.lines().count() < 3_950,
                "Main must not regain extracted shell or dashboard code.");
    }
}
