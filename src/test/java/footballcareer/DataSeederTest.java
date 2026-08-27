package footballcareer;

import footballcareer.database.DataSeeder;
import footballcareer.database.Database;
import footballcareer.database.DatabaseInitializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataSeederTest {

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
    }

    @Test
    void shouldPopulateInitialFootballData() throws Exception {

        DataSeeder.seed();

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet seasons = statement.executeQuery(
                    "SELECT COUNT(*) FROM seasons"
            );

            seasons.next();

            assertEquals(
                    1,
                    seasons.getInt(1)
            );

            ResultSet leagues = statement.executeQuery(
                    "SELECT COUNT(*) FROM leagues"
            );

            leagues.next();

            assertEquals(
                    5,
                    leagues.getInt(1)
            );

            ResultSet teams = statement.executeQuery(
                    "SELECT COUNT(*) FROM teams"
            );

            teams.next();

            assertEquals(
                    15,
                    teams.getInt(1)
            );

            ResultSet players = statement.executeQuery(
                    "SELECT COUNT(*) FROM players"
            );

            players.next();

            assertEquals(
                    6,
                    players.getInt(1)
            );

            ResultSet playerTeams = statement.executeQuery(
                    "SELECT COUNT(*) FROM player_team"
            );

            playerTeams.next();

            assertEquals(
                    6,
                    playerTeams.getInt(1)
            );
        }
    }

    @Test
    void shouldNotDuplicateInitialData() throws Exception {

        DataSeeder.seed();
        DataSeeder.seed();

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet seasons = statement.executeQuery(
                    "SELECT COUNT(*) FROM seasons"
            );

            seasons.next();

            assertEquals(
                    1,
                    seasons.getInt(1)
            );

            ResultSet leagues = statement.executeQuery(
                    "SELECT COUNT(*) FROM leagues"
            );

            leagues.next();

            assertEquals(
                    5,
                    leagues.getInt(1)
            );

            ResultSet teams = statement.executeQuery(
                    "SELECT COUNT(*) FROM teams"
            );

            teams.next();

            assertEquals(
                    15,
                    teams.getInt(1)
            );

            ResultSet players = statement.executeQuery(
                    "SELECT COUNT(*) FROM players"
            );

            players.next();

            assertEquals(
                    6,
                    players.getInt(1)
            );

            ResultSet playerTeams = statement.executeQuery(
                    "SELECT COUNT(*) FROM player_team"
            );

            playerTeams.next();

            assertEquals(
                    6,
                    playerTeams.getInt(1)
            );
        }
    }
}