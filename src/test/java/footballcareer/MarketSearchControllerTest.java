package footballcareer;

import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.enums.Position;
import footballcareer.ui.MarketSearchController;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketSearchControllerTest {
    @Test
    void combinesScoutingFiltersAndSortWithoutJavaFxOrDatabase() {
        Player striker = player(1, "Álex Nueve", Position.ST, 84, 25, 60_000_000, 2_000_000);
        Player keeper = player(2, "Hugo Uno", Position.GK, 76, 21, 15_000_000, 800_000);
        Team arsenal = team(10, "Arsenal");
        Team valencia = team(20, "Valencia");
        MarketSearchController.Catalogue catalogue = new MarketSearchController.Catalogue(
                List.of(striker, keeper), List.of(keeper),
                Map.of(1L, arsenal, 2L, valencia),
                Map.of(10L, "Premier League", 20L, "LaLiga"),
                Map.of(1L, 70_000_000.0, 2L, 12_000_000.0), Set.of(2L), 99,
                LocalDate.of(2026, 8, 1));
        MarketSearchController controller = new MarketSearchController();

        var listed = controller.search(catalogue, new MarketSearchController.Query(
                false, "hugo", "LaLiga", "TODOS LOS CLUBES", "GK", "70", "23",
                "20", "1", "GRL ↓", true));
        assertEquals(List.of(keeper), listed);

        var global = controller.search(catalogue, new MarketSearchController.Query(
                true, "", "TODAS LAS LIGAS", "TODOS LOS CLUBES", "TODAS", "", "",
                "", "", "PRECIO ↓", false));
        assertEquals(List.of(striker, keeper), global);
    }

    private Player player(long id, String name, Position position, int overall, int age,
            double value, double salary) {
        String[] parts = name.split(" ", 2);
        Player player = new Player(); player.setId(id); player.setFirstName(parts[0]);
        player.setLastName(parts[1]); player.setPosition(position); player.setOverall(overall);
        player.setBirthDate(LocalDate.of(2026 - age, 1, 1));
        player.setMarketValue(value); player.setSalary(salary); return player;
    }

    private Team team(long id, String name) {
        Team team = new Team(); team.setId(id); team.setName(name); return team;
    }
}
