package footballcareer;

import footballcareer.database.DatabaseInitializer;
import footballcareer.database.SeasonRepository;
import footballcareer.model.Season;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class SeasonRepositoryTest {

    private SeasonRepository seasonRepository;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.initialize();
        seasonRepository = new SeasonRepository();
    }

    @Test
    void shouldSaveAndFindSeason() {

        Season season = new Season();

        season.setStartYear(2026);
        season.setEndYear(2027);
        season.setStartDate(LocalDate.of(2026, 8, 15));
        season.setEndDate(LocalDate.of(2027, 5, 30));
        season.setFinished(false);

        seasonRepository.save(season);

        assertTrue(season.getId() > 0);

        Season loadedSeason =
                seasonRepository.findById(season.getId());

        assertNotNull(loadedSeason);

        assertEquals(
                season.getId(),
                loadedSeason.getId()
        );

        assertEquals(
                2026,
                loadedSeason.getStartYear()
        );

        assertEquals(
                2027,
                loadedSeason.getEndYear()
        );

        assertEquals(
                LocalDate.of(2026, 8, 15),
                loadedSeason.getStartDate()
        );

        assertEquals(
                LocalDate.of(2027, 5, 30),
                loadedSeason.getEndDate()
        );

        assertFalse(loadedSeason.isFinished());
    }
}