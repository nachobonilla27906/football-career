package footballcareer.service;

import footballcareer.database.MatchLineupRepository;
import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.model.Match;
import footballcareer.model.MatchLineup;
import footballcareer.model.Player;
import footballcareer.model.PlayerState;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SquadDynamicsService {
    public record Concern(Player player, String role, int morale, String message) {}

    public void processUnusedPlayers(Match match) {
        processTeam(match, match.getHomeTeam().getId());
        processTeam(match, match.getAwayTeam().getId());
    }

    public List<Concern> concerns(long teamId) {
        List<Player> squad = new PlayerRepository().findCurrentPlayersByTeam(teamId);
        PlayerStateRepository states = new PlayerStateRepository();
        return squad.stream().map(player -> {
            PlayerState state = states.findByPlayer(player.getId());
            String role = role(player, squad);
            String message = state.getMorale() < 30 ? "Exige una respuesta inmediata del mánager."
                    : state.getMorale() < 45 ? "Está preocupado por su situación deportiva."
                    : null;
            return new Concern(player, role, state.getMorale(), message);
        }).filter(concern -> concern.message() != null)
                .sorted(Comparator.comparingInt(Concern::morale)).toList();
    }

    private void processTeam(Match match, long teamId) {
        MatchLineup lineup = new MatchLineupRepository().find(match.getId(), teamId);
        if (lineup == null) return;
        Set<Long> starters = lineup.getStarters().stream().map(Player::getId)
                .collect(Collectors.toSet());
        List<Player> squad = new PlayerRepository().findCurrentPlayersByTeam(teamId);
        PlayerStateRepository states = new PlayerStateRepository();
        for (Player player : squad) {
            if (starters.contains(player.getId())) continue;
            PlayerState state = states.findByPlayer(player.getId());
            if (state == null) continue;
            String role = role(player, squad);
            int loss = "CLAVE".equals(role) ? 3 : "TITULAR".equals(role) ? 2 : 1;
            state.setMorale(state.getMorale() - loss);
            states.update(state);
        }
    }

    public String role(Player player, List<Player> squad) {
        long stronger = squad.stream().filter(other -> other.getOverall() > player.getOverall()).count();
        if (stronger < 5) return "CLAVE";
        if (stronger < 11) return "TITULAR";
        if (stronger < 18) return "ROTACIÓN";
        return "PROMESA";
    }
}
