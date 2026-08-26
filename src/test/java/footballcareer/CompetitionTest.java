package footballcareer;

import footballcareer.model.Competition;
import footballcareer.model.Season;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CompetitionTest {

    @Test
    void shouldCreateCompetition() {

        Season season = new Season(
                1,
                2026,
                2027,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2027, 5, 30)
        );

        Competition competition = new Competition(
                1,
                "LaLiga",
                "Spain",
                1,
                season
        );

        assertEquals(1, competition.getId());
        assertEquals("LaLiga", competition.getName());
        assertEquals("Spain", competition.getCountry());
        assertEquals(1, competition.getTier());
        assertEquals(season, competition.getSeason());
    }
}