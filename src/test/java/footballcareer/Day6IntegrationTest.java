package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.MatchEventType;
import footballcareer.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class Day6IntegrationTest {
    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
    }

    @Test
    void shouldProcessACompleteMatchAndBuildItsReport() {
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Competition competition = new CompetitionRepository()
                .findByNameAndSeason("LaLiga", season.getId());
        MatchRepository matches = new MatchRepository();
        Match scheduled = matches.findByCompetition(competition.getId()).getFirst();
        PlayerStateRepository states = new PlayerStateRepository();
        LineupService lineups = new LineupService(new PlayerRepository(), states);
        MatchEventRepository events = new MatchEventRepository();
        MatchTeamStatsRepository teamStats = new MatchTeamStatsRepository();
        Player trackedStarter = lineups.selectStartingEleven(
                scheduled.getHomeTeam().getId()).getFirst();
        MatchDayService matchDay = new MatchDayService(
                matches,
                new LeagueStandingRepository(),
                new MatchSimulationService(new Random(6), lineups, states),
                new PlayerMatchService(lineups,
                        new PlayerSeasonStatsRepository(), states, events),
                new MatchEventGenerationService(lineups, events, new Random(7)),
                new MatchStatisticsService(events, teamStats, new Random(8))
        );

        List<Match> processed = matchDay.processMatchesOn(scheduled.getDate());
        Match played = matches.findById(scheduled.getId());
        MatchReport report = new MatchReportService().build(played.getId());

        assertFalse(processed.isEmpty());
        assertTrue(played.isPlayed());
        assertEquals(played.getHomeGoals() + played.getAwayGoals(),
                report.getEvents().stream()
                        .filter(event -> event.getType() == MatchEventType.GOAL).count());
        assertEquals(100, report.getHomeStats().getPossession()
                + report.getAwayStats().getPossession());
        assertNotNull(report.getPlayerOfTheMatch());
        PlayerSeasonStats playerStats = new PlayerSeasonStatsRepository()
                .find(trackedStarter.getId(), season.getId());
        assertNotNull(playerStats);
        assertTrue(playerStats.getAppearances() > 0);
        assertTrue(matchDay.processMatchesOn(scheduled.getDate()).isEmpty());
    }
}
