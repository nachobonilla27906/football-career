package footballcareer;

import footballcareer.database.CareerRepository;
import footballcareer.database.Database;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerService;
import footballcareer.service.ManagerReputationService;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagerReputationServiceTest {
    @Test
    void reputationKeepsDailyHistoryAndReflectsBoardConfidence() throws Exception {
        DatabaseInitializer.resetAndSeedForTests();
        TeamRepository teams = new TeamRepository();
        Team arsenal = teams.findByShortName("ARS");
        Season season = new SeasonRepository().findFirst();
        CareerRepository careers = new CareerRepository();
        Career career = new CareerService(careers, teams, new SeasonRepository())
                .createCareer("Reputation Test", arsenal.getId(), season.getId());
        ManagerReputationService reputation = new ManagerReputationService();

        assertEquals(0, reputation.find(career).change());
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE career_club_finances
                     SET current_wage_spend = wage_budget + 1
                     WHERE career_id = ? AND team_id = ?
                     """)) {
            statement.setLong(1, career.getId());
            statement.setLong(2, arsenal.getId());
            statement.executeUpdate();
        }
        career.setCurrentDate(career.getCurrentDate().plusDays(1));
        careers.updateCurrentDate(career);

        ManagerReputationService.Reputation fallen = reputation.record(career);
        assertTrue(fallen.change() < 0);
        assertTrue(fallen.display().contains("▼"));
    }
}
