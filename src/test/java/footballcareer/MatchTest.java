package footballcareer;

import footballcareer.model.Competition;
import footballcareer.model.Match;
import footballcareer.model.Season;
import footballcareer.model.Team;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MatchTest {

    @Test
    void shouldCreateUnplayedMatch() {

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

        Team home = new Team(
                1,
                "Real Madrid",
                "RMA",
                "Spain",
                "Santiago Bernabéu",
                83186,
                95
        );

        Team away = new Team(
                2,
                "Barcelona",
                "BAR",
                "Spain",
                "Spotify Camp Nou",
                99354,
                95
        );

        LocalDate date = LocalDate.of(2026, 9, 1);

        Match match = new Match(
                1,
                competition,
                home,
                away,
                date
        );

        assertEquals(1, match.getId());
        assertEquals(competition, match.getCompetition());
        assertEquals(home, match.getHomeTeam());
        assertEquals(away, match.getAwayTeam());
        assertEquals(date, match.getDate());

        assertEquals(0, match.getHomeGoals());
        assertEquals(0, match.getAwayGoals());

        assertFalse(match.isPlayed());
    }

    @Test
    void shouldSetMatchResult() {

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

        Team home = new Team(
                1,
                "Real Madrid",
                "RMA",
                "Spain",
                "Santiago Bernabéu",
                83186,
                95
        );

        Team away = new Team(
                2,
                "Barcelona",
                "BAR",
                "Spain",
                "Spotify Camp Nou",
                99354,
                95
        );

        Match match = new Match(
                1,
                competition,
                home,
                away,
                LocalDate.of(2026, 9, 1)
        );

        match.setResult(3, 1);

        assertEquals(3, match.getHomeGoals());
        assertEquals(1, match.getAwayGoals());
        assertTrue(match.isPlayed());
    }
}