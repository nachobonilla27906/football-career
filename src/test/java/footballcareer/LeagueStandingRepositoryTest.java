package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeagueStandingRepositoryTest {
    private Competition competition;
    private List<Team> teams;
    private LeagueStandingRepository repository;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetAndSeedForTests();
        Season season = new SeasonRepository().findFirst();
        competition = new CompetitionRepository()
                .findByNameAndSeason("LaLiga", season.getId());
        teams = new CompetitionTeamRepository()
                .findTeamsByCompetition(competition.getId());
        repository = new LeagueStandingRepository();
        repository.initialize(competition, teams);
    }

    @Test
    void shouldInitializeStandingsWithoutDuplicates() {
        repository.initialize(competition, teams);
        List<LeagueStanding> standings =
                repository.findByCompetition(competition.getId());

        assertEquals(4, standings.size());
        assertEquals(0, standings.getFirst().getPlayed());
    }

    @Test
    void shouldApplyResultAndOrderStandings() {
        Match match = new Match(
                0, competition, teams.get(0), teams.get(1),
                competition.getSeason().getStartDate().plusWeeks(1)
        );
        match.setResult(3, 1);
        repository.applyResult(match);

        List<LeagueStanding> standings =
                repository.findByCompetition(competition.getId());

        assertEquals(teams.get(0).getId(), standings.getFirst().getTeam().getId());
        assertEquals(3, standings.getFirst().getPoints());
        assertEquals(1, standings.getFirst().getPlayed());
        assertEquals(2, standings.getFirst().getGoalDifference());
    }
}
