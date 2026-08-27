package footballcareer.service;

import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.model.Player;
import footballcareer.model.enums.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LineupService {
    private final PlayerRepository playerRepository;
    private final PlayerStateRepository stateRepository;

    public LineupService(
            PlayerRepository playerRepository,
            PlayerStateRepository stateRepository
    ) {
        this.playerRepository = playerRepository;
        this.stateRepository = stateRepository;
    }

    public List<Player> selectStartingEleven(long teamId) {
        List<Player> squad = new ArrayList<>(
                playerRepository.findCurrentPlayersByTeam(teamId));
        if (squad.size() < 11) {
            throw new IllegalStateException("Team needs at least eleven players.");
        }

        Comparator<Player> quality = Comparator
                .comparingInt(this::selectionScore).reversed();
        Player goalkeeper = squad.stream()
                .filter(player -> player.getPosition() == Position.GK)
                .max(quality).orElseThrow(() ->
                        new IllegalStateException("Team needs a goalkeeper."));
        squad.remove(goalkeeper);
        squad.sort(quality);

        List<Player> lineup = new ArrayList<>();
        lineup.add(goalkeeper);
        lineup.addAll(squad.subList(0, 10));
        return lineup;
    }

    private int selectionScore(Player player) {
        var state = stateRepository.findByPlayer(player.getId());
        return player.getOverall() * 2 + state.getForm() + state.getFitness();
    }
}
