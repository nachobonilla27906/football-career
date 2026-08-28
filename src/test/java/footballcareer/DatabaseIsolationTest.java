package footballcareer;

import footballcareer.database.Database;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseIsolationTest {

    @Test
    void testsUseAnIsolatedDatabase() throws Exception {
        try (Connection connection = Database.getConnection()) {
            String url = connection.getMetaData().getURL().replace('\\', '/');
            assertTrue(url.endsWith("target/football-career-test.db"),
                    () -> "Tests must not use the player database: " + url);
        }
    }
}
