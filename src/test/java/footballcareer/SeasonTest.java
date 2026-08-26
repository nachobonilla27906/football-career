package footballcareer;

import footballcareer.model.Season;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SeasonTest {

    @Test
    void shouldCreateSeason() {

        Season season = new Season(
                1,
                2026,
                2027,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2027, 5, 30)
        );

        assertEquals(1, season.getId());

        assertEquals(2026, season.getStartYear());
        assertEquals(2027, season.getEndYear());

        assertEquals(
                LocalDate.of(2026, 8, 15),
                season.getStartDate()
        );

        assertEquals(
                LocalDate.of(2027, 5, 30),
                season.getEndDate()
        );

        assertEquals("2026/27", season.getName());

        assertFalse(season.isFinished());
    }

    @Test
    void shouldFinishSeason() {

        Season season = new Season(
                1,
                2026,
                2027,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2027, 5, 30)
        );

        season.setFinished(true);

        assertTrue(season.isFinished());
    }
}