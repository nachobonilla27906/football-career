package footballcareer.service;

import footballcareer.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class TransferExecutionService {
    private final TransferWindowService transferWindowService;

    public TransferExecutionService() {
        this(new TransferWindowService());
    }

    TransferExecutionService(TransferWindowService transferWindowService) {
        this.transferWindowService = transferWindowService;
    }

    public void completeTransfer(long offerId, double newSalary,
            LocalDate contractEndDate, long seasonId, LocalDate transferDate) {
        if (newSalary < 0) throw new IllegalArgumentException("Salary cannot be negative.");
        if (!contractEndDate.isAfter(transferDate)) {
            throw new IllegalArgumentException("Contract must end after the transfer date.");
        }
        transferWindowService.requireOpen(transferDate);

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                OfferData offer = loadAcceptedOffer(connection, offerId);
                verifyPlayerStillBelongsToSeller(connection, offer);
                verifyBuyerCanAffordTransfer(connection, offer, newSalary);
                double oldSalary = findActiveSalary(connection, offer.playerId(), offer.sellerId());

                update(connection, """
                        UPDATE contracts SET active = 0, end_date = ?
                        WHERE player_id = ? AND team_id = ? AND active = 1
                        """, transferDate.toString(), offer.playerId(), offer.sellerId());
                update(connection, """
                        UPDATE player_team SET end_date = ?
                        WHERE player_id = ? AND team_id = ? AND end_date IS NULL
                        """, transferDate.toString(), offer.playerId(), offer.sellerId());
                update(connection, """
                        INSERT INTO player_team (player_id, team_id, start_date)
                        VALUES (?, ?, ?)
                        """, offer.playerId(), offer.buyerId(), transferDate.toString());
                update(connection, """
                        INSERT INTO contracts
                            (player_id, team_id, start_date, end_date, salary, active)
                        VALUES (?, ?, ?, ?, ?, 1)
                        """, offer.playerId(), offer.buyerId(), transferDate.toString(),
                        contractEndDate.toString(), newSalary);

                updateExactlyOne(connection, """
                        UPDATE club_finances
                        SET transfer_budget = transfer_budget - ?, balance = balance - ?,
                            current_wage_spend = current_wage_spend + ?
                        WHERE team_id = ? AND transfer_budget >= ?
                          AND wage_budget - current_wage_spend >= ?
                        """, "Buyer can no longer afford the transfer.", offer.amount(),
                        offer.amount(), newSalary, offer.buyerId(), offer.amount(), newSalary);
                updateExactlyOne(connection, """
                        UPDATE club_finances
                        SET transfer_budget = transfer_budget + ?, balance = balance + ?,
                            current_wage_spend = MAX(0, current_wage_spend - ?)
                        WHERE team_id = ?
                        """, "Seller finances do not exist.", offer.amount(), offer.amount(),
                        oldSalary, offer.sellerId());

                update(connection, """
                        INSERT INTO player_market_status (player_id, status, asking_price)
                        VALUES (?, 'NOT_LISTED', NULL)
                        ON CONFLICT(player_id) DO UPDATE
                        SET status = 'NOT_LISTED', asking_price = NULL
                        """, offer.playerId());
                updateExactlyOne(connection, """
                        UPDATE transfer_offers SET status = 'COMPLETED'
                        WHERE id = ? AND status = 'ACCEPTED'
                        """, "Offer is no longer accepted.", offerId);
                update(connection, """
                        INSERT INTO transfers
                            (player_id, from_team_id, to_team_id, amount,
                             transfer_date, season_id, offer_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, offer.playerId(), offer.sellerId(), offer.buyerId(), offer.amount(),
                        transferDate.toString(), seasonId, offerId);

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not complete transfer.", e);
        }
    }

    private OfferData loadAcceptedOffer(Connection connection, long offerId) throws SQLException {
        String sql = """
                SELECT player_id, buying_team_id, selling_team_id, amount
                FROM transfer_offers WHERE id = ? AND status = 'ACCEPTED'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, offerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Offer is not accepted.");
                return new OfferData(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getDouble(4));
            }
        }
    }

    private void verifyPlayerStillBelongsToSeller(Connection connection, OfferData offer)
            throws SQLException {
        String sql = "SELECT 1 FROM player_team WHERE player_id = ? AND team_id = ? AND end_date IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, offer.playerId());
            statement.setLong(2, offer.sellerId());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Player no longer belongs to seller.");
            }
        }
    }

    private void verifyBuyerCanAffordTransfer(Connection connection, OfferData offer, double salary)
            throws SQLException {
        String sql = """
                SELECT 1 FROM club_finances
                WHERE team_id = ? AND transfer_budget >= ?
                  AND wage_budget - current_wage_spend >= ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, offer.buyerId());
            statement.setDouble(2, offer.amount());
            statement.setDouble(3, salary);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Buyer cannot afford transfer and salary.");
            }
        }
    }

    private double findActiveSalary(Connection connection, long playerId, long teamId)
            throws SQLException {
        String sql = "SELECT salary FROM contracts WHERE player_id = ? AND team_id = ? AND active = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            statement.setLong(2, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Player has no active contract with seller.");
                return rs.getDouble(1);
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
        for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
    }

    private record OfferData(long playerId, long buyerId, long sellerId, double amount) {}
}
