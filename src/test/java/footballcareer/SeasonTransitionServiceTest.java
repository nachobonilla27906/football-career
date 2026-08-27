package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.CareerService;
import footballcareer.service.FootballWorldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeasonTransitionServiceTest {
    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
    }

    @Test
    void shouldCloseSeasonAndPrepareTheNextOne() {
        SeasonRepository seasons = new SeasonRepository();
        Season current = seasons.findFirst();
        new FootballWorldService().prepareSeason(current.getId());
        Team team = new TeamRepository().findByShortName("VCF");
        CareerRepository careers = new CareerRepository();
        CareerService service = new CareerService(careers,
                new TeamRepository(), seasons);
        Career career = service.createCareer("Nacho", team.getId(), current.getId());
        career.setCurrentDate(current.getEndDate());
        careers.updateCurrentDate(career);
        int oldCompetitionCount = new CompetitionRepository()
                .findBySeason(current.getId()).size();

        service.advanceDay(career);

        Season next = career.getCurrentSeason();
        assertTrue(seasons.findById(current.getId()).isFinished());
        assertEquals(current.getStartYear() + 1, next.getStartYear());
        assertEquals(next.getStartDate(), career.getCurrentDate());
        assertEquals(oldCompetitionCount,
                new CompetitionRepository().findBySeason(next.getId()).size());
        assertFalse(new MatchRepository().findByCompetition(
                new CompetitionRepository().findBySeason(next.getId()).getFirst().getId())
                .isEmpty());
        Career reloaded = service.loadCareer(career.getId());
        assertEquals(next.getId(), reloaded.getCurrentSeason().getId());
        assertEquals(next.getStartDate(), reloaded.getCurrentDate());
    }
}
