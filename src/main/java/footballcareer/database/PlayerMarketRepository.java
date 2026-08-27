package footballcareer.database;

import footballcareer.model.Player;
import footballcareer.model.enums.TransferStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerMarketRepository {
    private final PlayerRepository playerRepository = new PlayerRepository();

    public void initializeMissingStatuses() {
        String sql = """
                INSERT OR IGNORE INTO player_market_status (player_id)
                SELECT id FROM players
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize market statuses.", e);
        }
    }

    public void listForTransfer(long playerId, double askingPrice) {
        if (askingPrice <= 0) {
            throw new IllegalArgumentException("Asking price must be positive.");
        }
        updateStatus(playerId, TransferStatus.TRANSFER_LISTED, askingPrice);
    }

    public void removeFromTransferList(long playerId) {
        updateStatus(playerId, TransferStatus.NOT_LISTED, null);
    }

    public List<Player> findTransferListed(long excludedTeamId) {
        String sql = """
                SELECT pms.player_id
                FROM player_market_status pms
                JOIN player_team pt ON pms.player_id = pt.player_id
                WHERE pms.status = 'TRANSFER_LISTED'
                  AND pt.end_date IS NULL
                  AND pt.team_id <> ?
                ORDER BY pms.asking_price, pms.player_id
                """;
        List<Player> players = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, excludedTeamId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    players.add(playerRepository.findById(rs.getLong(1)));
                }
            }
            return players;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find transfer-listed players.", e);
        }
    }

    public Double findAskingPrice(long playerId) {
        String sql = "SELECT asking_price FROM player_market_status WHERE player_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || rs.getObject(1) == null) return null;
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find asking price.", e);
        }
    }

    private void updateStatus(long playerId, TransferStatus status, Double price) {
        String sql = """
                UPDATE player_market_status SET status = ?, asking_price = ?
                WHERE player_id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            if (price == null) statement.setNull(2, Types.REAL);
            else statement.setDouble(2, price);
            statement.setLong(3, playerId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Player market status does not exist.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not update market status.", e);
        }
    }
}
