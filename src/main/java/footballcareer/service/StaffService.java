package footballcareer.service;

import footballcareer.database.CareerContext;
import footballcareer.database.Database;
import footballcareer.model.Career;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

public class StaffService {
    public enum Role { COACH, PHYSIO, ANALYST }
    public record Staff(Role role, String name, int level, LocalDate hiredDate) {}

    public static double hiringCost(int level) {
        if (level < 1 || level > 5) throw new IllegalArgumentException("Level must be 1-5.");
        return level * 500_000.0;
    }

    public Staff hire(Career career, Role role, String name, int level) {
        if (career == null || CareerContext.getCareerId() == null
                || CareerContext.getCareerId() != career.getId()) {
            throw new IllegalStateException("This career must be active.");
        }
        if (role == null || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role and employee name are required.");
        }
        double cost = hiringCost(level);
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement budget = connection.prepareStatement("""
                        UPDATE career_club_finances
                        SET transfer_budget = transfer_budget - ?, balance = balance - ?
                        WHERE career_id = ? AND team_id = ? AND transfer_budget >= ?
                        """)) {
                    bind(budget, cost, cost, career.getId(),
                            career.getControlledTeam().getId(), cost);
                    if (budget.executeUpdate() != 1) throw new IllegalStateException(
                            "Insufficient budget to hire this employee.");
                }
                try (PreparedStatement employee = connection.prepareStatement("""
                        INSERT INTO career_staff (career_id, role, name, level, hired_date)
                        VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT(career_id, role) DO UPDATE SET name = excluded.name,
                            level = excluded.level, hired_date = excluded.hired_date
                        """)) {
                    bind(employee, career.getId(), role.name(), name.trim(), level,
                            career.getCurrentDate().toString());
                    employee.executeUpdate();
                }
                connection.commit();
                return new Staff(role, name.trim(), level, career.getCurrentDate());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback(); throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not hire technical staff.", exception);
        }
    }

    public Map<Role, Staff> findAll(long careerId) {
        Map<Role, Staff> staff = new EnumMap<>(Role.class);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM career_staff WHERE career_id = ?")) {
            statement.setLong(1, careerId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Role role = Role.valueOf(rows.getString("role"));
                    staff.put(role, new Staff(role, rows.getString("name"),
                            rows.getInt("level"), LocalDate.parse(rows.getString("hired_date"))));
                }
            }
            return staff;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load technical staff.", exception);
        }
    }

    public int level(long careerId, Role role) {
        Staff employee = findAll(careerId).get(role);
        return employee == null ? 0 : employee.level();
    }

    private void bind(PreparedStatement statement, Object... values) throws SQLException {
        for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
    }
}
