package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MatchDayServiceTest {
    private MatchRepository matchRepository;
    private LeagueStandingRepository standingRepository;
    private Competition competition;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        Season season = new SeasonRepository().findFirst();
        competition = new CompetitionRepository()
                .findByNameAndSeason("LaLiga", season.getId());

        new FootballWorldService().prepareSeason(season.getId());
        matchRepository = new MatchRepository();
        standingRepository = new LeagueStandingRepository();
    }

    @Test
    void shouldSimulatePersistAndApplyMatchesForDate() {
        Match scheduled = matchRepository
                .findByCompetition(competition.getId()).getFirst();

        MatchDayService service = new MatchDayService(
                matchRepository,
                standingRepository,
                new MatchSimulationService(new Random(42))
        );

        List<Match> processed = service.processMatchesOn(scheduled.getDate());

        assertFalse(processed.isEmpty());
        assertTrue(matchRepository.findById(scheduled.getId()).isPlayed());
        long laLigaMatches = processed.stream()
                .filter(match -> match.getCompetition().getId()
                        == competition.getId())
                .count();

        assertEquals(
                laLigaMatches * 2,
                standingRepository.findByCompetition(competition.getId())
                        .stream().mapToInt(LeagueStanding::getPlayed).sum()
        );

        assertTrue(service.processMatchesOn(scheduled.getDate()).isEmpty());
    }
}
