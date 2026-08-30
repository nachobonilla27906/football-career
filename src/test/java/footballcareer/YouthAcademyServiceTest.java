package footballcareer;

import footballcareer.database.CareerRepository;
import footballcareer.database.ClubFinanceRepository;
import footballcareer.database.ContractRepository;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.PlayerRepository;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Career;
import footballcareer.model.Player;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerService;
import footballcareer.service.YouthAcademyService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YouthAcademyServiceTest {
    @Test
    void scoutDiscoversAndPromotesCareerScopedProspects() {
        DatabaseInitializer.resetAndSeedForTests();
        TeamRepository teams = new TeamRepository();
        Team arsenal = teams.findByShortName("ARS");
        Season season = new SeasonRepository().findFirst();
        Career career = new CareerService(new CareerRepository(), teams,
                new SeasonRepository()).createCareer("Academy Test", arsenal.getId(), season.getId());
        YouthAcademyService academy = new YouthAcademyService();
        double budget = new ClubFinanceRepository().findByTeam(arsenal.getId())
                .getTransferBudget();

        academy.hireScout(career, "Lucía Torres", 4);
        assertEquals(budget - YouthAcademyService.hiringCost(4),
                new ClubFinanceRepository().findByTeam(arsenal.getId()).getTransferBudget());
        assertEquals(5, academy.generateReport(career).size());
        assertThrows(IllegalStateException.class, () -> academy.generateReport(career));

        YouthAcademyService.Prospect prospect = academy.findCandidates(career.getId()).getFirst();
        Player promoted = academy.promote(career, prospect.id());

        assertTrue(new PlayerRepository().findCurrentPlayersByTeam(arsenal.getId()).stream()
                .anyMatch(player -> player.getId() == promoted.getId()));
        assertEquals("PROSPECT", new ContractRepository().findActiveByPlayer(promoted.getId())
                .getSquadRole());
        assertEquals(4, academy.findCandidates(career.getId()).size());
        assertThrows(IllegalArgumentException.class, () -> academy.promote(career, prospect.id()));
    }
}
