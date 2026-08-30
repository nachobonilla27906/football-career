package footballcareer;

import footballcareer.database.Database;
import footballcareer.database.DatabaseInitializer;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    @Test
    void shouldInitializeDatabase() throws SQLException {

        DatabaseInitializer.initialize();

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master " +
                     "WHERE type='table' " +
                     "AND name NOT LIKE 'sqlite_%' " +
                     "ORDER BY name"
             )) {

            java.util.Set<String> tables = new java.util.HashSet<>();

            while (resultSet.next()) {
                tables.add(resultSet.getString("name"));
            }

            assertTrue(tables.containsAll(java.util.Set.of("players", "teams", "careers",
                    "matches", "career_match_events", "career_team_sheets",
                    "career_player_progress_history")));
            assertTrue(tables.size() >= 50, "The complete career schema must be initialized.");
        }
    }
}
