package footballcareer;

import footballcareer.service.AppDiagnostics;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppDiagnosticsTest {
    @Test
    void unexpectedErrorsAreKeptInMemoryAndWrittenToLog() throws Exception {
        Path log = Path.of("target", "diagnostics-test.log");
        System.setProperty("footballcareer.log.path", log.toString());
        try {
            AppDiagnostics.clearMemory();
            AppDiagnostics.record(new IllegalStateException("diagnostic failure"), "test-flow");

            assertTrue(AppDiagnostics.recent().stream().anyMatch(entry ->
                    entry.context().equals("test-flow")
                            && entry.message().equals("diagnostic failure")));
            assertTrue(Files.readString(log).contains("IllegalStateException"));
        } finally {
            System.clearProperty("footballcareer.log.path");
            AppDiagnostics.clearMemory();
        }
    }
}
