package footballcareer.service;

import footballcareer.database.CareerContext;
import footballcareer.database.Database;
import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerTeamRepository;
import footballcareer.model.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Executes career-scoped loans and restores players to their parent club. */
public class LoanService {
    public record LoanQuote(double marketValue, double requiredFee, int months) {}

    private final TransferWindowService windowService = new TransferWindowService();

    public LoanQuote quote(long playerId, int months) {
        if (months != 6 && months != 12) {
            throw new IllegalArgumentException("Loan duration must be 6 or 12 months.");
        }
        Player player = new PlayerRepository().findById(playerId);
        if (player == null) throw new IllegalArgumentException("Player does not exist.");
        double fee = player.getMarketValue() * 0.12 * months / 12.0;
        return new LoanQuote(player.getMarketValue(), fee, months);
    }

    public long requestLoan(long playerId, long borrowingTeamId, double fee,
            int months, LocalDate date) {
        Long careerId = CareerContext.getCareerId();
        if (careerId == null) throw new IllegalStateException("An active career is required.");
        windowService.requireOpen(date);
        LoanQuote quote = quote(playerId, months);
        if (fee < quote.requiredFee() * 0.80) {
            throw new IllegalStateException("The parent club rejected the loan fee.");
        }
        Long parentTeamId = new PlayerTeamRepository().findCurrentTeamId(playerId);
        if (parentTeamId == null) throw new IllegalStateException("Player has no current club.");
        if (parentTeamId == borrowingTeamId) {
            throw new IllegalArgumentException("A club cannot loan its own player.");
        }

        LocalDate endDate = date.plusMonths(months);
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                requireBudget(connection, careerId, borrowingTeamId, fee);
                updateExactlyOne(connection, """
                        UPDATE career_player_team SET end_date = ?
                        WHERE career_id = ? AND player_id = ? AND team_id = ?
                          AND end_date IS NULL
                        """, "Player no longer belongs to the parent club.", date.toString(),
                        careerId, playerId, parentTeamId);
                update(connection, """
                        INSERT INTO career_player_team
                            (career_id, player_id, team_id, start_date)
                        VALUES (?, ?, ?, ?)
                        """, careerId, playerId, borrowingTeamId, date.toString());
                updateExactlyOne(connection, """
                        UPDATE career_club_finances
                        SET transfer_budget = transfer_budget - ?, balance = balance - ?
                        WHERE career_id = ? AND team_id = ? AND transfer_budget >= ?
                        """, "Borrowing club cannot afford the loan.", fee, fee,
                        careerId, borrowingTeamId, fee);
                updateExactlyOne(connection, """
                        UPDATE career_club_finances
                        SET transfer_budget = transfer_budget + ?, balance = balance + ?
                        WHERE career_id = ? AND team_id = ?
                        """, "Parent club finances do not exist.", fee, fee,
                        careerId, parentTeamId);
                long loanId;
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO career_loans
                            (career_id, player_id, parent_team_id, borrowing_team_id,
                             fee, start_date, end_date, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                        """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    bind(statement, careerId, playerId, parentTeamId, borrowingTeamId,
                            fee, date.toString(), endDate.toString());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Missing loan id.");
                        loanId = keys.getLong(1);
                    }
                }
                connection.commit();
                return loanId;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not complete loan.", exception);
        }
    }

    public int processReturns(LocalDate currentDate) {
        Long careerId = CareerContext.getCareerId();
        if (careerId == null) return 0;
        int returned = 0;
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement loans = connection.prepareStatement("""
                    SELECT id, player_id, parent_team_id, borrowing_team_id
                    FROM career_loans
                    WHERE career_id = ? AND status = 'ACTIVE' AND date(end_date) <= date(?)
                    """)) {
                loans.setLong(1, careerId);
                loans.setString(2, currentDate.toString());
                try (ResultSet rows = loans.executeQuery()) {
                    while (rows.next()) {
                        long id = rows.getLong("id");
                        long player = rows.getLong("player_id");
                        updateExactlyOne(connection, """
                                UPDATE career_player_team SET end_date = ?
                                WHERE career_id = ? AND player_id = ? AND team_id = ?
                                  AND end_date IS NULL
                                """, "Loaned player is not at the borrowing club.",
                                currentDate.toString(), careerId, player,
                                rows.getLong("borrowing_team_id"));
                        update(connection, """
                                INSERT INTO career_player_team
                                    (career_id, player_id, team_id, start_date)
                                VALUES (?, ?, ?, ?)
                                """, careerId, player, rows.getLong("parent_team_id"),
                                currentDate.toString());
                        updateExactlyOne(connection, """
                                UPDATE career_loans SET status = 'RETURNED'
                                WHERE id = ? AND career_id = ? AND status = 'ACTIVE'
                                """, "Loan is no longer active.", id, careerId);
                        returned++;
                    }
                }
                connection.commit();
                return returned;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not return loaned players.", exception);
        }
    }

    private void requireBudget(Connection connection, long careerId, long teamId, double fee)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM career_club_finances
                WHERE career_id = ? AND team_id = ? AND transfer_budget >= ?
                """)) {
            bind(statement, careerId, teamId, fee);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalStateException(
                        "Borrowing club cannot afford the loan.");
            }
        }
    }

    private void update(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            statement.executeUpdate();
        }
    }

    private void updateExactlyOne(Connection connection, String sql, String message,
            Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            if (statement.executeUpdate() != 1) throw new IllegalStateException(message);
        }
    }

    private void bind(PreparedStatement statement, Object... values) throws SQLException {
        for (int index = 0; index < values.length; index++) {
            statement.setObject(index + 1, values[index]);
        }
    }
}
