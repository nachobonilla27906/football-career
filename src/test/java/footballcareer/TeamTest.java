package footballcareer;

import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TeamTest {

    @Test
    void shouldCreateTeam() {

        Team team = new Team(
                1,
                "Real Madrid",
                "RMA",
                "Spain",
                "Santiago Bernabéu",
                83186,
                95
        );

        assertEquals(1, team.getId());
        assertEquals("Real Madrid", team.getName());
        assertEquals("RMA", team.getShortName());
        assertEquals("Spain", team.getCountry());
        assertEquals("Santiago Bernabéu", team.getStadiumName());
        assertEquals(83186, team.getStadiumCapacity());
        assertEquals(95, team.getReputation());
    }

    @Test
    void shouldAddPlayerToSquad() {

        Team team = new Team(
                1,
                "Real Madrid",
                "RMA",
                "Spain",
                "Santiago Bernabéu",
                83186,
                95
        );

        Player player = new Player(
                1,
                "Lamine",
                "Yamal",
                LocalDate.of(2007, 7, 13),
                "Spain",
                Position.RW,
                PreferredFoot.LEFT,
                91,
                96,
                90,
                86,
                88,
                94,
                35,
                70,
                150_000_000,
                10_000_000
        );

        team.addPlayer(player);

        assertEquals(1, team.getSquad().size());
        assertEquals(player, team.getSquad().get(0));
    }

    @Test
    void shouldRemovePlayerFromSquad() {

        Team team = new Team(
                1,
                "Real Madrid",
                "RMA",
                "Spain",
                "Santiago Bernabéu",
                83186,
                95
        );

        Player player = new Player(
                1,
                "Lamine",
                "Yamal",
                LocalDate.of(2007, 7, 13),
                "Spain",
                Position.RW,
                PreferredFoot.LEFT,
                91,
                96,
                90,
                86,
                88,
                94,
                35,
                70,
                150_000_000,
                10_000_000
        );

        team.addPlayer(player);
        team.removePlayer(player);

        assertTrue(team.getSquad().isEmpty());
    }
}