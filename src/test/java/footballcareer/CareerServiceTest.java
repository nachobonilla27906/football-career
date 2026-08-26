package footballcareer;

import footballcareer.database.CareerRepository;

import footballcareer.database.DatabaseInitializer;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CareerServiceTest {

    private CareerService careerService;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.initialize();

        careerService = new CareerService(
                new CareerRepository(),
                new TeamRepository(),
                new SeasonRepository()
        );
    }

    @Test
    void shouldCreateAndLoadCareer() {

        Team team = new Team();

        team.setName("Valencia CF");
        team.setShortName("VCF");
        team.setCountry("Spain");
        team.setStadiumName("Mestalla");
        team.setStadiumCapacity(49430);
        team.setReputation(80);

        new TeamRepository().save(team);

        Season season = new Season();

        season.setStartYear(2026);
        season.setEndYear(2027);
        season.setStartDate(LocalDate.of(2026, 8, 15));
        season.setEndDate(LocalDate.of(2027, 5, 30));
        season.setFinished(false);

        new SeasonRepository().save(season);

        Career career = careerService.createCareer(
                "Nacho",
                team.getId(),
                season.getId()
        );

        assertNotNull(career);
        assertTrue(career.getId() > 0);
        assertEquals("Nacho", career.getManagerName());
        assertEquals(team.getId(), career.getControlledTeam().getId());
        assertEquals(season.getId(), career.getCurrentSeason().getId());
        assertEquals(
                LocalDate.of(2026, 8, 15),
                career.getCurrentDate()
        );

        Career loadedCareer =
                careerService.loadCareer(career.getId());

        assertNotNull(loadedCareer);
        assertEquals(career.getId(), loadedCareer.getId());
        assertEquals("Nacho", loadedCareer.getManagerName());
    }

    @Test
    void shouldAdvanceCareerOneDay() {

        Career career = new Career();

        career.setCurrentDate(
                LocalDate.of(2026, 8, 15)
        );

        careerService.advanceDay(career);

        assertEquals(
                LocalDate.of(2026, 8, 16),
                career.getCurrentDate()
        );
    }
}