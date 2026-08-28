package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Competition;
import footballcareer.model.Match;
import footballcareer.model.Season;
import footballcareer.service.FootballWorldService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchTacticsRepositoryTest {

    @Test
    void formationIsPersistedForTeamAndMatch() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Competition competition = new CompetitionRepository().findBySeason(season.getId()).getFirst();
        Match match = new MatchRepository().findByCompetition(competition.getId()).getFirst();
        long teamId = match.getHomeTeam().getId();
        MatchTacticsRepository repository = new MatchTacticsRepository();

        assertEquals("4-3-3", repository.findFormation(match.getId(), teamId));
        repository.saveFormation(match.getId(), teamId, "4-2-3-1");
        assertEquals("4-2-3-1", repository.findFormation(match.getId(), teamId));
        assertThrows(IllegalArgumentException.class,
                () -> repository.saveFormation(match.getId(), teamId, "3-2-5"));
    }
}
