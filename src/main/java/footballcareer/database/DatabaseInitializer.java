package footballcareer.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {

        try (Connection connection = Database.getConnection();
             InputStream inputStream = DatabaseInitializer.class
                     .getClassLoader()
                     .getResourceAsStream("schema.sql")) {

            if (inputStream == null) {
                throw new RuntimeException(
                        "Could not find schema.sql."
                );
            }

            String schema = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            try (Statement statement = connection.createStatement()) {

                statement.execute("PRAGMA foreign_keys = OFF");

                for (String sql : schema.split(";")) {

                    String trimmedSql = sql.trim();

                    if (!trimmedSql.isEmpty()) {
                        statement.execute(trimmedSql);
                    }
                }

                statement.execute("PRAGMA foreign_keys = ON");
            }

            System.out.println("Database initialized successfully.");

        } catch (IOException | SQLException e) {
            throw new RuntimeException(
                    "Could not initialize database.",
                    e
            );
        }
    }

    public static void resetForTests() {

        String[] tables = {
                "careers",
                "transfers",
                "transfer_offers",
                "contracts",
                "player_state",
                "club_finances",
                "player_market_status",
                "league_standings",
                "player_season_stats",
                "match_team_stats",
                "match_events",
                "matches",
                "player_team",
                "competition_teams",
                "competitions",
                "players",
                "teams",
                "leagues",
                "seasons"
        };

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = OFF");

            for (String table : tables) {
                statement.executeUpdate(
                        "DROP TABLE IF EXISTS " + table
                );
            }

            statement.execute("PRAGMA foreign_keys = ON");

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not reset database for tests.",
                    e
            );
        }

        initialize();
    }
}
