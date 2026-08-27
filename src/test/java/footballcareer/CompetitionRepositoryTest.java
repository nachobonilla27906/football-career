package footballcareer;

import footballcareer.database.CompetitionRepository;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.SeasonRepository;
import footballcareer.model.Competition;
import footballcareer.model.Season;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CompetitionRepositoryTest {

    private CompetitionRepository repository;
    private SeasonRepository seasonRepository;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();

        repository = new CompetitionRepository();
        seasonRepository = new SeasonRepository();
    }

    @Test
    void shouldSaveAndFindCompetition() {

        Season season = new Season(
                0,
                2026,
                2027,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2027, 5, 30)
        );

        seasonRepository.save(season);

        Competition competition = new Competition(
                0,
                "LaLiga",
                "Spain",
                1,
                season
        );

        repository.save(competition);

        Competition savedCompetition =
                repository.findById(competition.getId());

        assertNotNull(savedCompetition);

        assertEquals(
                "LaLiga",
                savedCompetition.getName()
        );

        assertEquals(
                "Spain",
                savedCompetition.getCountry()
        );

        assertEquals(
                1,
                savedCompetition.getTier()
        );

        assertNotNull(
                savedCompetition.getSeason()
        );

        assertEquals(
                season.getId(),
                savedCompetition.getSeason().getId()
        );
    }

    @Test
    void shouldFindCompetitionByNameAndSeason() {

        Season season = new Season(
                0,
                2026,
                2027,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2027, 5, 30)
        );

        seasonRepository.save(season);

        Competition competition = new Competition(
                0,
                "LaLiga",
                "Spain",
                1,
                season
        );

        repository.save(competition);

        Competition found =
                repository.findByNameAndSeason(
                        "LaLiga",
                        season.getId()
                );

        assertNotNull(found);

        assertEquals(
                competition.getId(),
                found.getId()
        );

        assertEquals(
                "LaLiga",
                found.getName()
        );

        assertEquals(
                season.getId(),
                found.getSeason().getId()
        );
    }

    @Test
    void shouldReturnNullWhenCompetitionDoesNotExist() {

        Competition competition =
                repository.findById(999);

        assertNull(competition);
    }
}