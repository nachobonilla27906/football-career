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
        DatabaseInitializer.resetAndSeedForTests();
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

        tactics.save(match.getId(), teamId, new MatchTacticsRepository.TacticalSetup(
                "4-3-3", "ATTACKING", "HIGH", "FAST"));
        MatchSimulationService.TacticalProfile aggressive =
                simulation.tacticalProfile(match.getId(), teamId);
        tactics.save(match.getId(), teamId, new MatchTacticsRepository.TacticalSetup(
                "4-2-3-1", "DEFENSIVE", "LOW", "SLOW"));
        MatchSimulationService.TacticalProfile conservative =
                simulation.tacticalProfile(match.getId(), teamId);

        assertTrue(aggressive.attackBonus() > conservative.attackBonus());
        assertTrue(conservative.defenceBonus() > aggressive.defenceBonus());
        assertTrue(tactics.find(match.getId(), teamId).mentality().equals("DEFENSIVE"));
    }
}
