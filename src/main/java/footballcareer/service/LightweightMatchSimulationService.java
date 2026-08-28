package footballcareer.service;

import footballcareer.model.Match;

import java.util.Random;

public class LightweightMatchSimulationService {
    private final Random random;

    public LightweightMatchSimulationService() {
        this(new Random());
    }

    public LightweightMatchSimulationService(Random random) {
        this.random = random;
    }

    public void simulate(Match match) {
        if (match.isPlayed()) throw new IllegalArgumentException("Match has already been played.");
        int home = goals(match.getHomeTeam().getReputation() + 4,
                match.getAwayTeam().getReputation());
        int away = goals(match.getAwayTeam().getReputation(),
                match.getHomeTeam().getReputation() + 4);
        match.setResult(home, away);
    }

    private int goals(int attack, int defence) {
        double chance = Math.max(0.08,
                Math.min(0.48, 0.18 + (attack - defence) * 0.009));
        int goals = 0;
        for (int opportunity = 0; opportunity < 6; opportunity++) {
            if (random.nextDouble() < chance) goals++;
        }
        return goals;
    }
}
