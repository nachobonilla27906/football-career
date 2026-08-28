package footballcareer.service;

import footballcareer.model.Match;
import footballcareer.model.MatchLineup;
import footballcareer.model.Player;
import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.database.MatchTacticsRepository;

import java.util.Random;

public class MatchSimulationService {

    private final Random random;
    private final LineupService lineupService;
    private final PlayerStateRepository stateRepository;
    private final MatchTacticsRepository tacticsRepository;

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
        this.tacticsRepository = new MatchTacticsRepository();
    }

    public void simulate(Match match) {
        if (match.isPlayed()) {
            throw new IllegalArgumentException("Match has already been played.");
        }

        MatchLineup home = lineupService.selectMatchLineup(
                match.getId(), match.getHomeTeam().getId());
        MatchLineup away = lineupService.selectMatchLineup(
                match.getId(), match.getAwayTeam().getId());
        double homeStrength = calculateStrength(home);
        double awayStrength = calculateStrength(away);
        TacticalProfile homeTactics = tacticalProfile(match.getId(), match.getHomeTeam().getId());
        TacticalProfile awayTactics = tacticalProfile(match.getId(), match.getAwayTeam().getId());
        double homeAttack = homeStrength + 4 + homeTactics.attackBonus();
        double homeDefence = homeStrength + 4 + homeTactics.defenceBonus();
        double awayAttack = awayStrength + awayTactics.attackBonus();
        double awayDefence = awayStrength + awayTactics.defenceBonus();
        int homeGoals = generateGoals(homeAttack, awayDefence);
        int awayGoals = generateGoals(awayAttack, homeDefence);

        match.setResult(homeGoals, awayGoals);
    }

    double calculateStrength(MatchLineup lineup) {
        return lineup.getStarters().stream().mapToDouble(this::playerStrength).average()
                .orElseThrow(() -> new IllegalStateException("Lineup is empty."));
    }

    TacticalProfile tacticalProfile(long matchId, long teamId) {
        return switch (tacticsRepository.findFormation(matchId, teamId)) {
            case "4-2-3-1" -> new TacticalProfile(-0.5, 2.0);
            case "4-4-2" -> new TacticalProfile(0.5, 0.5);
            default -> new TacticalProfile(2.0, -1.0);
        };
    }

    record TacticalProfile(double attackBonus, double defenceBonus) {}

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
