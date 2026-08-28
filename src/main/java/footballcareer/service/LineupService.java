package footballcareer.service;

import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.database.MatchLineupRepository;
import footballcareer.model.Player;
import footballcareer.model.MatchLineup;
import footballcareer.model.Team;
import footballcareer.model.enums.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class LineupService {
    private final PlayerRepository playerRepository;
    private final PlayerStateRepository stateRepository;
    private final MatchLineupRepository matchLineupRepository;

    public LineupService(
            PlayerRepository playerRepository,
            PlayerStateRepository stateRepository
    ) {
        this(playerRepository, stateRepository, new MatchLineupRepository());
    }

    public LineupService(PlayerRepository playerRepository,
            PlayerStateRepository stateRepository,
            MatchLineupRepository matchLineupRepository) {
        this.playerRepository = playerRepository;
        this.stateRepository = stateRepository;
        this.matchLineupRepository = matchLineupRepository;
    }

    public List<Player> selectStartingEleven(long teamId) {
        return selectMatchLineup(teamId).getStarters();
    }

    public MatchLineup selectMatchLineup(long teamId) {
        List<Player> eligible = new ArrayList<>(playerRepository
                .findCurrentPlayersByTeam(teamId).stream()
                .filter(this::isFitToPlay)
                .toList());
        if (eligible.size() < 11) {
            throw new IllegalStateException("Team needs at least eleven players.");
        }

        Comparator<Player> quality = Comparator
                .comparingInt(this::selectionScore).reversed();
        Player goalkeeper = eligible.stream()
                .filter(player -> player.getPosition() == Position.GK)
                .max(quality).orElseThrow(() ->
                        new IllegalStateException("Team needs a goalkeeper."));
        eligible.remove(goalkeeper);

        List<Player> starters = new ArrayList<>();
        starters.add(goalkeeper);
        pickBest(eligible, starters, quality,
                Set.of(Position.CB, Position.LB, Position.RB), 4);
        pickBest(eligible, starters, quality,
                Set.of(Position.CDM, Position.CM, Position.CAM), 3);
        pickBest(eligible, starters, quality,
                Set.of(Position.LW, Position.RW, Position.ST), 3);
        eligible.sort(quality);
        while (starters.size() < 11 && !eligible.isEmpty()) {
            starters.add(eligible.removeFirst());
        }
        if (starters.size() != 11) {
            throw new IllegalStateException("Could not build a complete starting eleven.");
        }
        eligible.sort(quality);
        List<Player> substitutes = new ArrayList<>(
                eligible.subList(0, Math.min(7, eligible.size())));
        Team team = new Team(); team.setId(teamId);
        return new MatchLineup(team, starters, substitutes);
    }

    public MatchLineup selectMatchLineup(long matchId, long teamId) {
        MatchLineup saved = matchLineupRepository.find(matchId, teamId);
        return saved != null ? saved : selectMatchLineup(teamId);
    }

    private void pickBest(List<Player> available, List<Player> selected,
            Comparator<Player> quality, Set<Position> positions, int amount) {
        List<Player> candidates = available.stream()
                .filter(player -> positions.contains(player.getPosition()))
                .sorted(quality).limit(amount).toList();
        selected.addAll(candidates);
        available.removeAll(candidates);
    }

    private boolean isFitToPlay(Player player) {
        var state = stateRepository.findByPlayer(player.getId());
        return state != null && state.getFitness() >= 20;
    }

    private int selectionScore(Player player) {
        var state = stateRepository.findByPlayer(player.getId());
        return player.getOverall() * 2 + state.getForm() + state.getFitness();
    }
}
