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

            int tableCount = 0;

            while (resultSet.next()) {
                tableCount++;
            }

            assertEquals(17, tableCount);
        }
    }
}
