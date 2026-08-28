package footballcareer.service;

import footballcareer.database.PlayerSeasonStatsRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.database.MatchEventRepository;
import footballcareer.model.Match;
import footballcareer.model.MatchEvent;
import footballcareer.model.Player;
import footballcareer.model.enums.MatchEventType;

import java.util.List;

public class PlayerMatchService {
    private final LineupService lineupService;
    private final PlayerSeasonStatsRepository statsRepository;
    private final PlayerStateRepository stateRepository;
    private final MatchEventRepository eventRepository;

    public PlayerMatchService(LineupService lineupService,
            PlayerSeasonStatsRepository statsRepository,
            PlayerStateRepository stateRepository) {
        this(lineupService, statsRepository, stateRepository,
                new MatchEventRepository());
    }

    public PlayerMatchService(LineupService lineupService,
            PlayerSeasonStatsRepository statsRepository,
            PlayerStateRepository stateRepository,
            MatchEventRepository eventRepository) {
        this.lineupService = lineupService;
        this.statsRepository = statsRepository;
        this.stateRepository = stateRepository;
        this.eventRepository = eventRepository;
    }

    public void process(Match match) {
        processTeam(match, true);
        processTeam(match, false);
    }

    private void processTeam(Match match, boolean home) {
        long teamId = (home ? match.getHomeTeam() : match.getAwayTeam()).getId();
        boolean won = home ? match.getHomeGoals() > match.getAwayGoals()
                : match.getAwayGoals() > match.getHomeGoals();
        boolean draw = match.getHomeGoals() == match.getAwayGoals();
        List<MatchEvent> teamEvents = eventRepository.findByMatch(match.getId())
                .stream().filter(event -> event.getTeam().getId() == teamId).toList();

        for (Player player : lineupService.selectMatchLineup(
                match.getId(), teamId).getStarters()) {
            int goals = countPrimary(teamEvents, player.getId(), MatchEventType.GOAL);
            int assists = (int) teamEvents.stream()
                    .filter(event -> event.getType() == MatchEventType.GOAL)
                    .filter(event -> event.getSecondaryPlayer() != null
                            && event.getSecondaryPlayer().getId() == player.getId()).count();
            int yellowCards = countPrimary(teamEvents, player.getId(), MatchEventType.YELLOW_CARD);
            int redCards = countPrimary(teamEvents, player.getId(), MatchEventType.RED_CARD);
            double rating = (won ? 7.2 : draw ? 6.7 : 6.2)
                    + goals * 0.8 + assists * 0.4
                    - yellowCards * 0.2 - redCards * 1.0;
            statsRepository.recordAppearance(player.getId(),
                    match.getCompetition().getSeason().getId(), true, 90,
                    goals, assists, yellowCards, redCards,
                    Math.max(1, Math.min(10, rating)));
            var state = stateRepository.findByPlayer(player.getId());
            state.setFitness(state.getFitness() - 10);
            state.setForm(state.getForm() + (won ? 3 : draw ? 1 : -2));
            state.setMorale(state.getMorale() + (won ? 3 : draw ? 0 : -2));
            stateRepository.update(state);
        }
    }

    private int countPrimary(List<MatchEvent> events, long playerId,
            MatchEventType type) {
        return (int) events.stream().filter(event -> event.getType() == type)
                .filter(event -> event.getPlayer().getId() == playerId).count();
    }
}
