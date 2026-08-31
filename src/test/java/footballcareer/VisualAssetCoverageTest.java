package footballcareer;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualAssetCoverageTest {
    @Test void cinematicBackgroundAndClubCrestsArePackaged() throws Exception {
        Path resources = Path.of("src", "main", "resources");
        assertTrue(Files.size(resources.resolve(
                "assets/backgrounds/stadium-tunnel-beta.png")) > 100_000);
        try (var rows = Files.lines(resources.resolve("data/team_crests.csv"))) {
            assertEquals(95, rows.skip(1).filter(line -> !line.isBlank()).count());
        }
        try (var files = Files.list(resources.resolve("assets/crests"))) {
            assertEquals(190, files.filter(Files::isRegularFile).count());
        }
    }
}
