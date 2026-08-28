package footballcareer.service;

import footballcareer.database.*;
import footballcareer.model.Competition;
import footballcareer.model.Match;
import footballcareer.model.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSimulationTacticsTest {

    @Test
    void formationsProvideDifferentAttackAndDefenceProfiles() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Competition competition = new CompetitionRepository().findBySeason(season.getId()).getFirst();
        Match match = new MatchRepository().findByCompetition(competition.getId()).getFirst();
        long teamId = match.getHomeTeam().getId();
        MatchTacticsRepository tactics = new MatchTacticsRepository();
        MatchSimulationService simulation = new MatchSimulationService();

        tactics.saveFormation(match.getId(), teamId, "4-3-3");
        MatchSimulationService.TacticalProfile attacking =
                simulation.tacticalProfile(match.getId(), teamId);
        tactics.saveFormation(match.getId(), teamId, "4-2-3-1");
        MatchSimulationService.TacticalProfile defensive =
                simulation.tacticalProfile(match.getId(), teamId);

        assertTrue(attacking.attackBonus() > defensive.attackBonus());
        assertTrue(defensive.defenceBonus() > attacking.defenceBonus());
    }
}
