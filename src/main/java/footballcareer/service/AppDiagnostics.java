package footballcareer.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class AppDiagnostics {
    public record Entry(LocalDateTime time, String thread, String context,
                        String type, String message) {}

    private static final int MAX_ENTRIES = 50;
    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    private AppDiagnostics() {}

    public static synchronized void record(Throwable error, String context) {
        if (error == null) return;
        Entry entry = new Entry(LocalDateTime.now(), Thread.currentThread().getName(),
                context == null ? "unknown" : context, error.getClass().getSimpleName(),
                error.getMessage() == null ? "Sin mensaje" : error.getMessage());
        ENTRIES.addFirst(entry);
        while (ENTRIES.size() > MAX_ENTRIES) ENTRIES.removeLast();
        writeLog(entry, error);
    }

    public static synchronized List<Entry> recent() {
        return new ArrayList<>(ENTRIES);
    }

    public static synchronized void clearMemory() { ENTRIES.clear(); }

    public static Path logPath() {
        return Path.of(System.getProperty("footballcareer.log.path",
                Path.of("logs", "football-career.log").toString())).toAbsolutePath();
    }

    private static void writeLog(Entry entry, Throwable error) {
        try {
            Path path = logPath();
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            StringWriter stack = new StringWriter();
            error.printStackTrace(new PrintWriter(stack));
            String block = "[" + entry.time() + "] [" + entry.thread() + "] ["
                    + entry.context() + "] " + entry.type() + ": " + entry.message()
                    + System.lineSeparator() + stack + System.lineSeparator();
            Files.writeString(path, block, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Diagnostics must never crash the application while reporting another failure.
        }
    }
}
