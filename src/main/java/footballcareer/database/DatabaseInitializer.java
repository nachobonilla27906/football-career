package footballcareer.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseInitializer {

    public static void initialize() {

        String schema = loadSchema();

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("PRAGMA foreign_keys = ON;");

            for (String sql : schema.split(";")) {

                String command = sql.trim();

                if (!command.isEmpty()
                        && !command.equalsIgnoreCase("PRAGMA foreign_keys = ON")) {

                    statement.executeUpdate(
                            command.replaceFirst(
                                    "CREATE TABLE ",
                                    "CREATE TABLE IF NOT EXISTS "
                            )
                    );
                }
            }

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not initialize database.",
                    e
            );
        }
    }

    private static String loadSchema() {

        try (InputStream inputStream =
                     DatabaseInitializer.class
                             .getClassLoader()
                             .getResourceAsStream("schema.sql")) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "schema.sql not found in resources."
                );
            }

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         inputStream,
                                         StandardCharsets.UTF_8))) {

                return reader.lines()
                        .collect(Collectors.joining("\n"));
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not read schema.sql.",
                    e
            );
        }
    }
}