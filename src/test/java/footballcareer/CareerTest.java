package footballcareer;

import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CareerTest {

    @Test
    void shouldCreateCareer() {

        Team team = new Team(
                1,
                "Real Madrid",
                "RMA",
                "Spain",
                "Santiago Bernabéu",
                83186,
                95
        );

        Season season = new Season(
                1,
                2026,
                2027,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2027, 5, 30)
        );

        LocalDate date = LocalDate.of(2026, 8, 15);

        Career career = new Career(
                1,
                "Nacho",
                team,
                season,
                date
        );

        assertEquals(1, career.getId());
        assertEquals("Nacho", career.getManagerName());
        assertEquals(team, career.getControlledTeam());
        assertEquals(season, career.getCurrentSeason());
        assertEquals(date, career.getCurrentDate());
    }

    @Test
    void shouldAdvanceDate() {

        Career career = new Career(
                1,
                "Nacho",
                null,
                null,
                LocalDate.of(2026, 8, 15)
        );

        career.setCurrentDate(
                career.getCurrentDate().plusDays(1)
        );

        assertEquals(
                LocalDate.of(2026, 8, 16),
                career.getCurrentDate()
        );
    }
}