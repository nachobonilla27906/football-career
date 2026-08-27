package footballcareer;

import footballcareer.database.CompetitionRepository;
import footballcareer.database.CompetitionTeamRepository;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Competition;
import footballcareer.model.Season;
import footballcareer.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompetitionTeamRepositoryTest {

    private CompetitionTeamRepository repository;
    private Competition competition;
    private Team team;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();

        repository = new CompetitionTeamRepository();

        Season season = new Season(
                0,
                2026,
                2027,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2027, 5, 30)
        );

        new SeasonRepository().save(season);

        competition = new Competition(
                0,
                "LaLiga",
                "Spain",
                1,
                season
        );

        new CompetitionRepository().save(competition);

        team = new Team(
                0,
                "Real Madrid",
                "RMA",
                "Spain",
                "Santiago Bernabeu",
                83186,
                95
        );

        new TeamRepository().save(team);
    }

    @Test
    void shouldAddTeamToCompetition() {
        repository.addTeamToCompetition(
                competition.getId(),
                team.getId()
        );

        assertTrue(repository.belongsToCompetition(
                competition.getId(),
                team.getId()
        ));
    }

    @Test
    void shouldReturnFalseWhenTeamDoesNotBelongToCompetition() {
        assertFalse(repository.belongsToCompetition(
                competition.getId(),
                team.getId()
        ));
    }

    @Test
    void shouldNotDuplicateCompetitionTeam() {
        repository.addTeamToCompetition(
                competition.getId(),
                team.getId()
        );

        repository.addTeamToCompetition(
                competition.getId(),
                team.getId()
        );

        assertTrue(repository.belongsToCompetition(
                competition.getId(),
                team.getId()
        ));
    }
}
