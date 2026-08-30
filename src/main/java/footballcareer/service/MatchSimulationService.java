package footballcareer.service;

import footballcareer.model.Match;
import footballcareer.model.MatchLineup;
import footballcareer.model.Player;
import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.database.MatchTacticsRepository;
import footballcareer.database.MatchRoleRepository;

import java.util.Random;

public class MatchSimulationService {

    private final Random random;
    private final LineupService lineupService;
    private final PlayerStateRepository stateRepository;
    private final MatchTacticsRepository tacticsRepository;
    private final CareerDifficultyService difficultyService = new CareerDifficultyService();

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
        var states = stateRepository.findAll();
        double homeStrength = calculateStrength(home, states)
                + difficultyService.modifier(match.getHomeTeam().getId())
                + leadershipBonus(match.getId(), match.getHomeTeam().getId(), home);
        double awayStrength = calculateStrength(away, states)
                + difficultyService.modifier(match.getAwayTeam().getId())
                + leadershipBonus(match.getId(), match.getAwayTeam().getId(), away);
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
        return calculateStrength(lineup, stateRepository.findAll());
    }

    private double calculateStrength(MatchLineup lineup,
            java.util.Map<Long, footballcareer.model.PlayerState> states) {
        return lineup.getStarters().stream().mapToDouble(player -> playerStrength(player, states))
                .average()
                .orElseThrow(() -> new IllegalStateException("Lineup is empty."));
    }

    TacticalProfile tacticalProfile(long matchId, long teamId) {
        MatchTacticsRepository.TacticalSetup setup = tacticsRepository.find(matchId, teamId);
        TacticalProfile formation = switch (setup.formation()) {
            case "4-2-3-1" -> new TacticalProfile(-0.5, 2.0);
            case "4-4-2" -> new TacticalProfile(0.5, 0.5);
            default -> new TacticalProfile(2.0, -1.0);
        };
        double attack = formation.attackBonus();
        double defence = formation.defenceBonus();
        switch (setup.mentality()) {
            case "ATTACKING" -> { attack += 2.5; defence -= 1.5; }
            case "DEFENSIVE" -> { attack -= 1.5; defence += 2.5; }
            default -> { }
        }
        switch (setup.pressing()) {
            case "HIGH" -> { attack += 1.0; defence += 0.5; }
            case "LOW" -> { attack -= 0.5; defence += 0.75; }
            default -> { }
        }
        switch (setup.tempo()) {
            case "FAST" -> { attack += 1.5; defence -= 0.5; }
            case "SLOW" -> { attack -= 1.0; defence += 1.0; }
            default -> { }
        }
        return new TacticalProfile(attack, defence);
    }

    record TacticalProfile(double attackBonus, double defenceBonus) {}

    private double leadershipBonus(long matchId, long teamId, MatchLineup lineup) {
        MatchRoleRepository.Assignment roles = new MatchRoleRepository().find(matchId, teamId);
        if (roles == null) return 0;
        return lineup.getStarters().stream().anyMatch(player -> player.getId() == roles.captainId())
                ? 0.6 : 0;
    }

    private double playerStrength(Player player,
            java.util.Map<Long, footballcareer.model.PlayerState> states) {
        var state = states.get(player.getId());
        if (state == null) throw new IllegalStateException("Player state is missing.");
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
