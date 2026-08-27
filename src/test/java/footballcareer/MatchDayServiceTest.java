package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import footballcareer.model.enums.MatchEventType;

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
        Match played = matchRepository.findById(scheduled.getId());
        var events = new MatchEventRepository().findByMatch(played.getId());
        assertEquals(played.getHomeGoals() + played.getAwayGoals(),
                events.stream().filter(event ->
                        event.getType() == MatchEventType.GOAL).count());
        PlayerTeamRepository playerTeams = new PlayerTeamRepository();
        assertTrue(events.stream().allMatch(event -> {
            Long playerTeam = playerTeams.findCurrentTeamId(event.getPlayer().getId());
            return playerTeam != null && playerTeam == event.getTeam().getId();
        }));
        MatchTeamStatsRepository matchStats = new MatchTeamStatsRepository();
        MatchTeamStats homeStats = matchStats.find(played.getId(),
                played.getHomeTeam().getId());
        MatchTeamStats awayStats = matchStats.find(played.getId(),
                played.getAwayTeam().getId());
        assertNotNull(homeStats);
        assertNotNull(awayStats);
        assertEquals(100, homeStats.getPossession() + awayStats.getPossession());
        assertTrue(homeStats.getShotsOnTarget() <= homeStats.getShots());
        assertTrue(awayStats.getShotsOnTarget() <= awayStats.getShots());
        assertTrue(homeStats.getShotsOnTarget() >= played.getHomeGoals());
        assertTrue(awayStats.getShotsOnTarget() >= played.getAwayGoals());
        MatchReport report = new MatchReportService().build(played.getId());
        assertEquals(played.getId(), report.getMatch().getId());
        assertEquals(events.size(), report.getEvents().size());
        assertNotNull(report.getHomeStats());
        assertNotNull(report.getAwayStats());
        assertNotNull(report.getPlayerOfTheMatch());
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
