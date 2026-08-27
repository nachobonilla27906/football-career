package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class Day7IntegrationTest {
    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
    }

    @Test
    void shouldEvolveWorldAndStartANewSeason() {
        SeasonRepository seasons = new SeasonRepository();
        Season firstSeason = seasons.findFirst();
        new FootballWorldService().prepareSeason(firstSeason.getId());
        Team controlled = new TeamRepository().findByShortName("VCF");
        PlayerRepository players = new PlayerRepository();
        Set<Long> originalSquad = players.findCurrentPlayersByTeam(controlled.getId())
                .stream().map(Player::getId).collect(Collectors.toSet());
        PlayerStateRepository states = new PlayerStateRepository();
        LineupService lineups = new LineupService(players, states);
        MatchEventRepository events = new MatchEventRepository();
        MatchDayService matchDays = new MatchDayService(
                new MatchRepository(), new LeagueStandingRepository(),
                new MatchSimulationService(new Random(70), lineups, states),
                new PlayerMatchService(lineups,
                        new PlayerSeasonStatsRepository(), states, events),
                new MatchEventGenerationService(lineups, events, new Random(71)),
                new MatchStatisticsService(events,
                        new MatchTeamStatsRepository(), new Random(72)));
        CareerRepository careers = new CareerRepository();
        CareerService service = new CareerService(careers,
                new TeamRepository(), seasons, matchDays);
        Career career = service.createCareer("Nacho", controlled.getId(),
                firstSeason.getId());

        service.advanceDays(career, 10);

        assertEquals(firstSeason.getStartDate().plusDays(10), career.getCurrentDate());
        assertEquals(originalSquad, players.findCurrentPlayersByTeam(controlled.getId())
                .stream().map(Player::getId).collect(Collectors.toSet()));
        assertTrue(new CompetitionRepository().findBySeason(firstSeason.getId())
                .stream().flatMap(competition -> new MatchRepository()
                        .findByCompetition(competition.getId()).stream())
                .anyMatch(Match::isPlayed));

        career.setCurrentDate(firstSeason.getEndDate());
        careers.updateCurrentDate(career);
        service.advanceDay(career);

        assertTrue(seasons.findById(firstSeason.getId()).isFinished());
        assertEquals(firstSeason.getStartYear() + 1,
                career.getCurrentSeason().getStartYear());
        assertFalse(new CompetitionRepository()
                .findBySeason(career.getCurrentSeason().getId()).isEmpty());
    }
}
