package footballcareer;

import footballcareer.model.Player;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;
import footballcareer.ui.LineupSlotPlanner;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineupSlotPlannerTest {
    @Test
    void keepsTacticalSlotsStableWhenTwoStartersSwapPlaces() {
        LineupSlotPlanner planner = new LineupSlotPlanner();
        List<Player> arranged = new ArrayList<>(planner.arrange(List.of(
                player("Portero", Position.GK), player("Central 1", Position.CB),
                player("Central 2", Position.CB), player("Lateral I", Position.LB),
                player("Lateral D", Position.RB), player("Medio 1", Position.CM),
                player("Medio 2", Position.CM), player("Medio 3", Position.CM),
                player("Extremo I", Position.LW), player("Hugo Duro", Position.ST),
                player("Extremo D", Position.RW)), "4-3-3"));
        List<String> attackingSlots = planner.slots("4-3-3").getFirst();

        assertEquals(List.of("LW", "ST", "RW"), attackingSlots);
        assertEquals("Hugo Duro", arranged.get(1).getLastName());
        Collections.swap(arranged, 1, 2);
        assertEquals("Hugo Duro", arranged.get(2).getLastName());
        assertEquals("RW", attackingSlots.get(2));
    }

    private Player player(String name, Position position) {
        return new Player(0, "Test", name, LocalDate.of(2000, 1, 1), "Spain",
                position, PreferredFoot.RIGHT, 75, 75, 70, 70, 70, 70, 70, 70,
                10_000_000, 1_000_000);
    }
}
