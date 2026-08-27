package footballcareer.service;

import footballcareer.model.Match;

import java.util.Random;

public class MatchSimulationService {

    private final Random random;

    public MatchSimulationService() {
        this(new Random());
    }

    public MatchSimulationService(Random random) {
        this.random = random;
    }

    public void simulate(Match match) {
        if (match.isPlayed()) {
            throw new IllegalArgumentException("Match has already been played.");
        }

        int homeGoals = generateGoals(
                match.getHomeTeam().getReputation() + 5,
                match.getAwayTeam().getReputation()
        );
        int awayGoals = generateGoals(
                match.getAwayTeam().getReputation(),
                match.getHomeTeam().getReputation() + 5
        );

        match.setResult(homeGoals, awayGoals);
    }

    private int generateGoals(int attackStrength, int oppositionStrength) {
        double chance = 0.20
                + (attackStrength - oppositionStrength) * 0.008;
        chance = Math.max(0.08, Math.min(0.55, chance));

        int goals = 0;
        for (int opportunity = 0; opportunity < 6; opportunity++) {
            if (random.nextDouble() < chance) {
                goals++;
            }
        }
        return goals;
    }
}
