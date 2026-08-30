package footballcareer.service;

import footballcareer.database.CareerContext;
import footballcareer.database.CareerMatchStateRepository;
import footballcareer.database.CareerRepository;
import footballcareer.database.Database;
import footballcareer.model.Career;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class CareerRepairService {
    public record RepairReport(long careerId, LocalDate date, boolean dateAdjusted,
                               int reconstructedRecords, String detail) {}

    public RepairReport repair(long careerId) {
        CareerRepository careers = new CareerRepository();
        boolean rawDateAdjusted = repairRawDate(careerId);
        Career career = careers.findById(careerId);
        if (career == null) throw new IllegalArgumentException("Career cannot be loaded.");
        Long previous = CareerContext.getCareerId();
        try {
            CareerContext.activate(careerId);
            LocalDate original = career.getCurrentDate();
            LocalDate repaired = original;
            if (repaired.isBefore(career.getCurrentSeason().getStartDate())) {
                repaired = career.getCurrentSeason().getStartDate();
            } else if (repaired.isAfter(career.getCurrentSeason().getEndDate())) {
                repaired = career.getCurrentSeason().getEndDate();
            }
            boolean adjusted = rawDateAdjusted || !original.equals(repaired);
            if (adjusted) {
                career.setCurrentDate(repaired);
                careers.updateCurrentDate(career);
            }
            int before = integrityCount(career);
            new CareerMatchStateRepository().initialize(career, false);
            new ManagerReputationService().record(career);
            int after = integrityCount(career);
            int reconstructed = Math.max(0, after - before);
            return new RepairReport(careerId, repaired, adjusted, reconstructed,
                    (adjusted ? "Fecha corregida. " : "")
                            + "Integridad verificada; " + reconstructed
                            + " registro(s) esenciales reconstruidos.");
        } finally {
            if (previous == null) CareerContext.clear();
            else CareerContext.activate(previous);
        }
    }

    private boolean repairRawDate(long careerId) {
        String sql = """
                SELECT c.current_date, s.start_date, s.end_date
                FROM careers c LEFT JOIN seasons s ON s.id = c.current_season_id
                WHERE c.id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("Career cannot be loaded.");
                String startValue = row.getString("start_date");
                String endValue = row.getString("end_date");
                if (startValue == null || endValue == null) {
                    throw new IllegalStateException("Career season reference is irrecoverable.");
                }
                LocalDate start = LocalDate.parse(startValue);
                LocalDate end = LocalDate.parse(endValue);
                LocalDate current;
                boolean malformed = false;
                try {
                    current = LocalDate.parse(row.getString("current_date"));
                } catch (DateTimeParseException | NullPointerException exception) {
                    current = start;
                    malformed = true;
                }
                LocalDate valid = current.isBefore(start) ? start
                        : current.isAfter(end) ? end : current;
                if (!malformed && valid.equals(current)) return false;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE careers SET current_date = ? WHERE id = ?")) {
                    update.setString(1, valid.toString()); update.setLong(2, careerId);
                    update.executeUpdate();
                }
                return true;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not inspect career integrity.", exception);
        }
    }

    private int integrityCount(Career career) {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM career_match_states WHERE career_id = ?) +
                  (SELECT COUNT(*) FROM career_player_team WHERE career_id = ? AND end_date IS NULL) +
                  (SELECT COUNT(*) FROM career_contracts WHERE career_id = ? AND active = 1) +
                  (SELECT COUNT(*) FROM career_player_state WHERE career_id = ?) +
                  (SELECT COUNT(*) FROM career_club_finances WHERE career_id = ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 5; index++) statement.setLong(index, career.getId());
            try (ResultSet row = statement.executeQuery()) { return row.next() ? row.getInt(1) : 0; }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not count career integrity records.", exception);
        }
    }
}
