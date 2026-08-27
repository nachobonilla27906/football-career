package footballcareer.service;

import footballcareer.model.Match;
import footballcareer.model.MatchLineup;
import footballcareer.model.Player;
import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerStateRepository;

import java.util.Random;

public class MatchSimulationService {

    private final Random random;
    private final LineupService lineupService;
    private final PlayerStateRepository stateRepository;

    public MatchSimulationService() {
        this(new Random(), defaultLineupService(), new PlayerStateRepository());
    }

    public MatchSimulationService(Random random) {
        this(random, defaultLineupService(), new PlayerStateRepository());
    }

    public MatchSimulationService(Random random, LineupService lineupService,
            PlayerStateRepository stateRepository) {
        this.random = random;
        this.lineupService = lineupService;
        this.stateRepository = stateRepository;
    }

    public void simulate(Match match) {
        if (match.isPlayed()) {
            throw new IllegalArgumentException("Match has already been played.");
        }

        MatchLineup home = lineupService.selectMatchLineup(match.getHomeTeam().getId());
        MatchLineup away = lineupService.selectMatchLineup(match.getAwayTeam().getId());
        double homeStrength = calculateStrength(home) + 4;
        double awayStrength = calculateStrength(away);
        int homeGoals = generateGoals(homeStrength, awayStrength);
        int awayGoals = generateGoals(awayStrength, homeStrength);

        match.setResult(homeGoals, awayGoals);
    }

    double calculateStrength(MatchLineup lineup) {
        return lineup.getStarters().stream().mapToDouble(this::playerStrength).average()
                .orElseThrow(() -> new IllegalStateException("Lineup is empty."));
    }

    private double playerStrength(Player player) {
        var state = stateRepository.findByPlayer(player.getId());
        return player.getOverall() * 0.70
                + state.getForm() * 0.12
                + state.getMorale() * 0.08
                + state.getFitness() * 0.10;
    }

    private int generateGoals(double attackStrength, double oppositionStrength) {
        double chance = 0.19
                + (attackStrength - oppositionStrength) * 0.012;
        chance = Math.max(0.08, Math.min(0.55, chance));

        int goals = 0;
        for (int opportunity = 0; opportunity < 6; opportunity++) {
            if (random.nextDouble() < chance) {
                goals++;
            }
        }
        return goals;
    }

    private static LineupService defaultLineupService() {
        return new LineupService(new PlayerRepository(), new PlayerStateRepository());
    }
}
