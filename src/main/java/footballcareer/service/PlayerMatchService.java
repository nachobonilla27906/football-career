package footballcareer.service;

import footballcareer.database.PlayerSeasonStatsRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.model.Match;
import footballcareer.model.Player;

public class PlayerMatchService {
    private final LineupService lineupService;
    private final PlayerSeasonStatsRepository statsRepository;
    private final PlayerStateRepository stateRepository;

    public PlayerMatchService(LineupService lineupService,
            PlayerSeasonStatsRepository statsRepository,
            PlayerStateRepository stateRepository) {
        this.lineupService = lineupService;
        this.statsRepository = statsRepository;
        this.stateRepository = stateRepository;
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

        for (Player player : lineupService.selectStartingEleven(teamId)) {
            statsRepository.recordAppearance(player.getId(),
                    match.getCompetition().getSeason().getId(), true, 90,
                    0, 0, 0, 0, won ? 7.2 : draw ? 6.7 : 6.2);
            var state = stateRepository.findByPlayer(player.getId());
            state.setFitness(state.getFitness() - 10);
            state.setForm(state.getForm() + (won ? 3 : draw ? 1 : -2));
            state.setMorale(state.getMorale() + (won ? 3 : draw ? 0 : -2));
            stateRepository.update(state);
        }
    }
}
