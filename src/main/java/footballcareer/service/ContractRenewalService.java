package footballcareer.service;

import footballcareer.database.Database;
import footballcareer.database.CareerContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class ContractRenewalService {

    public void renew(long playerId, long teamId, LocalDate newEndDate, double newSalary) {
        if (newEndDate == null || newSalary <= 0) {
            throw new IllegalArgumentException("La fecha y el salario deben ser válidos.");
        }
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ContractData current = currentContract(connection, playerId, teamId);
                if (!newEndDate.isAfter(current.endDate())) {
                    throw new IllegalArgumentException("La nueva fecha debe ampliar el contrato actual.");
                }
                double difference = newSalary - current.salary();
                if (difference > availableWageBudget(connection, teamId)) {
                    throw new IllegalStateException("No hay margen salarial suficiente.");
                }
                updateContract(connection, current.id(), newEndDate, newSalary);
                updatePlayerSalary(connection, playerId, newSalary);
                updateWageSpend(connection, teamId, difference);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("No se pudo renovar el contrato.", exception);
        }
    }

    private ContractData currentContract(Connection connection, long playerId, long teamId)
            throws SQLException {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "contracts" : "career_contracts";
        String sql = """
                SELECT id, end_date, salary FROM %s
                WHERE player_id = ? AND team_id = ? AND active = 1 %s
                """.formatted(table, careerId == null ? "" : "AND career_id = " + careerId);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            statement.setLong(2, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new IllegalArgumentException(
                        "El jugador no tiene un contrato activo con tu club.");
                return new ContractData(resultSet.getLong("id"),
                        LocalDate.parse(resultSet.getString("end_date")),
                        resultSet.getDouble("salary"));
            }
        }
    }

    private double availableWageBudget(Connection connection, long teamId) throws SQLException {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "club_finances" : "career_club_finances";
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT wage_budget - current_wage_spend AS available
                FROM %s WHERE team_id = ? %s
                """.formatted(table, careerId == null ? "" : "AND career_id = " + careerId))) {
            statement.setLong(1, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new IllegalStateException("No existen finanzas del club.");
                return resultSet.getDouble("available");
            }
        }
    }

    private void updateContract(Connection connection, long contractId, LocalDate endDate,
            double salary) throws SQLException {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "contracts" : "career_contracts";
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + table + " SET end_date = ?, salary = ? WHERE id = ?"
                        + (careerId == null ? "" : " AND career_id = " + careerId))) {
            statement.setString(1, endDate.toString());
            statement.setDouble(2, salary);
            statement.setLong(3, contractId);
            statement.executeUpdate();
        }
    }

    private void updatePlayerSalary(Connection connection, long playerId, double salary)
            throws SQLException {
        if (CareerContext.getCareerId() != null) return;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE players SET salary = ? WHERE id = ?")) {
            statement.setDouble(1, salary);
            statement.setLong(2, playerId);
            statement.executeUpdate();
        }
    }

    private void updateWageSpend(Connection connection, long teamId, double difference)
            throws SQLException {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "club_finances" : "career_club_finances";
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE %s SET current_wage_spend = current_wage_spend + ?
                WHERE team_id = ? %s
                """.formatted(table, careerId == null ? "" : "AND career_id = " + careerId))) {
            statement.setDouble(1, difference);
            statement.setLong(2, teamId);
            statement.executeUpdate();
        }
    }

    private record ContractData(long id, LocalDate endDate, double salary) {}
}
