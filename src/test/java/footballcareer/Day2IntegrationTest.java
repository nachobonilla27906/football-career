package footballcareer;

import footballcareer.database.CareerRepository;
import footballcareer.database.DataSeeder;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Day2IntegrationTest {

    private CareerService careerService;
    private SeasonRepository seasonRepository;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();

        seasonRepository = new SeasonRepository();
        careerService = new CareerService(
                new CareerRepository(),
                new TeamRepository(),
                seasonRepository
        );
    }

    @Test
    void shouldCompleteDay2CareerFlowWithRealData() {
        List<Team> availableTeams =
                careerService.getAvailableTeams();

        assertEquals(15, availableTeams.size());

        Team selectedTeam = availableTeams.stream()
                .filter(team -> "VCF".equals(team.getShortName()))
                .findFirst()
                .orElseThrow();

        Season season = seasonRepository.findFirst();
        assertNotNull(season);

        Career createdCareer = careerService.createCareer(
                "Nacho",
                selectedTeam.getId(),
                season.getId()
        );

        careerService.advanceDay(createdCareer);

        Career loadedCareer =
                careerService.loadCareer(createdCareer.getId());

        assertTrue(loadedCareer.getId() > 0);
        assertEquals("Nacho", loadedCareer.getManagerName());
        assertEquals("Valencia CF", loadedCareer.getControlledTeam().getName());
        assertEquals("VCF", loadedCareer.getControlledTeam().getShortName());
        assertEquals("2026/27", loadedCareer.getCurrentSeason().getName());
        assertEquals(
                LocalDate.of(2026, 8, 16),
                loadedCareer.getCurrentDate()
        );
    }
}
