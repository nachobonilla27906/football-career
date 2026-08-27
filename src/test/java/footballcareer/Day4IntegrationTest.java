package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import org.junit.jupiter.api.Test;

import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Day4IntegrationTest {
    @Test
    void shouldUsePlayersAndUpdateTheirSeasonState() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        MatchRepository matches = new MatchRepository();
        Competition competition = new CompetitionRepository()
                .findByNameAndSeason("LaLiga", season.getId());
        Match match = matches.findByCompetition(competition.getId()).getFirst();
        PlayerStateRepository states = new PlayerStateRepository();
        PlayerSeasonStatsRepository stats = new PlayerSeasonStatsRepository();
        LineupService lineups = new LineupService(new PlayerRepository(), states);
        Player tracked = lineups.selectStartingEleven(match.getHomeTeam().getId()).getFirst();

        new MatchDayService(matches, new LeagueStandingRepository(),
                new MatchSimulationService(new Random(4)),
                new PlayerMatchService(lineups, stats, states))
                .processMatchesOn(match.getDate());

        assertEquals(1, stats.find(tracked.getId(), season.getId()).getAppearances());
        assertEquals(90, stats.find(tracked.getId(), season.getId()).getMinutes());
        assertEquals(90, states.findByPlayer(tracked.getId()).getFitness());
    }
}
