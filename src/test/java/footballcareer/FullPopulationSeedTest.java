package footballcareer;

import footballcareer.database.Database;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.DataSeeder;
import footballcareer.database.SeasonRepository;
import footballcareer.service.FootballWorldService;
import footballcareer.service.CareerService;
import footballcareer.service.EuropeanCompetitionService;
import footballcareer.database.CareerRepository;
import footballcareer.database.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullPopulationSeedTest {
    @Test
    @EnabledIfSystemProperty(named = "footballcareer.verifyFullPopulation", matches = "true")
    void importsCompletePopulationIntoIsolatedSqliteDatabase() throws Exception {
        Path database = Path.of("target", "football-career-test.db");
        Path template = Path.of("target", "football-career-seeded-template.db");
        Files.deleteIfExists(database); Files.deleteIfExists(template);
        System.setProperty("footballcareer.seed.compact", "false");
        try {
            DatabaseInitializer.resetForTests();
            System.setProperty("footballcareer.seed.compact", "true");
            DataSeeder.seed();
            System.setProperty("footballcareer.seed.compact", "false");
            DataSeeder.seed();
            new FootballWorldService().prepareSeason(new SeasonRepository().findFirst().getId());
            try (var connection = Database.getConnection(); var statement = connection.createStatement()) {
                assertEquals(96, scalar(statement.executeQuery(
                        "SELECT COUNT(DISTINCT team_id) FROM competition_teams")));
                var available = new CareerService(new CareerRepository(), new TeamRepository(),
                        new SeasonRepository()).getAvailableTeams();
                assertEquals(96, available.size());
                assertEquals(96, available.stream().map(team -> team.getName().toLowerCase())
                        .distinct().count());
                assertEquals(1, scalar(statement.executeQuery("""
                        SELECT COUNT(*) FROM competition_teams ct
                        JOIN teams t ON t.id = ct.team_id
                        WHERE t.name = 'Valencia CF'
                        """)));
                assertTrue(scalar(statement.executeQuery("SELECT COUNT(*) FROM players")) >= 2_600);
                assertEquals(2_040, scalar(statement.executeQuery("SELECT COUNT(*) FROM matches")));
                assertEquals(3, scalar(statement.executeQuery(
                        "SELECT COUNT(*) FROM competitions WHERE format = 'EUROPEAN'")));
                assertEquals(72, scalar(statement.executeQuery("""
                        SELECT COUNT(*) FROM (
                          SELECT competition_id FROM competition_teams
                          WHERE competition_id IN (SELECT id FROM competitions WHERE format = 'EUROPEAN')
                          GROUP BY competition_id, team_id
                        )
                        """)));
                assertEquals(288, scalar(statement.executeQuery(
                        "SELECT COUNT(*) FROM matches WHERE stage = 'LEAGUE_PHASE'")));
                assertEquals(0, scalar(statement.executeQuery("""
                        SELECT COUNT(*) FROM matches m JOIN competitions c ON c.id=m.competition_id
                        WHERE c.format='EUROPEAN' AND strftime('%w', m.date) <> '3'
                        """)));
                assertEquals(0, scalar(statement.executeQuery("""
                        SELECT COUNT(*) FROM (
                          SELECT pt.team_id FROM player_team pt
                          WHERE pt.end_date IS NULL GROUP BY pt.team_id HAVING COUNT(*) < 18
                        )
                        """)));
                assertEquals(1, scalar(statement.executeQuery("""
                        SELECT COUNT(*) FROM player_team pt
                        JOIN players p ON p.id = pt.player_id
                        WHERE pt.end_date IS NULL AND p.first_name = 'Kylian'
                          AND p.last_name LIKE 'Mbapp%'
                        """)));
                try (ResultSet keepers = statement.executeQuery("""
                        SELECT p.last_name, p.overall FROM players p
                        JOIN player_team pt ON pt.player_id = p.id AND pt.end_date IS NULL
                        JOIN teams t ON t.id = pt.team_id
                        WHERE t.short_name = 'RMA' AND p.position = 'GK'
                        ORDER BY p.overall DESC
                        """)) {
                    assertTrue(keepers.next());
                    assertTrue(keepers.getString(1).contains("Courtois"));
                    assertEquals(89, keepers.getInt(2));
                }

                var careerService = new CareerService(new CareerRepository(),
                        new TeamRepository(), new SeasonRepository());
                var career = careerService.createCareer("UEFA test", available.getFirst().getId(),
                        new SeasonRepository().findFirst().getId());
                statement.executeUpdate("""
                        UPDATE career_match_states SET played=1, home_goals=1, away_goals=0
                        WHERE career_id=%d AND match_id IN
                          (SELECT id FROM matches WHERE stage='LEAGUE_PHASE')
                        """.formatted(career.getId()));
                new EuropeanCompetitionService().progress(career.getCurrentSeason().getId(),
                        java.time.LocalDate.of(2027, 2, 3));
                assertEquals(24, scalar(statement.executeQuery(
                        "SELECT COUNT(*) FROM european_ties WHERE stage='ROUND_OF_16'")));
                assertEquals(24, scalar(statement.executeQuery(
                        "SELECT COUNT(*) FROM matches WHERE stage='ROUND_OF_16' AND career_id IS NOT NULL")));
            }
        } finally {
            System.setProperty("footballcareer.seed.compact", "true");
            Files.deleteIfExists(database); Files.deleteIfExists(template);
        }
    }

    private int scalar(ResultSet resultSet) throws Exception {
        try (resultSet) { resultSet.next(); return resultSet.getInt(1); }
    }
}
