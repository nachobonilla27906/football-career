package footballcareer;

import footballcareer.database.CompetitionRepository;
import footballcareer.database.CompetitionTeamRepository;
import footballcareer.database.DataSeeder;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.MatchRepository;
import footballcareer.database.SeasonRepository;
import footballcareer.model.Competition;
import footballcareer.model.Match;
import footballcareer.model.Season;
import footballcareer.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleServiceTest {

    private CompetitionRepository competitionRepository;
    private ScheduleService scheduleService;
    private Season season;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();

        competitionRepository = new CompetitionRepository();
        season = new SeasonRepository().findFirst();
        scheduleService = new ScheduleService(
                new CompetitionTeamRepository(),
                new MatchRepository()
        );
    }

    @Test
    void shouldGenerateDoubleRoundRobinForEvenNumberOfTeams() {
        Competition laLiga = competitionRepository
                .findByNameAndSeason("LaLiga", season.getId());

        List<Match> matches =
                scheduleService.generateLeagueSchedule(laLiga);

        assertEquals(12, matches.size());
        assertValidDoubleRoundRobin(matches, 4);
    }

    @Test
    void shouldGenerateScheduleWithByesForOddNumberOfTeams() {
        Competition serieA = competitionRepository
                .findByNameAndSeason("Serie A", season.getId());

        List<Match> matches =
                scheduleService.generateLeagueSchedule(serieA);

        assertEquals(6, matches.size());
        assertValidDoubleRoundRobin(matches, 3);
    }

    @Test
    void shouldNotGenerateScheduleTwice() {
        Competition laLiga = competitionRepository
                .findByNameAndSeason("LaLiga", season.getId());

        scheduleService.generateLeagueSchedule(laLiga);
        List<Match> secondResult =
                scheduleService.generateLeagueSchedule(laLiga);

        assertEquals(12, secondResult.size());
        assertEquals(
                12,
                new MatchRepository()
                        .findByCompetition(laLiga.getId())
                        .size()
        );
    }

    private void assertValidDoubleRoundRobin(
            List<Match> matches,
            int teamCount
    ) {
        Set<String> directedFixtures = new HashSet<>();

        for (Match match : matches) {
            assertNotEquals(
                    match.getHomeTeam().getId(),
                    match.getAwayTeam().getId()
            );
            assertTrue(match.getDate().isAfter(season.getStartDate()));
            assertTrue(!match.getDate().isAfter(season.getEndDate()));

            String fixture = match.getHomeTeam().getId()
                    + "-" + match.getAwayTeam().getId();
            assertTrue(directedFixtures.add(fixture));
        }

        assertEquals(teamCount * (teamCount - 1), matches.size());
    }
}
