package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class Day3IntegrationTest {
    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
    }

    @Test
    void shouldAdvanceCareerAndSimulateFootballWorld() {
        Season season = new SeasonRepository().findFirst();
        Team team = new TeamRepository().findByShortName("VCF");
        new FootballWorldService().prepareSeason(season.getId());

        MatchRepository matchRepository = new MatchRepository();
        CareerService careerService = new CareerService(
                new CareerRepository(), new TeamRepository(),
                new SeasonRepository(),
                new MatchDayService(
                        matchRepository,
                        new LeagueStandingRepository(),
                        new MatchSimulationService(new Random(7))
                )
        );
        Career career = careerService.createCareer(
                "Nacho", team.getId(), season.getId()
        );

        for (int day = 0; day < 7; day++) {
            careerService.advanceDay(career);
        }

        assertEquals(season.getStartDate().plusWeeks(1), career.getCurrentDate());
        assertFalse(matchRepository.findByDate(career.getCurrentDate()).isEmpty());
        assertTrue(matchRepository.findByDate(career.getCurrentDate())
                .stream().allMatch(Match::isPlayed));

        Competition laLiga = new CompetitionRepository()
                .findByNameAndSeason("LaLiga", season.getId());
        assertTrue(new LeagueStandingRepository()
                .findByCompetition(laLiga.getId())
                .stream().anyMatch(standing -> standing.getPlayed() > 0));

        Career reloaded = careerService.loadCareer(career.getId());
        assertEquals(career.getCurrentDate(), reloaded.getCurrentDate());
        assertEquals("Valencia CF", reloaded.getControlledTeam().getName());
    }
}
