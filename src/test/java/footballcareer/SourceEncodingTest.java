package footballcareer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceEncodingTest {
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "java", "css", "csv", "md", "sql", "properties", "xml", "json", "txt", "ps1");

    @Test
    void sourceFilesAreUtf8WithoutMojibakeMarkers() throws Exception {
        List<String> broken = new ArrayList<>();
        for (Path root : List.of(Path.of("src", "main"), Path.of("src", "test"),
                Path.of("docs"))) {
            if (!Files.exists(root)) continue;
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(Files::isRegularFile)
                        .filter(SourceEncodingTest::isTextFile)
                        .toList()) {
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

    private static boolean isTextFile(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && TEXT_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }
}
