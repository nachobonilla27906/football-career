package footballcareer;

import footballcareer.database.CompetitionTeamRepository;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.MatchRepository;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.FootballWorldService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Day10CalendarRepairTest {

    @Test
    void liverpoolAlwaysHasAnUpcomingFixtureAfterWorldPreparation() {
        DatabaseInitializer.resetAndSeedForTests();
        Season season = new SeasonRepository().findFirst();

        new FootballWorldService().prepareSeason(season.getId());

        Team liverpool = new TeamRepository().findByShortName("LIV");
        MatchRepository matches = new MatchRepository();
        boolean hasUpcomingFixture = new CompetitionTeamRepository()
                .findCompetitionsByTeam(liverpool.getId()).stream()
                .filter(competition -> competition.getSeason().getId() == season.getId())
                .flatMap(competition -> matches.findByCompetition(competition.getId()).stream())
                .anyMatch(match -> !match.getDate().isBefore(season.getStartDate())
                        && (match.getHomeTeam().getId() == liverpool.getId()
                        || match.getAwayTeam().getId() == liverpool.getId()));

        assertTrue(hasUpcomingFixture);
    }
}
