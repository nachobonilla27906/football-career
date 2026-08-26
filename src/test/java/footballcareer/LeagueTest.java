package footballcareer;

import footballcareer.model.League;
import footballcareer.model.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeagueTest {

    @Test
    void shouldCreateLeague() {

        League league = new League(
                1,
                "LaLiga",
                "Spain",
                1
        );

        assertEquals("LaLiga", league.getName());
        assertEquals("Spain", league.getCountry());
        assertEquals(1, league.getTier());
    }

    @Test
    void shouldAddTeamToLeague() {

        League league = new League(
                1,
                "LaLiga",
                "Spain",
                1
        );

        Team team = new Team(
                1,
                "Real Madrid",
                "RMA",
                "Spain",
                "Santiago Bernabéu",
                83186,
                95
        );

        league.addTeam(team);

        assertEquals(1, league.getTeams().size());
        assertEquals(team, league.getTeams().get(0));
    }

    @Test
    void shouldRemoveTeamFromLeague() {

        League league = new League(
                1,
                "LaLiga",
                "Spain",
                1
        );

        Team team = new Team(
                1,
                "Real Madrid",
                "RMA",
                "Spain",
                "Santiago Bernabéu",
                83186,
                95
        );

        league.addTeam(team);
        league.removeTeam(team);

        assertTrue(league.getTeams().isEmpty());
    }
}