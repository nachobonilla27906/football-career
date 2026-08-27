package footballcareer;

import footballcareer.database.CareerRepository;
import footballcareer.database.Database;
import footballcareer.database.DatabaseInitializer;

import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CareerRepositoryTest {

    private CareerRepository careerRepository;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        careerRepository = new CareerRepository();
    }

    @Test
    void shouldSaveAndFindCareer() throws Exception {

        long teamId;
        long seasonId;

        try (Connection connection = Database.getConnection()) {

            String teamSql = """
                    INSERT INTO teams (
                        name,
                        short_name,
                        country,
                        stadium_name,
                        stadium_capacity,
                        reputation
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement statement = connection.prepareStatement(
                    teamSql,
                    java.sql.Statement.RETURN_GENERATED_KEYS
            )) {

                statement.setString(1, "Valencia CF");
                statement.setString(2, "VCF");
                statement.setString(3, "Spain");
                statement.setString(4, "Mestalla");
                statement.setInt(5, 49430);
                statement.setInt(6, 80);

                statement.executeUpdate();

                try (var keys = statement.getGeneratedKeys()) {
                    keys.next();
                    teamId = keys.getLong(1);
                }
            }

            String seasonSql = """
                    INSERT INTO seasons (
                        start_year,
                        end_year,
                        start_date,
                        end_date,
                        finished
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement statement = connection.prepareStatement(
                    seasonSql,
                    java.sql.Statement.RETURN_GENERATED_KEYS
            )) {

                statement.setInt(1, 2026);
                statement.setInt(2, 2027);
                statement.setString(3, "2026-08-15");
                statement.setString(4, "2027-05-30");
                statement.setInt(5, 0);

                statement.executeUpdate();

                try (var keys = statement.getGeneratedKeys()) {
                    keys.next();
                    seasonId = keys.getLong(1);
                }
            }
        }

        Team team = new Team();
        team.setId(teamId);

        Season season = new Season();
        season.setId(seasonId);

        Career career = new Career(
                0,
                "Nacho",
                team,
                season,
                LocalDate.of(2026, 8, 15)
        );

        careerRepository.save(career);

        assertTrue(career.getId() > 0);

        Career loadedCareer =
                careerRepository.findById(career.getId());

        assertNotNull(loadedCareer);
        assertEquals(career.getId(), loadedCareer.getId());
        assertEquals("Nacho", loadedCareer.getManagerName());
        assertEquals(teamId, loadedCareer.getControlledTeam().getId());
        assertEquals(seasonId, loadedCareer.getCurrentSeason().getId());
        assertEquals(
                LocalDate.of(2026, 8, 15),
                loadedCareer.getCurrentDate()
        );
    }
}