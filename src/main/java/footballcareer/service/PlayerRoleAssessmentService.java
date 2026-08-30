package footballcareer.service;

import footballcareer.model.Player;
import footballcareer.model.PlayerState;
import footballcareer.model.enums.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerRoleAssessmentService {
    public record AttributeImpact(String name, int value, String effect, boolean key) {}
    public record Assessment(List<AttributeImpact> attributes, String strongest,
                             String weakness, int effectiveLevel, String condition) {}

    public Assessment assess(Player player, PlayerState state) {
        List<String> keys = keyAttributes(player.getPosition());
        List<AttributeImpact> attributes = new ArrayList<>(List.of(
                impact("Ritmo", player.getPace(), "desmarques, transiciones y recuperación", keys),
                impact("Tiro", player.getShooting(), "definición y amenaza desde media distancia", keys),
                impact("Pase", player.getPassing(), "progresión, creación y balón parado", keys),
                impact("Regate", player.getDribbling(), "superación de rivales y conservación", keys),
                impact("Defensa", player.getDefending(), "marcaje, entradas e intercepciones", keys),
                impact("Físico", player.getPhysical(), "duelos, resistencia y juego aéreo", keys)));
        List<AttributeImpact> relevant = attributes.stream().filter(AttributeImpact::key).toList();
        AttributeImpact strongest = relevant.stream().max(Comparator.comparingInt(
                AttributeImpact::value)).orElseThrow();
        AttributeImpact weakest = relevant.stream().min(Comparator.comparingInt(
                AttributeImpact::value)).orElseThrow();
        int form = state == null ? 50 : state.getForm();
        int fitness = state == null ? 100 : state.getFitness();
        int effective = Math.max(1, Math.min(99, (int) Math.round(player.getOverall()
                + (form - 50) * 0.08 + (fitness - 80) * 0.04)));
        String condition = fitness < 40 ? "Muy limitado por cansancio"
                : fitness < 65 ? "Rendimiento condicionado por fitness"
                : form >= 70 ? "En gran momento de forma"
                : form < 40 ? "Por debajo de su nivel habitual" : "Rendimiento estable";
        return new Assessment(List.copyOf(attributes), strongest.name(), weakest.name(),
                effective, condition);
    }

    private AttributeImpact impact(String name, int value, String effect, List<String> keys) {
        return new AttributeImpact(name, value, effect, keys.contains(name));
    }

    private List<String> keyAttributes(Position position) {
        return switch (position) {
            case GK -> List.of("Defensa", "Físico", "Pase");
            case CB -> List.of("Defensa", "Físico", "Ritmo");
            case LB, RB -> List.of("Ritmo", "Defensa", "Pase");
            case CDM -> List.of("Defensa", "Pase", "Físico");
            case CM -> List.of("Pase", "Regate", "Físico");
            case CAM -> List.of("Pase", "Regate", "Tiro");
            case LW, RW -> List.of("Ritmo", "Regate", "Tiro");
            case ST -> List.of("Tiro", "Ritmo", "Físico");
        };
    }
}
