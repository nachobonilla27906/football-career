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

            assertEquals(453, players.getInt(1));

            ResultSet playerTeams = statement.executeQuery(
                    "SELECT COUNT(*) FROM player_team"
            );

            playerTeams.next();

            assertEquals(453, playerTeams.getInt(1));

            assertEquals(28, countCurrentPlayers(statement, "ARS"));
            assertEquals(32, countCurrentPlayers(statement, "LIV"));
            assertEquals(30, countCurrentPlayers(statement, "MCI"));
            assertEquals(32, countCurrentPlayers(statement, "MUN"));
            assertEquals(36, countCurrentPlayers(statement, "RMA"));
            assertEquals(28, countCurrentPlayers(statement, "BAR"));
            assertEquals(36, countCurrentPlayers(statement, "ATM"));
            assertEquals(27, countCurrentPlayers(statement, "VCF"));
            assertEquals(28, countCurrentPlayers(statement, "INT"));
            assertEquals(26, countCurrentPlayers(statement, "JUV"));
            assertEquals(26, countCurrentPlayers(statement, "MIL"));
            assertEquals(32, countCurrentPlayers(statement, "BAY"));
            assertEquals(27, countCurrentPlayers(statement, "BVB"));
            assertEquals(28, countCurrentPlayers(statement, "PSG"));
            assertEquals(37, countCurrentPlayers(statement, "OM"));

            ResultSet competitions = statement.executeQuery(
                    "SELECT COUNT(*) FROM competitions"
            );

            competitions.next();

            assertEquals(
                    5,
                    competitions.getInt(1)
            );

            ResultSet linkedCompetitions = statement.executeQuery(
                    "SELECT COUNT(*) FROM competitions WHERE league_id IS NOT NULL"
            );

            linkedCompetitions.next();

            assertEquals(
                    5,
                    linkedCompetitions.getInt(1)
            );

            ResultSet competitionTeams = statement.executeQuery(
                    "SELECT COUNT(*) FROM competition_teams"
            );

            competitionTeams.next();

            assertEquals(
                    15,
                    competitionTeams.getInt(1)
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

            assertEquals(453, players.getInt(1));

            ResultSet playerTeams = statement.executeQuery(
                    "SELECT COUNT(*) FROM player_team"
            );

            playerTeams.next();

            assertEquals(453, playerTeams.getInt(1));

            ResultSet competitions = statement.executeQuery(
                    "SELECT COUNT(*) FROM competitions"
            );

            competitions.next();

            assertEquals(
                    5,
                    competitions.getInt(1)
            );

            ResultSet competitionTeams = statement.executeQuery(
                    "SELECT COUNT(*) FROM competition_teams"
            );

            competitionTeams.next();

            assertEquals(
                    15,
                    competitionTeams.getInt(1)
            );
        }
    }

    private int countCurrentPlayers(
            Statement statement,
            String teamShortName
    ) throws Exception {
        ResultSet resultSet = statement.executeQuery("""
                SELECT COUNT(*)
                FROM player_team pt
                JOIN teams t ON pt.team_id = t.id
                WHERE t.short_name = '%s'
                  AND pt.end_date IS NULL
                """.formatted(teamShortName));
        resultSet.next();
        return resultSet.getInt(1);
    }
}
