package footballcareer;

import footballcareer.database.CompetitionRepository;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.MatchRepository;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Competition;
import footballcareer.model.Match;
import footballcareer.model.Season;
import footballcareer.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchRepositoryTest {

    private MatchRepository repository;
    private Competition competition;
    private Team homeTeam;
    private Team awayTeam;
    private LocalDate matchDate;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        repository = new MatchRepository();

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

        homeTeam = saveTeam("Real Madrid", "RMA", 95);
        awayTeam = saveTeam("Valencia CF", "VCF", 80);
        matchDate = LocalDate.of(2026, 8, 22);
    }

    @Test
    void shouldSaveAndFindMatch() {
        Match match = createMatch();
        repository.save(match);

        Match found = repository.findById(match.getId());

        assertNotNull(found);
        assertEquals("Real Madrid", found.getHomeTeam().getName());
        assertEquals("Valencia CF", found.getAwayTeam().getName());
        assertEquals(matchDate, found.getDate());
        assertFalse(found.isPlayed());
    }

    @Test
    void shouldFindMatchesByCompetitionAndDate() {
        repository.save(createMatch());

        assertEquals(
                1,
                repository.findByCompetition(competition.getId()).size()
        );
        assertEquals(1, repository.findByDate(matchDate).size());
    }

    @Test
    void shouldFindOnlyTheNextMatchInvolvingRequestedTeam() {
        Team atletico = saveTeam("Atlético de Madrid", "ATM", 90);
        Match unrelated = new Match(0, competition, homeTeam, atletico,
                matchDate.minusDays(2));
        repository.save(unrelated);
        Match controlled = createMatch();
        repository.save(controlled);

        Match next = repository.findNextForTeam(awayTeam.getId(),
                competition.getSeason().getId(), matchDate.minusDays(3));

        assertNotNull(next);
        assertEquals(controlled.getId(), next.getId());
        assertTrue(next.getHomeTeam().getId() == awayTeam.getId()
                || next.getAwayTeam().getId() == awayTeam.getId());
    }

    @Test
    void shouldUpdateMatchResult() {
        Match match = createMatch();
        repository.save(match);

        match.setResult(2, 1);
        repository.updateResult(match);

        Match found = repository.findById(match.getId());

        assertTrue(found.isPlayed());
        assertEquals(2, found.getHomeGoals());
        assertEquals(1, found.getAwayGoals());
    }

    private Match createMatch() {
        return new Match(
                0,
                competition,
                homeTeam,
                awayTeam,
                matchDate
        );
    }

    private Team saveTeam(
            String name,
            String shortName,
            int reputation
    ) {
        Team team = new Team(
                0,
                name,
                shortName,
                "Spain",
                name + " Stadium",
                50000,
                reputation
        );
        new TeamRepository().save(team);
        return team;
    }
}
