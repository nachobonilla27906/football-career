package footballcareer.service;

import footballcareer.database.MatchEventRepository;
import footballcareer.database.MatchLineupRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.database.PlayerRepository;
import footballcareer.model.Match;
import footballcareer.model.MatchEvent;
import footballcareer.model.MatchLineup;
import footballcareer.model.Player;
import footballcareer.model.PlayerState;
import footballcareer.model.enums.MatchEventType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerAvailabilityService {
    private final PlayerStateRepository states;
    private final MatchLineupRepository lineups;
    private final MatchEventRepository events;
    private final Random random;

    public PlayerAvailabilityService() {
        this(new PlayerStateRepository(), new MatchLineupRepository(),
                new MatchEventRepository(), new Random());
    }

    PlayerAvailabilityService(PlayerStateRepository states, MatchLineupRepository lineups,
            MatchEventRepository events, Random random) {
        this.states = states;
        this.lineups = lineups;
        this.events = events;
        this.random = random;
    }

    public List<String> processMatch(Match match) {
        List<String> consequences = new ArrayList<>();
        for (MatchEvent event : events.findByMatch(match.getId())) {
            if (event.getType() == MatchEventType.RED_CARD && event.getPlayer() != null) {
                Player player = new PlayerRepository().findById(event.getPlayer().getId());
                LocalDate until = match.getDate().plusDays(7);
                states.setUnavailable(event.getPlayer().getId(), until, "SUSPENSION");
                consequences.add(player.getFullName() + " sancionado hasta " + until);
            }
        }
        processInjuries(match, match.getHomeTeam().getId(), consequences);
        processInjuries(match, match.getAwayTeam().getId(), consequences);
        return consequences;
    }

    private void processInjuries(Match match, long teamId, List<String> consequences) {
        MatchLineup lineup = lineups.find(match.getId(), teamId);
        if (lineup == null) return;
        for (Player player : lineup.getStarters()) {
            PlayerState state = states.findByPlayer(player.getId());
            double risk = state != null && state.getFitness() < 55 ? 0.08 : 0.025;
            if (random.nextDouble() < risk) {
                int days = 5 + random.nextInt(17);
                LocalDate until = match.getDate().plusDays(days);
                states.setUnavailable(player.getId(), until, "INJURY");
                consequences.add(player.getFullName() + " lesionado hasta " + until);
            }
        }
    }
}
