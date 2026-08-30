package footballcareer;

import footballcareer.database.CareerRepository;
import footballcareer.database.Database;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerRepairService;
import footballcareer.service.CareerService;
import footballcareer.service.FootballWorldService;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CareerRepairServiceTest {
    @Test
    void repairsMalformedDateAndPartiallyMissingEssentialState() throws Exception {
        DatabaseInitializer.resetAndSeedForTests();
        TeamRepository teams = new TeamRepository();
        Team arsenal = teams.findByShortName("ARS");
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Career career = new CareerService(new CareerRepository(), teams,
                new SeasonRepository()).createCareer("Broken Save", arsenal.getId(), season.getId());
        try (Connection connection = Database.getConnection()) {
            execute(connection, "UPDATE careers SET current_date = 'not-a-date' WHERE id = ?",
                    career.getId());
            execute(connection, "DELETE FROM career_match_states WHERE career_id = ? "
                    + "AND match_id IN (SELECT match_id FROM career_match_states "
                    + "WHERE career_id = ? LIMIT 3)", career.getId(), career.getId());
            execute(connection, "DELETE FROM career_contracts WHERE career_id = ? "
                    + "AND player_id IN (SELECT player_id FROM career_player_team "
                    + "WHERE career_id = ? AND end_date IS NULL LIMIT 1)",
                    career.getId(), career.getId());
        }

        CareerRepairService.RepairReport report = new CareerRepairService().repair(career.getId());

        assertTrue(report.dateAdjusted());
        assertEquals(season.getStartDate(), report.date());
        assertTrue(report.reconstructedRecords() >= 4, report.toString());
        Career loaded = new CareerRepository().findById(career.getId());
        assertNotNull(loaded);
        assertEquals(season.getStartDate(), loaded.getCurrentDate());
    }

    private void execute(Connection connection, String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            statement.executeUpdate();
        }
    }
}
