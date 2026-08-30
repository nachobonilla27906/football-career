package footballcareer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceEncodingTest {
    @Test
    void sourceFilesAreUtf8WithoutMojibakeMarkers() throws Exception {
        List<String> broken = new ArrayList<>();
        for (Path root : List.of(Path.of("src", "main"), Path.of("src", "test"),
                Path.of("docs"))) {
            if (!Files.exists(root)) continue;
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    String text = Files.readString(path, StandardCharsets.UTF_8);
                    if (text.contains("\u00c3") || text.contains("\u00c2")
                            || text.contains("\u00e2\u0080")
                            || text.contains("\u00e2\u20ac")
                            || text.contains("\u00e2\u201a")) {
                        broken.add(path.toString());
                    }
                }
            }
        }
        assertTrue(broken.isEmpty(), "Possible mojibake in: " + broken);
    }
}
