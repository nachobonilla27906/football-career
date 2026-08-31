package footballcareer.ui;

import footballcareer.model.Player;
import footballcareer.model.enums.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class LineupSlotPlanner {
    private static final Set<Position> ATTACK = Set.of(Position.ST, Position.LW,
            Position.RW, Position.CAM);
    private static final Set<Position> MIDFIELD = Set.of(Position.CDM, Position.CM, Position.CAM);
    private static final Set<Position> DEFENCE = Set.of(Position.LB, Position.CB, Position.RB);

    public List<Player> arrange(List<Player> players, String formation) {
        List<Player> pool = new ArrayList<>(players);
        List<Player> arranged = new ArrayList<>();
        for (List<String> row : slots(formation)) for (String slotName : row) {
            Position slot = Position.valueOf(slotName);
            Player selected = pool.stream().filter(player -> player.getPosition() == slot)
                    .findFirst().orElseGet(() -> pool.stream().filter(player ->
                            compatible(player.getPosition(), slot)).findFirst()
                            .orElse(pool.isEmpty() ? null : pool.getFirst()));
            if (selected != null) { arranged.add(selected); pool.remove(selected); }
        }
        arranged.addAll(pool);
        return arranged;
    }

    public List<List<String>> slots(String formation) {
        return switch (formation) {
            case "4-2-3-1" -> List.of(List.of("ST"), List.of("LW", "CAM", "RW"),
                    List.of("CDM", "CM"), List.of("LB", "CB", "CB", "RB"), List.of("GK"));
            case "4-4-2" -> List.of(List.of("ST", "ST"),
                    List.of("LW", "CM", "CM", "RW"),
                    List.of("LB", "CB", "CB", "RB"), List.of("GK"));
            default -> List.of(List.of("LW", "ST", "RW"), List.of("CM", "CM", "CM"),
                    List.of("LB", "CB", "CB", "RB"), List.of("GK"));
        };
    }

    private boolean compatible(Position player, Position slot) {
        return ATTACK.contains(player) && ATTACK.contains(slot)
                || MIDFIELD.contains(player) && MIDFIELD.contains(slot)
                || DEFENCE.contains(player) && DEFENCE.contains(slot);
    }
}
