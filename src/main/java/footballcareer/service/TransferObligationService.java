package footballcareer.service;

import footballcareer.database.CareerContext;
import footballcareer.database.Database;

import java.sql.*;
import java.time.LocalDate;

public class TransferObligationService {
    public int process(LocalDate date) {
        Long careerId = CareerContext.getCareerId();
        String scope = careerId == null ? "career_id IS NULL" : "career_id = " + careerId;
        String sql = "SELECT * FROM transfer_obligations WHERE paid=0 AND " + scope
                + " AND ((condition_type='DATE' AND due_date<=?) OR "
                + "(condition_type='APPEARANCES' AND ? >= condition_value))";
        int paid = 0;
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, date.toString()); statement.setInt(2, Integer.MAX_VALUE);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        if ("APPEARANCES".equals(rows.getString("condition_type"))
                                && appearances(connection, careerId, rows.getLong("player_id"))
                                < rows.getInt("condition_value")) continue;
                        settle(connection, careerId, rows); paid++;
                    }
                }
                connection.commit(); return paid;
            } catch (Exception exception) { connection.rollback(); throw exception; }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not process transfer obligations.", exception);
        }
    }

    private int appearances(Connection connection, Long careerId, long playerId) throws SQLException {
        String table = careerId == null ? "player_season_stats" : "career_player_season_stats";
        String scope = careerId == null ? "" : " AND career_id=" + careerId;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(appearances),0) FROM " + table + " WHERE player_id=?" + scope)) {
            statement.setLong(1, playerId);
            try (ResultSet row = statement.executeQuery()) { return row.next() ? row.getInt(1) : 0; }
        }
    }

    private void settle(Connection connection, Long careerId, ResultSet row) throws SQLException {
        String finances = careerId == null ? "club_finances" : "career_club_finances";
        String scope = careerId == null ? "" : " AND career_id=" + careerId;
        double amount = row.getDouble("amount");
        update(connection, "UPDATE " + finances + " SET balance=balance-? WHERE team_id=?" + scope,
                amount, row.getLong("debtor_team_id"));
        String credit = "APPEARANCES".equals(row.getString("condition_type"))
                ? "transfer_budget=transfer_budget+?, balance=balance+?" : "balance=balance+?";
        if ("APPEARANCES".equals(row.getString("condition_type"))) update(connection,
                "UPDATE " + finances + " SET " + credit + " WHERE team_id=?" + scope,
                amount, amount, row.getLong("creditor_team_id"));
        else update(connection, "UPDATE " + finances + " SET " + credit + " WHERE team_id=?" + scope,
                amount, row.getLong("creditor_team_id"));
        update(connection, "UPDATE transfer_obligations SET paid=1 WHERE id=?", row.getLong("id"));
    }

    private void update(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("Missing club finances.");
        }
    }
}
